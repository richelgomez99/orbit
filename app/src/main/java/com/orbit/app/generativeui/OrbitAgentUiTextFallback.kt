package com.orbit.app.generativeui

fun OrbitAgentUiDocument.asPlainText(): String = fallbackText

internal fun List<OrbitAgentUiComponent>.toOrbitAgentUiPlainText(): String {
    val components = this
    val text = buildString {
        components.forEach { component ->
            when (component) {
                is OrbitAgentUiTitle -> {
                    appendLine(component.text)
                }
                is OrbitAgentUiBody -> {
                    appendLine(component.text)
                }
                is OrbitAgentUiLimitations -> {
                    appendLine("Limitations")
                    component.items.forEach { appendLine("- $it") }
                }
                is OrbitAgentUiQuestion -> {
                    appendLine("Question: ${component.text}")
                    component.choices.forEach { appendLine("- ${it.label}") }
                }
                is OrbitAgentUiStep -> {
                    val approval = if (component.requiresApproval) " (requires approval)" else ""
                    appendLine("Step: ${component.label}$approval")
                    component.detail?.let { appendLine(it) }
                }
                is OrbitAgentUiEvidence -> {
                    val date = component.dayLocal?.let { " · $it" }.orEmpty()
                    appendLine("Evidence: ${component.label} [${component.sourceType}:${component.sourceId}]$date")
                }
            }
        }
    }.trim()

    return if (text.length <= OrbitAgentUiCaps.MAX_FALLBACK_CHARS) {
        text
    } else {
        text.take(OrbitAgentUiCaps.MAX_FALLBACK_CHARS).trimEnd()
    }
}
