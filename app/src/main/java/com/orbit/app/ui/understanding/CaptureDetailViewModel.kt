package com.orbit.app.ui.understanding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.data.entity.CaptureUnderstandingEntity
import com.orbit.app.understanding.ActiveIntentRepository
import com.orbit.app.understanding.ActiveIntentUiModel
import com.orbit.app.understanding.UnderstandingRepository
import com.orbit.app.understanding.domain.EscalationRequest
import com.orbit.app.understanding.domain.UnderstandingMode
import com.orbit.app.understanding.engine.InvalidatedCaptureException
import com.orbit.app.understanding.engine.InvalidationGuard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed interface CaptureDetailUiState {
    data object Loading : CaptureDetailUiState
    data class Error(val message: String) : CaptureDetailUiState
    data class Ready(
        val captureId: String,
        val mode: String,
        val status: String,
        val title: String?,
        val summaryText: String?,
        val sourceIdentity: SourceIdentityUi,
        val evidenceTypes: List<String>,
        val groundingConstraints: List<String>,
        val activeIntents: List<CaptureActiveIntentUi>,
        val duplicateWarning: DuplicateWarningUi? = null
    ) : CaptureDetailUiState
}

data class SourceIdentityUi(
    val provider: String?,
    val appLabel: String?,
    val category: String,
    val trustLevel: String? = null,
    val evidenceBasis: String? = null
)

data class CaptureActiveIntentUi(
    val intentType: String,
    val status: String,
    val primaryAction: String?,
    val completionStatus: String,
    val evidenceSummary: String?
)

data class DuplicateWarningUi(
    val existingCaptureId: String,
    val matchType: String
)

class CaptureDetailViewModel(
    private val captureId: String,
    private val repository: UnderstandingRepository,
    private val activeIntentRepository: ActiveIntentRepository? = null,
    private val invalidationGuard: InvalidationGuard,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val scopeOverride: CoroutineScope? = null
) : ViewModel() {

    private val mutableState = MutableStateFlow<CaptureDetailUiState>(CaptureDetailUiState.Loading)
    val state: StateFlow<CaptureDetailUiState> = mutableState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val scope = scopeOverride ?: viewModelScope
        scope.launch {
            runCatching {
                invalidationGuard.assertNotInvalidated(captureId)
                val understanding = repository.getUnderstanding(captureId).firstOrNull()
                    ?: return@launch run { mutableState.value = CaptureDetailUiState.Error("No evidence available") }
                val evidence = repository.getEvidenceBundles(captureId)
                val activeIntents = activeIntentRepository?.getByCapture(captureId).orEmpty()
                mutableState.value = understanding.toUiState(
                    evidenceTypes = evidence.map { it.bundleType }.distinct().sorted(),
                    activeIntents = activeIntents.map { it.toCaptureActiveIntentUi() }
                )
            }.onFailure { throwable ->
                mutableState.value = CaptureDetailUiState.Error(
                    when (throwable) {
                        is InvalidatedCaptureException -> "No evidence available"
                        else -> throwable.message ?: "Could not load capture understanding"
                    }
                )
            }
        }
    }

    fun requestEscalation(mode: UnderstandingMode) {
        val scope = scopeOverride ?: viewModelScope
        scope.launch {
            repository.requestEscalation(
                EscalationRequest(
                    captureId = captureId,
                    requestedMode = mode,
                    requestedAt = nowMillis()
                )
            )
        }
    }

    private fun CaptureUnderstandingEntity.toUiState(
        evidenceTypes: List<String>,
        activeIntents: List<CaptureActiveIntentUi>
    ): CaptureDetailUiState.Ready =
        CaptureDetailUiState.Ready(
            captureId = captureId,
            mode = mode,
            status = status,
            title = title,
            summaryText = summaryText,
            sourceIdentity = sourceIdentityJson.toSourceIdentityUi(),
            evidenceTypes = evidenceTypes,
            groundingConstraints = groundingConstraintsJson.parseConstraints(),
            activeIntents = activeIntents
        )

    private fun String?.toSourceIdentityUi(): SourceIdentityUi {
        if (isNullOrBlank()) return SourceIdentityUi(null, null, "UNKNOWN_SOURCE")
        return runCatching {
            val json = JSONObject(this)
            SourceIdentityUi(
                provider = json.optString("provider").takeUnless { it.isBlank() || it == "null" },
                appLabel = json.optString("appLabel").takeUnless { it.isBlank() || it == "null" },
                category = json.optString("category", "UNKNOWN_SOURCE"),
                trustLevel = json.optString("trustLevel").takeUnless { it.isBlank() || it == "null" },
                evidenceBasis = json.optString("evidenceBasis").takeUnless { it.isBlank() || it == "null" }
            )
        }.getOrDefault(SourceIdentityUi(null, null, "UNKNOWN_SOURCE"))
    }

    private fun ActiveIntentUiModel.toCaptureActiveIntentUi(): CaptureActiveIntentUi {
        val evidence = runCatching { JSONObject(primaryEvidenceJson) }.getOrNull()
        val completion = evidence?.optJSONObject("completionKey")
        return CaptureActiveIntentUi(
            intentType = intentType,
            status = status,
            primaryAction = primaryAction,
            completionStatus = completion?.optString("status")?.takeIf { it.isNotBlank() } ?: "MISSING",
            evidenceSummary = completion?.optString("evidenceSummary")?.takeIf { it.isNotBlank() }
        )
    }

    private fun String.parseConstraints(): List<String> = runCatching {
        val json = JSONObject(this)
        val array = json.optJSONArray("constraints") ?: return@runCatching emptyList()
        List(array.length()) { index -> array.optString(index) }.filter { it.isNotBlank() }
    }.getOrDefault(emptyList())
}
