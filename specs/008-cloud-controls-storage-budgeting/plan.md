# Implementation Plan: Cloud Controls, Storage, And Budgeting

**Branch**: `feature/008-cloud-controls-storage-budgeting-20260612` | **Date**: 2026-06-12 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/008-cloud-controls-storage-budgeting/spec.md`

## Summary

Spec 008 makes Orbit's existing cloud augmentation visible, reversible, and auditable before KG and agent coordinator work depend on it. The implementation adds durable user preferences and local policy gates for three separate cloud surfaces: compact memory indexing, cloud Ask synthesis, and cloud AI routing. It reuses existing Room/audit/Binder/gateway seams, adds bounded receipt metadata, and keeps local deterministic fallback behavior working when cloud is disabled.

## Technical Context

**Language/Version**: Kotlin, TypeScript for existing Supabase/Vercel gateway tests if touched  
**Primary Dependencies**: Android Jetpack Compose, Room/SQLCipher, AIDL Binder, kotlinx.serialization, existing memory/LLM gateway clients  
**Storage**: Local SharedPreferences for preferences; Room `audit_log_entry` for receipts; Atlas remains compact retrieval mirror only  
**Testing**: Gradle JVM unit tests, Compose/androidTest source compile, custom lint, Android lint, existing backend tests if gateway code changes  
**Target Platform**: Android app with four-process architecture (`:ui`, `:capture`, `:ml`, `:net`)  
**Project Type**: Mobile app plus existing gateway  
**Performance Goals**: Policy decisions are synchronous local reads under 5ms; denied cloud calls perform zero network work; Settings renders without layout instability  
**Constraints**: Local-first source of truth; no raw private content in receipts; no HTTP outside `:net`; no Android-held provider secrets; no phone required for local gate  
**Scale/Scope**: One active branch; Settings, Ask, LLM routing, audit policy, and focused tests

## Constitution Check

- **Principle I - Local-First Supremacy**: Pass. Device remains source of truth. Cloud is augmentation with visible controls.
- **Principle III - Intent Before Artifact**: Pass. This branch does not alter capture semantics; it protects future intent/KG use.
- **Principle VI - Privilege Separation**: Pass. No new network path; all cloud operations continue through `:net`.
- **Principle VIII - Collect Only What You Use**: Pass. Receipts store bounded metadata only.
- **Principle IX - User-Sovereign Cloud Escape Hatch**: Pass if implementation adds durable per-capability controls before relying on deeper cloud behavior.
- **Principle XII - Provenance Or It Didn't Happen**: Pass. Receipts and audit rows cite request/capability/outcome without raw content.

## Project Structure

### Documentation

```text
specs/008-cloud-controls-storage-budgeting/
├── spec.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── cloud-controls-contract.md
└── tasks.md
```

### Source Code

```text
app/src/main/java/com/orbit/app/settings/
├── PrivacyPreferences.kt
├── SettingsActivity.kt
└── SettingsScreen.kt

app/src/main/java/com/orbit/app/cloud/
├── CloudCapability.kt
├── CloudControlPolicy.kt
└── CloudUsageReceipt.kt

app/src/main/java/com/orbit/app/memory/
├── MemoryAudit.kt
├── MemoryIndexSyncCoordinator.kt
└── MemoryIndexSyncDelegate.kt

app/src/main/java/com/orbit/app/orbit/
└── AskOrbitRepository.kt

app/src/main/java/com/orbit/app/ai/
├── LlmProviderRouter.kt
└── CloudLlmProvider.kt

app/src/test/java/com/orbit/app/cloud/
app/src/test/java/com/orbit/app/memory/
app/src/test/java/com/orbit/app/orbit/
app/src/test/java/com/orbit/app/ai/

app/src/androidTest/java/com/orbit/app/settings/
```

**Structure Decision**: Add a small `com.orbit.app.cloud` policy package for shared local decision objects. Keep UI in `settings`, Ask wiring in `orbit`, index sync in `memory`, and LLM routing in `ai`.

## Implementation Strategy

### Phase A - Policy Foundation

Add the pure local policy/data model first. Extend preferences with two new booleans and budget cap. Add tests that prove disabled decisions return denied outcomes and forbidden keys cannot appear in receipt JSON.

### Phase B - Compact Index Control Cleanup

Reuse the existing `memoryIndexingEnabled` gate. Normalize it through the new policy vocabulary and expand tests around no-gateway-call behavior and skipped receipts.

### Phase C - Ask Cloud Synthesis Gate

Inject a cloud Ask enabled predicate/policy into `BinderAskOrbitRepository`. When disabled, skip `GroundedAsk`, write a local skip/fallback receipt, and use existing local deterministic cited retrieval/refusal.

### Phase D - Cloud AI Routing Gate

Teach `LlmProviderRouter` to accept a cloud-routing policy or durable preference. If cloud AI is disabled and no local provider is available, fail closed with graceful local-unavailable behavior. Keep process boundaries intact.

### Phase E - Settings And Receipt UI

Show three distinct controls in Settings. Add recent cloud activity counts or clear audit-link copy without creating an overbuilt dashboard. Ensure Quiet Almanac visual language remains consistent.

### Phase F - Validation

Run focused unit tests, Compose/androidTest compilation, custom lint, Android lint, and assemble. Build APK if code changes are included. Phone/manual validation is deferred until a device is available.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | N/A | N/A |
