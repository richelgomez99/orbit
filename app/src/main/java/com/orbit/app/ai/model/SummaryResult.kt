package com.orbit.app.ai.model

data class SummaryResult(
    val text: String,
    val generationLocale: String,
    val provenance: LlmProvenance
)
