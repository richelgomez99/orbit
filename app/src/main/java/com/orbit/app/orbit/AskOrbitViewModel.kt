package com.orbit.app.orbit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AskOrbitViewModel(
    private val repository: AskOrbitRepository,
    scopeOverride: CoroutineScope? = null,
) : ViewModel() {
    private val scope = scopeOverride ?: viewModelScope
    private val _state = MutableStateFlow(AskOrbitUiState())
    val state: StateFlow<AskOrbitUiState> = _state.asStateFlow()
    private var askJob: Job? = null

    fun onQuestionChanged(question: String) {
        _state.value = _state.value.copy(question = question, error = null)
    }

    fun onAskSubmitted() {
        val question = _state.value.question.trim()
        if (question.isBlank()) return
        askJob?.cancel()
        askJob = scope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val outcome = runCatching { repository.ask(question) }
            _state.value = outcome.fold(
                onSuccess = { answer ->
                    _state.value.copy(loading = false, answer = answer, error = null)
                },
                onFailure = {
                    _state.value.copy(
                        loading = false,
                        answer = null,
                        error = "Ask Orbit could not read saved memory right now.",
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

    fun reset() {
        askJob?.cancel()
        _state.value = AskOrbitUiState()
    }
}
