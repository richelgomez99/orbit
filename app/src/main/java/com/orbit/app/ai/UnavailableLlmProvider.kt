package com.orbit.app.ai

import com.orbit.app.ai.model.ActionExtractionResult
import com.orbit.app.ai.model.AppFunctionSummary
import com.orbit.app.ai.model.DayHeaderResult
import com.orbit.app.ai.model.IntentClassification
import com.orbit.app.ai.model.LlmProvenance
import com.orbit.app.ai.model.SensitivityResult
import com.orbit.app.ai.model.SummaryResult
import com.orbit.app.data.entity.StateSnapshot
import com.orbit.app.data.model.Intent
import java.util.Locale

/**
 * Spec 008 fail-closed provider for "cloud AI disabled, local model unavailable".
 *
 * This object never touches network. It gives deterministic no-op results for
 * best-effort capabilities and throws the existing local-model-unavailable
 * exception for summary paths where callers already degrade on provider
 * failure.
 */
class UnavailableLlmProvider(
    private val reason: String = "cloud AI disabled and local model unavailable",
) : LlmProvider {

    override suspend fun classifyIntent(text: String, appCategory: String): IntentClassification =
        IntentClassification(
            intent = Intent.AMBIGUOUS,
            confidence = 0f,
            provenance = LlmProvenance.LocalNano,
        )

    override suspend fun summarize(text: String, maxTokens: Int): SummaryResult {
        throw NanoUnavailableException(reason)
    }

    override suspend fun scanSensitivity(text: String): SensitivityResult =
        SensitivityResult(
            flagsJson = "[]",
            provenance = LlmProvenance.LocalNano,
        )

    override suspend fun generateDayHeader(
        dayIsoDate: String,
        envelopeSummaries: List<String>,
    ): DayHeaderResult =
        DayHeaderResult(
            text = "",
            generationLocale = Locale.getDefault().toLanguageTag(),
            provenance = LlmProvenance.LocalNano,
        )

    override suspend fun extractActions(
        text: String,
        contentType: String,
        state: StateSnapshot,
        registeredFunctions: List<AppFunctionSummary>,
        maxCandidates: Int,
    ): ActionExtractionResult =
        ActionExtractionResult(
            provenance = LlmProvenance.LocalNano,
            candidates = emptyList(),
        )

    override suspend fun embed(text: String): EmbeddingResult? = null
}
