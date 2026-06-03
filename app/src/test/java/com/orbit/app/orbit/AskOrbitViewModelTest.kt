package com.orbit.app.orbit

import com.orbit.app.memory.AskOrbitAnswer
import com.orbit.app.memory.AskOrbitCitation
import com.orbit.app.memory.MemorySearchFilters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AskOrbitViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun askPublishesCitedAnswer() = runTest(dispatcher) {
        val repo = FakeRepository(answer = answered())
        val viewModel = AskOrbitViewModel(repo, scopeOverride = this)

        viewModel.onQuestionChanged("What was rescheduled?")
        viewModel.onAskSubmitted()
        advanceUntilIdle()

        assertEquals("What was rescheduled?", repo.questions.single())
        assertFalse(viewModel.state.value.loading)
        assertEquals("answered", viewModel.state.value.answer?.status)
        assertEquals("env-1", viewModel.state.value.answer?.citations?.single()?.envelopeId)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun blankAskDoesNotCallRepository() = runTest(dispatcher) {
        val repo = FakeRepository(answer = answered())
        val viewModel = AskOrbitViewModel(repo, scopeOverride = this)

        viewModel.onQuestionChanged("   ")
        viewModel.onAskSubmitted()
        advanceUntilIdle()

        assertTrue(repo.questions.isEmpty())
        assertNull(viewModel.state.value.answer)
    }

    @Test
    fun repositoryFailureShowsError() = runTest(dispatcher) {
        val repo = FakeRepository(error = IllegalStateException("offline"))
        val viewModel = AskOrbitViewModel(repo, scopeOverride = this)

        viewModel.onQuestionChanged("Anything?")
        viewModel.onAskSubmitted()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.loading)
        assertEquals("Ask Orbit could not read saved memory right now.", viewModel.state.value.error)
    }

    @Test
    fun resetClearsPreviousQuestionAndAnswer() = runTest(dispatcher) {
        val viewModel = AskOrbitViewModel(FakeRepository(answer = answered()), scopeOverride = this)

        viewModel.onQuestionChanged("What moved?")
        viewModel.onAskSubmitted()
        advanceUntilIdle()
        viewModel.reset()

        assertEquals(AskOrbitUiState(), viewModel.state.value)
    }

    @Test
    fun queryVariantsNormalizeCancelledAndRescheduledQuestions() {
        assertTrue(AskOrbitQueryText.queryVariants("what got cancelled").contains("cancel"))
        assertTrue(AskOrbitQueryText.queryVariants("what got cancelled").contains("reschedul"))
        assertTrue(AskOrbitQueryText.queryVariants("what got cancelled").contains("postpon"))
        assertTrue(AskOrbitQueryText.queryVariants("what got canceled").contains("cancel"))
        assertTrue(AskOrbitQueryText.queryVariants("what got rescheduled").contains("reschedul"))
    }

    private class FakeRepository(
        private val answer: AskOrbitAnswer? = null,
        private val error: Throwable? = null,
    ) : AskOrbitRepository {
        val questions = mutableListOf<String>()

        override suspend fun ask(
            question: String,
            filters: MemorySearchFilters?,
            limit: Int,
        ): AskOrbitAnswer {
            questions += question
            error?.let { throw it }
            return answer ?: AskOrbitAnswer(
                status = "insufficient_evidence",
                answer = "I could not find enough saved evidence to answer that.",
                citations = emptyList(),
                candidates = emptyList(),
                modelLabel = "test",
            )
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
