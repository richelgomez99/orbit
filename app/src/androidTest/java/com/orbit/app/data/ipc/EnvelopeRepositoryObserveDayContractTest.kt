package com.orbit.app.data.ipc

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T044 — Contract test for `observeDay` per contracts/envelope-repository-contract.md §4.
 *
 * Exercises the same [EnvelopeRepositoryImpl] used in-process (same pattern as the
 * other repository contract tests — see class docstring of
 * `RepositoryContractTestBase` for the rationale: the impl IS the
 * `IEnvelopeRepository.Stub`, so driving it directly is semantically equivalent
 * to an AIDL bind while avoiding instrumented-service flake).
 *
 * Guarantees under test:
 *   (a) Initial emission within 1 s (SC-004 P50 ≤ 1 s safety margin).
 *   (b) Re-emission within 500 ms after an external `seal()` on the same day
 *       (US2 AS#2 "new captures appear without refresh").
 *   (c) Initial emission for an empty day is a `DayPageParcel` with zero
 *       envelopes (not a missing callback / not an error).
 */
@RunWith(AndroidJUnit4::class)
class EnvelopeRepositoryObserveDayContractTest : RepositoryContractTestBase() {

    private fun today(): String {
        // Must match the day_local that seal() computes from the pinned
        // FakeClock epoch (2023-11-14) — using the wall-clock date means
        // sealed fixtures land on a different day page than the one under
        // observation, and the re-emission never contains them.
        val zone = ZoneId.of("UTC")
        return java.time.Instant.ofEpochMilli(clock.now)
            .atZone(zone)
            .toLocalDate()
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    private class RecordingObserver(capacity: Int = 8) : IEnvelopeObserver.Stub() {
        val queue = ArrayBlockingQueue<DayPageParcel>(capacity)
        override fun onDayLoaded(page: DayPageParcel) {
            queue.offer(page)
        }
        fun poll(timeoutMs: Long): DayPageParcel? =
            queue.poll(timeoutMs, TimeUnit.MILLISECONDS)
    }

    @Test
    fun observeDay_emitsInitialPageWithinOneSecond_forEmptyDay() = runTest {
        val iso = today()
        val observer = RecordingObserver()

        val start = System.nanoTime()
        repository.observeDay(iso, observer)
        val page = observer.poll(1_000L)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000

        assertNotNull("initial DayPageParcel must arrive within 1000ms", page)
        assertEquals(iso, page!!.isoDate)
        assertEquals(0, page.envelopes.size)
        assertTrue("observed $elapsedMs ms, must be ≤ 1000", elapsedMs <= 1_000)

        repository.stopObserving(observer)
    }

    @Test
    fun observeDay_reEmitsWithinFiveHundredMs_afterExternalSeal() = runTest {
        val iso = today()
        val observer = RecordingObserver()

        repository.observeDay(iso, observer)
        // Drain the initial emission so the timing measurement below only
        // covers the post-seal re-emission.
        val initial = observer.poll(1_000L)
        assertNotNull("initial emission expected before seal", initial)

        // The 500ms budget covers the RE-EMISSION after seal, not the seal
        // itself — start the clock once seal returns, otherwise the seal's
        // own write+audit latency (variable on a loaded device) is unfairly
        // charged against the observation contract.
        val envelopeId = repository.seal(draftText("seeded-by-T044"), stateUtc())
        val start = System.nanoTime()
        var reemit = observer.poll(500L)
        // Room may deliver an emission that was already queued before the
        // seal committed (stale snapshot). Keep polling within the SAME
        // 500ms budget until the post-seal page arrives — the contract is
        // unchanged: the envelope must be observable within 500ms of seal.
        while (
            reemit != null &&
            reemit.envelopes.none { it.id == envelopeId } &&
            (System.nanoTime() - start) < 500_000_000L
        ) {
            val remainingMs = 500L - (System.nanoTime() - start) / 1_000_000L
            reemit = observer.poll(remainingMs.coerceAtLeast(1L))
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000

        assertNotNull("re-emission must arrive within 500ms of seal", reemit)
        assertEquals(iso, reemit!!.isoDate)
        assertTrue(
            "seeded envelope $envelopeId must appear in re-emission",
            reemit.envelopes.any { it.id == envelopeId }
        )
        assertTrue("re-emission took $elapsedMs ms, must be ≤ 500", elapsedMs <= 500)

        repository.stopObserving(observer)
    }

    @Test
    fun stopObserving_stopsFurtherEmissions() = runTest {
        val iso = today()
        val observer = RecordingObserver()

        repository.observeDay(iso, observer)
        assertNotNull(observer.poll(1_000L))

        repository.stopObserving(observer)
        // Drain any late emissions that were already in flight before stop.
        while (observer.poll(100L) != null) { /* drain */ }

        repository.seal(draftText("post-stop"), stateUtc())
        val afterStop = observer.poll(500L)
        assertEquals("no further emissions after stopObserving", null, afterStop)
    }

    @Test
    fun dayPageParcel_respectsParcelSizeBudget() = runTest {
        // contracts/envelope-repository-contract.md §7: ≤ 256 KB per day page.
        // Seed 50 envelopes (well under any realistic day) and assert the
        // marshalled parcel stays inside budget. This guards the `[text]`
        // field size post-scrub on today-scale days.
        val iso = today()
        repeat(50) { repository.seal(draftText("entry-$it"), stateUtc()) }

        val observer = RecordingObserver(capacity = 4)
        repository.observeDay(iso, observer)
        val page = observer.poll(1_000L)
        assertNotNull(page)

        val parcel = android.os.Parcel.obtain()
        try {
            page!!.writeToParcel(parcel, 0)
            val sizeBytes = parcel.dataSize()
            assertTrue(
                "day page marshalled size was $sizeBytes bytes, must be ≤ 256 KB",
                sizeBytes <= 256 * 1024
            )
        } finally {
            parcel.recycle()
        }

        repository.stopObserving(observer)
    }
}
