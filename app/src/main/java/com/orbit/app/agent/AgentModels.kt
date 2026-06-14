package com.orbit.app.agent

enum class AgentPlanOutcome {
    PLAN,
    ASK_USER,
    REFUSE,
    ERROR,
}

enum class AgentPlanStepKind {
    REVIEW_EVIDENCE,
    ASK_USER,
    DRAFT_ACTION,
    OPEN_CAPTURE,
    REFUSE,
}

enum class AgentEvidenceSourceType {
    ENVELOPE,
    GRAPH_FACT,
    GRAPH_RELATIONSHIP,
    PROMOTED_MEMORY,
    ACTION_PROPOSAL,
}

data class AgentRequest(
    val requestId: String,
    val query: String,
    val attachedEnvelopeIds: List<String> = emptyList(),
    val maxEvidence: Int = 10,
    val allowModelAssist: Boolean = false,
)

data class AgentPlanDraft(
    val planId: String,
    val outcome: AgentPlanOutcome,
    val title: String,
    val summary: String?,
    val steps: List<AgentPlanStep>,
    val questions: List<AgentQuestion>,
    val evidence: List<AgentEvidenceRef>,
    val limitations: List<String>,
    val modelLabel: String?,
    val createdAtMillis: Long,
)

data class AgentPlanStep(
    val stepId: String,
    val kind: AgentPlanStepKind,
    val label: String,
    val detail: String?,
    val requiredApproval: Boolean,
    val actionProposalId: String?,
    val functionId: String?,
    val evidenceIds: List<String>,
)

data class AgentEvidenceRef(
    val evidenceId: String,
    val sourceType: AgentEvidenceSourceType,
    val sourceId: String,
    val label: String,
    val dayLocal: String?,
    val whyThisTargetType: String?,
    val whyThisTargetId: String?,
)

data class AgentQuestion(
    val questionId: String,
    val text: String,
    val choices: List<AgentChoice>,
    val evidenceIds: List<String>,
)

data class AgentChoice(
    val choiceId: String,
    val label: String,
    val evidenceIds: List<String>,
)

data class AgentActionCapability(
    val functionId: String,
    val displayName: String,
    val verbs: List<String>,
    val requiresApproval: Boolean = true,
)

data class AgentTraceReceipt(
    val requestId: String,
    val queryDigest: String,
    val outcome: AgentPlanOutcome,
    val evidenceCount: Int,
    val stepCount: Int,
    val modelLabel: String?,
    val latencyMs: Long,
    val createdAtMillis: Long,
)

object AgentPlanValidator {
    private val rawPayloadWords = listOf(
        "rawOcr",
        "ocrText",
        "screenshot",
        "imageBytes",
        "prompt",
        "embedding",
        "modelResponse",
        "accessToken",
        "apiKey",
        "jwt",
        "cookie",
    )

    fun validate(plan: AgentPlanDraft): List<String> {
        val violations = mutableListOf<String>()
        val evidenceIds = plan.evidence.map { it.evidenceId }.toSet()

        if (plan.steps.size > MAX_STEPS) violations += "too_many_steps"
        if (plan.evidence.size > MAX_EVIDENCE) violations += "too_many_evidence"
        if (plan.questions.size > MAX_QUESTIONS) violations += "too_many_questions"

        plan.steps.forEach { step ->
            val missingEvidence = step.evidenceIds.any { it !in evidenceIds }
            if (missingEvidence) violations += "step_missing_known_evidence:${step.stepId}"

            when (step.kind) {
                AgentPlanStepKind.REVIEW_EVIDENCE,
                AgentPlanStepKind.DRAFT_ACTION,
                AgentPlanStepKind.OPEN_CAPTURE -> {
                    if (step.evidenceIds.isEmpty()) violations += "step_uncited:${step.stepId}"
                }
                AgentPlanStepKind.ASK_USER,
                AgentPlanStepKind.REFUSE -> Unit
            }

            if (step.kind == AgentPlanStepKind.DRAFT_ACTION) {
                if (!step.requiredApproval) violations += "action_without_approval:${step.stepId}"
                if (step.functionId.isNullOrBlank() && step.actionProposalId.isNullOrBlank()) {
                    violations += "action_without_function_or_proposal:${step.stepId}"
                }
            }
        }

        plan.questions.forEach { question ->
            val missingEvidence = question.evidenceIds.any { it !in evidenceIds }
            if (missingEvidence) violations += "question_missing_known_evidence:${question.questionId}"
            question.choices.forEach { choice ->
                if (choice.evidenceIds.any { it !in evidenceIds }) {
                    violations += "choice_missing_known_evidence:${choice.choiceId}"
                }
            }
        }

        val rendered = plan.toString()
        rawPayloadWords.forEach { forbidden ->
            if (rendered.contains(forbidden)) violations += "raw_payload_field:$forbidden"
        }

        return violations
    }

    const val MAX_STEPS = 5
    const val MAX_EVIDENCE = 10
    const val MAX_QUESTIONS = 3
}
