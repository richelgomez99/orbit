package com.capsule.app.ai

/**
 * T137 (spec 002 amendment Phase 11) — shared utility per FR-034.
 */
object PromptSanitizer {
    fun sanitizeInput(text: String): String {
        var sanitized = text
        sanitized = sanitized.replace(Regex("(?i)Ignore\\s+(prior|previous|all)"), "[redacted]")
        sanitized = sanitized.replace("</prompt>", "[redacted]")
        return sanitized
    }

    fun validateOutput(text: String): Boolean {
        if (text.contains("[redacted]")) return false
        return true
    }
}
