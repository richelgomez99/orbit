package com.orbit.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.orbit.app.data.model.MemoryCandidateKind
import com.orbit.app.data.model.MemoryCandidateSource
import com.orbit.app.data.model.MemoryCandidateState
import com.orbit.app.data.model.MemorySensitivity

@Entity(
    tableName = "memory_candidate",
    indices = [
        Index(value = ["state"]),
        Index(value = ["candidateKind"]),
        Index(value = ["sensitivity"]),
        Index(value = ["createdAt"]),
        Index(value = ["expiresAt"])
    ]
)
data class MemoryCandidateEntity(
    @PrimaryKey val id: String,
    val candidateKind: MemoryCandidateKind,
    val state: MemoryCandidateState,
    val displayLabel: String,
    val subject: String,
    val predicate: String,
    val objectValue: String,
    val confidence: Float,
    val sensitivity: MemorySensitivity,
    val supportingEnvelopeIdsJson: String,
    val supportingEvidenceIdsJson: String?,
    val supportingFeedbackIdsJson: String?,
    val askUserCopy: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val expiresAt: Long?,
    val decidedAt: Long?,
    val decisionReason: String?,
    val modelLabel: String?,
    val promptVersion: String?,
    val source: MemoryCandidateSource
)
