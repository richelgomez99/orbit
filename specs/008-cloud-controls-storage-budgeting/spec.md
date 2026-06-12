# Feature Specification: Cloud Controls, Storage, And Budgeting

**Feature Branch**: `feature/008-cloud-controls-storage-budgeting-20260612`
**Created**: 2026-06-12
**Status**: Draft
**Input**: Rebaselined Spec 008 placeholder, current Orbit MVP, Spec 005A/006/007 outputs, and the May 22 Orbit vision.

## User Scenarios & Testing

### User Story 1 - I Can See And Control Cloud Memory Indexing (Priority: P1)

As an Orbit user, I need a clear Settings surface that tells me whether compact memory indexing is enabled, what kind of data it sends, and what happened recently, so I can use Library/Ask cloud augmentation without wondering whether raw captures left the phone.

**Why this priority**: Compact Atlas indexing already exists and powers MVP semantic retrieval. It is the first cloud surface users can encounter, so it needs visible control before deeper KG/agent work layers on top.

**Independent Test**: Toggle compact memory indexing off, seed or capture a memory, and verify Orbit records a local skipped-sync receipt, keeps Diary/Library local behavior working, and does not call the memory gateway.

**Acceptance Scenarios**:

1. **Given** compact memory indexing is enabled, **When** the user opens Settings, **Then** Orbit shows the index as ON with copy that says only compact records are synced and raw screenshots/full OCR remain local.
2. **Given** compact memory indexing is disabled, **When** a capture is saved, **Then** Orbit skips the index sync, records a bounded local audit receipt, and continues to show the capture locally.
3. **Given** compact memory indexing is disabled, **When** the user runs Library search, **Then** Orbit uses the local fallback path and shows no crash or broken empty state.

---

### User Story 2 - I Can Disable Cloud Ask Synthesis Separately (Priority: P2)

As an Orbit user, I need Ask Orbit cloud synthesis to be a separate control from compact indexing, so I can keep semantic search available while requiring answers to come only from local deterministic cited retrieval.

**Why this priority**: Ask is an agent-facing surface. It should never silently upgrade from retrieval to cloud answer synthesis just because indexing is enabled.

**Independent Test**: Disable cloud Ask, ask a question with local matching evidence, and verify `BinderAskOrbitRepository` does not call `GroundedAsk`, still returns a cited local answer/refusal, and records a local policy receipt.

**Acceptance Scenarios**:

1. **Given** cloud Ask synthesis is disabled, **When** the user asks "what was rescheduled?", **Then** Orbit skips `MemoryGatewayRequest.GroundedAsk`, uses local cited retrieval, and labels the model/source as local deterministic.
2. **Given** cloud Ask synthesis is enabled but the gateway fails, **When** the user asks a supported question, **Then** Orbit falls back to local cited retrieval and records the gateway failure without raw question text.
3. **Given** a sensitive identifier question has no exact cited evidence, **When** Ask refuses, **Then** the copy is user-friendly and explains that Orbit needs a saved capture to answer sensitive identifiers.

---

### User Story 3 - I Can Understand Cloud AI Routing And Budget Use (Priority: P3)

As an Orbit user, I need Orbit to expose whether cloud AI routing is allowed for summaries/actions/classification and to keep a local receipt for each cloud AI request, so cloud augmentation remains inspectable and reversible.

**Why this priority**: The current `LlmProviderRouter` defaults to cloud when local Nano hardware is unavailable. That is acceptable for the current architecture only if the user can see and disable the policy before broader agentic features depend on it.

**Independent Test**: Turn cloud AI routing off, trigger a code path that would otherwise resolve `CloudLlmProvider`, and verify routing either uses local mode or returns a graceful local-unavailable result without opening the network.

**Acceptance Scenarios**:

1. **Given** cloud AI routing is disabled, **When** Orbit requests an LLM capability, **Then** the router does not return `CloudLlmProvider` and the caller receives a local fallback/unavailable result.
2. **Given** cloud AI routing is enabled, **When** Orbit calls the LLM gateway, **Then** the local audit receipt includes capability, provider/model label if known, request ID, latency, token/cost estimate if known, and hashes only for private text.
3. **Given** the budget cap is reached or set to zero, **When** a cloud AI request is attempted, **Then** Orbit skips the request, records a budget-denied receipt, and presents local fallback copy.

---

### User Story 4 - I Can Review Bounded Cloud Receipts (Priority: P4)

As an Orbit user, I need a readable "Cloud activity" view or section that summarizes recent compact index, Ask, and cloud AI decisions, so "what Orbit did today" can answer cloud-specific questions without exposing raw private content.

**Why this priority**: Audit rows already exist, but the current general audit log is not enough for a normal user to understand cloud policy decisions or cost/budget effects.

**Independent Test**: Generate one index skip, one Ask cloud skip, and one gateway failure, then open Settings/Audit and verify each appears with bounded, non-raw metadata.

**Acceptance Scenarios**:

1. **Given** several cloud decisions occurred today, **When** the user opens the cloud controls area, **Then** Orbit shows recent counts by category and links to the audit log.
2. **Given** a receipt is displayed, **When** it includes a private query/payload reference, **Then** only digests/counts/status are shown, never raw question text, raw OCR, prompts, embeddings, or model responses.
3. **Given** receipts age out, **When** retention cleanup runs, **Then** the audit log remains bounded by existing local retention rules.

### Edge Cases

