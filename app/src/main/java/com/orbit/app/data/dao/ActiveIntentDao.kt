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

    @Query("SELECT * FROM active_intent WHERE status = 'ACTIVE' ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<ActiveIntentEntity>>

    @Query("SELECT * FROM active_intent WHERE captureId = :captureId ORDER BY updatedAt DESC")
    suspend fun getByCapture(captureId: String): List<ActiveIntentEntity>

    @Query("SELECT * FROM active_intent WHERE intentId = :intentId")
    suspend fun getById(intentId: String): ActiveIntentEntity?

    @Query(
        """
        UPDATE active_intent
        SET status = :status,
            resolutionReason = :resolutionReason,
            resolvedAt = :resolvedAt,
            userConfirmed = :userConfirmed,
            updatedAt = :updatedAt
        WHERE intentId = :intentId
        """
    )
    suspend fun transition(
        intentId: String,
        status: String,
        resolutionReason: String?,
        resolvedAt: Long?,
        userConfirmed: Boolean,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE active_intent
        SET status = 'INVALIDATED',
            resolutionReason = :reason,
            resolvedAt = :invalidatedAt,
            userConfirmed = 0,
            updatedAt = :invalidatedAt
        WHERE captureId = :captureId AND status != 'INVALIDATED'
        """
    )
    suspend fun invalidateByCapture(captureId: String, reason: String, invalidatedAt: Long)

    @Query("DELETE FROM active_intent WHERE captureId = :captureId")
    suspend fun deleteByCapture(captureId: String)
}
