package com.orbit.app.generativeui

data class OrbitAgentUiDocument(
    val documentId: String,
    val components: List<OrbitAgentUiComponent>,
    val fallbackText: String
)

sealed interface OrbitAgentUiComponent

data class OrbitAgentUiTitle(val text: String) : OrbitAgentUiComponent
data class OrbitAgentUiBody(val text: String) : OrbitAgentUiComponent
data class OrbitAgentUiQuestion(
    val id: String,
    val text: String,
    val choices: List<OrbitAgentUiChoice>,
    val evidenceIds: List<String>
) : OrbitAgentUiComponent

data class OrbitAgentUiStep(
    val id: String,
    val kind: String,
    val label: String,
    val detail: String?,
    val requiresApproval: Boolean,
    val evidenceIds: List<String>
) : OrbitAgentUiComponent

data class OrbitAgentUiEvidence(
    val id: String,
    val label: String,
    val sourceType: String,
    val sourceId: String,
    val dayLocal: String?
) : OrbitAgentUiComponent

data class OrbitAgentUiLimitations(val items: List<String>) : OrbitAgentUiComponent

data class OrbitAgentUiChoice(
    val id: String,
    val label: String,
    val evidenceIds: List<String>
)

object OrbitAgentUiCaps {
    const val MAX_TITLE_CHARS = 96
    const val MAX_BODY_CHARS = 280
    const val MAX_LABEL_CHARS = 96
    const val MAX_DETAIL_CHARS = 180
    const val MAX_SOURCE_LABEL_CHARS = 96
    const val MAX_LIMITATION_CHARS = 120
    const val MAX_ID_CHARS = 96
    const val MAX_STEPS = 5
    const val MAX_QUESTIONS = 3
    const val MAX_CHOICES = 5
    const val MAX_EVIDENCE = 10
    const val MAX_EVIDENCE_IDS = 10
    const val MAX_FALLBACK_CHARS = 1_600
}

internal fun String.displayCap(maxChars: Int): String = replace(Regex("\\s+"), " ")
    .trim()
    .let { clean ->
        if (clean.length <= maxChars) clean else clean.take(maxChars).trimEnd()
    }

internal fun String?.displayCapOrNull(maxChars: Int): String? = this
    ?.displayCap(maxChars)
    ?.takeIf { it.isNotBlank() }

internal fun List<String>.cappedIds(): List<String> = mapNotNull { it.displayCapOrNull(OrbitAgentUiCaps.MAX_ID_CHARS) }
    .distinct()
    .take(OrbitAgentUiCaps.MAX_EVIDENCE_IDS)
