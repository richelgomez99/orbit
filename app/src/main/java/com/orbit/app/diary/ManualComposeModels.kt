package com.orbit.app.diary

data class ManualComposeDraft(
    val bodyText: String,
    val contextText: String? = null,
    val dayLocal: String,
    val intentName: String? = null,
)

sealed interface ManualComposeResult {
    data class Saved(
        val envelopeId: String,
        val contextAttached: Boolean,
    ) : ManualComposeResult

    data class AlreadySaved(
        val existingEnvelopeId: String,
        val matchedBy: String,
    ) : ManualComposeResult

    data class Blocked(
        val reason: String,
    ) : ManualComposeResult
}
