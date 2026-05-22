package com.orbit.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.orbit.app.understanding.domain.ActiveIntentStatus
import com.orbit.app.understanding.domain.CompletionKeyStatus
import com.orbit.app.understanding.domain.IntentCategory
import com.orbit.app.understanding.domain.ResolutionReason

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
        Index(value = ["status"]),
        Index(value = ["intentType"]),
        Index(value = ["dueAt"]),
        Index(value = ["expiresAt"])
    ]
)
data class ActiveIntentEntity(
    @PrimaryKey val intentId: String,
    val captureId: String,
    val intentType: IntentCategory,
    val status: ActiveIntentStatus,
    val completionKeyJson: String?,
    val completionKeyStatus: CompletionKeyStatus,
    val primaryEvidenceJson: String,
    val primaryAction: String?,
    val dueAt: Long?,
    val expiresAt: Long?,
    val resolutionReason: ResolutionReason?,
    val resolvedAt: Long?,
    val userConfirmed: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)