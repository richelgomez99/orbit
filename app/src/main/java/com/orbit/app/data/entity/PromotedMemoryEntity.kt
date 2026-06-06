package com.orbit.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.orbit.app.data.model.MemorySensitivity
import com.orbit.app.data.model.PromotedMemoryConfidence
import com.orbit.app.data.model.PromotedMemoryKind
import com.orbit.app.data.model.PromotedMemorySource
import com.orbit.app.data.model.PromotedMemoryState

@Entity(
    tableName = "promoted_memory",
    foreignKeys = [
        ForeignKey(
            entity = MemoryCandidateEntity::class,
            parentColumns = ["id"],
            childColumns = ["candidateId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["state"]),
        Index(value = ["memoryKind"]),
        Index(value = ["sensitivity"]),
        Index(value = ["candidateId"]),
        Index(value = ["updatedAt"])
    ]
)
data class PromotedMemoryEntity(
    @PrimaryKey val id: String,
    val candidateId: String?,
    val memoryKind: PromotedMemoryKind,
    val state: PromotedMemoryState,
    val displayLabel: String,
    val subject: String,
    val predicate: String,
    val objectValue: String,
    val confidenceLabel: PromotedMemoryConfidence,
    val sensitivity: MemorySensitivity,
    val source: PromotedMemorySource,
    val supportingEnvelopeIdsJson: String,
    val supportingEvidenceIdsJson: String?,
    val supportingFeedbackIdsJson: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val validFrom: Long,
    val validTo: Long?,
    val invalidatedAt: Long?,
    val lastUsedAt: Long?,
    val useCount: Int
)
