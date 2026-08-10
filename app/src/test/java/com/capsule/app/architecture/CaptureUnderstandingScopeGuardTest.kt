package com.capsule.app.architecture

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CaptureUnderstandingScopeGuardTest {
    @Test
    fun captureUnderstandingDoesNotCreateDeferredFeaturePackages() {
        val root = File("src/main/java/com/capsule/app")
        val forbiddenPathParts = listOf(
            "/ask/",
            "/retrieval/",
            "/knowledgegraph/",
            "/kg/",
            "/memory/",
            "/agent/",
            "/browser/",
            "/automation/"
        )
        val violations = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .map { it.invariantSeparatorsPath }
            .filter { path -> forbiddenPathParts.any { part -> part in path.lowercase() } }
            .toList()

        assertTrue("004 must not create deferred Ask/KG/memory/agent/browser packages: $violations", violations.isEmpty())
    }

    @Test
    fun captureUnderstandingDoesNotIntroduceDeferredFeatureClassNames() {
        val root = File("src/main/java/com/capsule/app")
        val forbiddenNames = Regex(
            "\\b(AskOrbit|RetrievalRanker|CitationRenderer|KnowledgeGraph|MemoryInspector|AgentCoordinator|MultiAgent|BrowserAutomation|GenericBrowserAutomation)\\b"
        )
        val violations = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { forbiddenNames.containsMatchIn(it.readText()) }
            .map { it.invariantSeparatorsPath }
            .toList()

        assertTrue("004 must not implement deferred feature classes: $violations", violations.isEmpty())
    }
}