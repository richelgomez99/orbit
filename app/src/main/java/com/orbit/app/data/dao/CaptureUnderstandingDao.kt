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
    fun observeByCaptureId(captureId: String): Flow<CaptureUnderstandingEntity?>

    @Query("SELECT * FROM capture_understanding WHERE captureId = :captureId")
    suspend fun getByCaptureId(captureId: String): CaptureUnderstandingEntity?

    @Query("SELECT * FROM capture_understanding WHERE contentHashHex = :hex ORDER BY createdAt ASC")
    suspend fun getByContentHash(hex: String): List<CaptureUnderstandingEntity>

    @Query("SELECT * FROM capture_understanding WHERE canonicalUrl = :canonicalUrl ORDER BY createdAt ASC")
    suspend fun getByCanonicalUrl(canonicalUrl: String): List<CaptureUnderstandingEntity>

    @Query("UPDATE capture_understanding SET invalidatedAt = :invalidatedAt, updatedAt = :invalidatedAt WHERE captureId = :captureId")
    suspend fun markInvalidated(captureId: String, invalidatedAt: Long): Int

    @Query("DELETE FROM capture_understanding WHERE captureId = :captureId")
    suspend fun deleteByCaptureId(captureId: String): Int
}
