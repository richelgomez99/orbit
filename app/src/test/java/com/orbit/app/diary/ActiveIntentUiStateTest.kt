package com.orbit.app.diary

import com.orbit.app.data.ipc.ActiveIntentParcel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveIntentUiStateTest {

    @Test
    fun groupsOnlyActionableActiveItemsByLifecycleAndCategory() {
        val state = ActiveIntentUiState.from(
            listOf(
                parcel(
                    id = "needs-context",
                    category = "CHAT_ACTION",
                    completion = "MISSING",
                    evidence = """{"kind":"CATEGORY","label":"Reply to Chelsea","source":"chat_action_text","excerpt":"Can you reply to Chelsea?"}"""
                ),
                parcel(
                    id = "buy-later",
                    category = "BUY_LATER_PRODUCT",
                    completion = "FOUND",
                    evidence = """{"kind":"COMPLETION_KEY","label":"PRICE","source":"local_regex","excerpt":"Trail shoes $129"}"""
                ),
                parcel(
                    id = "maybe-old",
                    category = "MAYBE_OLD_OR_INACTIVE",
                    completion = "MISSING",
                    evidence = """{"label":"Old hotel tab","source":"timestamp"}"""
                ),
                parcel(
                    id = "unknown",
                    category = "UNKNOWN",
                    completion = "NEEDS_ESCALATION",
                    evidence = """{"kind":"APP_CONTEXT","label":"source_app_label","source":"local"}"""
                ),
                parcel(
                    id = "resolved",
                    category = "RECIPE",
                    status = "RESOLVED"
                )
            )
        ) as ActiveIntentUiState.Ready

        assertEquals(1, state.activeCount)
        assertEquals(1, state.missingContextCount)
        assertEquals("Needs your input · Message follow-up", state.groups[0].title)
        assertEquals("Reply to Chelsea", state.groups[0].items.single().evidenceLabel)
        assertEquals("Message text", state.groups[0].items.single().evidenceSource)
        assertEquals(
            "Open the capture, add who needs a reply if it matters, or mark it handled.",
            state.groups[0].items.single().guidanceLabel
        )
    }

    @Test
    fun eventTicketCopyUsesPlainGuidance() {
        val state = ActiveIntentUiState.from(
            listOf(
                parcel(
                    id = "event",
                    category = "EVENT_TICKET_RESERVATION",
                    completion = "FOUND",
                    evidence = """{"kind":"COMPLETION_KEY","label":"DATE","source":"local_regex","excerpt":"Concert ticket confirmed. May 20, 2026."}"""
                )
            )
        ) as ActiveIntentUiState.Ready

        val item = state.groups.single().items.single()
        assertEquals("Event or reservation", item.evidenceLabel)
        assertEquals("Local text", item.evidenceSource)
        assertEquals("This looks like event, ticket, reservation, or booking information.", item.reasonLabel)
        assertEquals("Orbit found event or reservation details in this capture.", item.whyLabel)
        assertEquals("Save the details, attend it, or clear it if you do not need it.", item.guidanceLabel)
        assertEquals("Saved or attended", item.resolveActionLabel)
        assertEquals("Not needed", item.archiveActionLabel)
        assertEquals(false, item.needsEscalation)
        assertEquals("ACTIVE", item.lifecycleStatus)
    }

    @Test
    fun appContextEvidenceDoesNotLeakInternalKeys() {
        val state = ActiveIntentUiState.from(
            listOf(
                parcel(
                    id = "event",
                    category = "EVENT_TICKET_RESERVATION",
                    completion = "FOUND",
                    evidence = """{"label":"source_app_label,app_category","source":"local"}"""
                )
            )
        ) as ActiveIntentUiState.Ready

        assertEquals("Event or reservation", state.groups.single().title)
        assertEquals("Event or reservation", state.groups.single().items.single().evidenceLabel)
        assertEquals("Local signals", state.groups.single().items.single().evidenceSource)
        assertEquals(true, state.groups.single().items.single().sourceLabel.startsWith("Local signals"))
    }

    @Test
    fun compactEvidenceExcerptIsAvailableForContext() {
        val state = ActiveIntentUiState.from(
            listOf(
                parcel(
                    id = "message",
                    category = "CHAT_ACTION",
                    evidence = """{"kind":"CATEGORY","label":"CHAT_ACTION","source":"messaging_source","excerpt":"Chelsea has a scheduled appointment at 2pm"}"""
                )
            )
        ) as ActiveIntentUiState.Ready

        val item = state.groups.single().items.single()
        assertEquals("Message follow-up", item.evidenceLabel)
        assertEquals("Messages", item.evidenceSource)
        assertEquals("Category clue", item.evidenceKind)
        assertEquals("Chelsea has a scheduled appointment at 2pm", item.clueLabel)
        assertEquals("This came from a message-like source and may need a reply.", item.reasonLabel)
    }

    @Test
    fun completionKeyEvidenceDoesNotBecomeTheCardTitle() {
        val state = ActiveIntentUiState.from(
            listOf(
                parcel(
                    id = "message-date",
                    category = "CHAT_ACTION",
                    evidence = """{"kind":"COMPLETION_KEY","label":"DATE","source":"local_regex","excerpt":"Chelsea has a scheduled appointment at 2pm"}"""
                )
            )
        ) as ActiveIntentUiState.Ready

        val item = state.groups.single().items.single()
        assertEquals("Message follow-up", item.evidenceLabel)
        assertEquals("Found date", item.evidenceKind)
        assertEquals("Local text", item.sourceLabel.substringBefore(" · "))
        assertEquals("This looks like a message and Orbit found a date or time in the text.", item.reasonLabel)
    }

    @Test
    fun orbitReviewEvidenceShowsDecisionBriefAndReason() {
        val state = ActiveIntentUiState.from(
            listOf(
                parcel(
                    id = "reviewed-message",
                    category = "CHAT_ACTION",
                    evidence = """{"kind":"ORBIT_REVIEW","label":"CHAT_ACTION","source":"orbit_review","reason":"Orbit thinks this is a message follow-up. Decide whether you replied, still need to reply, or can clear it.","excerpt":"Chelsea has a scheduled appointment at 2pm"}"""
                )
            )
        ) as ActiveIntentUiState.Ready

        val item = state.groups.single().items.single()
        assertEquals("Message follow-up", item.evidenceLabel)
        assertEquals("Orbit review", item.evidenceSource)
        assertEquals("Decision brief", item.evidenceKind)
        assertEquals("Refresh review", item.askOrbitActionLabel)
        assertEquals("Chelsea has a scheduled appointment at 2pm", item.clueLabel)
        assertEquals(
            "Orbit thinks this is a message follow-up. Decide whether you replied, still need to reply, or can clear it.",
            item.reasonLabel
        )
    }

    @Test
    fun emptyWhenOnlyTerminalRowsRemain() {
        val state = ActiveIntentUiState.from(
            listOf(parcel(id = "done", category = "RECIPE", status = "RESOLVED"))
        )

        assertTrue(state is ActiveIntentUiState.Empty)
    }

    @Test
    fun emptyWhenOnlyWeakUnknownRowsRemain() {
        val state = ActiveIntentUiState.from(
            listOf(
                parcel(
                    id = "unknown",
                    category = "UNKNOWN",
                    completion = "NEEDS_ESCALATION",
                    evidence = """{"kind":"APP_CONTEXT","label":"source_app_label","source":"local"}"""
                )
            )
        )

        assertTrue(state is ActiveIntentUiState.Empty)
    }

    @Test
    fun collapsesDuplicateFollowUpsByContentKeepingLatest() {
        val state = ActiveIntentUiState.from(
            listOf(
                parcel(
                    id = "coupon-old",
                    category = "COUPON_OR_PROMO",
                    completion = "FOUND",
                    evidence = """{"kind":"COMPLETION_KEY","label":"DATE","source":"local_regex","excerpt":"Headphones expires Monday. Print label or drop off before 5pm."}""",
                    updatedAtMillis = 1_700_000_000_000L
                ),
                parcel(
                    id = "coupon-latest",
                    category = "COUPON_OR_PROMO",
                    completion = "FOUND",
                    evidence = """{"kind":"COMPLETION_KEY","label":"DATE","source":"local_regex","excerpt":"Headphones expires Monday.   Print label or drop off before 5pm."}""",
                    updatedAtMillis = 1_700_000_060_000L
                ),
                parcel(
                    id = "coupon-truncated",
                    category = "COUPON_OR_PROMO",
                    completion = "FOUND",
                    evidence = """{"kind":"COMPLETION_KEY","label":"DATE","source":"local_regex","excerpt":"r headphones expires Monday. Print label or drop off before 5pm."}""",
                    updatedAtMillis = 1_700_000_030_000L
                )
            )
        ) as ActiveIntentUiState.Ready

        assertEquals(1, state.activeCount)
        assertEquals("coupon-latest", state.groups.single().items.single().intentId)
    }

    private fun parcel(
        id: String,
        category: String,
        status: String = "ACTIVE",
        completion: String = "FOUND",
        evidence: String = """{"label":"Item","source":"foreground_app"}""",
        updatedAtMillis: Long = 1_700_000_000_000L
    ) = ActiveIntentParcel(
        intentId = id,
        captureId = "capture-$id",
        intentType = category,
        status = status,
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
        updatedAtMillis = updatedAtMillis
    )
}
