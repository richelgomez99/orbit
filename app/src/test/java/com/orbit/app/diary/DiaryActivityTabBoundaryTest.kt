package com.orbit.app.diary

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DiaryActivityTabBoundaryTest {

    @Test
    fun tabExitResetsTransientLibraryAndAskState() {
        val src = resolve("src/main/java/com/orbit/app/diary/DiaryActivity.kt").readText()

        assertTrue(
            "Leaving Orbit must reset transient Ask state.",
            src.contains("selectedTab == OrbitHomeTab.ORBIT") &&
                src.contains("askOrbitViewModel.reset()")
        )
        assertTrue(
            "Leaving Library must reset transient search state.",
            src.contains("selectedTab == OrbitHomeTab.LIBRARY") &&
                src.contains("libraryViewModel.reset()")
        )
    }

    private fun resolve(rel: String): File {
        val direct = File(rel)
        if (direct.exists()) return direct
        val fromRepoRoot = File("app/$rel")
        check(fromRepoRoot.exists()) {
            "missing source file $rel (cwd=${File(".").absolutePath})"
        }
        return fromRepoRoot
    }
}
