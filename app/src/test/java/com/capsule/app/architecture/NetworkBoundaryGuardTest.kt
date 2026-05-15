package com.capsule.app.architecture

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NetworkBoundaryGuardTest {
    @Test
    fun directNetworkClientsStayInsideNetBoundary() {
        val root = File("src/main/java/com/capsule/app")
        val forbidden = Regex(
            "(import\\s+okhttp3\\.OkHttpClient|\\bOkHttpClient\\s*\\(|HttpURLConnection|URL\\.openConnection|import\\s+io\\.github\\.jan\\.supabase|createSupabaseClient|import\\s+.*\\.anthropic\\.|import\\s+.*\\.openai\\.)"
        )
        val violations = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.invariantSeparatorsPath.contains("/com/capsule/app/net/") }
            .filter { file ->
                file.readLines().any { line ->
                    val trimmed = line.trimStart()
                    !trimmed.startsWith("//") &&
                        !trimmed.startsWith("*") &&
                        forbidden.containsMatchIn(line)
                }
            }
            .map { it.invariantSeparatorsPath }
            .toList()

        assertTrue("Direct network/Supabase/provider clients must stay under com.capsule.app.net: $violations", violations.isEmpty())
    }
}