package com.orbit.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.orbit.app.data.model.MemorySupportType

@Entity(
    tableName = "promoted_memory_support",
    primaryKeys = ["memoryId", "envelopeId", "supportType"],
    foreignKeys = [
        ForeignKey(
            entity = PromotedMemoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["memoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = IntentEnvelopeEntity::class,
            parentColumns = ["id"],
            childColumns = ["envelopeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["memoryId"]),
        Index(value = ["envelopeId"])
    ]
)
data class PromotedMemorySupportEntity(
    val memoryId: String,
    val envelopeId: String,
    val supportType: MemorySupportType,
    val evidenceId: String?,
    val createdAt: Long
)
