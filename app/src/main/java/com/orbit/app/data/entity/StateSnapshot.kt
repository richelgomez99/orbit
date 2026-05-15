package com.orbit.app.data.entity

import com.orbit.app.data.model.AppCategory
import com.orbit.app.data.model.ActivityState

/** Embedded snapshot of device state at capture time. Immutable once sealed. */
data class StateSnapshot(
    val appCategory: AppCategory,
    val activityState: ActivityState,
    val tzId: String,
    val hourLocal: Int,
    val dayOfWeekLocal: Int,
    val sourceAppLabel: String? = null
)
