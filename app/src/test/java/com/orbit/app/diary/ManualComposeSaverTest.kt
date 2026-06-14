package com.orbit.app.diary

import com.orbit.app.data.ipc.IntentEnvelopeDraftParcel
import com.orbit.app.data.ipc.SealResultParcel
import com.orbit.app.data.ipc.StateSnapshotParcel
import com.orbit.app.data.model.Intent
import com.orbit.app.data.model.IntentSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class ManualComposeSaverTest {

    @Test
    fun blankBodyBlocksBeforeSeal() = runTest {
        val fake = FakeGateway()
        val result = fake.saver().save(
            ManualComposeDraft(
                bodyText = "   ",
                contextText = "context",
                dayLocal = "2026-06-13",
            )
        )

        assertEquals(ManualComposeResult.Blocked("blank_body"), result)
        assertEquals(0, fake.seals.size)
        assertEquals(0, fake.notes.size)
    }

    @Test
    fun createdEnvelopeAttachesContextAfterSeal() = runTest {
        val fake = FakeGateway(sealResult = SealResultParcel.created("env-1"))
        val result = fake.saver().save(
            ManualComposeDraft(
                bodyText = "Remember to compare passport renewal options",
                contextText = "Need this for travel planning",
                dayLocal = "2026-06-13",
                intentName = Intent.REFERENCE.name,
            )
        )

        assertEquals(ManualComposeResult.Saved("env-1", contextAttached = true), result)
        assertEquals("env-1" to "Need this for travel planning", fake.notes.single())
        val sealed = fake.seals.single().first
        assertEquals("Remember to compare passport renewal options", sealed.textContent)
        assertEquals(Intent.REFERENCE.name, sealed.intent)
        assertEquals(IntentSource.USER_CHIP.name, sealed.intentSource)
        assertEquals("Orbit", fake.seals.single().second.sourceAppLabel)
    }

    @Test
    fun blankContextDoesNotCreateNote() = runTest {
        val fake = FakeGateway(sealResult = SealResultParcel.created("env-2"))
        val result = fake.saver().save(
            ManualComposeDraft(
                bodyText = "Manual thought",
                contextText = "   ",
                dayLocal = "2026-06-13",
            )
        )

        assertEquals(ManualComposeResult.Saved("env-2", contextAttached = false), result)
        assertEquals(0, fake.notes.size)
        assertEquals(Intent.AMBIGUOUS.name, fake.seals.single().first.intent)
        assertEquals(IntentSource.FALLBACK.name, fake.seals.single().first.intentSource)
    }

    @Test
    fun duplicateManualComposeDoesNotAttachContextAutomatically() = runTest {
        val fake = FakeGateway(
            sealResult = SealResultParcel.alreadySaved(
                existingEnvelopeId = "existing-1",
                matchedBy = SealResultParcel.MATCHED_BY_EXACT_TEXT,
            )
        )
        val result = fake.saver().save(
            ManualComposeDraft(
                bodyText = "same exact note",
                contextText = "new context for existing",
                dayLocal = "2026-06-13",
            )
        )

        assertEquals(
            ManualComposeResult.AlreadySaved(
                existingEnvelopeId = "existing-1",
                matchedBy = SealResultParcel.MATCHED_BY_EXACT_TEXT,
            ),
            result,
        )
        assertEquals(1, fake.seals.size)
        assertEquals(0, fake.notes.size)
    }

    @Test
    fun sealFailureReturnsBlocked() = runTest {
        val fake = FakeGateway(sealFailure = IllegalStateException("repo unavailable"))
        val result = fake.saver().save(
            ManualComposeDraft(
                bodyText = "Manual thought",
                contextText = "context",
                dayLocal = "2026-06-13",
            )
        )

        assertEquals(ManualComposeResult.Blocked("repo unavailable"), result)
        assertEquals(0, fake.notes.size)
    }

    private class FakeGateway(
        private val sealResult: SealResultParcel = SealResultParcel.created("env-default"),
        private val sealFailure: Throwable? = null,
    ) {
        val seals = mutableListOf<Pair<IntentEnvelopeDraftParcel, StateSnapshotParcel>>()
        val notes = mutableListOf<Pair<String, String>>()

        fun saver() = ManualComposeSaver(
            sealWithResult = { draft, state ->
                seals += draft to state
                sealFailure?.let { throw it }
                sealResult
            },
            createOrUpdateLatestNote = { envelopeId, text ->
                notes += envelopeId to text
                true
            },
            clock = { 1_780_000_000_000L },
            zoneId = { ZoneId.of("America/New_York") },
        )
    }
}
