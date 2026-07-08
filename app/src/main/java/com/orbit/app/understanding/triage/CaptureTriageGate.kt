package com.orbit.app.understanding.triage

import com.orbit.app.understanding.domain.CompletionKeyKind
import com.orbit.app.understanding.domain.IntentCategory

/**
 * Phase A · Tier 1 — the deterministic triage gate. Pure heuristics decide
 * whether a capture is worth the agent's attention *before* any model runs,
 * so the throwaway majority is dropped for free. Modeled on
 * `ai/extract/ActionExtractionPrefilter`.
 *
 * Policy (conservative): a capture is worth enhancing if a *strong* signal
 * fires (a deliberate user classification, or an actionable completion key),
 * or if at least two independent signals fire. A lone long note is not enough.
 */
object CaptureTriageGate {

    private const val MIN_SUBSTANCE_CHARS = 80

    /** Completion keys that indicate something actionable/worth remembering. */
    private val STRONG_KEYS = setOf(
        CompletionKeyKind.DATE,
        CompletionKeyKind.ORDER_ID,
        CompletionKeyKind.COUPON_CODE,
        CompletionKeyKind.ADDRESS,
    )

    private val UNINFORMATIVE_CATEGORIES = setOf(
        IntentCategory.UNKNOWN,
        IntentCategory.MAYBE_OLD_OR_INACTIVE,
    )

    fun evaluate(input: TriageInput): TriageVerdict {
        val fired = mutableSetOf<TriageSignal>()

        val text = input.text?.trim().orEmpty()
        if (text.length >= MIN_SUBSTANCE_CHARS && !isBareUrl(text)) {
            fired += TriageSignal.HAS_TEXT_OF_SUBSTANCE
        }
        if (!input.canonicalUrl.isNullOrBlank()) {
            fired += TriageSignal.HAS_CANONICAL_URL
        }
        if (input.category !in UNINFORMATIVE_CATEGORIES && input.categoryConfidence >= 0.6f) {
            fired += TriageSignal.CATEGORY_KNOWN
        }
        if (input.completionKey != null && input.completionKey.kind in STRONG_KEYS) {
            fired += TriageSignal.HAS_COMPLETION_KEY
        }
        if (input.intentSource == com.orbit.app.data.model.IntentSource.USER_CHIP) {
            fired += TriageSignal.USER_CHIP
        }

        val strong = TriageSignal.HAS_COMPLETION_KEY in fired || TriageSignal.USER_CHIP in fired
        val worth = strong || fired.size >= 2
        return TriageVerdict(worthEnhancing = worth, fired = fired)
    }

    private val URL_ONLY = Regex("""^\s*https?://\S+\s*$""", RegexOption.IGNORE_CASE)
    private fun isBareUrl(text: String): Boolean = URL_ONLY.matches(text)
}
