package com.orbit.app.ui.understanding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.understanding.ActiveIntentGroup
import com.orbit.app.understanding.ActiveIntentRepository
import com.orbit.app.understanding.ActiveIntentUiModel
import com.orbit.app.understanding.domain.ActiveIntentResolutionReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed interface ActiveIntentUiState {
    data object Loading : ActiveIntentUiState
    data class Ready(val groups: List<ActiveIntentGroupUi>) : ActiveIntentUiState
    data class Error(val message: String) : ActiveIntentUiState
}

data class ActiveIntentGroupUi(
    val intentType: String,
    val items: List<ActiveIntentCardUi>
)

data class ActiveIntentCardUi(
    val intentId: String,
    val captureId: String,
    val intentType: String,
    val status: String,
    val primaryAction: String?,
    val completionStatus: String,
    val evidenceSummary: String?,
    val sourceBasis: String?,
    val sourceTrust: String?,
    val dueAt: Long?,
    val expiresAt: Long?
)

class ActiveIntentViewModel(
    private val repository: ActiveIntentRepository,
    private val scopeOverride: CoroutineScope? = null
) : ViewModel() {
    private val mutableState = MutableStateFlow<ActiveIntentUiState>(ActiveIntentUiState.Loading)
    val state: StateFlow<ActiveIntentUiState> = mutableState.asStateFlow()

    init {
        observe()
    }

    fun resolve(intentId: String) = launchAction {
        repository.resolve(intentId, ActiveIntentResolutionReason.REPLIED_OR_DONE)
    }

    fun archive(intentId: String) = launchAction {
        repository.archive(intentId)
    }

    fun notInterested(intentId: String) = launchAction {
        repository.markNotInterested(intentId)
    }

    private fun observe() {
        val scope = scopeOverride ?: viewModelScope
        scope.launch {
            runCatching {
                repository.observeActiveGroups().collect { groups ->
                    mutableState.value = ActiveIntentUiState.Ready(groups.map { it.toUi() })
                }
            }.onFailure { throwable ->
                mutableState.value = ActiveIntentUiState.Error(throwable.message ?: "Could not load active intent")
            }
        }
    }

    private fun launchAction(block: suspend () -> Unit) {
        val scope = scopeOverride ?: viewModelScope
        scope.launch { block() }
    }

    private fun ActiveIntentGroup.toUi(): ActiveIntentGroupUi = ActiveIntentGroupUi(
        intentType = intentType,
        items = items.map { it.toCardUi() }
    )

    private fun ActiveIntentUiModel.toCardUi(): ActiveIntentCardUi {
        val evidence = runCatching { JSONObject(primaryEvidenceJson) }.getOrNull()
        val completion = evidence?.optJSONObject("completionKey")
        val source = evidence?.optJSONObject("source")
        return ActiveIntentCardUi(
            intentId = intentId,
            captureId = captureId,
            intentType = intentType,
            status = status,
            primaryAction = primaryAction,
            completionStatus = completion?.optString("status")?.takeIf { it.isNotBlank() } ?: "MISSING",
            evidenceSummary = completion?.optString("evidenceSummary")?.takeIf { it.isNotBlank() },
            sourceBasis = source?.optString("evidenceBasis")?.takeIf { it.isNotBlank() },
            sourceTrust = source?.optString("trustLevel")?.takeIf { it.isNotBlank() },
            dueAt = dueAt,
            expiresAt = expiresAt
        )
    }
}
