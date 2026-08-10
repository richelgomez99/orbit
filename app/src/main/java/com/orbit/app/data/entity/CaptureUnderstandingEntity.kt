package com.orbit.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.orbit.app.understanding.domain.CompletionKeyStatus
import com.orbit.app.understanding.domain.IntentCategory
import com.orbit.app.understanding.domain.UnderstandingMode
import com.orbit.app.understanding.domain.UnderstandingStatus

@Entity(
    tableName = "capture_understanding",
    foreignKeys = [
        ForeignKey(
            entity = IntentEnvelopeEntity::class,
            parentColumns = ["id"],
            childColumns = ["captureId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["category"]),
        Index(value = ["completionKeyStatus"]),
        Index(value = ["contentHashHex"]),
        Index(value = ["canonicalUrl"])
    ]
)
data class CaptureUnderstandingEntity(
    @PrimaryKey val captureId: String,
    val mode: UnderstandingMode,
    val status: UnderstandingStatus,
    val category: IntentCategory,
    val categoryConfidence: Float,
    val title: String?,
    val summaryText: String?,
    val completionKeyJson: String?,
    val completionKeyStatus: CompletionKeyStatus,
    val sourceIdentityJson: String?,
    val contentHashHex: String?,
    val canonicalUrl: String?,
    val groundingConstraintsJson: String,
    val createdAt: Long,
    val updatedAt: Long,
    val invalidatedAt: Long?
)
