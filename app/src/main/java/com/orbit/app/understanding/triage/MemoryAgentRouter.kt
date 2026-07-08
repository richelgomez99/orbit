package com.orbit.app.understanding.triage

import com.orbit.app.data.model.IntentSource
import com.orbit.app.data.model.MemoryCandidateKind
import com.orbit.app.data.model.MemoryCandidateSource
import com.orbit.app.data.model.MemorySensitivity
import com.orbit.app.understanding.domain.CompletionKeyKind
import com.orbit.app.understanding.domain.IntentCategory

/**
 * Phase A · Tier-1 deterministic tool router. Given a capture that passed the
 * triage gate, decides the single action to take. This is the rule-based v1 —
 * a model-backed router (constrained decoding) drops in behind the same
 * interface once the on-device stack exposes it (see roadmap §6).
 *
 * Conservative, precision-first: a small fixed fact vocabulary, most captures
 * become review candidates, only high-confidence non-sensitive event facts
 * auto-promote.
 */
object MemoryAgentRouter {

    const val CANDIDATE_MIN = 0.50f
    const val AUTO_PROMOTE_MIN = 0.85f
    val AUTO_PROMOTE_ALLOWLIST = setOf("has_upcoming_event", "treats_topic_as")

    fun route(input: TriageInput, verdict: TriageVerdict): TriageAction {
        if (!verdict.worthEnhancing) return TriageAction.Ignore
        val sensitivity = FactSensitivityPolicy.sensitivityOf(input)

        // 1. Most specific: an event with a concrete date → upcoming-event fact,
        //    auto-promoted only when non-sensitive.
        if (input.category == IntentCategory.EVENT_TICKET_RESERVATION &&
            input.completionKey?.kind == CompletionKeyKind.DATE
        ) {
            return save(eventFact(input, sensitivity))
        }

        // 2. A deliberate user classification is a gold cold-start seed.
        if (input.intentSource == IntentSource.USER_CHIP) {
            interestFact(input, sensitivity, confidence = 0.80f, source = MemoryCandidateSource.USER_DECLARATION)
                ?.let { return save(it) }
        }

        // 3. An informative interest category → a review candidate.
        interestFact(input, sensitivity, confidence = 0.55f, source = MemoryCandidateSource.CAPTURE_PATTERN)
            ?.let { return save(it) }

        return TriageAction.Ignore
    }

    /** Quality gate: drop below the candidate floor; auto-promote only when safe. */
    private fun save(fact: CandidateFact): TriageAction =
        if (fact.confidence < CANDIDATE_MIN) {
            TriageAction.Ignore
        } else {
            TriageAction.SaveToMemory(fact, autoPromote = shouldAutoPromote(fact))
        }

    fun shouldAutoPromote(fact: CandidateFact): Boolean =
        fact.confidence >= AUTO_PROMOTE_MIN &&
            fact.sensitivity == MemorySensitivity.NORMAL &&
            fact.predicate in AUTO_PROMOTE_ALLOWLIST

    private fun interestFact(
        input: TriageInput,
        sensitivity: MemorySensitivity,
        confidence: Float,
        source: MemoryCandidateSource,
    ): CandidateFact? {
        val topic = topicFor(input.category) ?: return null
        return CandidateFact(
            subject = "user",
            predicate = "interested_in",
            objectValue = topic,
            displayLabel = "Interested in $topic",
            kind = MemoryCandidateKind.INTEREST,
            sensitivity = sensitivity,
            confidence = confidence,
            evidenceExcerpt = excerpt(input),
            source = source,
        )
    }

    private fun eventFact(input: TriageInput, sensitivity: MemorySensitivity): CandidateFact {
        val label = input.completionKey?.label?.takeIf { it.isNotBlank() } ?: "upcoming event"
        return CandidateFact(
            subject = "user",
            predicate = "has_upcoming_event",
            objectValue = label,
            displayLabel = "Upcoming: $label",
            kind = MemoryCandidateKind.PATTERN,
            sensitivity = sensitivity,
            confidence = 0.85f,
            evidenceExcerpt = excerpt(input),
            source = MemoryCandidateSource.CAPTURE_PATTERN,
        )
    }

    private fun topicFor(category: IntentCategory): String? = when (category) {
        IntentCategory.RECIPE -> "cooking"
        IntentCategory.PLACE_OR_TRAVEL_IDEA -> "travel"
        IntentCategory.BUY_LATER_PRODUCT -> "shopping"
        IntentCategory.READ_OR_WATCH_LATER -> "reading"
        IntentCategory.GIFT_IDEA -> "gifts"
        IntentCategory.EVENT_TICKET_RESERVATION -> "events"
        else -> null
    }

    private fun excerpt(input: TriageInput): String? =
        input.text?.trim()?.take(160)?.takeIf { it.isNotBlank() }
}
