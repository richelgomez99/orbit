package com.orbit.app.generativeui

import com.orbit.app.data.ipc.AgentChoiceParcel
import com.orbit.app.data.ipc.AgentEvidenceParcel
import com.orbit.app.data.ipc.AgentPlanParcel
import com.orbit.app.data.ipc.AgentPlanStepParcel
import com.orbit.app.data.ipc.AgentQuestionParcel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentPlanUiAdapterTest {

    @Test
    fun mapsAgentPlanToKnownComponentsAndPreservesEvidence() {
        val document = samplePlan().toOrbitAgentUiDocument()

        assertEquals("plan-1", document.documentId)
        assertTrue(document.components.first() is OrbitAgentUiTitle)
        val evidence = document.components.filterIsInstance<OrbitAgentUiEvidence>()
        assertEquals(listOf("ev-1"), evidence.map { it.id })
        assertEquals("ENVELOPE", evidence.single().sourceType)
        assertEquals("env-1", evidence.single().sourceId)
    }

    @Test
    fun capsComponentTextBeforeRendering() {
        val long = "x".repeat(500)
        val document = samplePlan(
            title = long,
            summary = long,
            stepLabel = long,
            stepDetail = long,
            evidenceLabel = long
        ).toOrbitAgentUiDocument()

        val title = document.components.filterIsInstance<OrbitAgentUiTitle>().single()
        val body = document.components.filterIsInstance<OrbitAgentUiBody>().single()
        val step = document.components.filterIsInstance<OrbitAgentUiStep>().single()
        val evidence = document.components.filterIsInstance<OrbitAgentUiEvidence>().single()

        assertTrue(title.text.length <= OrbitAgentUiCaps.MAX_TITLE_CHARS)
        assertTrue(body.text.length <= OrbitAgentUiCaps.MAX_BODY_CHARS)
        assertTrue(step.label.length <= OrbitAgentUiCaps.MAX_LABEL_CHARS)
        assertTrue(step.detail!!.length <= OrbitAgentUiCaps.MAX_DETAIL_CHARS)
        assertTrue(evidence.label.length <= OrbitAgentUiCaps.MAX_SOURCE_LABEL_CHARS)
        assertTrue(document.asPlainText().length <= OrbitAgentUiCaps.MAX_FALLBACK_CHARS)
    }

    @Test
    fun fallbackIncludesApprovalAndCitedEvidence() {
        val fallback = samplePlan().toOrbitAgentUiDocument().asPlainText()

        assertTrue(fallback.contains("requires approval"))
        assertTrue(fallback.contains("Evidence: Dentist appointment [ENVELOPE:env-1]"))
    }

    @Test
    fun adapterAppliesOutputCapsToLists() {
        val plan = samplePlan(
            steps = (0..10).map { step("step-$it") },
            questions = (0..10).map { question("q-$it") },
            evidence = (0..20).map { evidence("ev-$it", "env-$it") }
        )

        val document = plan.toOrbitAgentUiDocument()

        assertEquals(
            OrbitAgentUiCaps.MAX_STEPS,
            document.components.filterIsInstance<OrbitAgentUiStep>().size
        )
        assertEquals(
            OrbitAgentUiCaps.MAX_QUESTIONS,
            document.components.filterIsInstance<OrbitAgentUiQuestion>().size
        )
        assertEquals(
            OrbitAgentUiCaps.MAX_EVIDENCE,
            document.components.filterIsInstance<OrbitAgentUiEvidence>().size
        )
    }

    @Test
    fun displayPayloadDoesNotExposeFunctionOrProposalIdsInFallback() {
        val fallback = samplePlan().toOrbitAgentUiDocument().asPlainText()

        assertFalse(fallback.contains("orbit.calendar.create"))
        assertFalse(fallback.contains("proposal-1"))
    }

    private fun samplePlan(
        title: String = "Close the dentist loop",
        summary: String = "The appointment appears to need rescheduling.",
        stepLabel: String = "Review the appointment",
        stepDetail: String = "Use the cited capture before drafting anything.",
        evidenceLabel: String = "Dentist appointment",
        steps: List<AgentPlanStepParcel> = listOf(step("step-1", stepLabel, stepDetail)),
        questions: List<AgentQuestionParcel> = listOf(question("q-1")),
        evidence: List<AgentEvidenceParcel> = listOf(evidence("ev-1", "env-1", evidenceLabel))
    ): AgentPlanParcel = AgentPlanParcel(
        planId = "plan-1",
        outcome = "PLAN",
        title = title,
        summary = summary,
        steps = steps,
        questions = questions,
        evidence = evidence,
        limitations = listOf("needs_confirmation"),
        modelLabel = null,
        createdAtMillis = 1L
    )

    private fun step(
        id: String,
        label: String = "Review the appointment",
        detail: String = "Use the cited capture before drafting anything."
    ): AgentPlanStepParcel = AgentPlanStepParcel(
        stepId = id,
        kind = "REVIEW_EVIDENCE",
        label = label,
        detail = detail,
        requiredApproval = true,
        actionProposalId = "proposal-1",
        functionId = "orbit.calendar.create",
        evidenceIds = listOf("ev-1")
    )

    private fun question(id: String): AgentQuestionParcel = AgentQuestionParcel(
        questionId = id,
        text = "Should Orbit draft a message?",
        choices = listOf(
            AgentChoiceParcel("yes", "Draft it", listOf("ev-1")),
            AgentChoiceParcel("no", "Not now", listOf("ev-1"))
        ),
        evidenceIds = listOf("ev-1")
    )

    private fun evidence(
        id: String,
        sourceId: String,
        label: String = "Dentist appointment"
    ): AgentEvidenceParcel = AgentEvidenceParcel(
        evidenceId = id,
        sourceType = "ENVELOPE",
        sourceId = sourceId,
        label = label,
        dayLocal = "2026-06-14",
        whyThisTargetType = null,
        whyThisTargetId = null
    )
}
