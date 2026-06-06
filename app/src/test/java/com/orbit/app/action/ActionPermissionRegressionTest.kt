package com.orbit.app.action

import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class ActionPermissionRegressionTest {

    @Test
    fun actionRuntimeDoesNotAddCalendarContactsOrStorageWritePermissions() {
        val manifest = File(
            File(requireNotNull(System.getProperty("user.dir"))),
            "src/main/AndroidManifest.xml"
        ).readText()

        val forbidden = listOf(
            "android.permission.READ_CALENDAR",
            "android.permission.WRITE_CALENDAR",
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.WRITE_EXTERNAL_STORAGE"
        )
        forbidden.forEach { permission ->
            assertFalse(
                "$permission must not be requested by the action runtime.",
                manifest.contains(permission)
            )
        }
    }
}
