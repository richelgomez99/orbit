package com.orbit.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.orbit.app.data.entity.MemoryCandidateEntity
import com.orbit.app.data.model.MemoryCandidateState
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryCandidateDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(candidate: MemoryCandidateEntity): Long

    @Query("SELECT * FROM memory_candidate WHERE id = :id")
    suspend fun getById(id: String): MemoryCandidateEntity?

    @Query(
        """
        SELECT * FROM memory_candidate
        WHERE state IN ('PENDING', 'ASKED')
        ORDER BY createdAt ASC
        LIMIT :limit
        """
    )
    fun observePending(limit: Int): Flow<List<MemoryCandidateEntity>>

    @Query(
        """
        SELECT
            c.id AS candidateId,
            c.candidateKind AS candidateKind,
            c.state AS state,
            c.displayLabel AS displayLabel,
            c.subject AS subject,
            c.predicate AS predicate,
            c.objectValue AS objectValue,
            c.confidence AS confidence,
            c.sensitivity AS sensitivity,
            c.askUserCopy AS askUserCopy,
            c.createdAt AS createdAtMillis,
            c.updatedAt AS updatedAtMillis,
            COUNT(DISTINCT s.envelopeId) AS sourceCount,
            MIN(s.envelopeId) AS primarySourceEnvelopeId,
            CASE
                WHEN e.contentType = 'IMAGE' THEN 'Screenshot'
                WHEN e.textContent IS NULL OR TRIM(e.textContent) = '' THEN 'Saved capture'
                WHEN LENGTH(e.textContent) > 120 THEN SUBSTR(e.textContent, 1, 120) || '...'
                ELSE e.textContent
            END AS primarySourceTitle,
            e.day_local AS primarySourceDayLocal
        FROM memory_candidate c
        LEFT JOIN memory_candidate_support s ON s.candidateId = c.id
        LEFT JOIN intent_envelope e ON e.id = s.envelopeId
        WHERE c.state IN ('PENDING', 'ASKED')
        GROUP BY c.id
        ORDER BY c.createdAt ASC
        LIMIT :limit
        """
    )
    fun observePendingProjections(limit: Int): Flow<List<MemoryCandidateProjection>>

    @Query(
        """
        SELECT * FROM memory_candidate
        WHERE candidateKind = :candidateKind
            AND subject = :subject
            AND predicate = :predicate
            AND objectValue = :objectValue
            AND state IN ('PENDING', 'ASKED')
        LIMIT 1
        """
    )
    suspend fun findActiveDuplicate(
        candidateKind: String,
        subject: String,
        predicate: String,
        objectValue: String
    ): MemoryCandidateEntity?

    @Query(
        """
        UPDATE memory_candidate
        SET state = :state,
            updatedAt = :at,
            decidedAt = :at,
            decisionReason = :reason
        WHERE id = :id
            AND state IN ('PENDING', 'ASKED')
        """
    )
    suspend fun markTerminal(
        id: String,
        state: MemoryCandidateState,
        reason: String?,
        at: Long
    ): Int

    @Query(
        """
        UPDATE memory_candidate
        SET state = 'INVALIDATED',
            updatedAt = :at,
            decidedAt = :at,
            decisionReason = :reason
        WHERE state IN ('PENDING', 'ASKED')
            AND NOT EXISTS (
                SELECT 1
                FROM memory_candidate_support
                WHERE memory_candidate_support.candidateId = memory_candidate.id
            )
        """
    )
    suspend fun invalidateSourceLessPending(reason: String, at: Long): Int
}

data class MemoryCandidateProjection(
    val candidateId: String,
    val candidateKind: String,
    val state: String,
    val displayLabel: String,
    val subject: String,
    val predicate: String,
    val objectValue: String,
    val confidence: Float,
    val sensitivity: String,
    val askUserCopy: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val sourceCount: Int,
    val primarySourceEnvelopeId: String?,
    val primarySourceTitle: String?,
    val primarySourceDayLocal: String?
)
