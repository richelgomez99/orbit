package com.orbit.app.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ManualComposeViewModel(
    private val repository: DiaryRepository,
    private val dayLocal: String,
    scopeOverride: CoroutineScope? = null,
) : ViewModel() {

    private val scope = scopeOverride ?: viewModelScope

    private val _state = MutableStateFlow(ManualComposeUiState(dayLocal = dayLocal))
    val state: StateFlow<ManualComposeUiState> = _state.asStateFlow()

    fun onBodyChanged(value: String) {
        _state.value = _state.value.copy(bodyText = value, error = null)
    }

    fun onContextChanged(value: String) {
        _state.value = _state.value.copy(contextText = value, error = null)
    }

    /** Tapping the selected intent again clears it (defer to auto-classify). */
    fun onIntentSelected(intent: com.orbit.app.data.model.Intent) {
        val next = if (_state.value.selectedIntent == intent) null else intent
        _state.value = _state.value.copy(selectedIntent = next, error = null)
    }

    fun save() {
        val current = _state.value
        if (current.bodyText.isBlank()) {
            _state.value = current.copy(error = "Add something to save.")
            return
        }
        if (current.saving) return

        scope.launch {
            _state.value = current.copy(saving = true, error = null)
            val result = runCatching {
                repository.createManualTextCapture(
                    bodyText = current.bodyText,
                    contextText = current.contextText,
                    dayLocal = dayLocal,
                    // User-picked intent → USER_CHIP downstream; null defers to
                    // auto-classification (unchanged fallback path).
                    intentName = current.selectedIntent?.name,
                )
            }.getOrElse { error ->
                ManualComposeResult.Blocked(error.message ?: "manual_compose_failed")
            }
            _state.value = _state.value.copy(
                saving = false,
                result = result,
                error = when (result) {
                    is ManualComposeResult.Blocked -> result.reason.toUserMessage()
                    is ManualComposeResult.AlreadySaved,
                    is ManualComposeResult.Saved -> null
                },
            )
        }
    }

    fun consumeResult() {
        _state.value = _state.value.copy(result = null)
    }

    private fun String.toUserMessage(): String = when (this) {
        "blank_body" -> "Add something to save."
        "manual_compose_unavailable" -> "Manual compose is not available yet."
        else -> "Could not save this capture."
    }
}

data class ManualComposeUiState(
    val dayLocal: String,
    val bodyText: String = "",
    val contextText: String = "",
    val selectedIntent: com.orbit.app.data.model.Intent? = null,
    val saving: Boolean = false,
    val result: ManualComposeResult? = null,
    val error: String? = null,
)
