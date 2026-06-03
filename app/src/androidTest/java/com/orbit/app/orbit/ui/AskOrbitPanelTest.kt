package com.orbit.app.orbit.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.orbit.app.memory.AskOrbitAnswer
import com.orbit.app.memory.AskOrbitCitation
import com.orbit.app.memory.MemorySearchFilters
import com.orbit.app.orbit.AskOrbitRepository
import com.orbit.app.orbit.AskOrbitViewModel
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AskOrbitPanelTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun citedAnswerOpensCapture() {
        val repo = FakeRepository(answer = answered())
        val viewModel = AskOrbitViewModel(repo, scopeOverride = TestScope())
        val opened = mutableListOf<String>()

        composeRule.setContent {
            MaterialTheme {
                AskOrbitPanel(
                    viewModel = viewModel,
                    onOpenCapture = { opened += it },
                )
            }
        }

        composeRule.onNodeWithTag(AskOrbitPanelTestTags.QUESTION).performTextInput("What moved?")
        composeRule.onNodeWithTag(AskOrbitPanelTestTags.SUBMIT).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { repo.questions.isNotEmpty() }

        composeRule.onNodeWithTag(AskOrbitPanelTestTags.ANSWER).assertIsDisplayed()
        composeRule.onNodeWithText("Found 1 related capture").assertIsDisplayed()
        composeRule.onNodeWithText("Due to a conflict, it will be rescheduled.").assertIsDisplayed()
        composeRule.onNodeWithTag(AskOrbitPanelTestTags.citation("env-1")).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { opened.isNotEmpty() }

        assertEquals(listOf("env-1"), opened)
    }

    private class FakeRepository(
        private val answer: AskOrbitAnswer,
    ) : AskOrbitRepository {
        val questions = mutableListOf<String>()

        override suspend fun ask(
            question: String,
            filters: MemorySearchFilters?,
            limit: Int,
        ): AskOrbitAnswer {
            questions += question
            return answer
        }
    }

    private fun answered() = AskOrbitAnswer(
        status = "answered",
        answer = "The event was rescheduled.",
        citations = listOf(
            AskOrbitCitation(
                citationId = "c1",
                envelopeId = "env-1",
                title = "Event update",
                excerpt = "Due to a conflict, it will be rescheduled.",
                dayLocal = "2026-05-30",
                sourceAppLabel = "Email",
            )
        ),
        candidates = emptyList(),
        modelLabel = "test",
    )
}
