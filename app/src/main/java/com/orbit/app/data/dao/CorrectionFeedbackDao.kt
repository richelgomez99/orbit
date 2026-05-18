package com.orbit.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.orbit.app.data.entity.CorrectionFeedbackEntity

@Dao
interface CorrectionFeedbackDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: CorrectionFeedbackEntity)

    @Query("SELECT * FROM correction_feedback WHERE captureId = :captureId ORDER BY createdAt ASC")
    suspend fun getByCaptureId(captureId: String): List<CorrectionFeedbackEntity>
}
