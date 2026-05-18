package com.orbit.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Spec 004 — persisted understanding result for a single capture.
 * captureId is a 1:1 FK to [IntentEnvelopeEntity]; set invalidatedAt
 * via [com.orbit.app.understanding.engine.InvalidationService] when the
 * capture is deleted or user-requested.
 */
@Entity(
    tableName = "capture_understanding",
    foreignKeys = [
        ForeignKey(
            entity = IntentEnvelopeEntity::class,
            parentColumns = ["id"],
            childColumns = ["captureId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CaptureUnderstandingEntity(
    @PrimaryKey val captureId: String,
    /** [com.orbit.app.understanding.domain.UnderstandingMode] name. */
    val mode: String,
    /** [com.orbit.app.understanding.domain.UnderstandingStatus] name. */
    val status: String,
    val title: String?,
    val summaryText: String?,
    /** JSON-encoded [com.orbit.app.understanding.domain.GroundingConstraints]. */
    val groundingConstraintsJson: String,
    /** SHA-256 hex of the captured artifact bytes (URL bytes for URL captures). */
    val contentHashHex: String?,
    val canonicalUrl: String?,
    /** JSON-encoded [com.orbit.app.understanding.domain.SourceIdentity]. */
    val sourceIdentityJson: String?,
    val createdAt: Long,
    val updatedAt: Long,
    /** Non-null when understanding has been tombstoned by [com.orbit.app.understanding.engine.InvalidationService]. */
    val invalidatedAt: Long?
)
