package com.orbit.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.orbit.app.data.entity.InvalidationRecordEntity

@Dao
interface InvalidationRecordDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: InvalidationRecordEntity)

    @Query("SELECT * FROM invalidation_record WHERE captureId = :captureId")
    suspend fun getById(captureId: String): InvalidationRecordEntity?

    @Query("SELECT COUNT(*) > 0 FROM invalidation_record WHERE captureId = :captureId")
    suspend fun existsForCapture(captureId: String): Boolean
}
