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
    fun rendersActionableGroupingAndDropsWeakCleanupNoise() {
        render(
            state = ActiveIntentUiState.from(
                listOf(
                    parcel(
                        id = "intent-context",
                        category = "CHAT_ACTION",
                        completion = "MISSING",
                        evidence = """{"kind":"CATEGORY","label":"Reply to Chelsea","source":"chat_action_text","excerpt":"Can you reply to Chelsea?"}"""
                    ),
                    parcel(
                        id = "intent-old",
                        category = "MAYBE_OLD_OR_INACTIVE",
                        completion = "MISSING",
                        evidence = """{"label":"Old flight search","source":"timestamp"}"""
                    ),
                    parcel(
                        id = "intent-unknown",
                        category = "UNKNOWN",
                        completion = "NEEDS_ESCALATION",
                        evidence = """{"kind":"APP_CONTEXT","label":"source_app_label","source":"local"}"""
                    )
                )
            ),
            initiallyExpanded = true
        )

        composeRule.onNodeWithTag(DiaryScreenTestTags.ACTIVE_INTENT_PANEL).assertIsDisplayed()
        composeRule.onNodeWithText("Needs your input · Message follow-up").assertIsDisplayed()
        composeRule.onNodeWithText("Reply to Chelsea").assertIsDisplayed()
        composeRule.onAllNodesWithText("Old flight search").assertCountEquals(0)
    }

    @Test
    fun collapseAndFiltersKeepFollowUpQueueScannable() {
        render(
            state = ActiveIntentUiState.from(
                listOf(
                    parcel(
                        id = "intent-context",
                        category = "CHAT_ACTION",
                        completion = "MISSING",
                        evidence = """{"kind":"CATEGORY","label":"Reply to Chelsea","source":"chat_action_text","excerpt":"Can you reply to Chelsea?"}"""
                    ),
                    parcel(
                        id = "intent-ready",
                        category = "EVENT_TICKET_RESERVATION",
                        completion = "FOUND",
                        evidence = """{"kind":"COMPLETION_KEY","label":"DATE","source":"local_regex","excerpt":"Concert ticket confirmed. May 20, 2026."}"""
                    ),
                    parcel(
                        id = "intent-old",
                        category = "MAYBE_OLD_OR_INACTIVE",
                        completion = "MISSING",
                        evidence = """{"label":"Old flight search","source":"timestamp"}"""
                    )
                )
            ),
            initiallyExpanded = true
        )

        composeRule.onNodeWithText("All 2").assertIsDisplayed()
        composeRule.onNodeWithText("Needs context 1").assertIsDisplayed()
        composeRule.onNodeWithText("Ready 1").assertIsDisplayed()

        composeRule.onNodeWithTag(DiaryScreenTestTags.ACTIVE_INTENT_TOGGLE).performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Trail shoes").assertCountEquals(0)
        composeRule.onNodeWithTag(DiaryScreenTestTags.ACTIVE_INTENT_TOGGLE).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentFilter("NeedsContext")).performClick()
        composeRule.onNodeWithText("Reply to Chelsea").assertIsDisplayed()
        composeRule.onAllNodesWithText("Event or reservation").assertCountEquals(0)
        composeRule.onAllNodesWithText("Old flight search").assertCountEquals(0)
    }

    @Test
    fun actionButtonsInvokeCallbacks() {
        val calls = mutableListOf<String>()
        render(
            state = ActiveIntentUiState.from(
                listOf(
                    parcel(
                        id = "intent-context",
                        category = "CHAT_ACTION",
                        completion = "MISSING",
                        evidence = """{"kind":"CATEGORY","label":"Reply to Chelsea","source":"chat_action_text","excerpt":"Can you reply to Chelsea?"}"""
                    )
                )
            ),
            initiallyExpanded = true,
            onResolve = { calls += "resolve:${it.intentId}" },
            onArchive = { calls += "archive:${it.intentId}" },
            onOpenCapture = { calls += "open:${it.intentId}" },
            onAddContext = { calls += "context:${it.intentId}" },
            onNotNow = { calls += "not-now:${it.intentId}" },
            onSnooze = { calls += "snooze:${it.intentId}" },
            onEscalate = { calls += "escalate:${it.intentId}" }
        )

        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentOpenCapture("intent-context")).performClick()
        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentAddContext("intent-context")).performClick()
        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentResolve("intent-context")).performClick()
        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentArchive("intent-context")).performClick()
        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentNotNow("intent-context")).performClick()
        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentSnooze("intent-context")).performClick()
        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentEscalate("intent-context")).performClick()
        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentDecisionReview("intent-context")).performClick()

        assertEquals(
            listOf(
                "open:intent-context",
                "context:intent-context",
                "resolve:intent-context",
                "archive:intent-context",
                "not-now:intent-context",
                "snooze:intent-context",
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
            initiallyExpanded = true,
            onResolve = { calls += "resolve:${it.intentId}" },
            onArchive = { calls += "archive:${it.intentId}" },
            onNotNow = { calls += "not-now:${it.intentId}" },
            onSnooze = { calls += "snooze:${it.intentId}" },
            onEscalate = { calls += "review:${it.intentId}" }
        )

        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentEscalate("intent-review")).performClick()

        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentDecisionDialog("intent-review")).assertIsDisplayed()
        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentDecisionOpenCapture("intent-review")).assertIsDisplayed()
        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentDecisionAddContext("intent-review")).assertIsDisplayed()
        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentDecisionReview("intent-review")).assertIsDisplayed()
        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentDecisionNotNow("intent-review")).assertIsDisplayed()
        composeRule.onNodeWithTag(DiaryScreenTestTags.activeIntentDecisionSnooze("intent-review")).assertIsDisplayed()
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
                        evidence = """{"kind":"COMPLETION_KEY","label":"DATE","source":"local_regex","excerpt":"Concert ticket confirmed. May 20, 2026."}"""
                    )
                )
            ),
            initiallyExpanded = true
        )

        composeRule.onNodeWithText("1 saved moment looks actionable.").assertIsDisplayed()
        composeRule.onNodeWithText("From: Local text", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Why it appears: this looks like event, ticket, reservation, or booking information.").assertIsDisplayed()
        composeRule.onNodeWithText("Save the details, attend it, or clear it if you do not need it.").assertIsDisplayed()
        composeRule.onNodeWithText("View capture").assertIsDisplayed()
        composeRule.onNodeWithText("Saved or attended").assertIsDisplayed()
        composeRule.onNodeWithText("Not needed").assertIsDisplayed()
        composeRule.onNodeWithText("Ask Orbit").assertIsDisplayed()
    }

    @Test
    fun defaultStateIsCollapsedAndExpandedStateCapsPreview() {
        val rows = (1..8).map { index ->
            parcel(
                id = "intent-$index",
                category = "EVENT_TICKET_RESERVATION",
                completion = "FOUND",
                evidence = """{"kind":"COMPLETION_KEY","label":"DATE","source":"local_regex","excerpt":"Capture $index on May 20, 2026."}"""
            )
        }
        render(state = ActiveIntentUiState.from(rows))

        composeRule.onNodeWithText("Follow-ups").assertIsDisplayed()
        composeRule.onNodeWithText("Review").assertIsDisplayed()
        composeRule.onAllNodesWithText("Capture 1", substring = true).assertCountEquals(0)

        composeRule.onNodeWithTag(DiaryScreenTestTags.ACTIVE_INTENT_TOGGLE).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Collapse").assertIsDisplayed()
        composeRule.onNodeWithText("Showing 6 of 8. Use filters or Library search to narrow this down.").assertIsDisplayed()
        composeRule.onNodeWithText("Capture 1", substring = true).assertIsDisplayed()
        // Capture 6 renders below the fold on tall fixtures — the subject
        // here is the 6-item cap, not viewport visibility.
        composeRule.onNodeWithText("Capture 6", substring = true).assertExists()
        composeRule.onAllNodesWithText("Capture 7", substring = true).assertCountEquals(0)
    }

    @Test
    fun duplicateFollowUpsCollapseToOneVisibleRow() {
        render(
            state = ActiveIntentUiState.from(
                listOf(
                    parcel(
                        id = "coupon-1",
                        category = "COUPON_OR_PROMO",
                        completion = "FOUND",
                        evidence = """{"kind":"COMPLETION_KEY","label":"DATE","source":"local_regex","excerpt":"Headphones expires Monday. Print label or drop off before 5pm."}"""
                    ),
                    parcel(
                        id = "coupon-2",
                        category = "COUPON_OR_PROMO",
                        completion = "FOUND",
                        evidence = """{"kind":"COMPLETION_KEY","label":"DATE","source":"local_regex","excerpt":"Headphones expires Monday.   Print label or drop off before 5pm."}"""
                    ),
                    parcel(
                        id = "coupon-3",
                        category = "COUPON_OR_PROMO",
                        completion = "FOUND",
                        evidence = """{"kind":"COMPLETION_KEY","label":"DATE","source":"local_regex","excerpt":"r headphones expires Monday. Print label or drop off before 5pm."}"""
                    )
                )
            ),
            initiallyExpanded = true,
        )

        composeRule.onNodeWithText("All 1").assertIsDisplayed()
        composeRule.onAllNodesWithText("Coupon or promo").assertCountEquals(2)
    }

    private fun render(
        state: ActiveIntentUiState,
        initiallyExpanded: Boolean = false,
        onResolve: (ActiveIntentItem) -> Unit = {},
        onArchive: (ActiveIntentItem) -> Unit = {},
        onOpenCapture: (ActiveIntentItem) -> Unit = {},
        onAddContext: (ActiveIntentItem) -> Unit = {},
        onNotNow: (ActiveIntentItem) -> Unit = {},
        onSnooze: (ActiveIntentItem) -> Unit = {},
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
                    onNotNow = onNotNow,
                    onSnooze = onSnooze,
                    onEscalate = onEscalate,
                    initiallyExpanded = initiallyExpanded,
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
