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
 * Current/legacy local LlmProvider — delegates to on-device Gemini Nano
 * via AICore when that path is available.
 *
 * All results carry [LlmProvenance.LocalNano] provenance.
 * Implementation will be fleshed out when AICore SDK integration lands
 * where still useful. Spec 022's strategic local path is the BYOM/local
 * model manager; do not treat this provider as the final architecture.
 *
 * **Diagnostic seam (T097, spec/003)**: when [LlmProviderDiagnostics.forceNanoUnavailable]
 * is `true`, the methods that production code routes through the LLM in 003's
 * Orbit Actions / Weekly Digest paths ([extractActions], [summarize]) throw
 * [NanoUnavailableException] *before* doing any work. The flag is intended to
 * be flipped only from the debug-build `DiagnosticsActivity`; production code
 * paths never set it. See quickstart §6 N2.
 */
class NanoLlmProvider : LlmProvider {

    // AICore/Gemini Nano is not integrated in v1. Rather than `TODO()`
    // (a NotImplementedError landmine if any path reaches this provider),
    // fail SAFE exactly like UnavailableLlmProvider: best-effort methods
    // return deterministic degraded results, and summarize throws the typed
    // NanoUnavailableException that callers (NanoSummariser, DigestComposer,
    // ClusterSummariser) already catch and degrade on.

    override suspend fun classifyIntent(text: String, appCategory: String): IntentClassification =
        IntentClassification(
            intent = Intent.AMBIGUOUS,
            confidence = 0f,
            provenance = LlmProvenance.LocalNano,
        )

    override suspend fun summarize(text: String, maxTokens: Int): SummaryResult {
        throw NanoUnavailableException(
            if (LlmProviderDiagnostics.forceNanoUnavailable) {
                "forced via LlmProviderDiagnostics (debug seam)"
            } else {
                "Gemini Nano (AICore) not integrated"
            },
        )
    }

    override suspend fun scanSensitivity(text: String): SensitivityResult =
        SensitivityResult(flagsJson = "[]", provenance = LlmProvenance.LocalNano)

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
        maxCandidates: Int
    ): ActionExtractionResult {
        if (LlmProviderDiagnostics.forceNanoUnavailable) {
            throw NanoUnavailableException("forced via LlmProviderDiagnostics (debug seam)")
        }
        // Until AICore is wired up, return an empty list with LocalNano
        // provenance. Callers treat this the same as a model that decided
        // there are no actions in the text — extraction-contract §5.
        return ActionExtractionResult(
            provenance = LlmProvenance.LocalNano,
            candidates = emptyList()
        )
    }

    /**
     * T124 — embedding via AICore (TODO until AICore SDK lands).
     *
     * Graceful-degrade contract per [LlmProvider.embed]:
     *  - blank input → `null` (no AICore call),
     *  - [LlmProviderDiagnostics.forceNanoUnavailable] flag set → `null`
     *    (no exception escapes — embedding is a best-effort signal that
     *    must never crash the cluster worker),
     *  - any [Throwable] escaping the underlying call → `null`.
     *
     * When the AICore embedding API lands, this method will:
     *  1. early-return `null` for `text.isBlank()`,
     *  2. invoke the AICore embedding model,
     *  3. wrap the float[] in [EmbeddingResult] stamped with [MODEL_LABEL]
     *     and the model's dimensionality,
     *  4. catch [Throwable] → log + return `null`.
     */
    override suspend fun embed(text: String): EmbeddingResult? {
        if (text.isBlank()) return null
        if (LlmProviderDiagnostics.forceNanoUnavailable) return null
        return try {
            // TODO(US8 — AICore embedding integration): replace with real call.
            // Returning null until the AICore embedding API is available; the
            // cluster worker treats null as "no embedding available, skip".
            null
        } catch (_: Throwable) {
            null
        }
    }

    companion object {
        /**
         * Stamped onto every [EmbeddingResult] this provider produces, and
         * onto every [com.orbit.app.data.entity.ClusterEntity.modelLabel]
         * so the worker can refuse to compute similarity across model rev
         * boundaries (FR-038, FR-039). Bumped when AICore ships a new build.
         */
        const val MODEL_LABEL: String = "nano-v4-build-2026-05-01"
    }
}
