package com.capsule.app.architecture

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CompactIpcPayloadGuardTest {
    @Test
    fun ipcPayloadsDoNotCarryRawOrBulkUnderstandingContent() {
        val roots = listOf(
            File("src/main/aidl"),
            File("src/main/java/com/capsule/app/data/ipc")
        )
        val forbidden = Regex(
            "\\b(rawHtml|readableHtml|fullPageText|fullOcrText|fullEvidence|evidenceBundles|embedding|embeddings|vector|vectors|rawPrompt|modelResponse|screenshotBytes|bitmapBytes)\\b",
            RegexOption.IGNORE_CASE
        )
        val violations = roots
            .filter { it.exists() }
            .flatMap { root ->
                root.walkTopDown()
                    .filter { it.isFile && (it.extension == "kt" || it.extension == "aidl") }
                    .filter { forbidden.containsMatchIn(it.readText()) }
                    .map { it.invariantSeparatorsPath }
                    .toList()
            }

        assertTrue("IPC payloads must stay compact and content-free: $violations", violations.isEmpty())
    }
}