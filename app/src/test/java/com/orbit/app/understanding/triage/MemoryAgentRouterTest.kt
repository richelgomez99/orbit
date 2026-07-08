package com.orbit.app.understanding.triage

import com.orbit.app.data.model.IntentSource
import com.orbit.app.data.model.MemoryCandidateSource
import com.orbit.app.understanding.domain.CompletionKey
import com.orbit.app.understanding.domain.CompletionKeyKind
import com.orbit.app.understanding.domain.IntentCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryAgentRouterTest {

    private val worth = TriageVerdict(worthEnhancing = true, fired = emptySet())

    @Test
    fun eventWithDateAutoPromotes() {
        val action = MemoryAgentRouter.route(
            input(category = IntentCategory.EVENT_TICKET_RESERVATION, key = key(CompletionKeyKind.DATE, "Sat 8pm")),
            worth,
        )
        val save = action as TriageAction.SaveToMemory
        assertEquals("has_upcoming_event", save.fact.predicate)
        assertTrue(save.autoPromote)
    }

    @Test
    fun sensitiveEventNeverAutoPromotes() {
        val action = MemoryAgentRouter.route(
            input(
                category = IntentCategory.EVENT_TICKET_RESERVATION,
                key = key(CompletionKeyKind.DATE, "Sat 8pm"),
                text = "ticket for [REDACTED_NAME], Sat 8pm",
            ),
            worth,
        )
        assertFalse((action as TriageAction.SaveToMemory).autoPromote)
    }

    @Test
    fun userChipSeedsAnInterestCandidate() {
        val action = MemoryAgentRouter.route(
            input(category = IntentCategory.RECIPE, intentSource = IntentSource.USER_CHIP),
            worth,
        )
        val save = action as TriageAction.SaveToMemory
        assertEquals("interested_in", save.fact.predicate)
        assertEquals("cooking", save.fact.objectValue)
        assertEquals(MemoryCandidateSource.USER_DECLARATION, save.fact.source)
        assertFalse(save.autoPromote) // 0.80 < auto-promote floor
    }

    @Test
    fun singleInterestCategoryIsCandidateOnly() {
        val action = MemoryAgentRouter.route(
            input(category = IntentCategory.PLACE_OR_TRAVEL_IDEA),
            worth,
        )
        val save = action as TriageAction.SaveToMemory
        assertEquals("travel", save.fact.objectValue)
        assertEquals(MemoryCandidateSource.CAPTURE_PATTERN, save.fact.source)
        assertFalse(save.autoPromote)
    }

    @Test
    fun notWorthEnhancingIsIgnored() {
        val action = MemoryAgentRouter.route(
            input(category = IntentCategory.RECIPE),
            TriageVerdict(worthEnhancing = false, fired = emptySet()),
        )
        assertEquals(TriageAction.Ignore, action)
    }

    @Test
    fun uninformativeCategoryYieldsNoFact() {
        val action = MemoryAgentRouter.route(
            input(category = IntentCategory.UNKNOWN),
            worth,
        )
        assertEquals(TriageAction.Ignore, action)
    }

    private fun input(
        category: IntentCategory = IntentCategory.RECIPE,
        key: CompletionKey? = null,
        intentSource: IntentSource = IntentSource.AUTO_AMBIGUOUS,
        text: String? = "a normal capture about the topic at hand",
    ) = TriageInput(
        captureId = "cap-1",
        text = text,
        category = category,
        categoryConfidence = 0.9f,
        completionKey = key,
        canonicalUrl = null,
        contentHashHex = "hash",
        intentSource = intentSource,
        redactionMarkersPresent = text?.contains("[REDACTED_") == true,
    )

    private fun key(kind: CompletionKeyKind, label: String = "x") =
        CompletionKey(kind, label, "test", 0.9f, null)
}
