package com.capsule.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.capsule.app.understanding.CanonicalUrlRole

@Entity(
    tableName = "canonical_url",
    foreignKeys = [
        ForeignKey(
            entity = IntentEnvelopeEntity::class,
            parentColumns = ["id"],
            childColumns = ["captureId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("captureId"),
        Index("canonicalUrlHash"),
        Index(value = ["captureId", "role", "invalidatedAt"]),
        Index(value = ["canonicalUrlHash", "isEligibleForDuplicateMatching"]),
        Index("host")
    ]
)
data class CanonicalUrlEntity(
    @PrimaryKey val id: String,
    val captureId: String,
    val originalUrl: String,
    val normalizedUrl: String,
    val canonicalUrlHash: String,
    val role: CanonicalUrlRole,
    val providerFamily: String?,
    val host: String?,
    val normalizationVersion: Int,
    val detectedAt: Long,
    val hydratedAt: Long? = null,
    val isEligibleForDuplicateMatching: Boolean = true,
    val invalidatedAt: Long? = null
)