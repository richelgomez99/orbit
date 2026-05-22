package com.orbit.app.capture

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.orbit.app.data.ipc.IEnvelopeObserver
import com.orbit.app.data.ipc.IEnvelopeRepository
import com.orbit.app.data.ipc.IntentEnvelopeDraftParcel
import com.orbit.app.data.ipc.SealResultParcel
import com.orbit.app.data.ipc.StateSnapshotParcel
import com.orbit.app.data.ipc.EnvelopeViewParcel
import com.orbit.app.data.model.ActivityState
import com.orbit.app.data.model.AppCategory
import com.orbit.app.data.model.ContentType
import com.orbit.app.data.model.Intent
import com.orbit.app.data.model.IntentSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * T071 (Phase 6 US4) — ScreenshotObserver contract test.
 *
 * Drives the observer with a deterministic [Hit] source (the production
 * path relies on MediaStore, which is noisy to prime in instrumented
 * tests) and asserts that:
 *   - [ScreenshotObserver.onChange] triggers a single `seal()` call on
 *     the bound [IEnvelopeRepository] with `contentType=IMAGE` and the
 *     correct content URI.
 *   - Re-firing `onChange` for the same media id is a no-op (idempotency
 *     against MediaStore's insert+metadata-update double-fire).
 *   - A newer media id triggers a second seal.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotObserverTest {

    @Test
    fun onChange_sealsImageEnvelopeWithContentUri() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val recorder = RecordingRepository()
        val collector = staticCollector()
        val observer = ScreenshotObserver.createForTest(
            contentResolver = context.contentResolver,
            repositoryProvider = { recorder },
            stateCollector = collector
        )
        val screenshotUri = Uri.parse("content://media/external/images/media/101")
        observer.hitSourceOverride = {
            ScreenshotObserver.Hit(mediaId = 101L, contentUri = screenshotUri)
        }

        observer.onChange(false, null)

        assertEquals(1, recorder.seals.size)
        val (draft, _) = recorder.seals.first()
        assertEquals(ContentType.IMAGE.name, draft.contentType)
        assertEquals(screenshotUri.toString(), draft.imageUri)
        assertNull(draft.textContent)
        assertEquals(Intent.AMBIGUOUS.name, draft.intent)
        assertEquals(IntentSource.AUTO_AMBIGUOUS.name, draft.intentSource)
    }

    @Test
    fun onChange_isIdempotentForSameMediaId() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val recorder = RecordingRepository()
        val observer = ScreenshotObserver.createForTest(
            contentResolver = context.contentResolver,
            repositoryProvider = { recorder },
            stateCollector = staticCollector()
        )
        observer.hitSourceOverride = {
            ScreenshotObserver.Hit(mediaId = 42L, contentUri = Uri.parse("content://media/external/images/media/42"))
        }

        observer.onChange(false, null)
        observer.onChange(false, null)
        observer.onChange(false, null)

        assertEquals(1, recorder.seals.size)
    }

    @Test
    fun onChange_sealsTwiceForDistinctMediaIds() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val recorder = RecordingRepository()
        val observer = ScreenshotObserver.createForTest(
            contentResolver = context.contentResolver,
            repositoryProvider = { recorder },
            stateCollector = staticCollector()
        )
        var currentId = 10L
        observer.hitSourceOverride = {
            ScreenshotObserver.Hit(
                mediaId = currentId,
                contentUri = Uri.parse("content://media/external/images/media/$currentId")
            )
        }

        observer.onChange(false, null)
        currentId = 11L
        observer.onChange(false, null)

        assertEquals(2, recorder.seals.size)
    }

    @Test
    fun onChange_retriesWhenRepositoryBindIsStillSettling() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val recorder = RecordingRepository(expectedSeals = 1)
        var providerCalls = 0
        val observer = ScreenshotObserver.createForTest(
            contentResolver = context.contentResolver,
            repositoryProvider = {
                providerCalls += 1
                if (providerCalls == 1) null else recorder
            },
            stateCollector = staticCollector(),
            handler = Handler(Looper.getMainLooper()),
            repoRetryDelayMillis = 10L,
            maxRepoBindRetries = 2
        )
        observer.hitSourceOverride = {
            ScreenshotObserver.Hit(mediaId = 88L, contentUri = Uri.parse("content://media/external/images/media/88"))
        }

        observer.onChange(false, null)

        assertTrue(recorder.awaitSeals())
        assertEquals(1, recorder.seals.size)
        assertEquals(2, providerCalls)
    }

    private fun staticCollector(): StateSnapshotCollector = StateSnapshotCollector(
        packageResolver = { _, _ -> StateSnapshotCollector.ForegroundApp(AppCategory.OTHER) },
        activityStateSource = { ActivityState.STILL }
    )

    private class RecordingRepository(expectedSeals: Int = 0) : IEnvelopeRepository.Stub() {
        val seals = CopyOnWriteArrayList<Pair<IntentEnvelopeDraftParcel, StateSnapshotParcel>>()
        private val latch = CountDownLatch(expectedSeals)

        fun awaitSeals(): Boolean = latch.await(1, TimeUnit.SECONDS)

        override fun seal(
            draft: IntentEnvelopeDraftParcel,
            state: StateSnapshotParcel
        ): String {
            seals.add(draft to state)
            latch.countDown()
            return "test-envelope-${seals.size}"
        }

        override fun sealWithResult(
            draft: IntentEnvelopeDraftParcel,
            state: StateSnapshotParcel
        ): SealResultParcel = SealResultParcel.created(seal(draft, state))

        // ---- Unused surface: every other method errors so mis-use is loud. ----
        override fun observeDay(isoDate: String, observer: IEnvelopeObserver) = error("unused")
        override fun stopObserving(observer: IEnvelopeObserver) = error("unused")
        override fun getEnvelope(envelopeId: String): EnvelopeViewParcel = error("unused")
        override fun getLatestNote(envelopeId: String): String? = error("unused")
        override fun createOrUpdateLatestNote(envelopeId: String, text: String): Boolean = error("unused")
        override fun reassignIntent(envelopeId: String, newIntentName: String, reasonOpt: String?) = error("unused")
        override fun archive(envelopeId: String) = error("unused")
        override fun delete(envelopeId: String) = error("unused")
        override fun undo(envelopeId: String): Boolean = error("unused")
        override fun restoreFromTrash(envelopeId: String) = error("unused")
        override fun listSoftDeletedWithinDays(days: Int): List<EnvelopeViewParcel> = error("unused")
        override fun countSoftDeletedWithinDays(days: Int): Int = error("unused")
        override fun hardDelete(envelopeId: String) = error("unused")
        override fun distinctDayLocalsWithContent(limit: Int, offset: Int): MutableList<String> = mutableListOf()
        override fun countAll(): Int = error("unused")
        override fun countArchived(): Int = error("unused")
        override fun countDeleted(): Int = error("unused")
        override fun existsPriorIntent(appCategory: String, intent: String): Boolean = error("unused")
        override fun completeUrlHydration(
            continuationId: String,
            envelopeId: String,
            canonicalUrl: String?,
            canonicalUrlHash: String?,
            ok: Boolean,
            title: String?,
            domain: String?,
            summary: String?,
            summaryModel: String?,
            failureReason: String?
        ) = error("unused")
        override fun retryHydration(envelopeId: String) = error("unused")
        override fun seedScreenshotHydrations(
            envelopeId: String,
            ocrText: String?,
            urls: Array<String>?
        ) = error("unused")

        // ---- Spec 003 v1.1 IPC additions (unused by capture-side tests). ----
        override fun lookupAppFunction(functionId: String): com.orbit.app.data.ipc.AppFunctionSummaryParcel? = null
        override fun listAppFunctions(appPackage: String): MutableList<com.orbit.app.data.ipc.AppFunctionSummaryParcel> = mutableListOf()
        override fun recordActionInvocation(
            executionId: String,
            proposalId: String,
            functionId: String,
            outcome: String,
            outcomeReason: String,
            dispatchedAtMillis: Long,
            completedAtMillis: Long,
            latencyMs: Long,
            episodeId: String?
        ) = error("unused")
        override fun markProposalConfirmed(proposalId: String): Boolean = error("unused")
        override fun markProposalDismissed(proposalId: String): Boolean = error("unused")
        override fun observeProposalsForEnvelope(
            envelopeId: String,
            observer: com.orbit.app.data.ipc.IActionProposalObserver
        ) = error("unused")
        override fun stopObservingProposals(observer: com.orbit.app.data.ipc.IActionProposalObserver) = error("unused")

        override fun observeClusters(observer: com.orbit.app.data.ipc.IClusterObserver) = error("unused")

        override fun stopObservingClusters(observer: com.orbit.app.data.ipc.IClusterObserver) = error("unused")
        override fun markClusterDismissed(clusterId: String?): Boolean = error("unused")
        override fun summarizeCluster(clusterId: String?): String = error("unused")
        override fun observeActiveIntents(observer: com.orbit.app.data.ipc.IActiveIntentObserver) = error("unused")
        override fun stopObservingActiveIntents(observer: com.orbit.app.data.ipc.IActiveIntentObserver) = error("unused")
        override fun resolveActiveIntent(
            intentId: String?,
            resolutionReason: String?,
            userConfirmed: Boolean
        ): Boolean = error("unused")
        override fun requestActiveIntentEscalation(intentId: String?, mode: String?): Boolean = error("unused")
        override fun extractActionsForEnvelope(envelopeId: String): String = error("unused")
        override fun createDerivedTodoEnvelope(
            parentEnvelopeId: String,
            itemsJson: String,
            proposalId: String
        ): MutableList<String> = error("unused")
        override fun setTodoItemDone(envelopeId: String, itemIndex: Int, done: Boolean) = error("unused")
        override fun runWeeklyDigest(targetDayLocal: String): String = error("unused")
    }
}
