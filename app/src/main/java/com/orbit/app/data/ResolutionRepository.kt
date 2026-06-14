package com.orbit.app.data

import com.orbit.app.data.dao.ResolutionReceiptDao
import com.orbit.app.data.entity.ResolutionReceiptEntity
import com.orbit.app.resolution.ResolutionActor
import com.orbit.app.resolution.ResolutionKind
import com.orbit.app.resolution.ResolutionReceipt
import com.orbit.app.resolution.ResolutionSurface
import com.orbit.app.resolution.ResolutionTargetType
import com.orbit.app.resolution.ResolutionVerdict
import com.orbit.app.resolution.ResolutionVerdictResolver
import org.json.JSONArray
import org.json.JSONObject

fun interface ResolutionReceiptSink {
    suspend fun record(receipt: ResolutionReceipt): Boolean
}

interface ResolutionVerdictProvider {
    suspend fun verdictFor(
        targetType: ResolutionTargetType,
        targetId: String,
        nowMillis: Long,
        surface: ResolutionSurface = ResolutionSurface.CLEANUP_QUEUE,
    ): ResolutionVerdict
}

class ResolutionRepository(
    private val dao: ResolutionReceiptDao,
    private val resolver: ResolutionVerdictResolver = ResolutionVerdictResolver(),
) : ResolutionReceiptSink, ResolutionVerdictProvider {

    override suspend fun record(receipt: ResolutionReceipt): Boolean {
        if (!ResolutionReceiptValidator.isValid(receipt)) return false
        return dao.insert(receipt.toEntity()) >= 0
    }

    override suspend fun verdictFor(
        targetType: ResolutionTargetType,
        targetId: String,
        nowMillis: Long,
        surface: ResolutionSurface,
    ): ResolutionVerdict {
        if (targetId.isBlank()) {
            return resolver.resolve(emptyList(), nowMillis = nowMillis, surface = surface)
        }
        return resolver.resolve(
            receipts = dao.getForTarget(targetType, targetId).map { it.toDomain() },
            nowMillis = nowMillis,
            surface = surface,
        )
    }
}

internal object ResolutionReceiptValidator {
    private const val METADATA_MAX_CHARS = 2_000

    private val bannedKeys = setOf(
        "rawScreenshot",
        "screenshot",
        "rawOcr",
        "rawOCR",
        "ocrBody",
        "rawText",
        "fullText",
        "prompt",
        "systemPrompt",
        "modelResponse",
        "embedding",
        "embeddings",
        "jwt",
        "cookie",
        "cookies",
        "apiKey",
        "accessToken",
        "rawHtml",
        "htmlBody",
    ).map { it.lowercase() }.toSet()

    fun isValid(receipt: ResolutionReceipt): Boolean {
        if (receipt.id.isBlank()) return false
        if (receipt.targetId.isBlank()) return false
        if (receipt.occurredAtMillis <= 0L) return false
        if (receipt.kind == ResolutionKind.SNOOZED &&
            (receipt.effectiveUntilMillis == null || receipt.effectiveUntilMillis <= receipt.occurredAtMillis)
        ) {
            return false
        }
        if (receipt.kind in setOf(ResolutionKind.DONE, ResolutionKind.RESOLVED) &&
            receipt.actor !in setOf(ResolutionActor.USER, ResolutionActor.ACTION_RUNTIME)
        ) {
            return false
        }
        val metadata = receipt.metadataJson
        if (!metadata.isNullOrBlank()) {
            if (metadata.length > METADATA_MAX_CHARS) return false
            if (containsBannedKey(metadata)) return false
        }
        return true
    }

    private fun containsBannedKey(raw: String): Boolean {
        val parsed = runCatching { JSONObject(raw) }.getOrNull()
            ?: return bannedKeys.any { raw.lowercase().contains("\"$it\"") }
        return containsBannedKey(parsed)
    }

    private fun containsBannedKey(obj: JSONObject): Boolean {
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key.lowercase() in bannedKeys) return true
            when (val value = obj.opt(key)) {
                is JSONObject -> if (containsBannedKey(value)) return true
                is JSONArray -> if (containsBannedKey(value)) return true
            }
        }
        return false
    }

    private fun containsBannedKey(arr: JSONArray): Boolean {
        for (i in 0 until arr.length()) {
            when (val value = arr.opt(i)) {
                is JSONObject -> if (containsBannedKey(value)) return true
                is JSONArray -> if (containsBannedKey(value)) return true
            }
        }
        return false
    }
}

internal fun ResolutionReceipt.toEntity() = ResolutionReceiptEntity(
    id = id,
    targetType = targetType,
    targetId = targetId,
    envelopeId = envelopeId,
    relatedType = relatedType,
    relatedId = relatedId,
    kind = kind,
    actor = actor,
    reason = reason,
    occurredAtMillis = occurredAtMillis,
    effectiveUntilMillis = effectiveUntilMillis,
    invalidatesReceiptId = invalidatesReceiptId,
    metadataJson = metadataJson,
)

internal fun ResolutionReceiptEntity.toDomain() = ResolutionReceipt(
    id = id,
    targetType = targetType,
    targetId = targetId,
    envelopeId = envelopeId,
    relatedType = relatedType,
    relatedId = relatedId,
    kind = kind,
    actor = actor,
    reason = reason,
    occurredAtMillis = occurredAtMillis,
    effectiveUntilMillis = effectiveUntilMillis,
    invalidatesReceiptId = invalidatesReceiptId,
    metadataJson = metadataJson,
)
