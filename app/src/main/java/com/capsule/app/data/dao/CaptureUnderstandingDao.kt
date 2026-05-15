package com.capsule.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.capsule.app.data.entity.CanonicalUrlEntity
import com.capsule.app.data.entity.CaptureUnderstandingEntity
import com.capsule.app.data.entity.CorrectionFeedbackEntity
import com.capsule.app.data.entity.DeletionInvalidationEntity
import com.capsule.app.data.entity.EvidenceBundleEntity
import com.capsule.app.data.entity.SourceIdentityEntity
import com.capsule.app.data.entity.UnderstandingDepthPolicyOverrideEntity
import com.capsule.app.data.entity.UnderstandingJobEntity
import com.capsule.app.understanding.UnderstandingJobStatus

@Dao
interface CaptureUnderstandingDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSourceIdentity(entity: SourceIdentityEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCanonicalUrl(entity: CanonicalUrlEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEvidenceBundle(entity: EvidenceBundleEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUnderstandingJob(entity: UnderstandingJobEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCaptureUnderstanding(entity: CaptureUnderstandingEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCorrectionFeedback(entity: CorrectionFeedbackEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDeletionInvalidation(entity: DeletionInvalidationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDepthPolicyOverride(entity: UnderstandingDepthPolicyOverrideEntity)

    @Transaction
    @Query(
        """
        SELECT * FROM source_identity
        WHERE captureId = :captureId
          AND invalidatedAt IS NULL
          AND supersededAt IS NULL
        ORDER BY version DESC, createdAt DESC
        LIMIT 1
        """
    )
    suspend fun currentSourceIdentity(captureId: String): SourceIdentityEntity?

    @Transaction
    @Query(
        """
        SELECT * FROM capture_understanding
        WHERE captureId = :captureId
          AND invalidatedAt IS NULL
          AND supersededAt IS NULL
        ORDER BY version DESC, producedAt DESC
        LIMIT 1
        """
    )
    suspend fun currentUnderstanding(captureId: String): CaptureUnderstandingEntity?

    @Query(
        """
        SELECT * FROM evidence_bundle
        WHERE captureId = :captureId
          AND invalidatedAt IS NULL
          AND status != 'INVALIDATED'
        ORDER BY createdAt DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun evidencePage(captureId: String, limit: Int, offset: Int): List<EvidenceBundleEntity>

    @Query(
        """
        SELECT COUNT(*) FROM evidence_bundle
        WHERE captureId = :captureId
          AND invalidatedAt IS NULL
          AND status != 'INVALIDATED'
        """
    )
    suspend fun evidenceCount(captureId: String): Int

    @Query(
        """
        SELECT * FROM understanding_depth_policy_override
        WHERE captureId = :captureId
          AND scope = 'CAPTURE_OVERRIDE'
        ORDER BY updatedAt DESC
        LIMIT 1
        """
    )
    suspend fun latestCaptureOverride(captureId: String): UnderstandingDepthPolicyOverrideEntity?

    @Query(
        """
        SELECT * FROM understanding_depth_policy_override
        WHERE domainSuppressionKey = :domainSuppressionKey
          AND scope = 'DOMAIN_SUPPRESSION'
        ORDER BY updatedAt DESC
        LIMIT 1
        """
    )
    suspend fun latestDomainSuppression(domainSuppressionKey: String): UnderstandingDepthPolicyOverrideEntity?

    @Query(
        """
        UPDATE understanding_job
        SET status = :status,
            attemptCount = :attemptCount,
            traceIdsJson = :traceIdsJson,
            failureCode = :failureCode,
            userVisibleReason = :userVisibleReason,
            startedAt = COALESCE(startedAt, :startedAt),
            finishedAt = :finishedAt
        WHERE id = :jobId
        """
    )
    suspend fun updateJobStatus(
        jobId: String,
        status: UnderstandingJobStatus,
        attemptCount: Int,
        traceIdsJson: String,
        failureCode: String?,
        userVisibleReason: String?,
        startedAt: Long?,
        finishedAt: Long?
    ): Int

    @Query(
        """
        UPDATE source_identity SET invalidatedAt = :invalidatedAt
        WHERE captureId = :captureId AND invalidatedAt IS NULL
        """
    )
    suspend fun invalidateSourceIdentities(captureId: String, invalidatedAt: Long): Int

    @Query(
        """
        UPDATE canonical_url
        SET invalidatedAt = :invalidatedAt,
            isEligibleForDuplicateMatching = 0
        WHERE captureId = :captureId AND invalidatedAt IS NULL
        """
    )
    suspend fun invalidateCanonicalUrls(captureId: String, invalidatedAt: Long): Int

    @Query(
        """
        UPDATE evidence_bundle SET invalidatedAt = :invalidatedAt, status = 'INVALIDATED'
        WHERE captureId = :captureId AND invalidatedAt IS NULL
        """
    )
    suspend fun invalidateEvidence(captureId: String, invalidatedAt: Long): Int

    @Query(
        """
        UPDATE understanding_job SET invalidatedAt = :invalidatedAt, status = 'INVALIDATED'
        WHERE captureId = :captureId AND invalidatedAt IS NULL
        """
    )
    suspend fun invalidateJobs(captureId: String, invalidatedAt: Long): Int

    @Query(
        """
        UPDATE capture_understanding SET invalidatedAt = :invalidatedAt, status = 'INVALIDATED'
        WHERE captureId = :captureId AND invalidatedAt IS NULL
        """
    )
    suspend fun invalidateUnderstandings(captureId: String, invalidatedAt: Long): Int
}