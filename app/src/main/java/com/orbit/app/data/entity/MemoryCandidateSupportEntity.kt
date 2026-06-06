package com.orbit.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.orbit.app.data.model.MemorySupportType

@Entity(
    tableName = "memory_candidate_support",
    primaryKeys = ["candidateId", "envelopeId", "supportType"],
    foreignKeys = [
        ForeignKey(
            entity = MemoryCandidateEntity::class,
            parentColumns = ["id"],
            childColumns = ["candidateId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = IntentEnvelopeEntity::class,
            parentColumns = ["id"],
            childColumns = ["envelopeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["candidateId"]),
        Index(value = ["envelopeId"])
    ]
)
data class MemoryCandidateSupportEntity(
    val candidateId: String,
    val envelopeId: String,
    val supportType: MemorySupportType,
    val evidenceId: String?,
    val createdAt: Long
)
