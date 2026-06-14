package com.orbit.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.orbit.app.resolution.ResolutionActor
import com.orbit.app.resolution.ResolutionKind
import com.orbit.app.resolution.ResolutionTargetType

@Entity(
    tableName = "resolution_receipt",
    indices = [
        Index(value = ["targetType", "targetId", "occurredAtMillis"]),
        Index(value = ["envelopeId", "occurredAtMillis"]),
        Index(value = ["kind", "occurredAtMillis"]),
        Index(value = ["effectiveUntilMillis"])
    ]
)
data class ResolutionReceiptEntity(
    @PrimaryKey val id: String,
    val targetType: ResolutionTargetType,
    val targetId: String,
    val envelopeId: String?,
    val relatedType: ResolutionTargetType?,
    val relatedId: String?,
    val kind: ResolutionKind,
    val actor: ResolutionActor,
    val reason: String?,
    val occurredAtMillis: Long,
    val effectiveUntilMillis: Long?,
    val invalidatesReceiptId: String?,
    val metadataJson: String?,
)
