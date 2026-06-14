package com.orbit.app.agent

import com.orbit.app.data.ipc.AgentChoiceParcel
import com.orbit.app.data.ipc.AgentEvidenceParcel
import com.orbit.app.data.ipc.AgentPlanParcel
import com.orbit.app.data.ipc.AgentPlanStepParcel
import com.orbit.app.data.ipc.AgentQuestionParcel

fun AgentPlanDraft.toParcel(): AgentPlanParcel =
    AgentPlanParcel(
        planId = planId,
        outcome = outcome.name,
        title = title,
        summary = summary,
        steps = steps.take(AgentPlanValidator.MAX_STEPS).map { it.toParcel() },
        questions = questions.take(AgentPlanValidator.MAX_QUESTIONS).map { it.toParcel() },
        evidence = evidence.take(AgentPlanValidator.MAX_EVIDENCE).map { it.toParcel() },
        limitations = limitations.take(MAX_LIMITATIONS),
        modelLabel = modelLabel,
        createdAtMillis = createdAtMillis,
    )

private fun AgentPlanStep.toParcel(): AgentPlanStepParcel =
    AgentPlanStepParcel(
        stepId = stepId,
        kind = kind.name,
        label = label.take(MAX_LABEL_CHARS),
        detail = detail?.take(MAX_DETAIL_CHARS),
        requiredApproval = requiredApproval,
        actionProposalId = actionProposalId,
        functionId = functionId,
        evidenceIds = evidenceIds.take(AgentPlanValidator.MAX_EVIDENCE),
    )

private fun AgentQuestion.toParcel(): AgentQuestionParcel =
    AgentQuestionParcel(
        questionId = questionId,
        text = text.take(MAX_DETAIL_CHARS),
        choices = choices.take(MAX_CHOICES).map { it.toParcel() },
        evidenceIds = evidenceIds.take(AgentPlanValidator.MAX_EVIDENCE),
    )

private fun AgentChoice.toParcel(): AgentChoiceParcel =
    AgentChoiceParcel(
        choiceId = choiceId,
        label = label.take(MAX_LABEL_CHARS),
        evidenceIds = evidenceIds.take(AgentPlanValidator.MAX_EVIDENCE),
    )

private fun AgentEvidenceRef.toParcel(): AgentEvidenceParcel =
    AgentEvidenceParcel(
        evidenceId = evidenceId,
        sourceType = sourceType.name,
        sourceId = sourceId,
        label = label.take(MAX_LABEL_CHARS),
        dayLocal = dayLocal,
        whyThisTargetType = whyThisTargetType,
        whyThisTargetId = whyThisTargetId,
    )

private const val MAX_LABEL_CHARS = 160
private const val MAX_DETAIL_CHARS = 240
private const val MAX_CHOICES = 5
private const val MAX_LIMITATIONS = 5
