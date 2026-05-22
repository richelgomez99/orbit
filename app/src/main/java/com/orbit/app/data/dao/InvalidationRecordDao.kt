package com.orbit.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.orbit.app.data.entity.InvalidationRecordEntity

@Dao
interface InvalidationRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: InvalidationRecordEntity)

    @Query("SELECT * FROM invalidation_record WHERE captureId = :captureId")
    suspend fun getByCaptureId(captureId: String): InvalidationRecordEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM invalidation_record WHERE captureId = :captureId)")
    suspend fun existsForCapture(captureId: String): Boolean
}
