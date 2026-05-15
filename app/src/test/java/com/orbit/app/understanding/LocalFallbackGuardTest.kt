package com.orbit.app.understanding

import com.orbit.app.understanding.engine.LocalFallbackGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalFallbackGuardTest {

    @Test
    fun checkState_offlineReturnsCloudUnavailable() {
        val result = LocalFallbackGuard().checkState(isOnline = false, isCloudEnabled = true)

        assertEquals("offline", result?.reason)
        assertEquals("cloud-unavailable", result?.groundingConstraint)
    }

    @Test
    fun checkState_cloudDisabledReturnsCloudUnavailable() {
        val result = LocalFallbackGuard().checkState(isOnline = true, isCloudEnabled = false)

        assertEquals("cloud-disabled", result?.reason)
        assertEquals("cloud-unavailable", result?.groundingConstraint)
    }

    @Test
    fun checkState_onlineAndCloudEnabledAllowsEscalation() {
        assertNull(LocalFallbackGuard().checkState(isOnline = true, isCloudEnabled = true))
    }
}
