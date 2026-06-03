package com.orbit.app.diary.ui

import com.orbit.app.data.ipc.EnvelopeViewParcel
import com.orbit.app.data.model.Intent
import com.orbit.app.memory.MemoryDisplayText
import com.orbit.app.memory.SourceAppLabelDisplay
import java.util.Locale

internal object EnvelopeDetailDisplayText {
    fun title(envelope: EnvelopeViewParcel): String = MemoryDisplayText.title(
        existingTitle = envelope.title,
        text = envelope.textContent,
        domain = envelope.domain,
    )
        ?: when (envelope.contentType.uppercase(Locale.ROOT)) {
            "IMAGE" -> SourceAppLabelDisplay.userFacingOrNull(envelope.sourceAppLabel)
                ?.let { "Screenshot from $it" }
                ?: "Screenshot saved from phone"
            else -> "Capture from ${sourceName(envelope)}"
        }

    fun sourceName(envelope: EnvelopeViewParcel): String = SourceAppLabelDisplay.userFacingOrNull(envelope.sourceAppLabel)
        ?: if (envelope.appCategory == "UNKNOWN_SOURCE") "an app" else envelope.appCategory.humanize()

    fun Intent.displayLabel(): String = when (this) {
        Intent.WANT_IT -> "Want it"
        Intent.REFERENCE -> "Reference"
        Intent.READ_LATER -> "Read later"
        Intent.FOR_SOMEONE -> "For someone"
        Intent.INTERESTING -> "Interesting"
        Intent.AMBIGUOUS -> "Unassigned"
    }

    private fun String.humanize(): String =
        lowercase(Locale.ROOT)
            .replace('_', ' ')
            .replaceFirstChar { it.titlecase(Locale.ROOT) }
}
