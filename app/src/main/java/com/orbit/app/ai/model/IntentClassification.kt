package com.orbit.app.ai.model

import com.orbit.app.data.model.Intent

data class IntentClassification(
    val intent: Intent,
    val confidence: Float,
    val provenance: LlmProvenance
)
