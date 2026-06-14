package com.orbit.app.agent

import com.orbit.app.data.ipc.ActionDraftParcel
import com.orbit.app.data.ipc.AppFunctionSummaryParcel
import com.orbit.app.data.ipc.GraphSourceParcel
import com.orbit.app.data.ipc.GraphWhyThisParcel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentIntegrationMappersTest {

    @Test
    fun appFunctionSummaryMapsToApprovalRequiredCapability() {
        val capability = calendarFunction().toAgentActionCapability()

        assertEquals("calendar.createEvent", capability.functionId)
        assertEquals(true, capability.requiresApproval)
        assertTrue(capability.verbs.contains("reschedule"))
    }

    @Test
    fun actionDraftMapsToApprovalRequiredPlanStepWithoutExecutionSignal() {
        val step = actionDraft().toAgentPlanStep(evidenceId = "evidence-1")

        assertEquals(AgentPlanStepKind.DRAFT_ACTION, step.kind)
        assertEquals(true, step.requiredApproval)
        assertEquals("proposal-1", step.actionProposalId)
        assertEquals("calendar.createEvent", step.functionId)
        assertEquals(listOf("evidence-1"), step.evidenceIds)
    }

    @Test
    fun graphWhyThisMapsToCompactEvidenceRefs() {
        val refs = graphWhyThis().toAgentEvidenceRefs()

        assertEquals(1, refs.size)
        assertEquals(AgentEvidenceSourceType.GRAPH_FACT, refs.single().sourceType)
        assertEquals("env-1", refs.single().sourceId)
        assertEquals("FACT", refs.single().whyThisTargetType)
        assertEquals("fact-1", refs.single().whyThisTargetId)
    }

    @Test
    fun mappedEvidenceAndDraftProduceValidPlan() {
        val draft = actionDraft()
        val evidence = draft.toAgentEvidenceRef()
        val plan = AgentPlanDraft(
            planId = "plan-1",
            outcome = AgentPlanOutcome.PLAN,
            title = "Review draft",
            summary = null,
            steps = listOf(draft.toAgentPlanStep(evidence.evidenceId)),
            questions = emptyList(),
            evidence = listOf(evidence),
            limitations = emptyList(),
            modelLabel = null,
            createdAtMillis = 1_780_000_000_000L,
        )

        assertEquals(emptyList<String>(), AgentPlanValidator.validate(plan))
        assertFalse(plan.toString().contains("execute"))
    }

    private fun calendarFunction() = AppFunctionSummaryParcel(
        functionId = "calendar.createEvent",
        appPackage = "com.orbit.app",
        displayName = "Add to Calendar",
        description = "Create a calendar event with a title, time, and optional location.",
        schemaVersion = 1,
        argsSchemaJson = "{}",
        sideEffects = "EXTERNAL_INTENT",
        reversibility = "EXTERNAL_MANAGED",
        sensitivityScope = "PERSONAL",
        registeredAtMillis = 1_780_000_000_000L,
        updatedAtMillis = 1_780_000_000_000L,
    )

    private fun actionDraft() = ActionDraftParcel(
        proposalId = "proposal-1",
        sourceEnvelopeId = "env-1",
        functionId = "calendar.createEvent",
        schemaVersion = 1,
        argsJson = "{}",
        previewTitle = "Review dentist appointment",
        previewSubtitle = "Opens Calendar only after confirmation.",
        confidence = 0.90f,
        provenance = "LOCAL",
        state = "PROPOSED",
        sensitivityScope = "PERSONAL",
        createdAtMillis = 1_780_000_000_000L,
        stateChangedAtMillis = 1_780_000_000_000L,
        displayName = "Add to Calendar",
        sideEffects = "EXTERNAL_INTENT",
        reversibility = "EXTERNAL_MANAGED",
        sourceTitle = "Dentist appointment",
        sourceAppLabel = "Messages",
        sourceDayLocal = "2026-06-12",
    )

    private fun graphWhyThis() = GraphWhyThisParcel(
        targetType = "FACT",
        targetId = "fact-1",
        title = "Dentist appointment",
        summary = null,
        sources = listOf(
            GraphSourceParcel(
                sourceType = "ENVELOPE",
                sourceId = "env-1",
                label = "Dentist appointment",
                dayLocal = "2026-06-12",
                createdAtMillis = 1_780_000_000_000L,
            )
        ),
    )
}
