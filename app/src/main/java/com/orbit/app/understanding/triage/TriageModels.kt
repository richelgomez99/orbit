package com.orbit.app.understanding.triage

import com.orbit.app.data.model.IntentSource
import com.orbit.app.data.model.MemoryCandidateKind
import com.orbit.app.data.model.MemoryCandidateSource
import com.orbit.app.data.model.MemorySensitivity
import com.orbit.app.understanding.BasicUnderstandingResult
import com.orbit.app.understanding.domain.CompletionKey
import com.orbit.app.understanding.domain.IntentCategory

/**
 * Phase A — the pure, deterministic inputs/outputs for the triage-gated memory
 * agent that runs at capture seal (spec-020 / `docs/agentic-memory-roadmap-*`).
 *
 * Everything here is decoupled from Room and the graph so the whole triage core
 * is unit-testable off-device. The side-effecting coordinator (later slice)
 * translates a [TriageAction] into writes against the existing
 * `MemoryRepositoryDelegate` / graph path.
 */
data class TriageInput(
    val captureId: String,
    val text: String?,
    val category: IntentCategory,
    val categoryConfidence: Float,
    val completionKey: CompletionKey?,
    val canonicalUrl: String?,
    val contentHashHex: String?,
    val intentSource: IntentSource,
    /** True when the scrubbed text still carries `[REDACTED_…]` markers — a free sensitivity signal. */
    val redactionMarkersPresent: Boolean,
) {
    companion object {
        fun from(
            result: BasicUnderstandingResult,
            text: String?,
            intentSource: IntentSource,
        ): TriageInput = TriageInput(
            captureId = result.captureId,
            text = text,
            category = result.category,
            categoryConfidence = result.categoryConfidence,
            completionKey = result.completionKey,
            canonicalUrl = result.canonicalUrl,
            contentHashHex = result.contentHashHex,
            intentSource = intentSource,
            redactionMarkersPresent = text?.contains("[REDACTED_") == true,
        )
    }
}

/**
 * One extracted fact, subject → predicate → object, with provenance. The
 * subject is `"user"` for profile/interest facts. Provenance (the capture id)
 * is attached at ingestion; the graph write path rejects facts without it.
 */
data class CandidateFact(
    val subject: String,
    val predicate: String,
    val objectValue: String,
    val displayLabel: String,
    val kind: MemoryCandidateKind,
    val sensitivity: MemorySensitivity,
    val confidence: Float,
    val evidenceExcerpt: String?,
    val source: MemoryCandidateSource,
)

/** Which deterministic signals fired in Tier 1. */
enum class TriageSignal {
    HAS_TEXT_OF_SUBSTANCE,
    HAS_CANONICAL_URL,
    CATEGORY_KNOWN,
    HAS_COMPLETION_KEY,
    USER_CHIP,
}

data class TriageVerdict(
    val worthEnhancing: Boolean,
    val fired: Set<TriageSignal>,
)

/**
 * The single action the agent takes for a capture. First slice emits
 * [SaveToMemory] or [Ignore]; [DraftConversation] is wired when the durable
 * draft store lands (integration slice).
 */
sealed interface TriageAction {
    data class SaveToMemory(val fact: CandidateFact, val autoPromote: Boolean) : TriageAction
    data class DraftConversation(val topicKey: String, val prompt: String, val excerpt: String?) : TriageAction
    data object Ignore : TriageAction
}
