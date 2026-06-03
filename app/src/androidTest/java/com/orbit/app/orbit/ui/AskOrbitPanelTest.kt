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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AskOrbitPanelTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
    }

    @Test
    fun citedAnswerOpensCapture() {
        val repo = FakeRepository(answer = answered())
        val viewModel = AskOrbitViewModel(repo, scopeOverride = uiScope())
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

    @Test
    fun sensitiveRefusalRendersAsGroundedRefusal() {
        val repo = FakeRepository(answer = sensitiveRefusal())
        val viewModel = AskOrbitViewModel(repo, scopeOverride = uiScope())

        composeRule.setContent {
            MaterialTheme {
                AskOrbitPanel(
                    viewModel = viewModel,
                    onOpenCapture = {},
                )
            }
        }

        composeRule.onNodeWithTag(AskOrbitPanelTestTags.QUESTION).performTextInput("What is my passport number?")
        composeRule.onNodeWithTag(AskOrbitPanelTestTags.SUBMIT).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { repo.questions.isNotEmpty() }

        composeRule.onNodeWithTag(AskOrbitPanelTestTags.REFUSAL).assertIsDisplayed()
        composeRule.onNodeWithText("Not enough saved evidence").assertIsDisplayed()
        composeRule.onNodeWithText(
            "I do not have a saved capture that explicitly contains that, so I will not guess.",
            substring = true,
        ).assertIsDisplayed()
    }

    private fun uiScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { scopes += it }

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

    private fun sensitiveRefusal() = AskOrbitAnswer(
        status = "sensitive_refusal",
        answer = "I do not have a saved capture that explicitly contains that, so I will not guess. For sensitive details, Orbit only answers when the exact value is present in saved evidence.",
        citations = emptyList(),
        candidates = emptyList(),
        modelLabel = "hybrid/sensitive-policy",
        limitations = listOf("Capture or add the document first if you want Orbit to recall that detail later."),
    )
}
