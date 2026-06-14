package com.orbit.app.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeterministicAgentPlannerTest {

    private val planner = DeterministicAgentPlanner(
        clock = { 1_780_000_000_000L },
        idGenerator = { "fixed" },
    )

    @Test
    fun citedEvidenceAndCapabilityProduceApprovalRequiredPlan() {
        val plan = planner.plan(
            request = AgentRequest(
                requestId = "request-1",
                query = "help me reschedule the dentist",
            ),
            evidence = listOf(evidence("evidence-1", "Dentist appointment cancelled")),
            actionCapabilities = listOf(calendarCapability()),
        )

        assertEquals(AgentPlanOutcome.PLAN, plan.outcome)
        assertNull(plan.modelLabel)
        assertEquals(emptyList<String>(), AgentPlanValidator.validate(plan))
        assertEquals(
            listOf(AgentPlanStepKind.REVIEW_EVIDENCE, AgentPlanStepKind.DRAFT_ACTION),
            plan.steps.map { it.kind }
        )
        assertEquals(true, plan.steps.last().requiredApproval)
        assertEquals("calendar.create_event", plan.steps.last().functionId)
    }

    @Test
    fun ambiguousEvidenceReturnsGapQuestionWithChoices() {
        val plan = planner.plan(
            request = AgentRequest(
                requestId = "request-1",
                query = "draft a reply to that event",
            ),
            evidence = listOf(
                evidence("evidence-1", "Founder breakfast"),
                evidence("evidence-2", "Demo night"),
            ),
            actionCapabilities = listOf(shareCapability()),
        )

        assertEquals(AgentPlanOutcome.ASK_USER, plan.outcome)
        assertEquals(listOf("ambiguous_target"), plan.limitations)
        assertEquals(2, plan.questions.single().choices.size)
        assertEquals(emptyList<String>(), AgentPlanValidator.validate(plan))
    }

    @Test
    fun missingEvidenceRefusesWithoutModelLabel() {
        val plan = planner.plan(
            request = AgentRequest(
                requestId = "request-1",
                query = "what is my passport number",
                allowModelAssist = true,
            ),
            evidence = emptyList(),
            actionCapabilities = emptyList(),
        )

        assertEquals(AgentPlanOutcome.REFUSE, plan.outcome)
        assertEquals(listOf("not_enough_saved_evidence"), plan.limitations)
        assertNull(plan.modelLabel)
        assertEquals(emptyList<String>(), AgentPlanValidator.validate(plan))
    }

    @Test
    fun attachedEnvelopeDisambiguatesMultipleEvidenceRows() {
        val plan = planner.plan(
            request = AgentRequest(
                requestId = "request-1",
                query = "help with this appointment",
                attachedEnvelopeIds = listOf("env-1"),
            ),
            evidence = listOf(
                evidence("evidence-1", "Dentist appointment", sourceId = "env-1"),
                evidence("evidence-2", "Vet appointment", sourceId = "env-2"),
            ),
            actionCapabilities = listOf(calendarCapability()),
        )

        assertEquals(AgentPlanOutcome.PLAN, plan.outcome)
        assertEquals(emptyList<String>(), AgentPlanValidator.validate(plan))
    }

    private fun evidence(
        id: String,
        label: String,
        sourceId: String = "env-1",
    ) = AgentEvidenceRef(
        evidenceId = id,
        sourceType = AgentEvidenceSourceType.ENVELOPE,
        sourceId = sourceId,
        label = label,
        dayLocal = "2026-06-12",
        whyThisTargetType = null,
        whyThisTargetId = null,
    )

    private fun calendarCapability() = AgentActionCapability(
        functionId = "calendar.create_event",
        displayName = "calendar event",
        verbs = listOf("schedule", "reschedule", "appointment"),
    )

    private fun shareCapability() = AgentActionCapability(
        functionId = "share.delegate",
        displayName = "message draft",
        verbs = listOf("reply", "message", "send"),
    )
}
