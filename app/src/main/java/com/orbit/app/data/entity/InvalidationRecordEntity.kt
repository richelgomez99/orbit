package com.orbit.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invalidation_record")
data class InvalidationRecordEntity(
    @PrimaryKey val captureId: String,
    val invalidatedAt: Long,
    val reason: String
)
