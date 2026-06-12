# Research: Cloud Controls, Storage, And Budgeting

**Date**: 2026-06-12  
**Branch**: `feature/008-cloud-controls-storage-budgeting-20260612`

## Inputs Reviewed

- `VISION-2026-05-22.md`
- `docs/mvp-to-vision-execution-plan-2026-06-02.md`
- `docs/orbit-roadmap-queue-2026-06-02.md`
- `docs/product-roadmap-audit-2026-05-12.md`
- `docs/spec-branch-reorganization-plan-2026-05-13.md`
- `.specify/memory/constitution.md`
- `specs/013-cloud-llm-routing/`
- `specs/014-edge-function-llm-gateway/`
- Current code seams:
  - `app/src/main/java/com/orbit/app/settings/PrivacyPreferences.kt`
  - `app/src/main/java/com/orbit/app/settings/SettingsActivity.kt`
  - `app/src/main/java/com/orbit/app/settings/SettingsScreen.kt`
  - `app/src/main/java/com/orbit/app/memory/MemoryIndexSyncCoordinator.kt`
  - `app/src/main/java/com/orbit/app/orbit/AskOrbitRepository.kt`
  - `app/src/main/java/com/orbit/app/ai/LlmProviderRouter.kt`
  - `app/src/main/java/com/orbit/app/memory/MemoryAudit.kt`

## Current Implementation Findings

### Compact Memory Index

`PrivacyPreferences.memoryIndexingEnabled` already exists and defaults to `false`. Settings exposes a "Cloud memory index" toggle. `MemoryIndexSyncCoordinator` already accepts an `indexingEnabled` lambda and writes `MEMORY_SYNC_SKIPPED` audit rows when disabled.

**Gap**: The Settings surface is a single row, not a full cloud-control model. Users cannot inspect recent cloud index decisions or distinguish index sync from Ask synthesis or cloud LLM routing.

### Library Search

`LibraryRepository` uses `MemoryGatewayRequest.SemanticSearch` through `INetworkGateway`, then falls back to local behavior when the gateway is unavailable. This branch should not rebuild search.

**Gap**: There is no explicit user control for whether Library may use the compact index beyond the existing indexing preference. That is acceptable for v1 if the copy says "indexing/search mirror" and local fallback is reliable.

### Ask Orbit

`BinderAskOrbitRepository.ask()` currently attempts `MemoryGatewayRequest.GroundedAsk(... allowSynthesis = true)` before local deterministic retrieval. It does not read a user preference.

**Gap**: Ask synthesis can happen when cloud gateway is available even if the user only intended to enable compact search indexing. This is the highest-priority code change after artifacts.

### Cloud LLM Routing

`LlmProviderRouter` chooses `NanoLlmProvider` only when `RuntimeFlags.useLocalAi == true` and a hardware probe returns true. The probe currently returns false, so production resolution generally returns `CloudLlmProvider` when a gateway is supplied.

**Gap**: `RuntimeFlags.useLocalAi` is a volatile developer/runtime flag, not a durable user setting. There is no cloud-routing budget or user-visible receipt for `CloudLlmProvider` calls.

### Audit And Receipts

`MemoryAudit` already stores request IDs, endpoints, digests, counts, latency, outcomes, and errors. It explicitly avoids raw query/payload text.

**Gap**: Receipts are memory-gateway-specific. Spec 008 needs a common policy/receipt vocabulary so indexing, Ask synthesis, and LLM routing can be reasoned about together without storing private raw content.

## Decisions

### D-008-001: Keep Three Independent Cloud Controls

**Decision**: Implement separate controls for compact memory index, cloud Ask synthesis, and cloud AI routing.

**Rationale**: These have different privacy and product implications. Index sync sends compact records for retrieval; Ask synthesis sends a question and retrieved evidence to produce an answer; cloud AI routing can affect capture understanding, summaries, actions, and future agent behavior.

**Rejected**: One global "cloud on/off" switch. It hides the actual policy surface and would either over-disable useful search or over-enable agentic cloud behavior.

### D-008-002: Reuse Local Audit Rows As Receipts

**Decision**: Store cloud usage receipts as bounded local audit rows or a thin local projection over audit rows, not as remote ledger rows.

**Rationale**: The constitution says audit log and consent ledger remain authoritative on device. Existing `MemoryAudit` already has the right payload discipline.

**Rejected**: Server-side receipt ledger. It would make remote state authoritative for user trust and introduce deletion/export complexity before the KG/agent layers are ready.

### D-008-003: Budget Policy Starts Local And Simple

**Decision**: Add a local `BudgetDecision` model now, with v1 behavior supporting "allowed", "disabled by user", and "budget denied" even if only a zero/no-cap budget UI ships initially.

**Rationale**: Future token/cost accounting needs a stable decision contract. The MVP can still be small.

**Rejected**: Full provider billing dashboard. Too much scope for this branch and not needed for the next KG/agent specs.

### D-008-004: Ask Cloud Disabled Means Skip GroundedAsk Entirely

**Decision**: If cloud Ask synthesis is disabled, `BinderAskOrbitRepository` must not call `MemoryGatewayRequest.GroundedAsk`; it should go straight to local cited retrieval/refusal and record a policy skip.

**Rationale**: This makes the user preference testable and preserves a working Ask surface without cloud synthesis.

**Rejected**: Calling GroundedAsk with `allowSynthesis=false`. That still sends the question to the gateway and does not satisfy a "disable cloud Ask" user expectation.

### D-008-005: Cloud AI Disabled Fails Closed

**Decision**: If cloud AI routing is disabled and the current local provider cannot satisfy a capability, the router/caller must return a graceful local-unavailable path, not silently route to cloud.

**Rationale**: Local-first means cloud changes quality/latency, not feature scope. When local model support is incomplete, the product must expose that honestly.

**Rejected**: Preserving today's cloud-default behavior when the user has disabled cloud AI. That would violate the user control.

## Open Implementation Questions

- Whether to extend `PrivacyPreferences` directly or introduce `CloudControlPreferences` wrapping the same SharedPreferences file. Preference: add `CloudControlPreferences` only if it reduces coupling; otherwise extend `PrivacyPreferences` conservatively.
- Whether `LlmProviderRouter.create()` can accept a policy lambda without broad call-site churn. Preference: add a pure `resolve()` overload first, then migrate call sites needed by tests.
- Whether the cloud receipt UI should be a new Settings section or a filtered Audit Log entry point. Preference: start with Settings counts/copy plus existing Audit Log link; add a dedicated activity only if tests or UX demand it.

## Non-Goals

- BYOC/BYOK setup UI.
- Remote authoritative memory or cloud backup.
- Server-side consent ledger.
- AppFunctions/Spark/platform-agent sharing.
- Full cost reconciliation against provider billing.
- Local BYOM model manager; this remains Spec 022.
