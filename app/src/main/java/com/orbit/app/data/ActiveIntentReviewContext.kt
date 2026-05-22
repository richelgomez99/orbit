package com.orbit.app.data

import com.orbit.app.data.entity.ActiveIntentEntity
import com.orbit.app.understanding.domain.CompactEvidencePayload
import com.orbit.app.understanding.domain.UnderstandingMode
import org.json.JSONObject

object ActiveIntentReviewContext {
    private val forbiddenKeys = setOf(
        "rawScreenshot",
        "screenshotBytes",
        "rawHtml",
        "fullOcrText",
        "ocrText",
        "embedding",
        "prompt",
        "modelResponse"
    )

    fun build(entity: ActiveIntentEntity, mode: UnderstandingMode): JSONObject = JSONObject().apply {
        put("schemaVersion", 1)
        put("intentId", entity.intentId)
        put("captureId", entity.captureId)
        put("mode", mode.name)
        put("intentType", entity.intentType.name)
        put("status", entity.status.name)
        put("completionKeyStatus", entity.completionKeyStatus.name)
        entity.primaryAction?.take(CompactEvidencePayload.MAX_ACTION_LABEL_CHARS)?.let { put("primaryAction", it) }
        entity.dueAt?.let { put("dueAt", it) }
        entity.expiresAt?.let { put("expiresAt", it) }
        put("evidence", compactJsonOrFallback(entity.primaryEvidenceJson, entity.intentType.name))
        compactJsonOrNull(entity.completionKeyJson)?.let { put("completionKey", it) }
    }

    private fun compactJsonOrFallback(value: String?, label: String): JSONObject =
        compactJsonOrNull(value) ?: JSONObject().apply {
            put("kind", "LIMITED")
            put("label", label)
            put("source", "active_intent")
        }

    private fun compactJsonOrNull(value: String?): JSONObject? {
        val trimmed = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (trimmed.length > CompactEvidencePayload.MAX_JSON_CHARS) return null
        val json = runCatching { JSONObject(trimmed) }.getOrNull() ?: return null
        val keys = json.keys().asSequence().toList()
        if (keys.any { it in forbiddenKeys }) return null
        if (keys.any { !CompactEvidencePayload.isAllowedKey(it) }) return null
        return json
    }
}