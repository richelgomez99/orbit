# Contract: Cloud Controls And Receipts

## Scope

This contract defines the local API shape for Spec 008 policy gates. It is not a network contract and does not authorize new cloud payload fields.

## Kotlin Contract Sketch

```kotlin
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

data class BudgetDecision(
    val capability: CloudCapability,
    val allowed: Boolean,
    val reason: BudgetDecisionReason,
    val requestId: String,
    val estimatedCostCents: Long? = null,
    val budgetRemainingCents: Long? = null,
    val fallbackMode: FallbackMode,
)

interface CloudControlPolicy {
    fun decide(capability: CloudCapability, requestId: String): BudgetDecision
}
```

## Required Call-Site Behavior

### Compact Index Sync

- `COMPACT_INDEX_UPSERT` and `COMPACT_INDEX_TOMBSTONE` must deny when compact memory indexing is disabled.
- Denied decisions must not call `INetworkGateway.callMemoryGateway`.
- Denied decisions must write a skipped receipt.

### Ask Orbit

- `ASK_GROUNDED_SYNTHESIS` must deny when cloud Ask synthesis is disabled.
- Denied decisions must skip `MemoryGatewayRequest.GroundedAsk` entirely.
- Ask must then use local deterministic cited retrieval/refusal.

### Cloud LLM Routing

- LLM capabilities must deny when cloud AI routing is disabled.
- Denied decisions must prevent `CloudLlmProvider` selection.
- If no local provider is available, caller must return graceful unavailable/fallback copy.

## Receipt JSON Requirements

Receipt/audit JSON may include:

```json
{
  "requestId": "uuid",
  "capability": "ASK_GROUNDED_SYNTHESIS",
  "endpoint": "grounded_ask",
  "outcome": "SKIPPED",
  "reason": "DISABLED_BY_USER",
  "latencyMs": 0,
  "resultCount": 0,
  "inputDigest": "sha256:...",
  "estimatedInputTokens": null,
  "estimatedOutputTokens": null,
  "estimatedCostCents": null
}
```

Receipt/audit JSON must not include:

- `question`
- `prompt`
- `rawOcr`
- `ocrText`
- `screenshot`
- `imageBytes`
- `embedding`
- `modelResponse`
- `accessToken`
- `refreshToken`
- `apiKey`
- `jwt`
- `cookie`

## Test Obligations

- Unit tests must verify policy-denied call sites do not invoke fake gateways.
- Serialization/persistence tests must scan receipt JSON for forbidden keys.
- Compose tests must verify Settings exposes distinct rows for compact index, cloud Ask, and cloud AI routing.
