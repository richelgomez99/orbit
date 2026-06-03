package com.orbit.app.memory

import java.util.Locale

internal object SourceAppLabelDisplay {
    private val nonUserFacingLabels = setOf(
        "intentresolver",
        "intentresolveractivity",
        "androidsystem",
        "systemui",
        "packageinstaller",
        "permissioncontroller",
    )

    fun userFacingOrNull(label: String?): String? {
        val trimmed = label?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val normalized = trimmed
            .lowercase(Locale.ROOT)
            .filter { it.isLetterOrDigit() }
        return trimmed.takeIf { normalized !in nonUserFacingLabels }
    }
}
