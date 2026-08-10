package com.capsule.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.capsule.app.understanding.CaptureUnderstandingStatus
import com.capsule.app.understanding.UnderstandingDepth

@Entity(
    tableName = "capture_understanding",
    foreignKeys = [
        ForeignKey(
            entity = IntentEnvelopeEntity::class,
            parentColumns = ["id"],
            childColumns = ["captureId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UnderstandingJobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = SourceIdentityEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceIdentityId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("captureId"),
        Index("jobId"),
        Index("sourceIdentityId"),
        Index(value = ["captureId", "version"], unique = true),
        Index(value = ["captureId", "status", "invalidatedAt", "supersededAt"])
    ]
)
data class CaptureUnderstandingEntity(
    @PrimaryKey val id: String,
    val captureId: String,
    val jobId: String?,
    val version: Int,
    val status: CaptureUnderstandingStatus,
    val title: String?,
    val compactSummary: String?,
    val basedOnEvidenceIdsJson: String,
    val sourceIdentityId: String?,
    val confidence: Float?,
    val limitationsJson: String,
    val depthUsed: UnderstandingDepth,
    val extractorProvenance: String?,
    val producedAt: Long,
    val supersededAt: Long? = null,
    val invalidatedAt: Long? = null
)