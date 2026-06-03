package com.orbit.app.library.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.orbit.app.library.LibraryRepository
import com.orbit.app.library.LibraryViewModel
import com.orbit.app.memory.MemoryEvidenceSnippet
import com.orbit.app.memory.MemorySearchFilters
import com.orbit.app.memory.MemorySearchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LibraryScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
    }

    @Test
    fun searchResultOpensLocalCapture() {
        val repo = FakeLibraryRepository(
            results = listOf(
                MemorySearchResult(
                    envelopeId = "env-startup",
                    rank = 1,
                    score = 0.92f,
                    title = "Startup event",
                    summary = "Reminder to follow up after the demo day event.",
                    dayLocal = "2026-05-30",
                    createdAtMillis = 1_780_000_000_000L,
                    intent = "REFERENCE",
                    sourceAppLabel = "Chrome",
                    domain = "example.com",
                    matchedEvidence = listOf(
                        MemoryEvidenceSnippet(
                            kind = "TEXT",
                            label = "Capture text",
                            excerpt = "startup event with Garry next Monday",
                            source = "local_text",
                        )
                    ),
                )
            )
        )
        val opened = mutableListOf<String>()
        val viewModel = LibraryViewModel(repo, scopeOverride = uiScope())

        composeRule.setContent {
            MaterialTheme {
                LibraryScreen(
                    viewModel = viewModel,
                    onOpenCapture = { opened += it },
                )
            }
        }

        composeRule.onNodeWithTag("library-query").performTextInput("startup")
        composeRule.onNodeWithTag("library-search").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            repo.queries.isNotEmpty()
        }
        composeRule.onNodeWithText("Startup event").assertIsDisplayed()
        composeRule.onNodeWithText("Reminder to follow up after the demo day event.").assertIsDisplayed()

        composeRule.onNodeWithTag("library-result-env-startup").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            opened.isNotEmpty()
        }

        assertEquals(listOf("startup"), repo.queries)
        assertEquals(listOf("env-startup"), opened)
    }

    private fun uiScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { scopes += it }

    private class FakeLibraryRepository(
        private val results: List<MemorySearchResult>,
    ) : LibraryRepository {
        val queries = mutableListOf<String>()

        override suspend fun search(
            query: String,
            filters: MemorySearchFilters?,
            limit: Int,
        ): List<MemorySearchResult> {
            queries += query
            return results
        }
    }
}
