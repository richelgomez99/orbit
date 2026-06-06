package com.orbit.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.orbit.app.data.entity.MemoryCandidateSupportEntity

@Dao
interface MemoryCandidateSupportDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(supports: List<MemoryCandidateSupportEntity>)

    @Query("SELECT * FROM memory_candidate_support WHERE candidateId = :candidateId")
    suspend fun listForCandidate(candidateId: String): List<MemoryCandidateSupportEntity>

    @Query("SELECT COUNT(*) FROM memory_candidate_support WHERE candidateId = :candidateId")
    suspend fun countForCandidate(candidateId: String): Int
}
