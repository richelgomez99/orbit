package com.orbit.app.diary

import com.orbit.app.data.ipc.ActiveIntentParcel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveIntentUiStateTest {

    @Test
    fun groupsActiveItemsByLifecycleAndCategory() {
        val state = ActiveIntentUiState.from(
            listOf(
                parcel(
                    id = "needs-context",
                    category = "BUY_LATER_PRODUCT",
                    completion = "NEEDS_ESCALATION",
                    evidence = """{"label":"Boots","source":"local_regex"}"""
                ),
                parcel(
                    id = "maybe-old",
                    category = "MAYBE_OLD_OR_INACTIVE",
                    completion = "MISSING",
                    evidence = """{"label":"Old hotel tab","source":"timestamp"}"""
                ),
                parcel(
                    id = "resolved",
                    category = "RECIPE",
                    status = "RESOLVED"
                )
            )
        ) as ActiveIntentUiState.Ready

        assertEquals(2, state.activeCount)
        assertEquals(1, state.missingContextCount)
        assertEquals("Needs your input · Buy later", state.groups[0].title)
        assertEquals("Maybe old · Maybe old", state.groups[1].title)
        assertEquals("Boots", state.groups[0].items.single().evidenceLabel)
        assertEquals("Local text", state.groups[0].items.single().evidenceSource)
        assertEquals(
            "Open the capture, add the missing context, or clear it.",
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
                    completion = "MISSING",
                    evidence = """{"label":"EVENT_TICKET_RESERVATION","source":"event_ticket_text"}"""
                )
            )
        ) as ActiveIntentUiState.Ready

        val item = state.groups.single().items.single()
        assertEquals("Event or reservation", item.evidenceLabel)
        assertEquals("Event text", item.evidenceSource)
        assertEquals("Why it appears: this looks like event, ticket, reservation, or booking information.", item.reasonLabel)
        assertEquals("Orbit found event or reservation details in this capture.", item.whyLabel)
        assertEquals("Save the details, attend it, or clear it if you do not need it.", item.guidanceLabel)
        assertEquals("Saved or attended", item.resolveActionLabel)
        assertEquals("Not needed", item.archiveActionLabel)
        assertEquals(true, item.needsEscalation)
        assertEquals("NEEDS_CONTEXT", item.lifecycleStatus)
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
        assertEquals(true, state.groups.single().items.single().sourceLabel.startsWith("From: Local signals"))
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
        assertEquals("Capture clue: Chelsea has a scheduled appointment at 2pm", item.clueLabel)
        assertEquals("Why it appears: this came from a message-like source and may need a reply.", item.reasonLabel)
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
        assertEquals("From: Local text", item.sourceLabel.substringBefore(" · Found date"))
        assertEquals("Why it appears: this looks like a message and Orbit found a date or time in the text.", item.reasonLabel)
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
        assertEquals("Capture clue: Chelsea has a scheduled appointment at 2pm", item.clueLabel)
        assertEquals(
            "Why it appears: Orbit thinks this is a message follow-up. Decide whether you replied, still need to reply, or can clear it.",
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

    private fun parcel(
        id: String,
        category: String,
        status: String = "ACTIVE",
        completion: String = "FOUND",
        evidence: String = """{"label":"Item","source":"foreground_app"}"""
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
        updatedAtMillis = 1_700_000_000_000L
    )
}
