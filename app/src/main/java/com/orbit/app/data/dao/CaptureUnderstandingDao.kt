package com.orbit.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.orbit.app.data.entity.CaptureUnderstandingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureUnderstandingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CaptureUnderstandingEntity)

    @Query("SELECT * FROM capture_understanding WHERE captureId = :captureId")
    fun getByCapture(captureId: String): Flow<CaptureUnderstandingEntity?>

    @Query(
        """
        SELECT * FROM capture_understanding
        WHERE contentHashHex = :hex AND invalidatedAt IS NULL
        """
    )
    suspend fun getByContentHash(hex: String): List<CaptureUnderstandingEntity>

    @Query(
        """
        SELECT * FROM capture_understanding
        WHERE canonicalUrl = :canonicalUrl AND invalidatedAt IS NULL
        """
    )
    suspend fun getByCanonicalUrl(canonicalUrl: String): List<CaptureUnderstandingEntity>

    @Query(
        """
        UPDATE capture_understanding
        SET invalidatedAt = :invalidatedAt, updatedAt = :invalidatedAt
        WHERE captureId = :captureId
        """
    )
    suspend fun markInvalidated(captureId: String, invalidatedAt: Long)

    @Query("DELETE FROM capture_understanding WHERE captureId = :captureId")
    suspend fun deleteByCapture(captureId: String)
}
