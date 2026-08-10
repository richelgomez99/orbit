package com.capsule.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.capsule.app.understanding.RetryEligibility
import com.capsule.app.understanding.UnderstandingDepth
import com.capsule.app.understanding.UnderstandingJobStatus

@Entity(
    tableName = "understanding_job",
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
        Index(value = ["captureId", "status", "createdAt"]),
        Index("policyDecision")
    ]
)
data class UnderstandingJobEntity(
    @PrimaryKey val id: String,
    val captureId: String,
    val requestedDepth: UnderstandingDepth,
    val effectiveDepth: UnderstandingDepth,
    val status: UnderstandingJobStatus,
    val policyDecision: String,
    val retryEligibility: RetryEligibility,
    val attemptCount: Int,
    val maxAttempts: Int,
    val traceIdsJson: String,
    val failureCode: String?,
    val userVisibleReason: String?,
    val startedAt: Long?,
    val finishedAt: Long?,
    val createdAt: Long,
    val invalidatedAt: Long? = null
)