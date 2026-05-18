package com.orbit.app.understanding.domain

/** User-initiated request to spend more effort understanding a capture. */
data class EscalationRequest(
    val captureId: String,
    val requestedMode: UnderstandingMode,
    val requestedAt: Long
) {
    init {
        require(requestedMode != UnderstandingMode.BASIC) {
            "EscalationRequest is only for user-triggered Smart or Deep understanding."
        }
    }
}
