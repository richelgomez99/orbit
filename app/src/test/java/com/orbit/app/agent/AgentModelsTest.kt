package com.orbit.app.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AgentModelsTest {

    @Test
    fun citedApprovalRequiredActionPlanPassesValidation() {
        val plan = validPlan()

        assertEquals(emptyList<String>(), AgentPlanValidator.validate(plan))
    }

    @Test
    fun actionStepMustRequireApproval() {
        val plan = validPlan().copy(
            steps = listOf(
                validPlan().steps.single().copy(requiredApproval = false)
            )
        )

        assertEquals(
            listOf("action_without_approval:step-1"),
            AgentPlanValidator.validate(plan)
        )
    }

    @Test
    fun evidenceBackedStepsMustCiteKnownEvidence() {
        val plan = validPlan().copy(
            steps = listOf(
                validPlan().steps.single().copy(evidenceIds = listOf("missing"))
            )
        )

        assertEquals(
            listOf("step_missing_known_evidence:step-1"),
            AgentPlanValidator.validate(plan)
        )
    }

    @Test
    fun planModelRejectsRawPayloadFieldNames() {
        val plan = validPlan().copy(summary = "modelResponse should never be surfaced")

        assertEquals(
            listOf("raw_payload_field:modelResponse"),
            AgentPlanValidator.validate(plan)
        )
    }

    @Test
    fun traceReceiptContainsOnlyBoundedMetadata() {
        val receipt = AgentTraceReceipt(
            requestId = "request-1",
            queryDigest = "abc123",
            outcome = AgentPlanOutcome.PLAN,
            evidenceCount = 1,
            stepCount = 1,
            modelLabel = null,
            latencyMs = 12,
            createdAtMillis = 1_780_000_000_000L,
        )

        val rendered = receipt.toString()
        listOf("rawOcr", "screenshot", "prompt", "embedding", "modelResponse", "apiKey").forEach {
            assertFalse(rendered.contains(it))
        }
    }

    private fun validPlan() = AgentPlanDraft(
        planId = "plan-1",
        outcome = AgentPlanOutcome.PLAN,
        title = "Reschedule dentist",
        summary = "Use the saved dentist capture as evidence.",
        steps = listOf(
            AgentPlanStep(
                stepId = "step-1",
                kind = AgentPlanStepKind.DRAFT_ACTION,
                label = "Draft calendar follow-up",
                detail = "Review before anything is sent.",
                requiredApproval = true,
                actionProposalId = null,
                functionId = "share.delegate",
                evidenceIds = listOf("evidence-1"),
            )
        ),
        questions = emptyList(),
        evidence = listOf(
            AgentEvidenceRef(
                evidenceId = "evidence-1",
                sourceType = AgentEvidenceSourceType.ENVELOPE,
                sourceId = "env-1",
                label = "Dentist appointment",
                dayLocal = "2026-06-12",
                whyThisTargetType = null,
                whyThisTargetId = null,
            )
        ),
        limitations = emptyList(),
        modelLabel = null,
        createdAtMillis = 1_780_000_000_000L,
    )
}
