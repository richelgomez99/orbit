package com.orbit.app.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: LibraryRepository,
    scopeOverride: CoroutineScope? = null,
) : ViewModel() {
    private val scope = scopeOverride ?: viewModelScope
    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()
    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        _state.value = _state.value.copy(
            query = query,
            unavailableTitle = null,
            unavailableDetail = null,
        )
    }

    fun onSearchSubmitted() {
        val query = _state.value.query.trim()
        if (query.isBlank()) {
            _state.value = _state.value.copy(
                results = emptyList(),
                searched = false,
                unavailableTitle = null,
                unavailableDetail = null,
            )
            return
        }
        searchJob?.cancel()
        searchJob = scope.launch {
            _state.value = _state.value.copy(
                loading = true,
                searched = true,
                unavailableTitle = null,
                unavailableDetail = null,
            )
            val outcome = runCatching { repository.search(query) }
            _state.value = outcome.fold(
                onSuccess = { results ->
                    _state.value.copy(
                        loading = false,
                        results = results,
                        unavailableTitle = null,
                        unavailableDetail = null,
                    )
                },
                onFailure = { error ->
                    val unavailable = error.toUnavailableCopy()
                    _state.value.copy(
                        loading = false,
                        results = emptyList(),
                        unavailableTitle = unavailable.title,
                        unavailableDetail = unavailable.detail,
                    )
                },
            )
        }
    }

    fun onOpenCapture(envelopeId: String) {
        _state.value = _state.value.copy(openEnvelopeId = envelopeId)
    }

    fun onOpenCaptureHandled() {
        _state.value = _state.value.copy(openEnvelopeId = null)
    }

    private fun Throwable.toUnavailableCopy(): UnavailableCopy = when (this) {
        is MemoryGatewayUnavailable -> when (code) {
            "UNAUTHORIZED" -> UnavailableCopy(
                title = "Library sign-in unavailable",
                detail = "Cloud memory search needs an active Orbit session.",
            )
            "NETWORK_UNAVAILABLE", "TIMEOUT" -> UnavailableCopy(
                title = "Library connection unavailable",
                detail = "Cloud memory search could not reach the memory gateway.",
            )
            else -> UnavailableCopy(
                title = "Library index unavailable",
                detail = "Cloud memory search could not read the memory index.",
            )
        }
        else -> UnavailableCopy(
            title = "Library index unavailable",
            detail = "Cloud memory search could not read the memory index.",
        )
    }

    private data class UnavailableCopy(
        val title: String,
        val detail: String,
    )
}
