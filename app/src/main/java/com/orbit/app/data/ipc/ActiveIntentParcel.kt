package com.orbit.app.data.ipc

import android.os.Parcel
import android.os.Parcelable
import com.orbit.app.data.entity.ActiveIntentEntity
import com.orbit.app.understanding.domain.CompactEvidencePayload
import org.json.JSONObject

/**
 * Compact Binder view of an Active Intent cleanup row.
 *
 * This parcel carries only display/action metadata. It must never include raw
 * screenshots, raw HTML, full OCR bodies, embeddings, prompts, or model output.
 */
data class ActiveIntentParcel(
    val intentId: String,
    val captureId: String,
    val intentType: String,
    val status: String,
    val completionKeyJson: String?,
    val completionKeyStatus: String,
    val primaryEvidenceJson: String,
    val primaryAction: String?,
    val dueAtMillis: Long?,
    val expiresAtMillis: Long?,
    val resolutionReason: String?,
    val resolvedAtMillis: Long?,
    val userConfirmed: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) : Parcelable {

    constructor(parcel: Parcel) : this(
        intentId = parcel.readString()!!,
        captureId = parcel.readString()!!,
        intentType = parcel.readString()!!,
        status = parcel.readString()!!,
        completionKeyJson = parcel.readString(),
        completionKeyStatus = parcel.readString()!!,
        primaryEvidenceJson = parcel.readString()!!,
        primaryAction = parcel.readString(),
        dueAtMillis = parcel.readNullableLong(),
        expiresAtMillis = parcel.readNullableLong(),
        resolutionReason = parcel.readString(),
        resolvedAtMillis = parcel.readNullableLong(),
        userConfirmed = parcel.readInt() == 1,
        createdAtMillis = parcel.readLong(),
        updatedAtMillis = parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(intentId)
        parcel.writeString(captureId)
        parcel.writeString(intentType)
        parcel.writeString(status)
        parcel.writeString(completionKeyJson)
        parcel.writeString(completionKeyStatus)
        parcel.writeString(primaryEvidenceJson)
        parcel.writeString(primaryAction)
        parcel.writeNullableLong(dueAtMillis)
        parcel.writeNullableLong(expiresAtMillis)
        parcel.writeString(resolutionReason)
        parcel.writeNullableLong(resolvedAtMillis)
        parcel.writeInt(if (userConfirmed) 1 else 0)
        parcel.writeLong(createdAtMillis)
        parcel.writeLong(updatedAtMillis)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ActiveIntentParcel> {
        override fun createFromParcel(parcel: Parcel) = ActiveIntentParcel(parcel)
        override fun newArray(size: Int) = arrayOfNulls<ActiveIntentParcel>(size)

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

        fun fromEntity(entity: ActiveIntentEntity): ActiveIntentParcel = ActiveIntentParcel(
            intentId = entity.intentId,
            captureId = entity.captureId,
            intentType = entity.intentType.name,
            status = entity.status.name,
            completionKeyJson = sanitizeCompactJson(entity.completionKeyJson),
            completionKeyStatus = entity.completionKeyStatus.name,
            primaryEvidenceJson = sanitizeCompactJson(entity.primaryEvidenceJson)
                ?: fallbackEvidence(entity.intentType.name),
            primaryAction = entity.primaryAction?.take(CompactEvidencePayload.MAX_ACTION_LABEL_CHARS),
            dueAtMillis = entity.dueAt,
            expiresAtMillis = entity.expiresAt,
            resolutionReason = entity.resolutionReason?.name,
            resolvedAtMillis = entity.resolvedAt,
            userConfirmed = entity.userConfirmed,
            createdAtMillis = entity.createdAt,
            updatedAtMillis = entity.updatedAt
        )

        private fun sanitizeCompactJson(value: String?): String? {
            val trimmed = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
            if (trimmed.length > CompactEvidencePayload.MAX_JSON_CHARS) return null
            val json = runCatching { JSONObject(trimmed) }.getOrNull() ?: return null
            val keys = json.keys().asSequence().toList()
            if (keys.any { it in forbiddenKeys }) return null
            if (keys.any { !CompactEvidencePayload.isAllowedKey(it) }) return null
            return trimmed
        }

        private fun fallbackEvidence(label: String): String = JSONObject().apply {
            put("kind", "LIMITED")
            put("label", label)
            put("source", "active_intent")
        }.toString()
    }
}

private fun Parcel.writeNullableLong(value: Long?) {
    writeInt(if (value == null) 0 else 1)
    if (value != null) writeLong(value)
}

private fun Parcel.readNullableLong(): Long? = if (readInt() == 1) readLong() else null