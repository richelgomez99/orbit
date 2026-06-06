package com.orbit.app.action

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ActionExecutorServicePreflightRegressionTest {

    @Test
    fun executeRejectsUnknownSkillBeforeHandlerDispatch() {
        val src = source("src/main/java/com/orbit/app/action/ActionExecutorService.kt")

        assertOccursBefore(
            src,
            "reason = \"unknown_skill\"",
            "handler.handle(this@ActionExecutorService, skill, request.argsJson, repo)",
            "unknown_skill must be recorded before any handler can fire an Intent."
        )
    }

    @Test
    fun executeRejectsSchemaMismatchBeforeHandlerDispatch() {
        val src = source("src/main/java/com/orbit/app/action/ActionExecutorService.kt")

        assertOccursBefore(
            src,
            "reason = \"schema_mismatch\"",
            "handler.handle(this@ActionExecutorService, skill, request.argsJson, repo)",
            "schema_mismatch must be recorded before any handler can fire an Intent."
        )
        assertTrue(
            "execute() must keep a local argsJson shape check before dispatch.",
            src.contains("if (!isValidArgsJsonShape(request.argsJson))")
        )
    }

    private fun assertOccursBefore(src: String, first: String, second: String, message: String) {
        val firstIndex = src.indexOf(first)
        val secondIndex = src.indexOf(second)
        assertTrue("missing marker: $first", firstIndex >= 0)
        assertTrue("missing marker: $second", secondIndex >= 0)
        assertTrue(message, firstIndex < secondIndex)
    }

    private fun source(relativePath: String): String {
        val cwd = File(requireNotNull(System.getProperty("user.dir")))
        return File(cwd, relativePath).readText()
    }
}
