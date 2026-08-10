package com.orbit.app.service

import com.orbit.app.data.ipc.StateSnapshotParcel
import com.orbit.app.data.model.ActivityState
import com.orbit.app.data.model.AppCategory
import com.orbit.app.overlay.CapturedContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CaptureStateSnapshotSelectionTest {

    @Test
    fun stateSnapshotForSeal_prefersTapTimeSnapshot() {
        val tapSnapshot = snapshot(AppCategory.BROWSER)
        var fallbackCalled = false
        val content = CapturedContent(
            text = "https://example.com",
            sourcePackage = null,
            timestamp = 123L,
            isSensitive = false,
            stateSnapshotAtCapture = tapSnapshot
        )

        val selected = content.stateSnapshotForSeal {
            fallbackCalled = true
            snapshot(AppCategory.OTHER)
        }

        assertEquals(tapSnapshot, selected)
        assertFalse("fallback must not run when tap-time snapshot exists", fallbackCalled)
    }

    @Test
    fun stateSnapshotForSeal_usesFallbackWhenTapTimeSnapshotMissing() {
        val fallbackSnapshot = snapshot(AppCategory.UNKNOWN_SOURCE)
        val content = CapturedContent(
            text = "note",
            sourcePackage = null,
            timestamp = 123L,
            isSensitive = false
        )

        val selected = content.stateSnapshotForSeal { fallbackSnapshot }

        assertEquals(fallbackSnapshot, selected)
    }

    private fun snapshot(category: AppCategory): StateSnapshotParcel = StateSnapshotParcel(
        appCategory = category.name,
        activityState = ActivityState.UNKNOWN.name,
        tzId = "UTC",
        hourLocal = 12,
        dayOfWeekLocal = 1
    )
}