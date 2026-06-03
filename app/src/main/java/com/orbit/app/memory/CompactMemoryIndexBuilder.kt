package com.orbit.app.memory

import com.orbit.app.data.entity.CaptureUnderstandingEntity
import com.orbit.app.data.entity.ContinuationResultEntity
import com.orbit.app.data.entity.EnvelopeNoteEntity
import com.orbit.app.data.entity.EvidenceBundleEntity
import com.orbit.app.data.entity.IntentEnvelopeEntity
import org.json.JSONObject

class CompactMemoryIndexBuilder {

    fun build(
        envelope: IntentEnvelopeEntity,
        latestResult: ContinuationResultEntity? = null,
        understanding: CaptureUnderstandingEntity? = null,
        note: EnvelopeNoteEntity? = null,
        evidenceBundles: List<EvidenceBundleEntity> = emptyList()
    ): MemoryIndexItem? {
        if (envelope.isDeleted) return null

        val generatedTitle = firstCapped(
            understanding?.title,
            latestResult?.title,
            note?.text?.lineSequence()?.firstOrNull()
        )
        val title = MemoryDisplayText.title(
            existingTitle = generatedTitle,
            text = envelope.textContent,
            domain = latestResult?.domain,
            maxChars = MemoryPayloadCaps.TITLE_MAX,
        )?.let { MemoryPayloadCaps.cap(it, MemoryPayloadCaps.TITLE_MAX) }

        val generatedSummary = firstCapped(
            understanding?.summaryText,
            latestResult?.summary,
            latestResult?.excerpt,
            note?.text
        )
        val summary = MemoryDisplayText.summary(
            existingSummary = generatedSummary,
            text = envelope.textContent,
            maxChars = MemoryPayloadCaps.SUMMARY_MAX,
        )?.let { MemoryPayloadCaps.cap(it, MemoryPayloadCaps.SUMMARY_MAX) }

        val evidence = buildEvidence(
            envelope = envelope,
            latestResult = latestResult,
            note = note,
            evidenceBundles = evidenceBundles
        )

        val sourceAppLabel = SourceAppLabelDisplay.userFacingOrNull(envelope.state.sourceAppLabel)

        val compactText = listOfNotNull(
            title,
            summary,
            MemoryPayloadCaps.cap(sourceAppLabel, MemoryPayloadCaps.TAG_MAX),
            envelope.state.appCategory.name,
            latestResult?.domain,
            latestResult?.canonicalUrl,
            evidence.joinToString(" ") { it.excerpt.orEmpty() }
        )
            .joinToString(" ")
            .let { MemoryPayloadCaps.cap(it, MemoryPayloadCaps.COMPACT_TEXT_MAX) }
            ?: return null

        val tags = buildTags(
            understanding?.category?.name,
            envelope.intent.name,
            envelope.contentType.name,
            envelope.state.appCategory.name,
            latestResult?.domain
        )

        return MemoryIndexItem(
            envelopeId = envelope.id,
            kind = envelope.kind.name,
            dayLocal = envelope.dayLocal,
            createdAtMillis = envelope.createdAt,
            intent = understanding?.category?.name ?: envelope.intent.name,
            contentType = envelope.contentType.name.lowercase(),
            title = title,
            summary = summary,
            sourceAppLabel = MemoryPayloadCaps.cap(sourceAppLabel, 128),
            appCategory = envelope.state.appCategory.name,
            canonicalUrl = latestResult?.canonicalUrl ?: understanding?.canonicalUrl,
            domain = latestResult?.domain,
            tags = tags,
            evidence = evidence,
            compactText = compactText,
            localContentHash = understanding?.contentHashHex ?: envelope.textContentSha256
        )
    }

    private fun buildEvidence(
        envelope: IntentEnvelopeEntity,
        latestResult: ContinuationResultEntity?,
        note: EnvelopeNoteEntity?,
        evidenceBundles: List<EvidenceBundleEntity>
    ): List<MemoryEvidenceSnippet> {
        val snippets = mutableListOf<MemoryEvidenceSnippet>()

        evidenceBundles.forEach { bundle ->
            parseEvidenceBundle(bundle)?.let { snippets += it }
        }

        latestResult?.summary?.let { summary ->
            snippets += MemoryEvidenceSnippet(
                kind = "URL_METADATA",
                label = "Summary",
                excerpt = MemoryPayloadCaps.cap(summary, MemoryPayloadCaps.EVIDENCE_EXCERPT_MAX),
                source = "continuation",
                createdAtMillis = latestResult.producedAt
            )
        }

        note?.text?.let { text ->
            snippets += MemoryEvidenceSnippet(
                kind = "NOTE",
                label = "Context",
                excerpt = MemoryPayloadCaps.cap(text, MemoryPayloadCaps.EVIDENCE_EXCERPT_MAX),
                source = "note",
                createdAtMillis = note.updatedAt ?: note.createdAt
            )
        }

        envelope.textContent?.let { text ->
            snippets += MemoryEvidenceSnippet(
                kind = "TEXT_EXCERPT",
                label = "Saved text",
                excerpt = MemoryPayloadCaps.cap(text, MemoryPayloadCaps.EVIDENCE_EXCERPT_MAX),
                source = "envelope",
                createdAtMillis = envelope.createdAt
            )
        }

        return snippets
            .filter { it.excerpt?.isNotBlank() != false }
            .take(MemoryPayloadCaps.EVIDENCE_MAX)
    }

    private fun parseEvidenceBundle(bundle: EvidenceBundleEntity): MemoryEvidenceSnippet? =
        runCatching {
            val json = JSONObject(bundle.payloadJson)
            val keys = json.keys().asSequence().toList()
            if (keys.any(MemoryPayloadCaps::isBannedKey)) return null

            MemoryEvidenceSnippet(
                kind = json.optString("kind", bundle.bundleType).take(64),
                label = json.optString("label", bundle.bundleType).take(80),
                excerpt = MemoryPayloadCaps.cap(
                    json.optString("excerpt", ""),
                    MemoryPayloadCaps.EVIDENCE_EXCERPT_MAX
                ),
                source = json.optString("source", "understanding").take(64),
                confidence = json.optDouble("confidence")
                    .takeIf { !it.isNaN() }
                    ?.toFloat(),
                hash = json.optString("hash", "").takeIf { it.isNotBlank() }?.take(128),
                createdAtMillis = json.optLong("createdAt")
                    .takeIf { it > 0 }
                    ?: bundle.createdAt
            )
        }.getOrNull()

    private fun firstCapped(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }

    private fun buildTags(vararg values: String?): List<String> =
        values
            .asSequence()
            .filterNotNull()
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .map { it.take(MemoryPayloadCaps.TAG_MAX) }
            .distinct()
            .take(MemoryPayloadCaps.TAGS_MAX)
            .toList()
}
