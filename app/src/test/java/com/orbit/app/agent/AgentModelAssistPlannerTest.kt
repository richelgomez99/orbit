package com.orbit.app.agent

import com.orbit.app.ai.EmbeddingResult
import com.orbit.app.ai.LlmProvider
import com.orbit.app.ai.UnavailableLlmProvider
import com.orbit.app.ai.model.ActionExtractionResult
import com.orbit.app.ai.model.AppFunctionSummary
import com.orbit.app.ai.model.DayHeaderResult
import com.orbit.app.ai.model.IntentClassification
import com.orbit.app.ai.model.LlmProvenance
import com.orbit.app.ai.model.SensitivityResult
import com.orbit.app.ai.model.SummaryResult
import com.orbit.app.data.entity.StateSnapshot
import com.orbit.app.data.model.Intent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AgentModelAssistPlannerTest {

    @Test
    fun validStructuredCopyUpdatesDisplayFieldsOnly() = runTest {
        val planner = AgentModelAssistPlanner(
            FakeLlm(
                """
                TITLE: Reschedule the dentist appointment
                SUMMARY: The saved dentist item says the appointment changed; review it before drafting anything.
                STEP step-review: Open the dentist cancellation note
                CITE step-review: evidence-1
                STEP step-action: Draft a reschedule request
                CITE step-action: evidence-1
                FUNCTION step-action: calendar.create_event
                """.trimIndent()
            )
        )

        val assisted = planner.assist(
            request = request(),
            deterministicPlan = plan(),
            actionCapabilities = listOf(calendarCapability()),
        )

        assertNotNull(assisted)
        requireNotNull(assisted)
        assertEquals("Reschedule the dentist appointment", assisted.title)
        assertEquals("openai:test-model", assisted.modelLabel)
        assertEquals("Open the dentist cancellation note", assisted.steps.first().label)
        assertEquals("calendar.create_event", assisted.steps.last().functionId)
        assertEquals(listOf("evidence-1"), assisted.steps.last().evidenceIds)
        assertEquals(emptyList<String>(), AgentPlanValidator.validate(assisted))
    }

    @Test
    fun unknownEvidenceCitationForcesFallback() = runTest {
        val planner = AgentModelAssistPlanner(
            FakeLlm(
                """
                TITLE: Unsafe title
                STEP step-review: Use something else
                CITE step-review: evidence-missing
                """.trimIndent()
            )
        )

        assertNull(
            planner.assist(
                request = request(),
                deterministicPlan = plan(),
                actionCapabilities = listOf(calendarCapability()),
            )
        )
    }

    @Test
    fun unknownFunctionForcesFallback() = runTest {
        val planner = AgentModelAssistPlanner(
            FakeLlm(
                """
                TITLE: Unsafe title
                FUNCTION step-action: mail.send
                """.trimIndent()
            )
        )

        assertNull(
            planner.assist(
                request = request(),
                deterministicPlan = plan(),
                actionCapabilities = listOf(calendarCapability()),
            )
        )
    }

    @Test
    fun unavailableProviderReturnsNullFallback() = runTest {
        val planner = AgentModelAssistPlanner(UnavailableLlmProvider())

        assertNull(
            planner.assist(
                request = request(),
                deterministicPlan = plan(),
                actionCapabilities = listOf(calendarCapability()),
            )
        )
    }

    @Test
    fun refusedPlanDoesNotCallModel() = runTest {
        val llm = FakeLlm("TITLE: Should not run")
        val planner = AgentModelAssistPlanner(llm)

        val assisted = planner.assist(
            request = request(),
            deterministicPlan = plan().copy(
                outcome = AgentPlanOutcome.REFUSE,
                evidence = emptyList(),
            ),
            actionCapabilities = emptyList(),
        )

        assertNull(assisted)
        assertEquals(0, llm.summarizeCalls)
    }

    private fun request() = AgentRequest(
        requestId = "request-1",
        query = "help me reschedule the dentist",
        allowModelAssist = true,
    )

    private fun plan() = AgentPlanDraft(
        planId = "plan-1",
        outcome = AgentPlanOutcome.PLAN,
        title = "Plan from saved evidence",
        summary = "I found saved evidence and prepared approval-first next steps.",
        steps = listOf(
            AgentPlanStep(
                stepId = "step-review",
                kind = AgentPlanStepKind.REVIEW_EVIDENCE,
                label = "Review the saved source",
                detail = "Dentist appointment cancelled",
                requiredApproval = false,
                actionProposalId = null,
                functionId = null,
                evidenceIds = listOf("evidence-1"),
            ),
            AgentPlanStep(
                stepId = "step-action",
                kind = AgentPlanStepKind.DRAFT_ACTION,
                label = "Draft calendar event",
                detail = "Review before anything is sent or opened.",
                requiredApproval = true,
                actionProposalId = null,
                functionId = "calendar.create_event",
                evidenceIds = listOf("evidence-1"),
            ),
        ),
        questions = emptyList(),
        evidence = listOf(
            AgentEvidenceRef(
                evidenceId = "evidence-1",
                sourceType = AgentEvidenceSourceType.ENVELOPE,
                sourceId = "env-1",
                label = "Dentist appointment cancelled",
                dayLocal = "2026-06-12",
                whyThisTargetType = null,
                whyThisTargetId = null,
            )
        ),
        limitations = emptyList(),
        modelLabel = null,
        createdAtMillis = 1_780_000_000_000L,
    )

    private fun calendarCapability() = AgentActionCapability(
        functionId = "calendar.create_event",
        displayName = "calendar event",
        verbs = listOf("schedule", "reschedule", "appointment"),
    )

    private class FakeLlm(private val summary: String) : LlmProvider {
        var summarizeCalls = 0

        override suspend fun classifyIntent(text: String, appCategory: String): IntentClassification =
            IntentClassification(Intent.AMBIGUOUS, 0f, LlmProvenance.OrbitManaged("openai:test-model"))

        override suspend fun summarize(text: String, maxTokens: Int): SummaryResult {
            summarizeCalls += 1
            return SummaryResult(
                text = summary,
                generationLocale = "en-US",
                provenance = LlmProvenance.OrbitManaged("openai:test-model"),
            )
        }

        override suspend fun scanSensitivity(text: String): SensitivityResult =
            SensitivityResult("[]", LlmProvenance.OrbitManaged("openai:test-model"))

        override suspend fun generateDayHeader(
            dayIsoDate: String,
            envelopeSummaries: List<String>,
        ): DayHeaderResult =
            DayHeaderResult("", "en-US", LlmProvenance.OrbitManaged("openai:test-model"))

        override suspend fun extractActions(
            text: String,
            contentType: String,
            state: StateSnapshot,
            registeredFunctions: List<AppFunctionSummary>,
            maxCandidates: Int,
        ): ActionExtractionResult =
            ActionExtractionResult(LlmProvenance.OrbitManaged("openai:test-model"), emptyList())

        override suspend fun embed(text: String): EmbeddingResult? = null
    }
}
