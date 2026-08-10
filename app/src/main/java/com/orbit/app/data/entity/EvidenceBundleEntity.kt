package com.orbit.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "evidence_bundle",
    foreignKeys = [
        ForeignKey(
            entity = IntentEnvelopeEntity::class,
            parentColumns = ["id"],
            childColumns = ["captureId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["captureId"]),
        Index(value = ["bundleType"])
    ]
)
data class EvidenceBundleEntity(
    @PrimaryKey val id: String,
    val captureId: String,
    val bundleType: String,
    val payloadJson: String,
    val createdAt: Long
)
