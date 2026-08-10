package com.orbit.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.orbit.app.data.entity.EvidenceBundleEntity

@Dao
interface EvidenceBundleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: EvidenceBundleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<EvidenceBundleEntity>)

    @Query("SELECT * FROM evidence_bundle WHERE captureId = :captureId ORDER BY createdAt ASC")
    suspend fun getByCaptureId(captureId: String): List<EvidenceBundleEntity>

    @Query("DELETE FROM evidence_bundle WHERE captureId = :captureId")
    suspend fun deleteByCaptureId(captureId: String): Int
}
