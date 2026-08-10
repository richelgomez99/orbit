package com.capsule.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.capsule.app.understanding.CloudReceiptStatus
import com.capsule.app.understanding.DownstreamEligibility
import com.capsule.app.understanding.InvalidationReason

@Entity(
    tableName = "deletion_invalidation",
    foreignKeys = [
        ForeignKey(
            entity = IntentEnvelopeEntity::class,
            parentColumns = ["id"],
            childColumns = ["captureId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("captureId"),
        Index(value = ["captureId", "createdAt"]),
        Index("auditTraceId"),
        Index("cloudReceiptStatus")
    ]
)
data class DeletionInvalidationEntity(
    @PrimaryKey val id: String,
    val captureId: String,
    val reason: InvalidationReason,
    val affectedRecordRefsJson: String,
    val downstreamEligibility: DownstreamEligibility,
    val auditTraceId: String?,
    val createdAt: Long,
    val cloudReceiptStatus: CloudReceiptStatus? = null
)