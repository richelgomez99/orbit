package com.orbit.app.generativeui

import com.orbit.app.data.ipc.AgentPlanParcel

fun AgentPlanParcel.toOrbitAgentUiDocument(): OrbitAgentUiDocument {
    val components = buildList {
        title.displayCapOrNull(OrbitAgentUiCaps.MAX_TITLE_CHARS)?.let {
            add(OrbitAgentUiTitle(it))
        }
        summary.displayCapOrNull(OrbitAgentUiCaps.MAX_BODY_CHARS)?.let {
            add(OrbitAgentUiBody(it))
        }
        limitations
            .mapNotNull { it.displayCapOrNull(OrbitAgentUiCaps.MAX_LIMITATION_CHARS) }
            .takeIf { it.isNotEmpty() }
            ?.let { add(OrbitAgentUiLimitations(it)) }
        questions.take(OrbitAgentUiCaps.MAX_QUESTIONS).forEach { question ->
            val text = question.text.displayCapOrNull(OrbitAgentUiCaps.MAX_BODY_CHARS)
                ?: return@forEach
            add(
                OrbitAgentUiQuestion(
                    id = question.questionId.displayCap(OrbitAgentUiCaps.MAX_ID_CHARS),
                    text = text,
                    choices = question.choices.take(OrbitAgentUiCaps.MAX_CHOICES).mapNotNull { choice ->
                        val label = choice.label.displayCapOrNull(OrbitAgentUiCaps.MAX_LABEL_CHARS)
                            ?: return@mapNotNull null
                        OrbitAgentUiChoice(
                            id = choice.choiceId.displayCap(OrbitAgentUiCaps.MAX_ID_CHARS),
                            label = label,
                            evidenceIds = choice.evidenceIds.cappedIds()
                        )
                    },
                    evidenceIds = question.evidenceIds.cappedIds()
                )
            )
        }
        steps.take(OrbitAgentUiCaps.MAX_STEPS).forEach { step ->
            val label = step.label.displayCapOrNull(OrbitAgentUiCaps.MAX_LABEL_CHARS)
                ?: return@forEach
            add(
                OrbitAgentUiStep(
                    id = step.stepId.displayCap(OrbitAgentUiCaps.MAX_ID_CHARS),
                    kind = step.kind.displayCap(OrbitAgentUiCaps.MAX_ID_CHARS),
                    label = label,
                    detail = step.detail.displayCapOrNull(OrbitAgentUiCaps.MAX_DETAIL_CHARS),
                    requiresApproval = step.requiredApproval,
                    evidenceIds = step.evidenceIds.cappedIds()
                )
            )
        }
        evidence.take(OrbitAgentUiCaps.MAX_EVIDENCE).forEach { evidenceRef ->
            val label = evidenceRef.label.displayCapOrNull(OrbitAgentUiCaps.MAX_SOURCE_LABEL_CHARS)
                ?: return@forEach
            add(
                OrbitAgentUiEvidence(
                    id = evidenceRef.evidenceId.displayCap(OrbitAgentUiCaps.MAX_ID_CHARS),
                    label = label,
                    sourceType = evidenceRef.sourceType.displayCap(OrbitAgentUiCaps.MAX_ID_CHARS),
                    sourceId = evidenceRef.sourceId.displayCap(OrbitAgentUiCaps.MAX_ID_CHARS),
                    dayLocal = evidenceRef.dayLocal.displayCapOrNull(OrbitAgentUiCaps.MAX_ID_CHARS)
                )
            )
        }
    }
    return OrbitAgentUiDocument(
        documentId = planId.displayCap(OrbitAgentUiCaps.MAX_ID_CHARS),
        components = components,
        fallbackText = components.toOrbitAgentUiPlainText()
    )
}
