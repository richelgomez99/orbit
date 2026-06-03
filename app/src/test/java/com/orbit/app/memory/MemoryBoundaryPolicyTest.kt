package com.orbit.app.memory

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MemoryBoundaryPolicyTest {
    @Test
    fun productionAndroidSourcesDoNotContainAtlasCredentialsOrDrivers() {
        val mainSource = locateAppMainSource()
        val banned = listOf(
            "MONGO" + "DB_ATLAS_URI",
            "mongo" + "db+srv://",
            "Mongo" + "Client",
            "mongo" + "db-driver",
            "org." + "mongodb",
        )
        val offenders = mainSource
            .walkTopDown()
            .filter { it.isFile }
            .filterNot { it.extension in setOf("png", "jpg", "jpeg", "webp", "avif") }
            .flatMap { file ->
                val text = runCatching { file.readText() }.getOrDefault("")
                banned
                    .filter { token -> text.contains(token, ignoreCase = true) }
                    .map { token -> "${file.relativeTo(mainSource)} contains $token" }
            }
            .toList()

        assertTrue(
            "Production Android source must not contain Atlas credentials, direct connection strings, or drivers: $offenders",
            offenders.isEmpty(),
        )
    }

    private fun locateAppMainSource(): File {
        val cwd = File(requireNotNull(System.getProperty("user.dir")))
        val candidates = listOf(
            File(cwd, "src/main"),
            File(cwd, "app/src/main"),
            File(cwd.parentFile ?: cwd, "app/src/main"),
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Could not locate app/src/main from ${cwd.absolutePath}")
    }
}
