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

    @Test
    fun askAndActionSurfacesDoNotReadMemoryCandidateReviewTables() {
        val appMain = locateAppMainSource()
        val roots = listOf(
            File(appMain, "java/com/orbit/app/orbit"),
            File(appMain, "java/com/orbit/app/action"),
            File(appMain, "java/com/orbit/app/ai/extract"),
        )
        val offenders = roots.flatMap { root ->
            scanForReviewMemoryTokens(root)
        }

        assertTrue(
            "Ask/action code must not treat pending or rejected memory candidates as facts: $offenders",
            offenders.isEmpty(),
        )
    }

    @Test
    fun compactMemoryIndexCodeDoesNotReadMemoryCandidateReviewTables() {
        val appMain = locateAppMainSource()
        val offenders = scanForReviewMemoryTokens(File(appMain, "java/com/orbit/app/memory"))

        assertTrue(
            "Compact memory/Atlas sync must not read candidate review tables before Spec 008/009 policy: $offenders",
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

    private fun scanForReviewMemoryTokens(root: File): List<String> {
        if (!root.isDirectory) return emptyList()
        val banned = listOf(
            "Memory" + "Candidate",
            "Promoted" + "Memory",
            "memory" + "_candidate",
            "promoted" + "_memory",
            "memory" + "CandidateDao",
            "promoted" + "MemoryDao",
        )
        return root
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.name == "MemoryPayloadCaps.kt" }
            .flatMap { file ->
                val text = runCatching { file.readText() }.getOrDefault("")
                banned
                    .filter { token -> text.contains(token) }
                    .map { token -> "${file.relativeTo(root)} contains $token" }
            }
            .toList()
    }
}
