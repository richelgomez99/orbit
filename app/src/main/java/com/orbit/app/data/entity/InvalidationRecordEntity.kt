package com.orbit.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Spec 004 — tombstone written when understanding is invalidated.
 * reason must be one of: CAPTURE_DELETED, USER_REQUESTED, CORRECTION_APPLIED.
 * Checked by [com.orbit.app.understanding.engine.InvalidationGuard].
 */
@Entity(tableName = "invalidation_record")
data class InvalidationRecordEntity(
    @PrimaryKey val captureId: String,
    val invalidatedAt: Long,
    /** CAPTURE_DELETED | USER_REQUESTED | CORRECTION_APPLIED */
    val reason: String
)
