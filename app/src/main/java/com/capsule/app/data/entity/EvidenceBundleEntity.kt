package com.capsule.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.capsule.app.understanding.AcquisitionDepth
import com.capsule.app.understanding.AcquisitionMethod
import com.capsule.app.understanding.EvidenceKind
import com.capsule.app.understanding.EvidenceRetentionClass
import com.capsule.app.understanding.EvidenceStatus

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
    indices = [
        Index("captureId"),
        Index(value = ["captureId", "status", "invalidatedAt"]),
        Index("kind"),
        Index("sourceReference")
    ]
)
data class EvidenceBundleEntity(
    @PrimaryKey val id: String,
    val captureId: String,
    val kind: EvidenceKind,
    val sourceReference: String?,
    val acquisitionDepth: AcquisitionDepth,
    val acquisitionMethod: AcquisitionMethod,
    val confidence: Float?,
    val contentHash: String?,
    val retentionClass: EvidenceRetentionClass,
    val status: EvidenceStatus,
    val limitationsJson: String,
    val createdAt: Long,
    val invalidatedAt: Long? = null
)