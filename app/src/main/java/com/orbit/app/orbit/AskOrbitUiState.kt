package com.orbit.app.orbit

import com.orbit.app.memory.AskOrbitAnswer

data class AskOrbitUiState(
    val question: String = "",
    val loading: Boolean = false,
    val answer: AskOrbitAnswer? = null,
    val error: String? = null,
    val openEnvelopeId: String? = null,
) {
    val canAsk: Boolean get() = question.isNotBlank() && !loading
}
