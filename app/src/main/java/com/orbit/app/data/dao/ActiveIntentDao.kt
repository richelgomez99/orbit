package com.orbit.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.orbit.app.data.entity.ActiveIntentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveIntentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ActiveIntentEntity)

    @Query("SELECT * FROM active_intent WHERE intentId = :intentId")
    suspend fun getById(intentId: String): ActiveIntentEntity?

    @Query("SELECT * FROM active_intent WHERE captureId = :captureId ORDER BY createdAt ASC")
    suspend fun getByCaptureId(captureId: String): List<ActiveIntentEntity>

    @Query("SELECT * FROM active_intent WHERE status = 'ACTIVE' ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<ActiveIntentEntity>>

    @Query("SELECT * FROM active_intent WHERE status IN ('ACTIVE', 'EXPIRED') ORDER BY updatedAt DESC")
    fun observeCleanupQueue(): Flow<List<ActiveIntentEntity>>

    @Query("SELECT * FROM active_intent WHERE status = 'ACTIVE' AND intentId LIKE 'basic:%' ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getActiveBasic(limit: Int): List<ActiveIntentEntity>

    @Query("UPDATE active_intent SET status = :status, resolutionReason = :resolutionReason, resolvedAt = :resolvedAt, userConfirmed = :userConfirmed, updatedAt = :updatedAt WHERE intentId = :intentId")
    suspend fun markResolved(
        intentId: String,
        status: String,
        resolutionReason: String?,
        resolvedAt: Long?,
        userConfirmed: Boolean,
        updatedAt: Long
    ): Int

    @Query("UPDATE active_intent SET status = 'INVALIDATED', resolutionReason = :resolutionReason, resolvedAt = :invalidatedAt, updatedAt = :invalidatedAt WHERE captureId = :captureId")
    suspend fun markInvalidatedForCapture(captureId: String, invalidatedAt: Long, resolutionReason: String): Int

    @Query("DELETE FROM active_intent WHERE captureId = :captureId")
    suspend fun deleteByCaptureId(captureId: String): Int
}