package com.orbit.app.diary.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.orbit.app.data.ipc.ActiveIntentParcel
import com.orbit.app.diary.ActiveIntentItem
import com.orbit.app.diary.ActiveIntentUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ActiveIntentCleanupPanelTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun rendersActiveGroupingAndMaybeOldGrouping() {
        render(
            state = ActiveIntentUiState.from(
                listOf(
                    parcel(
                        id = "intent-context",
                        category = "BUY_LATER_PRODUCT",
                        completion = "NEEDS_ESCALATION",
                        evidence = """{"label":"Trail shoes","source":"local_regex"}"""
                    ),
                    parcel(
                        id = "intent-old",
                        category = "MAYBE_OLD_OR_INACTIVE",
                        completion = "MISSING",
                        evidence = """{"label":"Old flight search","source":"timestamp"}"""
                    )
                )
            )
        )

        composeRule.onNodeWithTag(DiaryScreenTestTags.ACTIVE_INTENT_PANEL).assertIsDisplayed()
        composeRule.onNodeWithText("Needs your input · Buy later").assertIsDisplayed()
        composeRule.onNodeWithText("Maybe old · Maybe old").assertIsDisplayed()
        composeRule.onNodeWithText("Trail shoes").assertIsDisplayed()
        composeRule.onNodeWithText("Old flight search").assertIsDisplayed()
    }

    @Test
    fun collapseAndFiltersKeepFollowUpQueueScannable() {
        render(
            state = ActiveIntentUiState.from(
                listOf(
                    parcel(
                        id = "intent-context",
                        category = "BUY_LATER_PRODUCT",
                        completion = "NEEDS_ESCALATION",
                        evidence = """{"label":"Trail shoes","source":"local_regex"}"""
                    ),
                    parcel(
                        id = "intent-ready",
                        category = "CHAT_ACTION",
                        completion = "FOUND",
                        evidence = """{"label":"Reply to Chelsea","source":"messaging_source"}"""
                    ),
                    parcel(
                        id = "intent-old",
                        category = "MAYBE_OLD_OR_INACTIVE",
                        completion = "MISSING",
                        evidence = """{"label":"Old flight search","source":"timestamp"}"""
                    )
                )
            )
        )

        composeRule.onNodeWithText("All 3").assertIsDisplayed()
        composeRule.onNodeWithText("Needs context 1").assertIsDisplayed()
        composeRule.onNodeWithText("Ready 1").assertIsDisplayed()
        composeRule.onNodeWithText("Maybe old 1").assertIsDisplayed()

        composeRule.onNodeWithTag(DiaryScreenTestTags.ACTIVE_INTENT_TOGGLE).performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Trail shoes").assertCountEquals(0)
        composeRule.onNodeWithTag(DiaryScreenTestTags.ACTIVE_INTENT_TOGGLE).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentFilter("NeedsContext")).performClick()
        composeRule.onNodeWithText("Trail shoes").assertIsDisplayed()
        composeRule.onAllNodesWithText("Reply to Chelsea").assertCountEquals(0)
        composeRule.onAllNodesWithText("Old flight search").assertCountEquals(0)

        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentFilter("MaybeOld")).performClick()
        composeRule.onNodeWithText("Old flight search").assertIsDisplayed()
        composeRule.onAllNodesWithText("Trail shoes").assertCountEquals(0)
    }

    @Test
    fun actionButtonsInvokeCallbacks() {
        val calls = mutableListOf<String>()
        render(
            state = ActiveIntentUiState.from(
                listOf(parcel(id = "intent-context", completion = "NEEDS_ESCALATION"))
            ),
            onResolve = { calls += "resolve:${it.intentId}" },
            onArchive = { calls += "archive:${it.intentId}" },
            onOpenCapture = { calls += "open:${it.intentId}" },
            onAddContext = { calls += "context:${it.intentId}" },
            onEscalate = { calls += "escalate:${it.intentId}" }
        )

        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentOpenCapture("intent-context")).performClick()
        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentAddContext("intent-context")).performClick()
        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentResolve("intent-context")).performClick()
        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentArchive("intent-context")).performClick()
        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentEscalate("intent-context")).performClick()
        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentDecisionReview("intent-context")).performClick()

        assertEquals(
            listOf(
                "open:intent-context",
                "context:intent-context",
                "resolve:intent-context",
                "archive:intent-context",
                "escalate:intent-context"
            ),
            calls
        )
    }

    @Test
    fun askOrbitOpensDecisionDialogWithEvidenceAndChoices() {
        val calls = mutableListOf<String>()
        render(
            state = ActiveIntentUiState.from(
                listOf(
                    parcel(
                        id = "intent-review",
                        category = "CHAT_ACTION",
                        evidence = """{"kind":"CATEGORY","label":"CHAT_ACTION","source":"messaging_source","excerpt":"Chelsea has a scheduled appointment at 2pm"}"""
                    )
                )
            ),
            onResolve = { calls += "resolve:${it.intentId}" },
            onArchive = { calls += "archive:${it.intentId}" },
            onEscalate = { calls += "review:${it.intentId}" }
        )

        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentEscalate("intent-review")).performClick()

        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentDecisionDialog("intent-review")).assertIsDisplayed()
        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentDecisionOpenCapture("intent-review")).assertIsDisplayed()
        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentDecisionAddContext("intent-review")).assertIsDisplayed()
        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentDecisionReview("intent-review")).assertIsDisplayed()
        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentDecisionResolve("intent-review")).assertIsDisplayed()
        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentDecisionArchive("intent-review")).assertIsDisplayed()

        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentDecisionResolve("intent-review")).performClick()

        assertEquals(listOf("resolve:intent-review"), calls)
    }

    @Test
    fun labelsExplainWhyItAppearsAndWhatActionsMean() {
        render(
            state = ActiveIntentUiState.from(
                listOf(
                    parcel(
                        id = "event",
                        category = "EVENT_TICKET_RESERVATION",
                        evidence = """{"label":"source_app_label,app_category","source":"local"}"""
                    )
                )
            )
        )

        composeRule.onNodeWithText("1 capture may need a reply, plan, to-do, or clear decision").assertIsDisplayed()
        composeRule.onNodeWithText("From: Local signals", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Why it appears: this looks like event, ticket, reservation, or booking information.").assertIsDisplayed()
        composeRule.onNodeWithText("Save the details, attend it, or clear it if you do not need it.").assertIsDisplayed()
        composeRule.onNodeWithText("View capture").assertIsDisplayed()
        composeRule.onNodeWithText("Saved or attended").assertIsDisplayed()
        composeRule.onNodeWithText("Not needed").assertIsDisplayed()
        composeRule.onNodeWithText("Ask Orbit").assertIsDisplayed()
    }

    private fun render(
        state: ActiveIntentUiState,
        onResolve: (ActiveIntentItem) -> Unit = {},
        onArchive: (ActiveIntentItem) -> Unit = {},
        onOpenCapture: (ActiveIntentItem) -> Unit = {},
        onAddContext: (ActiveIntentItem) -> Unit = {},
        onEscalate: (ActiveIntentItem) -> Unit = {}
    ) {
        composeRule.setContent {
            MaterialTheme {
                ActiveIntentCleanupPanel(
                    state = state,
                    onResolve = onResolve,
                    onArchive = onArchive,
                    onOpenCapture = onOpenCapture,
                    onAddContext = onAddContext,
                    onEscalate = onEscalate
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun parcel(
        id: String,
        category: String = "BUY_LATER_PRODUCT",
        completion: String = "FOUND",
        evidence: String = """{"label":"Item","source":"foreground_app"}"""
    ) = ActiveIntentParcel(
        intentId = id,
        captureId = "capture-$id",
        intentType = category,
        status = "ACTIVE",
        completionKeyJson = null,
        completionKeyStatus = completion,
        primaryEvidenceJson = evidence,
        primaryAction = null,
        dueAtMillis = null,
        expiresAtMillis = null,
        resolutionReason = null,
        resolvedAtMillis = null,
        userConfirmed = false,
        createdAtMillis = 1_700_000_000_000L,
        updatedAtMillis = 1_700_000_000_000L
    )
}
