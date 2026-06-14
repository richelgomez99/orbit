package com.orbit.app.agent

import com.orbit.app.data.ipc.ActionDraftParcel
import com.orbit.app.data.ipc.AppFunctionSummaryParcel
import com.orbit.app.data.ipc.GraphWhyThisParcel
import java.util.Locale

fun AppFunctionSummaryParcel.toAgentActionCapability(): AgentActionCapability =
    AgentActionCapability(
        functionId = functionId,
        displayName = displayName,
        verbs = verbsForFunction(functionId) + words(displayName) + words(description),
        requiresApproval = true,
    )

fun ActionDraftParcel.toAgentPlanStep(evidenceId: String): AgentPlanStep =
    AgentPlanStep(
        stepId = "action-$proposalId",
        kind = AgentPlanStepKind.DRAFT_ACTION,
        label = previewTitle,
        detail = previewSubtitle ?: displayName,
        requiredApproval = true,
        actionProposalId = proposalId,
        functionId = functionId,
        evidenceIds = listOf(evidenceId),
    )

fun GraphWhyThisParcel.toAgentEvidenceRefs(): List<AgentEvidenceRef> =
    sources.take(AgentPlanValidator.MAX_EVIDENCE).mapIndexed { index, source ->
        AgentEvidenceRef(
            evidenceId = "graph-$targetType-$targetId-source-$index",
            sourceType = when (targetType) {
                "RELATIONSHIP" -> AgentEvidenceSourceType.GRAPH_RELATIONSHIP
                else -> AgentEvidenceSourceType.GRAPH_FACT
            },
            sourceId = source.sourceId,
            label = source.label.ifBlank { title },
            dayLocal = source.dayLocal,
            whyThisTargetType = targetType,
            whyThisTargetId = targetId,
        )
    }

fun ActionDraftParcel.toAgentEvidenceRef(): AgentEvidenceRef =
    AgentEvidenceRef(
        evidenceId = "action-source-$proposalId",
        sourceType = AgentEvidenceSourceType.ACTION_PROPOSAL,
        sourceId = proposalId,
        label = sourceTitle,
        dayLocal = sourceDayLocal,
        whyThisTargetType = null,
        whyThisTargetId = null,
    )

private fun verbsForFunction(functionId: String): List<String> = when (functionId) {
    "calendar.createEvent" -> listOf("calendar", "schedule", "reschedule", "appointment", "event")
    "tasks.createTodo" -> listOf("todo", "task", "list", "buy", "remember", "follow")
    "share.delegate" -> listOf("share", "reply", "message", "send", "draft")
    "cluster.summarize" -> listOf("summarize", "summary", "cluster", "research")
    else -> words(functionId)
}

private fun words(value: String): List<String> =
    value.lowercase(Locale.US)
        .split(Regex("[^a-z0-9]+"))
        .filter { it.length >= 3 }
