package com.orbit.app.ai.ipc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Spec 022 M1 — the sealed request/response carried as JSON in the
 * [LocalInferenceRequestParcel]/[LocalInferenceResponseParcel] `payloadJson`.
 *
 * Only the methods a BYOM model actually serves cross the boundary:
 * summarize, generateDayHeader, classifyIntent, scanSensitivity.
 * [com.orbit.app.ai.LlmProvider.extractActions] (empty) and `embed` (null)
 * are short-circuited in [RemoteLocalLlmProvider] without a round-trip.
 *
 * Provenance is reduced to a model label on the wire and rebuilt as
 * [com.orbit.app.ai.model.LlmProvenance.LocalByom] on the caller side.
 */
@Serializable
sealed class LocalInferenceRequest {
    abstract val requestId: String

    @Serializable
    @SerialName("summarize")
    data class Summarize(
        override val requestId: String,
        val text: String,
        val maxTokens: Int,
    ) : LocalInferenceRequest()

    @Serializable
    @SerialName("day_header")
    data class GenerateDayHeader(
        override val requestId: String,
        val dayIsoDate: String,
        val envelopeSummaries: List<String>,
    ) : LocalInferenceRequest()

    @Serializable
    @SerialName("classify_intent")
    data class ClassifyIntent(
        override val requestId: String,
        val text: String,
        val appCategory: String,
    ) : LocalInferenceRequest()

    @Serializable
    @SerialName("scan_sensitivity")
    data class ScanSensitivity(
        override val requestId: String,
        val text: String,
    ) : LocalInferenceRequest()
}

@Serializable
sealed class LocalInferenceResponse {
    abstract val requestId: String

    @Serializable
    @SerialName("summary")
    data class Summary(
        override val requestId: String,
        val text: String,
        val generationLocale: String,
        val modelLabel: String,
    ) : LocalInferenceResponse()

    @Serializable
    @SerialName("day_header")
    data class DayHeader(
        override val requestId: String,
        val text: String,
        val generationLocale: String,
        val modelLabel: String,
    ) : LocalInferenceResponse()

    @Serializable
    @SerialName("intent")
    data class Intent(
        override val requestId: String,
        val intent: String,
        val confidence: Float,
        val modelLabel: String,
    ) : LocalInferenceResponse()

    @Serializable
    @SerialName("sensitivity")
    data class Sensitivity(
        override val requestId: String,
        val flagsJson: String,
        val modelLabel: String,
    ) : LocalInferenceResponse()

    @Serializable
    @SerialName("error")
    data class Error(
        override val requestId: String,
        val code: String,
        val message: String,
    ) : LocalInferenceResponse()
}

/** Shared JSON codec for the local-inference IPC payloads. */
val LocalInferenceJson: Json = Json {
    classDiscriminator = "type"
    ignoreUnknownKeys = true
    encodeDefaults = true
}
