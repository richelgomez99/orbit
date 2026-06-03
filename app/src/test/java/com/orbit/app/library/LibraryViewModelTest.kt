package com.orbit.app.library

import com.orbit.app.memory.MemorySearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
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
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
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
    fun searchSuccessPublishesResults() = runTest(dispatcher) {
        val repo = FakeRepository(results = listOf(result("env-1")))
        val viewModel = LibraryViewModel(repo, scopeOverride = this)

        viewModel.onQueryChanged("flight receipt")
        viewModel.onSearchSubmitted()
        advanceUntilIdle()

        assertEquals("flight receipt", repo.queries.single())
        assertFalse(viewModel.state.value.loading)
        assertEquals(1, viewModel.state.value.results.size)
        assertNull(viewModel.state.value.unavailableMessage)
    }

    @Test
    fun loadingStateIsVisibleWhileSearchIsInFlight() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeRepository(
            results = listOf(result("env-1")),
            beforeReturn = { gate.await() },
        )
        val viewModel = LibraryViewModel(repo, scopeOverride = this)

        viewModel.onQueryChanged("flight")
        viewModel.onSearchSubmitted()
        dispatcher.scheduler.runCurrent()

        assertTrue(viewModel.state.value.loading)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun emptySearchResultKeepsSearchedState() = runTest(dispatcher) {
        val repo = FakeRepository(results = emptyList())
        val viewModel = LibraryViewModel(repo, scopeOverride = this)

        viewModel.onQueryChanged("nothing")
        viewModel.onSearchSubmitted()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.searched)
        assertTrue(viewModel.state.value.results.isEmpty())
        assertNull(viewModel.state.value.unavailableMessage)
    }

    @Test
    fun blankSearchDoesNotCallRepository() = runTest(dispatcher) {
        val repo = FakeRepository(results = listOf(result("env-1")))
        val viewModel = LibraryViewModel(repo, scopeOverride = this)

        viewModel.onQueryChanged("   ")
        viewModel.onSearchSubmitted()
        advanceUntilIdle()

        assertTrue(repo.queries.isEmpty())
        assertFalse(viewModel.state.value.searched)
    }

    @Test
    fun searchFailureShowsUnavailableState() = runTest(dispatcher) {
        val repo = FakeRepository(error = IOException("offline"))
        val viewModel = LibraryViewModel(repo, scopeOverride = this)

        viewModel.onQueryChanged("demo")
        viewModel.onSearchSubmitted()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.loading)
        assertTrue(viewModel.state.value.results.isEmpty())
        assertEquals("Library index unavailable", viewModel.state.value.unavailableMessage)
        assertEquals(
            "Cloud memory search could not read the memory index.",
            viewModel.state.value.unavailableDetail,
        )
    }

    @Test
    fun unauthorizedSearchShowsSignInState() = runTest(dispatcher) {
        val repo = FakeRepository(error = MemoryGatewayUnavailable("UNAUTHORIZED", "no active Supabase session"))
        val viewModel = LibraryViewModel(repo, scopeOverride = this)

        viewModel.onQueryChanged("demo")
        viewModel.onSearchSubmitted()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.loading)
        assertEquals("Library sign-in unavailable", viewModel.state.value.unavailableMessage)
        assertEquals(
            "Cloud memory search needs an active Orbit session.",
            viewModel.state.value.unavailableDetail,
        )
    }

    @Test
    fun openCaptureIsOneShotState() {
        val viewModel = LibraryViewModel(FakeRepository(emptyList()), scopeOverride = kotlinx.coroutines.CoroutineScope(dispatcher))

        viewModel.onOpenCapture("env-1")
        assertEquals("env-1", viewModel.state.value.openEnvelopeId)

        viewModel.onOpenCaptureHandled()
        assertNull(viewModel.state.value.openEnvelopeId)
    }

    private class FakeRepository(
        private val results: List<MemorySearchResult> = emptyList(),
        private val error: Throwable? = null,
        private val beforeReturn: suspend () -> Unit = {},
    ) : LibraryRepository {
        val queries = mutableListOf<String>()

        override suspend fun search(
            query: String,
            filters: com.orbit.app.memory.MemorySearchFilters?,
            limit: Int,
        ): List<MemorySearchResult> {
            queries += query
            beforeReturn()
            error?.let { throw it }
            return results
        }
    }

    private fun result(envelopeId: String) = MemorySearchResult(
        envelopeId = envelopeId,
        rank = 1,
        score = 0.9f,
        title = "Flight receipt",
        summary = "Receipt from last week",
        dayLocal = "2026-05-30",
        createdAtMillis = 1_780_000_000_000L,
        intent = "REFERENCE",
        sourceAppLabel = "Gmail",
    )
}
