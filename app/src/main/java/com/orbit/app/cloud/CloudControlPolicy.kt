package com.orbit.app.cloud

class CloudControlPolicy(
    private val settings: () -> CloudControlSettings,
    private val budgetRemainingCents: () -> Long? = { settings().dailyCloudBudgetCents },
    private val localModelAvailable: (CloudCapability) -> Boolean = { false },
) {
    fun decide(
        capability: CloudCapability,
        requestId: String,
        estimatedCostCents: Long? = null,
    ): BudgetDecision {
        val snapshot = settings()
        val disabledReason = disabledReason(capability, snapshot)
        if (disabledReason != null) {
            return denied(
                capability = capability,
                requestId = requestId,
                reason = disabledReason,
                estimatedCostCents = estimatedCostCents,
            )
        }

        val remaining = budgetRemainingCents()
        if (remaining != null && estimatedCostCents != null && estimatedCostCents > remaining) {
            return BudgetDecision(
                capability = capability,
                allowed = false,
                reason = BudgetDecisionReason.BUDGET_EXHAUSTED,
                requestId = requestId,
                estimatedCostCents = estimatedCostCents,
                budgetRemainingCents = remaining,
                fallbackMode = fallbackModeFor(capability),
            )
        }

        return BudgetDecision(
            capability = capability,
            allowed = true,
            reason = BudgetDecisionReason.ALLOWED,
            requestId = requestId,
            estimatedCostCents = estimatedCostCents,
            budgetRemainingCents = remaining,
            fallbackMode = FallbackMode.NONE,
        )
    }

    private fun disabledReason(
        capability: CloudCapability,
        snapshot: CloudControlSettings,
    ): BudgetDecisionReason? = when (capability) {
        CloudCapability.COMPACT_INDEX_UPSERT,
        CloudCapability.COMPACT_INDEX_TOMBSTONE,
        CloudCapability.LIBRARY_SEMANTIC_SEARCH,
        -> if (!snapshot.compactMemoryIndexEnabled) BudgetDecisionReason.DISABLED_BY_USER else null

        CloudCapability.ASK_GROUNDED_SYNTHESIS ->
            if (!snapshot.cloudAskSynthesisEnabled) BudgetDecisionReason.DISABLED_BY_USER else null

        CloudCapability.LLM_CLASSIFY_INTENT,
        CloudCapability.LLM_SUMMARIZE,
        CloudCapability.LLM_EXTRACT_ACTIONS,
        CloudCapability.LLM_EMBED,
        CloudCapability.LLM_DAY_HEADER,
        CloudCapability.LLM_SENSITIVITY_SCAN,
        -> if (!snapshot.cloudAiRoutingEnabled) {
            if (localModelAvailable(capability)) {
                BudgetDecisionReason.LOCAL_ONLY_MODE
            } else {
                BudgetDecisionReason.DISABLED_BY_USER
            }
        } else {
            null
        }
    }

    private fun denied(
        capability: CloudCapability,
        requestId: String,
        reason: BudgetDecisionReason,
        estimatedCostCents: Long?,
    ) = BudgetDecision(
        capability = capability,
        allowed = false,
        reason = reason,
        requestId = requestId,
        estimatedCostCents = estimatedCostCents,
        budgetRemainingCents = budgetRemainingCents(),
        fallbackMode = fallbackModeFor(capability),
    )

    private fun fallbackModeFor(capability: CloudCapability): FallbackMode = when (capability) {
        CloudCapability.COMPACT_INDEX_UPSERT,
        CloudCapability.COMPACT_INDEX_TOMBSTONE,
        -> FallbackMode.NONE
        CloudCapability.LIBRARY_SEMANTIC_SEARCH,
        CloudCapability.ASK_GROUNDED_SYNTHESIS,
        -> FallbackMode.LOCAL_DETERMINISTIC
        CloudCapability.LLM_CLASSIFY_INTENT,
        CloudCapability.LLM_SUMMARIZE,
        CloudCapability.LLM_EXTRACT_ACTIONS,
        CloudCapability.LLM_EMBED,
        CloudCapability.LLM_DAY_HEADER,
        CloudCapability.LLM_SENSITIVITY_SCAN,
        -> if (localModelAvailable(capability)) FallbackMode.LOCAL_MODEL else FallbackMode.UNAVAILABLE_COPY
    }
}
