package com.orbit.app.ai.gateway

import kotlinx.serialization.Serializable

/**
 * Spec 013 (data-model §1.4) — `@Serializable` mirrors of the on-device
 * Kotlin types referenced by [LlmGatewayRequest.ExtractActions] and
 * [LlmGatewayResponse.ExtractActionsResponse].
 *
 * The mirrors exist (rather than `@Serializable`-annotating the originals)
 * so the AI Gateway wire format is structurally decoupled from internal
 * model evolution. Conversion is mechanical and lives in `CloudLlmProvider`
 * / `NetworkGatewayImpl`.
 *
 * - [StateSnapshotJson] mirrors `com.orbit.app.data.entity.StateSnapshot`.
 *   Enum-typed fields (`appCategory`, `activityState`) are serialized as
 *   their `name` strings so wire decoding does not require enum class
 *   ordering stability.
 * - [AppFunctionSummaryJson] mirrors `com.orbit.app.ai.model.AppFunctionSummary`.
 *   `sensitivityScope` is the enum's name string for the same reason.
 * - [ActionProposalJson] mirrors the `ActionCandidate` shape returned by
 *   the on-device extractor (the Day-1 wire payload — Day-N may version it).
 */

@Serializable
data class StateSnapshotJson(
    val appCategory: String,
    val activityState: String,
    val tzId: String,
    val hourLocal: Int,
    val dayOfWeekLocal: Int,
)

@Serializable
data class AppFunctionSummaryJson(
    val functionId: String,
    val schemaVersion: Int,
    val displayName: String,
    val description: String,
    val argsSchemaJson: String,
    val sensitivityScope: String,
)

@Serializable
data class ActionProposalJson(
    val functionId: String,
    val schemaVersion: Int,
    val argsJson: String,
    val previewTitle: String,
    val previewSubtitle: String? = null,
    val confidence: Float,
    val sensitivityScope: String,
)

@Serializable
data class ActiveIntentCompactEvidenceJson(
    val kind: String? = null,
    val label: String? = null,
    val source: String? = null,
    val confidence: Float? = null,
    val excerpt: String? = null,
    val hash: String? = null,
    val reason: String? = null,
    val createdAt: Long? = null,
    val reviewedAt: Long? = null,
)

@Serializable
data class ActiveIntentReviewContextJson(
    val schemaVersion: Int = 1,
    val intentId: String,
    val captureId: String,
    val mode: String,
    val intentType: String,
    val status: String,
    val completionKeyStatus: String,
    val primaryAction: String? = null,
    val dueAt: Long? = null,
    val expiresAt: Long? = null,
    val evidence: ActiveIntentCompactEvidenceJson,
    val completionKey: ActiveIntentCompactEvidenceJson? = null,
)
