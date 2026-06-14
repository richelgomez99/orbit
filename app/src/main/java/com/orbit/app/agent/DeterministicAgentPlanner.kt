package com.orbit.app.agent

import java.util.Locale
import java.util.UUID

class DeterministicAgentPlanner(
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
) {

    fun plan(
        request: AgentRequest,
        evidence: List<AgentEvidenceRef>,
        actionCapabilities: List<AgentActionCapability> = emptyList(),
    ): AgentPlanDraft {
        val query = request.query.trim().take(MAX_QUERY_CHARS)
        if (query.isBlank()) {
            return refusal("empty_request", "Tell Orbit what loop you want to close.")
        }

        val cappedEvidence = evidence
            .distinctBy { it.evidenceId }
            .take(request.maxEvidence.coerceIn(1, AgentPlanValidator.MAX_EVIDENCE))

        if (cappedEvidence.isEmpty()) {
            return refusal(
                reason = "not_enough_saved_evidence",
                limitation = "I could not find saved evidence for that request."
            )
        }

        if (shouldAskForTarget(request, cappedEvidence)) {
            return askUser(query, cappedEvidence)
        }

        val selectedEvidence = cappedEvidence.first()
        val capability = actionCapabilities.firstOrNull { it.matches(query) }

        val steps = buildList {
            add(
                AgentPlanStep(
                    stepId = "step-review",
                    kind = AgentPlanStepKind.REVIEW_EVIDENCE,
                    label = "Review the saved source",
                    detail = selectedEvidence.label,
                    requiredApproval = false,
                    actionProposalId = null,
                    functionId = null,
                    evidenceIds = listOf(selectedEvidence.evidenceId),
                )
            )
            if (capability != null) {
                add(
                    AgentPlanStep(
                        stepId = "step-action",
                        kind = AgentPlanStepKind.DRAFT_ACTION,
                        label = "Draft ${capability.displayName}",
                        detail = "Review before anything is sent or opened.",
                        requiredApproval = capability.requiresApproval,
                        actionProposalId = null,
                        functionId = capability.functionId,
                        evidenceIds = listOf(selectedEvidence.evidenceId),
                    )
                )
            } else {
                add(
                    AgentPlanStep(
                        stepId = "step-question",
                        kind = AgentPlanStepKind.ASK_USER,
                        label = "Ask what you want to do next",
                        detail = "I found the source but need your preferred next action.",
                        requiredApproval = false,
                        actionProposalId = null,
                        functionId = null,
                        evidenceIds = listOf(selectedEvidence.evidenceId),
                    )
                )
            }
        }

        return AgentPlanDraft(
            planId = "plan-${idGenerator()}",
            outcome = AgentPlanOutcome.PLAN,
            title = "Plan from saved evidence",
            summary = "I found saved evidence and prepared approval-first next steps.",
            steps = steps,
            questions = emptyList(),
            evidence = cappedEvidence,
            limitations = emptyList(),
            modelLabel = null,
            createdAtMillis = clock(),
        )
    }

    private fun shouldAskForTarget(
        request: AgentRequest,
        evidence: List<AgentEvidenceRef>,
    ): Boolean {
        if (evidence.size <= 1) return false
        if (request.attachedEnvelopeIds.isNotEmpty()) return false
        val query = request.query.lowercase(Locale.US)
        return listOf("that", "this", "it", "event", "appointment", "message")
            .any { token -> query.contains(token) }
    }

    private fun askUser(query: String, evidence: List<AgentEvidenceRef>): AgentPlanDraft {
        val choices = evidence.take(MAX_CHOICES).map { ref ->
            AgentChoice(
                choiceId = "choice-${ref.evidenceId}",
                label = ref.label,
                evidenceIds = listOf(ref.evidenceId),
            )
        }
        return AgentPlanDraft(
            planId = "plan-${idGenerator()}",
            outcome = AgentPlanOutcome.ASK_USER,
            title = "Which saved item?",
            summary = null,
            steps = emptyList(),
            questions = listOf(
                AgentQuestion(
                    questionId = "question-target",
                    text = "Which saved item should I use for: $query?",
                    choices = choices,
                    evidenceIds = choices.flatMap { it.evidenceIds }.distinct(),
                )
            ),
            evidence = evidence.take(AgentPlanValidator.MAX_EVIDENCE),
            limitations = listOf("ambiguous_target"),
            modelLabel = null,
            createdAtMillis = clock(),
        )
    }

    private fun refusal(reason: String, limitation: String): AgentPlanDraft =
        AgentPlanDraft(
            planId = "plan-${idGenerator()}",
            outcome = AgentPlanOutcome.REFUSE,
            title = "Not enough saved evidence",
            summary = null,
            steps = listOf(
                AgentPlanStep(
                    stepId = "step-refuse",
                    kind = AgentPlanStepKind.REFUSE,
                    label = "Cannot plan safely",
                    detail = limitation,
                    requiredApproval = false,
                    actionProposalId = null,
                    functionId = null,
                    evidenceIds = emptyList(),
                )
            ),
            questions = emptyList(),
            evidence = emptyList(),
            limitations = listOf(reason),
            modelLabel = null,
            createdAtMillis = clock(),
        )

    private fun AgentActionCapability.matches(query: String): Boolean {
        val normalized = query.lowercase(Locale.US)
        return verbs.any { verb -> normalized.contains(verb.lowercase(Locale.US)) }
    }

    private companion object {
        const val MAX_QUERY_CHARS = 240
        const val MAX_CHOICES = 5
    }
}
