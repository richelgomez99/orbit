package com.orbit.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.orbit.app.data.entity.ActionProposalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionProposalDao {

    /**
     * Inserts a freshly extracted proposal. The `(envelopeId, functionId)`
     * unique index dedupes concurrent extractor runs; on conflict we
     * silently no-op (re-extraction idempotency, T094 + quickstart §6 N6).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(proposal: ActionProposalEntity): Long

    @Query("SELECT * FROM action_proposal WHERE id = :id")
    suspend fun getById(id: String): ActionProposalEntity?

    @Query(
        """
        SELECT * FROM action_proposal
        WHERE envelopeId = :envelopeId AND functionId = :functionId
        LIMIT 1
        """
    )
    suspend fun findByEnvelopeAndFunction(envelopeId: String, functionId: String): ActionProposalEntity?

    /** Live feed of non-terminal proposals for an envelope, ordered by extraction time. */
    @Query(
        """
        SELECT * FROM action_proposal
        WHERE envelopeId = :envelopeId AND state = 'PROPOSED'
        ORDER BY createdAt ASC
        """
    )
    fun observeProposedForEnvelope(envelopeId: String): Flow<List<ActionProposalEntity>>

    /** All proposals, any state, for the envelope. Used by audit + diagnostics. */
    @Query("SELECT * FROM action_proposal WHERE envelopeId = :envelopeId ORDER BY createdAt ASC")
    suspend fun listAllForEnvelope(envelopeId: String): List<ActionProposalEntity>

    @Query("UPDATE action_proposal SET state = 'DISMISSED', stateChangedAt = :at WHERE id = :id AND state = 'PROPOSED'")
    suspend fun markDismissed(id: String, at: Long): Int

    @Query("UPDATE action_proposal SET state = 'CONFIRMED', stateChangedAt = :at WHERE id = :id AND state = 'PROPOSED'")
    suspend fun markConfirmed(id: String, at: Long): Int

    @Query("UPDATE action_proposal SET state = 'INVALIDATED', stateChangedAt = :at WHERE id = :id")
    suspend fun markInvalidated(id: String, at: Long): Int

    @Query("SELECT COUNT(*) FROM action_proposal WHERE envelopeId = :envelopeId AND state = 'PROPOSED'")
    suspend fun countProposedForEnvelope(envelopeId: String): Int

    @Query(
        """
        SELECT
            p.id AS proposalId,
            p.envelopeId AS sourceEnvelopeId,
            p.functionId AS functionId,
            p.schemaVersion AS schemaVersion,
            p.argsJson AS argsJson,
            p.previewTitle AS previewTitle,
            p.previewSubtitle AS previewSubtitle,
            p.confidence AS confidence,
            p.provenance AS provenance,
            p.state AS state,
            p.sensitivityScope AS sensitivityScope,
            p.createdAt AS createdAtMillis,
            p.stateChangedAt AS stateChangedAtMillis,
            COALESCE(s.displayName, p.functionId) AS displayName,
            COALESCE(s.sideEffects, '') AS sideEffects,
            COALESCE(s.reversibility, '') AS reversibility,
            CASE
                WHEN e.contentType = 'IMAGE' THEN 'Screenshot'
                WHEN e.textContent IS NULL OR TRIM(e.textContent) = '' THEN 'Saved capture'
                WHEN LENGTH(e.textContent) > 120 THEN SUBSTR(e.textContent, 1, 120) || '...'
                ELSE e.textContent
            END AS sourceTitle,
            e.sourceAppLabel AS sourceAppLabel,
            e.day_local AS sourceDayLocal
        FROM action_proposal p
        INNER JOIN intent_envelope e ON e.id = p.envelopeId
        LEFT JOIN appfunction_skill s
            ON s.functionId = p.functionId AND s.schemaVersion = p.schemaVersion
        WHERE p.state = 'PROPOSED'
            AND e.isArchived = 0
            AND e.isDeleted = 0
        ORDER BY p.createdAt ASC
        LIMIT :limit
        """
    )
    fun observePendingDrafts(limit: Int): Flow<List<ActionDraftProjection>>
}

data class ActionDraftProjection(
    val proposalId: String,
    val sourceEnvelopeId: String,
    val functionId: String,
    val schemaVersion: Int,
    val argsJson: String,
    val previewTitle: String,
    val previewSubtitle: String?,
    val confidence: Float,
    val provenance: String,
    val state: String,
    val sensitivityScope: String,
    val createdAtMillis: Long,
    val stateChangedAtMillis: Long,
    val displayName: String,
    val sideEffects: String,
    val reversibility: String,
    val sourceTitle: String,
    val sourceAppLabel: String?,
    val sourceDayLocal: String
)
