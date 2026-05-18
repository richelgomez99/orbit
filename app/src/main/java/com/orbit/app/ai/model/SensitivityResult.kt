package com.orbit.app.ai.model

data class SensitivityResult(
    val flagsJson: String,
    val provenance: LlmProvenance
)
