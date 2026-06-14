package com.orbit.app.diary.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.orbit.app.action.ipc.ActionExecuteRequestParcel
import com.orbit.app.action.ipc.ActionExecuteResultParcel
import com.orbit.app.data.ipc.ActionProposalParcel
import com.orbit.app.data.ipc.DayPageParcel
import com.orbit.app.data.ipc.EnvelopeViewParcel
import com.orbit.app.diary.DiaryRepository
import com.orbit.app.diary.ManualComposeResult
import com.orbit.app.diary.ManualComposeViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ManualComposeDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun blankBodyShowsInlineError() {
        val repo = FakeRepo()
        render(repo)

        composeRule.onNodeWithTag(ManualComposeTestTags.DIALOG).assertIsDisplayed()
        composeRule.onNodeWithText("Save to Orbit").assertIsDisplayed()
        composeRule.onNodeWithTag(ManualComposeTestTags.SAVE).performClick()

        composeRule.onNodeWithText("Add something to save.").assertIsDisplayed()
        assertEquals(0, repo.manualCalls.size)
    }

    @Test
    fun saveBodyAndContextOpensSavedCapture() {
        val repo = FakeRepo(ManualComposeResult.Saved("env-42", contextAttached = true))
        val opened = mutableListOf<String>()

        render(repo, onOpenCapture = { opened += it })
        composeRule.onNodeWithTag(ManualComposeTestTags.BODY).performTextInput("Pack passport")
        composeRule.onNodeWithTag(ManualComposeTestTags.CONTEXT).performTextInput("For Monday flight")
        composeRule.onNodeWithTag(ManualComposeTestTags.SAVE).performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) { opened.isNotEmpty() }

        assertEquals(listOf("env-42"), opened)
        assertEquals(
            listOf(ManualCall("Pack passport", "For Monday flight", "2026-06-13", null)),
            repo.manualCalls,
        )
    }

    private fun render(
        repo: FakeRepo,
        onOpenCapture: (String) -> Unit = {},
    ) {
        val viewModel = ManualComposeViewModel(repo, dayLocal = "2026-06-13")
        composeRule.setContent {
            MaterialTheme {
                ManualComposeDialog(
                    viewModel = viewModel,
                    onDismiss = {},
                    onOpenCapture = onOpenCapture,
                )
            }
        }
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
