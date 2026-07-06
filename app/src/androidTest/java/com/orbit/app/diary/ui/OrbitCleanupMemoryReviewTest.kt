package com.orbit.app.diary.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.orbit.app.action.ipc.ActionExecuteRequestParcel
import com.orbit.app.action.ipc.ActionExecuteResultParcel
import com.orbit.app.ai.EmbeddingResult
import com.orbit.app.ai.LlmProvider
import com.orbit.app.ai.model.ActionExtractionResult
import com.orbit.app.ai.model.AppFunctionSummary
import com.orbit.app.ai.model.DayHeaderResult
import com.orbit.app.ai.model.IntentClassification
import com.orbit.app.ai.model.LlmProvenance
import com.orbit.app.ai.model.SensitivityResult
import com.orbit.app.ai.model.SummaryResult
import com.orbit.app.data.ClusterCardModel
import com.orbit.app.data.entity.StateSnapshot
import com.orbit.app.data.ipc.ActionProposalParcel
import com.orbit.app.data.ipc.DayPageParcel
import com.orbit.app.data.ipc.EnvelopeViewParcel
import com.orbit.app.data.ipc.MemoryCandidateParcel
import com.orbit.app.data.ipc.MemoryDecisionResultParcel
import com.orbit.app.diary.DayHeaderGenerator
import com.orbit.app.diary.DiaryRepository
import com.orbit.app.diary.DiaryViewModel
import com.orbit.app.diary.ThreadGrouper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OrbitCleanupMemoryReviewTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
    }

    @Test
    fun emptyMemoryCandidatesDoNotRenderReviewNoise() {
        val repo = FakeDiaryRepository()

        render(repo)

        composeRule.onAllNodesWithText("Memory review").assertCountEquals(0)
    }

    @Test
    fun memoryCandidateCardRendersEvidenceAndInvokesDirectActions() {
        val repo = FakeDiaryRepository(
            initialCandidates = listOf(candidate())
        )
        val opened = mutableListOf<String>()

        render(repo, onOpenCapture = { opened += it })

        composeRule.onNodeWithTag(DiaryScreenTestTags.MEMORY_REVIEW_PANEL).assertIsDisplayed()
        composeRule.onNodeWithTag(DiaryScreenTestTags.memoryCandidateCard("candidate-1")).assertIsDisplayed()
        composeRule.onNodeWithText("Interested in founder events").assertIsDisplayed()
        composeRule.onNodeWithText("user interested in founder events").assertIsDisplayed()
        composeRule.onNodeWithText("Remember this pattern?").assertIsDisplayed()
        composeRule.onNodeWithText("Founder office hours capture").assertIsDisplayed()

        composeRule.onNodeWithTag(DiaryScreenTestTags.memoryCandidateOpenCapture("candidate-1")).performClick()
        assertEquals(listOf("env-1"), opened)

        composeRule.onNodeWithTag(DiaryScreenTestTags.memoryCandidateReject("candidate-1")).performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { repo.rejectedMemoryCalls.isNotEmpty() }
        assertEquals(listOf("candidate-1" to "user_rejected"), repo.rejectedMemoryCalls)

        composeRule.onNodeWithTag(DiaryScreenTestTags.memoryCandidateAccept("candidate-1")).performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { repo.acceptedMemoryCalls.isNotEmpty() }
        assertEquals(listOf(Triple("candidate-1", null, null)), repo.acceptedMemoryCalls)
    }

    @Test
    fun editDialogValidatesAndPassesEditedMemoryText() {
        val repo = FakeDiaryRepository(
            initialCandidates = listOf(candidate())
        )

        render(repo)

        composeRule.onNodeWithTag(DiaryScreenTestTags.memoryCandidateEdit("candidate-1")).performClick()
        composeRule.onNodeWithTag(DiaryScreenTestTags.MEMORY_CANDIDATE_EDIT_DIALOG).assertIsDisplayed()

        composeRule.onNodeWithTag(DiaryScreenTestTags.MEMORY_CANDIDATE_EDIT_FACT).performTextClearance()
        composeRule.onNodeWithTag(DiaryScreenTestTags.MEMORY_CANDIDATE_EDIT_SAVE).assertIsNotEnabled()

        composeRule.onNodeWithTag(DiaryScreenTestTags.MEMORY_CANDIDATE_EDIT_LABEL).performTextClearance()
        composeRule.onNodeWithTag(DiaryScreenTestTags.MEMORY_CANDIDATE_EDIT_LABEL)
            .performTextInput("Tracks founder office hours")
        composeRule.onNodeWithTag(DiaryScreenTestTags.MEMORY_CANDIDATE_EDIT_FACT)
            .performTextInput("user follows founder office hour events")
        composeRule.onNodeWithTag(DiaryScreenTestTags.MEMORY_CANDIDATE_EDIT_SAVE).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { repo.acceptedMemoryCalls.isNotEmpty() }
        assertEquals(
            listOf(
                Triple(
                    "candidate-1",
                    "Tracks founder office hours",
                    "user follows founder office hour events"
                )
            ),
            repo.acceptedMemoryCalls,
        )
    }

    private fun render(
        repo: FakeDiaryRepository,
        onOpenCapture: (String) -> Unit = {},
    ) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            .also { scopes += it }
        val viewModel = DiaryViewModel(
            repository = repo,
            threadGrouper = ThreadGrouper(),
            dayHeaderGenerator = DayHeaderGenerator(NoopLlmProvider()),
            scopeOverride = scope,
        )

        composeRule.setContent {
            MaterialTheme {
                OrbitCleanupScreen(
                    viewModel = viewModel,
                    onOpenCapture = onOpenCapture,
                )
            }
        }
    }

    private class FakeDiaryRepository(
        initialCandidates: List<MemoryCandidateParcel> = emptyList(),
    ) : DiaryRepository {
        private val candidates = MutableStateFlow(initialCandidates)
        val acceptedMemoryCalls = mutableListOf<Triple<String, String?, String?>>()
        val rejectedMemoryCalls = mutableListOf<Pair<String, String?>>()

        override fun observeDay(isoDate: String): Flow<DayPageParcel> =
            flowOf(DayPageParcel(isoDate, emptyList()))

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
        override fun observeClusters(): Flow<List<ClusterCardModel>> = flowOf(emptyList())
        override fun observeMemoryCandidates(limit: Int): Flow<List<MemoryCandidateParcel>> = candidates

        override suspend fun acceptMemoryCandidate(
            candidateId: String,
            editedLabel: String?,
            editedFactText: String?
        ): MemoryDecisionResultParcel {
            acceptedMemoryCalls += Triple(candidateId, editedLabel, editedFactText)
            return MemoryDecisionResultParcel(
                ok = true,
                candidateId = candidateId,
                memoryId = "memory-$candidateId",
                status = "promoted",
                message = "Saved to Orbit memory."
            )
        }

        override suspend fun rejectMemoryCandidate(
            candidateId: String,
            reason: String?
        ): MemoryDecisionResultParcel {
            rejectedMemoryCalls += candidateId to reason
            return MemoryDecisionResultParcel(
                ok = true,
                candidateId = candidateId,
                memoryId = null,
                status = "rejected",
                message = "Dismissed. Orbit will not remember that."
            )
        }
    }

    private class NoopLlmProvider : LlmProvider {
        override suspend fun classifyIntent(text: String, appCategory: String): IntentClassification =
            error("unused")

        override suspend fun summarize(text: String, maxTokens: Int): SummaryResult = error("unused")
        override suspend fun scanSensitivity(text: String): SensitivityResult = error("unused")

        override suspend fun generateDayHeader(
            dayIsoDate: String,
            envelopeSummaries: List<String>
        ): DayHeaderResult = DayHeaderResult("", "en", LlmProvenance.LocalNano)

        override suspend fun extractActions(
            text: String,
            contentType: String,
            state: StateSnapshot,
            registeredFunctions: List<AppFunctionSummary>,
            maxCandidates: Int
        ): ActionExtractionResult = error("unused")

        override suspend fun embed(text: String): EmbeddingResult? = null
    }

    private fun candidate() = MemoryCandidateParcel(
        candidateId = "candidate-1",
        candidateKind = "INTEREST",
        state = "PENDING",
        displayLabel = "Interested in founder events",
        factText = "user interested in founder events",
        confidenceLabel = "medium",
        sensitivity = "NORMAL",
        sourceCount = 1,
        primarySourceEnvelopeId = "env-1",
        primarySourceTitle = "Founder office hours capture",
        primarySourceDayLocal = "2026-06-05",
        askUserCopy = "Remember this pattern?",
        createdAtMillis = 1_780_000_000_000L,
        updatedAtMillis = 1_780_000_000_000L,
    )
}
