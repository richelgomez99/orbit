package com.orbit.app.understanding

import com.orbit.app.understanding.domain.CompactEvidencePayload
import com.orbit.app.understanding.domain.CompletionKey
import com.orbit.app.understanding.domain.CompletionKeyStatus
import com.orbit.app.understanding.domain.GroundingConstraints
import com.orbit.app.understanding.domain.IntentCategory
import com.orbit.app.understanding.domain.SourceIdentity
import com.orbit.app.understanding.domain.UnderstandingMode
import com.orbit.app.understanding.domain.UnderstandingStatus

data class BasicUnderstandingInput(
    val captureId: String,
    val textContent: String?,
    val sourceAppLabel: String?,
    val appCategory: String?,
    val canonicalUrl: String?,
    val artifactBytes: ByteArray? = null,
    val capturedAtMillis: Long,
    val nowMillis: Long = System.currentTimeMillis()
)

data class BasicUnderstandingResult(
    val captureId: String,
    val mode: UnderstandingMode,
    val status: UnderstandingStatus,
    val category: IntentCategory,
    val categoryConfidence: Float,
    val title: String?,
    val summaryText: String?,
    val completionKey: CompletionKey?,
    val completionKeyStatus: CompletionKeyStatus,
    val sourceIdentity: SourceIdentity,
    val contentHashHex: String?,
    val canonicalUrl: String?,
    val groundingConstraints: GroundingConstraints,
    val evidencePayloadJson: List<String>
)

class BasicUnderstandingEngine(
    private val classifier: IntentCategoryClassifier = IntentCategoryClassifier(),
    private val completionKeyExtractor: CompletionKeyExtractor = CompletionKeyExtractor()
) {

    fun understand(input: BasicUnderstandingInput): BasicUnderstandingResult {
        val text = input.textContent?.trim()?.takeIf { it.isNotBlank() }
        val sourceIdentity = SourceIdentity.fromLocalSignals(
            sourceAppLabel = input.sourceAppLabel,
            appCategory = input.appCategory,
            canonicalUrl = input.canonicalUrl
        )
        val classification = classifier.classify(
            ClassificationInput(
                text = text,
                appCategory = input.appCategory,
                sourceAppLabel = input.sourceAppLabel,
                canonicalUrl = input.canonicalUrl,
                capturedAtMillis = input.capturedAtMillis,
                nowMillis = input.nowMillis
            )
        )
        val keys = completionKeyExtractor.extract(text, input.canonicalUrl)
        val primaryKey = keys.firstOrNull()
        val groundingConstraints = GroundingConstraints.basic(
            hasText = text != null,
            hasSourceIdentity = sourceIdentity.evidenceBasis.isNotEmpty(),
            hasCanonicalUrl = !input.canonicalUrl.isNullOrBlank()
        )

        return BasicUnderstandingResult(
            captureId = input.captureId,
            mode = UnderstandingMode.BASIC,
            status = if (text == null && sourceIdentity.evidenceBasis.isEmpty()) UnderstandingStatus.LIMITED else UnderstandingStatus.READY,
            category = classification.category,
            categoryConfidence = classification.confidence,
            title = titleFor(text, input.sourceAppLabel, classification.category),
            summaryText = text?.let { CompactEvidencePayload.cappedExcerpt(it).take(CompactEvidencePayload.MAX_SUMMARY_CHARS) },
            completionKey = primaryKey,
            completionKeyStatus = completionStatus(classification.category, primaryKey),
            sourceIdentity = sourceIdentity,
            contentHashHex = contentHash(input),
            canonicalUrl = input.canonicalUrl?.trim()?.takeIf { it.isNotBlank() },
            groundingConstraints = groundingConstraints,
            evidencePayloadJson = evidencePayloads(classification, keys, sourceIdentity, text)
        )
    }

    private fun completionStatus(category: IntentCategory, key: CompletionKey?): CompletionKeyStatus {
        if (key != null) return CompletionKeyStatus.FOUND
        return when (category) {
            IntentCategory.UNKNOWN -> CompletionKeyStatus.NEEDS_ESCALATION
            IntentCategory.MAYBE_OLD_OR_INACTIVE -> CompletionKeyStatus.NOT_ACTIONABLE
            IntentCategory.CHAT_ACTION,
            IntentCategory.READ_OR_WATCH_LATER,
            IntentCategory.GIFT_IDEA -> CompletionKeyStatus.MISSING
            else -> CompletionKeyStatus.MISSING
        }
    }

    private fun titleFor(text: String?, sourceAppLabel: String?, category: IntentCategory): String? {
        val firstLine = text?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim()
        val base = firstLine ?: sourceAppLabel?.trim()?.takeIf { it.isNotBlank() } ?: category.name
        return base.take(CompactEvidencePayload.MAX_TITLE_CHARS)
    }

    private fun contentHash(input: BasicUnderstandingInput): String? {
        input.artifactBytes?.takeIf { it.isNotEmpty() }?.let { return ContentHasher.hashArtifact(it) }
        return input.textContent?.takeIf { it.isNotBlank() }?.let { ContentHasher.hashNormalizedText(it) }
    }

    private fun evidencePayloads(
        classification: ClassificationResult,
        keys: List<CompletionKey>,
        sourceIdentity: SourceIdentity,
        text: String?
    ): List<String> = buildList {
        if (sourceIdentity.evidenceBasis.isNotEmpty()) {
            add(compactPayload("APP_CONTEXT", sourceIdentity.evidenceBasis.joinToString(","), "local", null, null))
        }
        classification.evidence.firstOrNull()?.let {
            add(compactPayload("CATEGORY", classification.category.name, it, classification.confidence, text))
        }
        keys.firstOrNull()?.let {
            add(compactPayload("COMPLETION_KEY", it.kind.name, it.source, it.confidence, it.excerpt))
        }
        if (isEmpty()) {
            add(compactPayload("LIMITED", "no_local_evidence", "basic", 0.2f, null))
        }
    }

    private fun compactPayload(
        kind: String,
        label: String,
        source: String,
        confidence: Float?,
        excerpt: String?
    ): String {
        val json = org.json.JSONObject().apply {
            put("kind", kind)
            put("label", label.take(120))
            put("source", source.take(80))
            confidence?.let { put("confidence", it.coerceIn(0f, 1f).toDouble()) }
            excerpt?.trim()?.takeIf { it.isNotBlank() }?.let { put("excerpt", CompactEvidencePayload.cappedExcerpt(it)) }
        }
        return json.toString().take(CompactEvidencePayload.MAX_JSON_CHARS)
    }
}