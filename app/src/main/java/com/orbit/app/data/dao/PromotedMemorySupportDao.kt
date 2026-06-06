package com.orbit.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.orbit.app.data.entity.PromotedMemorySupportEntity

@Dao
interface PromotedMemorySupportDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(supports: List<PromotedMemorySupportEntity>)

    @Query("SELECT * FROM promoted_memory_support WHERE memoryId = :memoryId")
    suspend fun listForMemory(memoryId: String): List<PromotedMemorySupportEntity>

    @Query("SELECT COUNT(*) FROM promoted_memory_support WHERE memoryId = :memoryId")
    suspend fun countForMemory(memoryId: String): Int
}
