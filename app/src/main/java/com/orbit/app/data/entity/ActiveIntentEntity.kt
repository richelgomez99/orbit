package com.orbit.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "active_intent",
    foreignKeys = [
        ForeignKey(
            entity = IntentEnvelopeEntity::class,
            parentColumns = ["id"],
            childColumns = ["captureId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["captureId"]),
        Index(value = ["status", "intentType"]),
        Index(value = ["expiresAt"]),
        Index(value = ["dueAt"])
    ]
)
data class ActiveIntentEntity(
    @PrimaryKey val intentId: String,
    val captureId: String,
    val intentType: String,
    val status: String,
    /** Compact display evidence only. No raw screenshot, full OCR body, HTML, prompts, or embeddings. */
    val primaryEvidenceJson: String,
    val primaryAction: String?,
    val dueAt: Long?,
    val expiresAt: Long?,
    val resolutionReason: String?,
    val resolvedAt: Long?,
    val userConfirmed: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
