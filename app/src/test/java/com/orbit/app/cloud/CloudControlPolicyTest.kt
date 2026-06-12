package com.orbit.app.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudControlPolicyTest {

    @Test
    fun compactIndexDisabledDeniesUpsertWithNoFallback() {
        val policy = policy(settings(compactMemoryIndexEnabled = false))

        val decision = policy.decide(CloudCapability.COMPACT_INDEX_UPSERT, requestId = "req-index")

        assertFalse(decision.allowed)
        assertEquals(BudgetDecisionReason.DISABLED_BY_USER, decision.reason)
        assertEquals(FallbackMode.NONE, decision.fallbackMode)
    }

    @Test
    fun cloudAskDisabledDeniesGroundedAskWithLocalFallback() {
        val policy = policy(settings(cloudAskSynthesisEnabled = false))

        val decision = policy.decide(CloudCapability.ASK_GROUNDED_SYNTHESIS, requestId = "req-ask")

        assertFalse(decision.allowed)
        assertEquals(BudgetDecisionReason.DISABLED_BY_USER, decision.reason)
        assertEquals(FallbackMode.LOCAL_DETERMINISTIC, decision.fallbackMode)
    }

    @Test
    fun cloudAiDisabledDeniesLlmWhenNoLocalModelExists() {
        val policy = policy(settings(cloudAiRoutingEnabled = false))

        val decision = policy.decide(CloudCapability.LLM_SUMMARIZE, requestId = "req-llm")

        assertFalse(decision.allowed)
        assertEquals(BudgetDecisionReason.DISABLED_BY_USER, decision.reason)
        assertEquals(FallbackMode.UNAVAILABLE_COPY, decision.fallbackMode)
    }

    @Test
    fun cloudAiDisabledRoutesToLocalModelWhenCapabilityExists() {
        val policy = policy(
            settings = settings(cloudAiRoutingEnabled = false),
            localModelAvailable = { it == CloudCapability.LLM_SUMMARIZE },
        )

        val decision = policy.decide(CloudCapability.LLM_SUMMARIZE, requestId = "req-llm")

        assertFalse(decision.allowed)
        assertEquals(BudgetDecisionReason.LOCAL_ONLY_MODE, decision.reason)
        assertEquals(FallbackMode.LOCAL_MODEL, decision.fallbackMode)
    }

    @Test
    fun budgetCapDeniesWhenEstimatedCostExceedsRemainingBudget() {
        val policy = CloudControlPolicy(
            settings = { settings(dailyCloudBudgetCents = 5L) },
            budgetRemainingCents = { 5L },
        )

        val decision = policy.decide(
            capability = CloudCapability.LLM_EXTRACT_ACTIONS,
            requestId = "req-budget",
            estimatedCostCents = 6L,
        )

        assertFalse(decision.allowed)
        assertEquals(BudgetDecisionReason.BUDGET_EXHAUSTED, decision.reason)
        assertEquals(6L, decision.estimatedCostCents)
        assertEquals(5L, decision.budgetRemainingCents)
        assertEquals(FallbackMode.UNAVAILABLE_COPY, decision.fallbackMode)
    }

    @Test
    fun allowedDecisionPreservesBudgetMetadata() {
        val policy = CloudControlPolicy(
            settings = { settings(dailyCloudBudgetCents = 10L) },
            budgetRemainingCents = { 10L },
        )

        val decision = policy.decide(
            capability = CloudCapability.ASK_GROUNDED_SYNTHESIS,
            requestId = "req-ok",
            estimatedCostCents = 4L,
        )

        assertTrue(decision.allowed)
        assertEquals(BudgetDecisionReason.ALLOWED, decision.reason)
        assertEquals(FallbackMode.NONE, decision.fallbackMode)
        assertEquals(10L, decision.budgetRemainingCents)
    }

    private fun policy(
        settings: CloudControlSettings,
        localModelAvailable: (CloudCapability) -> Boolean = { false },
    ) = CloudControlPolicy(
        settings = { settings },
        localModelAvailable = localModelAvailable,
    )

    private fun settings(
        compactMemoryIndexEnabled: Boolean = true,
        cloudAskSynthesisEnabled: Boolean = true,
        cloudAiRoutingEnabled: Boolean = true,
        dailyCloudBudgetCents: Long? = null,
    ) = CloudControlSettings(
        compactMemoryIndexEnabled = compactMemoryIndexEnabled,
        cloudAskSynthesisEnabled = cloudAskSynthesisEnabled,
        cloudAiRoutingEnabled = cloudAiRoutingEnabled,
        dailyCloudBudgetCents = dailyCloudBudgetCents,
    )
}