- No Supabase session: controls remain visible; cloud actions fail closed to local fallback with `UNAUTHORIZED`/session copy.
- Offline device: cloud controls remain editable; cloud calls skip or fail into local fallback without blocking capture.
- Existing compact index enabled from debug seeding: Settings must reflect the persisted preference rather than a default assumption.
- User disables indexing after records already exist in Atlas: this spec must at least stop future sync and expose the state; remote tombstone-all is optional unless implemented explicitly.
- User disables Ask synthesis but leaves index enabled: semantic Library search may still use the index; Ask answer synthesis must be skipped.
- User disables cloud AI routing while no local model is available: Orbit must not crash; it should use deterministic fallback or "local AI unavailable" copy.
- Raw payload leak attempt: tests must prove cloud receipts and compact index payloads exclude raw screenshots, full OCR bodies, prompts, embeddings, and model responses.

## Requirements

### Functional Requirements

- **FR-008-001**: System MUST expose compact memory indexing as a durable user preference with clear ON/OFF state in Settings.
- **FR-008-002**: System MUST expose cloud Ask synthesis as a separate durable user preference from compact memory indexing.
- **FR-008-003**: System MUST expose cloud AI routing as a separate durable user preference from compact memory indexing and cloud Ask synthesis.
- **FR-008-004**: System MUST keep local deterministic capture, Diary, Library fallback, and Ask refusal behavior functional when every cloud control is disabled.
- **FR-008-005**: System MUST record a local-only bounded receipt whenever compact index sync is allowed, skipped, tombstoned, or fails.
- **FR-008-006**: System MUST record a local-only bounded receipt whenever cloud Ask synthesis is allowed, skipped, falls back, or fails.
- **FR-008-007**: System MUST record a local-only bounded receipt whenever cloud LLM routing is allowed, skipped by policy, skipped by budget, falls back, or fails.
- **FR-008-008**: Receipts MUST NOT store raw screenshots, full OCR bodies, raw prompts, raw questions, raw model responses, embeddings, or access tokens.
- **FR-008-009**: Receipts MAY store request IDs, endpoint/capability names, outcome, latency, result counts, provider/model labels, token/cost estimates, envelope IDs, and SHA-256 digests of private inputs.
- **FR-008-010**: Ask Orbit MUST check the cloud Ask synthesis preference before attempting `MemoryGatewayRequest.GroundedAsk`.
- **FR-008-011**: Library search MUST degrade to existing local search behavior when compact indexing or gateway access is unavailable.
- **FR-008-012**: LLM provider resolution MUST check the cloud AI routing preference before choosing `CloudLlmProvider`.
- **FR-008-013**: If cloud AI routing is disabled and no production local model can satisfy the capability, callers MUST receive graceful fallback/unavailable copy rather than a crash.
- **FR-008-014**: Settings MUST explain the difference between compact index sync, Ask cloud synthesis, and cloud AI routing in user-facing language.
- **FR-008-015**: Budget policy MUST be represented as local data even if v1 only supports a simple "no cap" and "zero cap/off" path.
- **FR-008-016**: Budget-denied requests MUST be distinguishable from user-disabled requests in receipts and user-facing fallback copy.
- **FR-008-017**: Existing process boundaries MUST remain intact: network egress only through `:net`, corpus only through `:ml`, UI through Binder.
- **FR-008-018**: This branch MUST NOT introduce BYOC as the default path, raw cloud storage, remote authoritative memory, or external-agent context sharing.

### Key Entities

- **CloudControlPolicy**: Local decision object combining user preferences, budget state, capability, and fallback behavior.
- **CloudCapability**: A cloud-eligible operation category: compact index sync, semantic Library search, grounded Ask synthesis, or LLM inference capability.
- **BudgetDecision**: Local result of evaluating whether a cloud operation is allowed, skipped by user preference, skipped by budget, or requires fallback.
- **CloudUsageReceipt**: Local audit projection for one cloud policy decision or gateway result; stores bounded metadata only.
- **CloudControlPreferences**: Durable user preferences for indexing, Ask synthesis, AI routing, and budget cap.

## Success Criteria

### Measurable Outcomes

- **SC-008-001**: With all cloud controls disabled, capture -> Diary -> Library local search -> Ask local refusal/answer works without network calls or app crash.
- **SC-008-002**: Disabling cloud Ask synthesis prevents all `GroundedAsk` gateway calls in focused unit tests.
- **SC-008-003**: Disabling compact memory indexing prevents all upsert/tombstone gateway calls and writes a skipped receipt.
- **SC-008-004**: Disabling cloud AI routing prevents `LlmProviderRouter` from selecting `CloudLlmProvider` in focused unit tests.
- **SC-008-005**: Audit/receipt tests prove raw questions, prompts, full OCR, raw screenshots, embeddings, model responses, and tokens are absent from persisted receipt JSON.
- **SC-008-006**: Settings Compose tests show three distinct controls and preserve state across recomposition.
- **SC-008-007**: Full local gate passes: `:app:compileDebugKotlin`, focused unit tests, `:build-logic:lint:test`, `:app:lintDebug`, and `:app:assembleDebug`.

## Assumptions

- The current compact Atlas memory index remains the only cloud storage used by this branch.
- MongoDB Atlas is a compact retrieval mirror, not Orbit's source of truth.
- Supabase/Vercel gateway credentials already exist in local debug configuration; this branch does not add Android-held provider secrets.
- BYOK/BYOC remains deferred unless a later spec explicitly re-scopes it.
- Cost estimates may be approximate or unavailable for v1; the local data model still reserves a nullable field for them.
- Device/manual validation can be deferred if no phone is available, but local tests and APK build must pass before closeout.
