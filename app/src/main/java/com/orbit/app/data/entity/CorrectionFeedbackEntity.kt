package com.orbit.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Spec 004 — user-submitted correction for a capture understanding result.
 * feedbackType must be one of: WRONG_SOURCE, WRONG_SUMMARY, BAD_RELEVANCE.
 */
@Entity(
    tableName = "correction_feedback",
    indices = [Index(value = ["captureId"])]
)
data class CorrectionFeedbackEntity(
    @PrimaryKey val id: String,
    val captureId: String,
    /** WRONG_SOURCE | WRONG_SUMMARY | BAD_RELEVANCE */
    val feedbackType: String,
    val note: String?,
    val createdAt: Long
)
