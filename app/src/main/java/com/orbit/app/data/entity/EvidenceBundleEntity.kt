package com.orbit.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Spec 004 — one evidence artifact collected for a capture understanding pass.
 *
 * bundleType must be one of: OCR, URL_METADATA, PUBLIC_FETCH, PARSER_OUTPUT,
 * MODEL_ATTEMPT, SKIPPED, FAILURE.
 *
 * // IPC-SAFE: payloadJson carries metadata only, never raw content.
 * payloadJson MUST NOT contain: raw HTML body, full screenshot data, or
 * embedding vectors. Store structured metadata keys only.
 */
@Entity(
    tableName = "evidence_bundle",
    foreignKeys = [
        ForeignKey(
            entity = IntentEnvelopeEntity::class,
            parentColumns = ["id"],
            childColumns = ["captureId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["captureId"])]
)
data class EvidenceBundleEntity(
    @PrimaryKey val id: String,
    val captureId: String,
    /** OCR | URL_METADATA | PUBLIC_FETCH | PARSER_OUTPUT | MODEL_ATTEMPT | SKIPPED | FAILURE */
    val bundleType: String,
    /** Structured metadata JSON only — no raw HTML, screenshots, or embeddings. */
    val payloadJson: String,
    val createdAt: Long
)
