package com.orbit.app.ai.ipc

import com.orbit.app.ai.LlmProvider

/**
 * Spec 022 M1 — pure request→provider→response mapping for [ILocalInference].
 * Extracted from [LocalInferenceService] so the wire contract can be unit
 * tested with a fake [LlmProvider], no Android/Binder/engine required.
 */
object LocalInferenceDispatcher {

    suspend fun dispatch(
        request: LocalInferenceRequest,
        provider: LlmProvider,
        modelLabel: String,
    ): LocalInferenceResponse = when (request) {
        is LocalInferenceRequest.Summarize -> {
            val r = provider.summarize(request.text, request.maxTokens)
            LocalInferenceResponse.Summary(request.requestId, r.text, r.generationLocale, modelLabel)
        }
        is LocalInferenceRequest.GenerateDayHeader -> {
            val r = provider.generateDayHeader(request.dayIsoDate, request.envelopeSummaries)
            LocalInferenceResponse.DayHeader(request.requestId, r.text, r.generationLocale, modelLabel)
        }
        is LocalInferenceRequest.ClassifyIntent -> {
            val r = provider.classifyIntent(request.text, request.appCategory)
            LocalInferenceResponse.Intent(request.requestId, r.intent.name, r.confidence, modelLabel)
        }
        is LocalInferenceRequest.ScanSensitivity -> {
            val r = provider.scanSensitivity(request.text)
            LocalInferenceResponse.Sensitivity(request.requestId, r.flagsJson, modelLabel)
        }
    }
}
