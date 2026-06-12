# Data Model: Cloud Controls, Storage, And Budgeting

## CloudControlPreferences

Durable local preferences read by UI and policy gates.

Fields:

- `compactMemoryIndexEnabled: Boolean`
  - Existing backing key: `memory_indexing_enabled`.
  - Default: `false`.
  - Governs compact index upsert/tombstone and semantic Library cloud use.
- `cloudAskSynthesisEnabled: Boolean`
  - New durable key.
  - Default: `true` for continuity in debug/MVP builds unless product explicitly changes default before alpha.
  - Governs `MemoryGatewayRequest.GroundedAsk`.
- `cloudAiRoutingEnabled: Boolean`
  - New durable key.
  - Default: `true` for current cloud-default LLM architecture.
  - Governs `LlmProviderRouter` choosing `CloudLlmProvider`.
- `dailyCloudBudgetCents: Long?`
  - `null` means no local cap.
  - `0` means deny budgeted cloud AI requests.
  - Positive values are reserved for future real cost accounting.

Invariants:

- Stored locally only.
- Must be readable from the process performing the policy decision.
- No provider secrets or access tokens are stored here.

## CloudCapability

Closed set of cloud-eligible operation categories used for policy decisions and receipts.

Values:

- `COMPACT_INDEX_UPSERT`
- `COMPACT_INDEX_TOMBSTONE`
- `LIBRARY_SEMANTIC_SEARCH`
- `ASK_GROUNDED_SYNTHESIS`
- `LLM_CLASSIFY_INTENT`
- `LLM_SUMMARIZE`
- `LLM_EXTRACT_ACTIONS`
- `LLM_EMBED`
- `LLM_DAY_HEADER`
- `LLM_SENSITIVITY_SCAN`

Invariants:

- New cloud operations must add a capability before they call `:net`.
- Capabilities must be safe to show in Audit/Settings.

## BudgetDecision

Pure local decision result created before a cloud operation is attempted.

Fields:

- `capability: CloudCapability`
- `allowed: Boolean`
- `reason: BudgetDecisionReason`
- `requestId: String`
- `estimatedCostCents: Long?`
- `budgetRemainingCents: Long?`
- `fallbackMode: FallbackMode`

`BudgetDecisionReason` values:

- `ALLOWED`
- `DISABLED_BY_USER`
- `BUDGET_EXHAUSTED`
- `NO_SESSION`
- `LOCAL_ONLY_MODE`
- `CAPABILITY_UNAVAILABLE`

`FallbackMode` values:

- `NONE`
- `LOCAL_DETERMINISTIC`
- `LOCAL_MODEL`
- `UNAVAILABLE_COPY`

Invariants:

- A denied decision must be recorded locally if it corresponds to a user-visible operation.
- Request IDs are generated before the policy gate so skipped operations can still be correlated in logs.

## CloudUsageReceipt

Local-only audit projection for one cloud policy decision or gateway result.

Fields:

- `requestId: String`
- `capability: CloudCapability`
- `endpoint: String?`
- `outcome: CloudUsageOutcome`
- `reason: BudgetDecisionReason?`
- `envelopeId: String?`
- `latencyMs: Long?`
- `resultCount: Int?`
- `provider: String?`
- `model: String?`
- `inputDigest: String?`
- `payloadDigest: String?`
- `estimatedInputTokens: Int?`
- `estimatedOutputTokens: Int?`
- `estimatedCostCents: Long?`
- `createdAtMillis: Long`

`CloudUsageOutcome` values:

- `ALLOWED`
- `SUCCESS`
- `SKIPPED`
- `FALLBACK_USED`
- `FAILED`
- `BUDGET_DENIED`

Forbidden fields:

- Raw question text.
- Raw prompt text.
- Raw OCR/full body.
- Raw screenshot/image bytes.
- Embedding vectors.
- Raw model response.
- Access tokens, refresh tokens, API keys, JWTs, cookies.

Persistence:

- Preferred v1 persistence is `audit_log_entry.extraJson` using existing `AuditLogWriter`.
- A dedicated Room table is deferred unless receipt querying becomes too awkward for Settings.

## Policy Flow

1. Caller asks policy for a decision with `CloudCapability` and optional envelope/query metadata.
2. Policy reads local preferences and budget state.
3. If denied, caller records a `CloudUsageReceipt` and uses fallback.
4. If allowed, caller attempts the existing gateway/provider path through `:net`.
5. Caller records success/failure/fallback receipt with bounded metadata.

## Privacy Rules

- Receipts and preferences never leave the phone in this branch.
- Cloud compact index remains a mirror, not source of truth.
- Atlas stores compact index/search payloads only, as already constrained by Spec 005A/007 tests.
- Supabase/Vercel gateway remains the only cloud LLM/memory gateway path.
