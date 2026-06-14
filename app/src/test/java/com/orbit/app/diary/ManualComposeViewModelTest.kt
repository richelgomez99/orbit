package com.orbit.app.diary

import com.orbit.app.action.ipc.ActionExecuteRequestParcel
import com.orbit.app.action.ipc.ActionExecuteResultParcel
import com.orbit.app.data.ipc.ActionProposalParcel
import com.orbit.app.data.ipc.DayPageParcel
import com.orbit.app.data.ipc.EnvelopeViewParcel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ManualComposeViewModelTest {

    @Test
    fun blankBodyShowsErrorWithoutRepositoryCall() = runTest {
        val repo = FakeRepo()
        val vm = ManualComposeViewModel(repo, dayLocal = "2026-06-13", scopeOverride = TestScope(testScheduler))

        vm.onBodyChanged("   ")
        vm.save()

        assertEquals("Add something to save.", vm.state.value.error)
        assertEquals(0, repo.manualCalls.size)
    }

    @Test
    fun successfulSaveUpdatesResult() = runTest {
        val repo = FakeRepo(result = ManualComposeResult.Saved("env-1", contextAttached = true))
        val vm = ManualComposeViewModel(repo, dayLocal = "2026-06-13", scopeOverride = TestScope(testScheduler))

        vm.onBodyChanged("Remember this")
        vm.onContextChanged("For Monday")
        vm.save()
        advanceUntilIdle()

        assertFalse(vm.state.value.saving)
        assertEquals(ManualComposeResult.Saved("env-1", contextAttached = true), vm.state.value.result)
        assertEquals(ManualCall("Remember this", "For Monday", "2026-06-13", null), repo.manualCalls.single())
    }

    @Test
    fun duplicateResultSurfacesExistingEnvelope() = runTest {
        val repo = FakeRepo(
            result = ManualComposeResult.AlreadySaved(
                existingEnvelopeId = "existing-1",
                matchedBy = "EXACT_TEXT",
            )
        )
        val vm = ManualComposeViewModel(repo, dayLocal = "2026-06-13", scopeOverride = TestScope(testScheduler))

        vm.onBodyChanged("Same note")
        vm.save()
        advanceUntilIdle()

        assertEquals(
            ManualComposeResult.AlreadySaved("existing-1", "EXACT_TEXT"),
            vm.state.value.result,
        )
        assertEquals(null, vm.state.value.error)
    }

    @Test
    fun blockedResultShowsUserCopy() = runTest {
        val repo = FakeRepo(result = ManualComposeResult.Blocked("manual_compose_unavailable"))
        val vm = ManualComposeViewModel(repo, dayLocal = "2026-06-13", scopeOverride = TestScope(testScheduler))

        vm.onBodyChanged("Save me")
        vm.save()
        advanceUntilIdle()

        assertEquals("Manual compose is not available yet.", vm.state.value.error)
    }

    private data class ManualCall(
        val bodyText: String,
        val contextText: String?,
        val dayLocal: String,
        val intentName: String?,
    )

    private class FakeRepo(
        private val result: ManualComposeResult = ManualComposeResult.Saved("env-default", false),
    ) : DiaryRepository {
        val manualCalls = mutableListOf<ManualCall>()

        override fun observeDay(isoDate: String): Flow<DayPageParcel> = flowOf()
        override suspend fun reassignIntent(envelopeId: String, newIntentName: String, reason: String?) = Unit
        override suspend fun archive(envelopeId: String) = Unit
        override suspend fun delete(envelopeId: String) = Unit
        override suspend fun retryHydration(envelopeId: String) = Unit
        override suspend fun getEnvelope(envelopeId: String): EnvelopeViewParcel = error("unused")
        override suspend fun distinctDayLocalsWithContent(limit: Int, offset: Int): List<String> = emptyList()
        override fun observeProposals(envelopeId: String): Flow<List<ActionProposalParcel>> = flowOf(emptyList())
        override suspend fun markProposalConfirmed(proposalId: String): Boolean = false
        override suspend fun markProposalDismissed(proposalId: String): Boolean = false
        override suspend fun executeAction(request: ActionExecuteRequestParcel): ActionExecuteResultParcel = error("unused")
        override suspend fun cancelWithinUndoWindow(executionId: String): Boolean = false
        override suspend fun setTodoItemDone(envelopeId: String, itemIndex: Int, done: Boolean) = Unit

        override suspend fun createManualTextCapture(
            bodyText: String,
            contextText: String?,
            dayLocal: String,
            intentName: String?,
        ): ManualComposeResult {
            manualCalls += ManualCall(bodyText, contextText, dayLocal, intentName)
            return result
        }
    }
}
