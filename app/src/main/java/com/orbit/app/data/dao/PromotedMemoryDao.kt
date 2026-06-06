package com.orbit.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.orbit.app.data.entity.PromotedMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PromotedMemoryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(memory: PromotedMemoryEntity)

    @Query("SELECT * FROM promoted_memory WHERE id = :id")
    suspend fun getById(id: String): PromotedMemoryEntity?

    @Query("SELECT * FROM promoted_memory WHERE candidateId = :candidateId LIMIT 1")
    suspend fun findByCandidateId(candidateId: String): PromotedMemoryEntity?

    @Query(
        """
        SELECT * FROM promoted_memory
        WHERE state = 'ACTIVE'
        ORDER BY updatedAt DESC
        LIMIT :limit
        """
    )
    fun observeActive(limit: Int): Flow<List<PromotedMemoryEntity>>

    @Query(
        """
        SELECT
            m.id AS memoryId,
            m.memoryKind AS memoryKind,
            m.state AS state,
            m.displayLabel AS displayLabel,
            m.subject AS subject,
            m.predicate AS predicate,
            m.objectValue AS objectValue,
            m.confidenceLabel AS confidenceLabel,
            m.sensitivity AS sensitivity,
            COUNT(DISTINCT s.envelopeId) AS sourceCount,
            m.lastUsedAt AS lastUsedAtMillis,
            m.useCount AS useCount,
            m.updatedAt AS updatedAtMillis
        FROM promoted_memory m
        LEFT JOIN promoted_memory_support s ON s.memoryId = m.id
        WHERE m.state = 'ACTIVE'
        GROUP BY m.id
        ORDER BY m.updatedAt DESC
        LIMIT :limit
        """
    )
    fun observeActiveProjections(limit: Int): Flow<List<PromotedMemoryProjection>>

    @Query(
        """
        UPDATE promoted_memory
        SET state = 'INVALIDATED',
            updatedAt = :at,
            invalidatedAt = :at
        WHERE state = 'ACTIVE'
            AND NOT EXISTS (
                SELECT 1
                FROM promoted_memory_support
                WHERE promoted_memory_support.memoryId = promoted_memory.id
            )
        """
    )
    suspend fun invalidateSourceLessActive(at: Long): Int
}

data class PromotedMemoryProjection(
    val memoryId: String,
    val memoryKind: String,
    val state: String,
    val displayLabel: String,
    val subject: String,
    val predicate: String,
    val objectValue: String,
    val confidenceLabel: String,
    val sensitivity: String,
    val sourceCount: Int,
    val lastUsedAtMillis: Long?,
    val useCount: Int,
    val updatedAtMillis: Long
)
