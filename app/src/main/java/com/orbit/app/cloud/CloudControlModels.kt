package com.orbit.app.cloud

enum class CloudCapability {
    COMPACT_INDEX_UPSERT,
    COMPACT_INDEX_TOMBSTONE,
    LIBRARY_SEMANTIC_SEARCH,
    ASK_GROUNDED_SYNTHESIS,
    LLM_CLASSIFY_INTENT,
    LLM_SUMMARIZE,
    LLM_EXTRACT_ACTIONS,
    LLM_EMBED,
    LLM_DAY_HEADER,
    LLM_SENSITIVITY_SCAN,
}

enum class BudgetDecisionReason {
    ALLOWED,
    DISABLED_BY_USER,
    BUDGET_EXHAUSTED,
    NO_SESSION,
    LOCAL_ONLY_MODE,
    CAPABILITY_UNAVAILABLE,
}

enum class FallbackMode {
    NONE,
    LOCAL_DETERMINISTIC,
    LOCAL_MODEL,
    UNAVAILABLE_COPY,
}

enum class CloudUsageOutcome {
    ALLOWED,
    SUCCESS,
    SKIPPED,
    FALLBACK_USED,
    FAILED,
    BUDGET_DENIED,
}

data class CloudControlSettings(
    val compactMemoryIndexEnabled: Boolean,
    val cloudAskSynthesisEnabled: Boolean,
    val cloudAiRoutingEnabled: Boolean,
    val dailyCloudBudgetCents: Long?,
)

data class BudgetDecision(
    val capability: CloudCapability,
    val allowed: Boolean,
    val reason: BudgetDecisionReason,
    val requestId: String,
    val estimatedCostCents: Long? = null,
    val budgetRemainingCents: Long? = null,
    val fallbackMode: FallbackMode,
)
