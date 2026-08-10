package com.capsule.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.capsule.app.understanding.CorrectionFeedbackKind

@Entity(
    tableName = "correction_feedback",
    foreignKeys = [
        ForeignKey(
            entity = IntentEnvelopeEntity::class,
            parentColumns = ["id"],
            childColumns = ["captureId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CaptureUnderstandingEntity::class,
            parentColumns = ["id"],
            childColumns = ["understandingId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = SourceIdentityEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceIdentityId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CaptureUnderstandingEntity::class,
            parentColumns = ["id"],
            childColumns = ["resolvedByUnderstandingId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("captureId"),
        Index("understandingId"),
        Index("sourceIdentityId"),
        Index("resolvedByUnderstandingId"),
        Index(value = ["captureId", "feedbackKind", "createdAt"])
    ]
)
data class CorrectionFeedbackEntity(
    @PrimaryKey val id: String,
    val captureId: String,
    val understandingId: String?,
    val sourceIdentityId: String?,
    val feedbackKind: CorrectionFeedbackKind,
    val targetReference: String?,
    val note: String?,
    val createdAt: Long,
    val resolvedByUnderstandingId: String? = null
)