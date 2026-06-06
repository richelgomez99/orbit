# Implementation Plan: Approval Action Runtime

**Branch**: `feature/006-approval-action-runtime-20260603` | **Date**: 2026-06-03 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `specs/006-approval-action-runtime/spec.md`

## Summary

Spec 006 turns Orbit's existing action proposal primitives into a reliable, user-approved action runtime. The implementation should first reconcile existing contracts (`tasks.createTodo` schema vs handler), then add an Orbit action-draft workspace backed by local proposal/source projections, then harden typed approval sheets and execution outcomes for Calendar and local todo/list drafts.

## Technical Context

**Language/Version**: Kotlin, Android Gradle project, JDK 21  
**Primary Dependencies**: Jetpack Compose, Room, SQLCipher, WorkManager, AIDL, Android intents  
**Storage**: Existing local Room v8 action tables; no new table planned  
**Testing**: Gradle JVM tests, custom lint tests, Android lint, focused connected/device validation  
**Target Platform**: Android minSdk 33, S24 physical demo target  
**Project Type**: Android mobile app with multi-process architecture  
**Performance Goals**: Orbit tab Action Drafts visible within 5 seconds for seeded demo proposals; confirm tap dispatches local/external action without UI freeze  
**Constraints**: Local-first; no silent writes; no direct Android network clients outside `:net`; `:capture` executes external intents; `:ml` owns DB/audit  
**Scale/Scope**: Calendar and local todo/list approval runtime only; no platform-agent interop, KG, or local model manager work

## Constitution Check

- **I Local-First Supremacy**: PASS. Source of truth remains Room/SQLCipher. Calendar execution opens system UI; local todos write to local DB.
- **III Intent Before Artifact**: PASS. Actions are grounded in source envelopes and proposal provenance.
- **IV Continuations Grow Captures**: PASS. Production extraction remains continuation-based. Debug proposals are fixture-only for validation.
- **VI Privilege Separation**: PASS. `:ml` stores/audits, `:ui` renders, `:capture` dispatches intents, `:net` only serves cloud inference.
- **VIII Collect Only What You Use**: PASS. ActionDraft is a projection, not a speculative new table.
- **IX User-Sovereign Cloud Escape Hatch**: PASS. Existing proposals remain reviewable offline; cloud/local model availability changes extraction quality, not approval runtime.
- **XII Provenance Or It Didn't Happen**: PASS. Every draft must link to source envelope/proposal/audit lifecycle.

## Project Structure

### Documentation

```text
specs/006-approval-action-runtime/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── action-approval-contract.md
└── tasks.md
```

### Source Code

```text
app/src/main/aidl/com/orbit/app/data/ipc/
├── ActionDraftParcel.aidl              # likely new
├── IActionDraftObserver.aidl           # likely new
└── IEnvelopeRepository.aidl            # extend with pending drafts projection

app/src/main/java/com/orbit/app/data/
├── ActionsRepositoryDelegate.kt        # projection, audit, state semantics
├── EnvelopeRepositoryImpl.kt           # AIDL forwarding
└── dao/ActionProposalDao.kt            # pending draft query

app/src/main/java/com/orbit/app/action/
├── BuiltInAppFunctionSchemas.kt        # fix tasks.createTodo schema
├── ActionExecutorService.kt            # failure/undo hardening as needed
└── handler/TodoActionHandler.kt        # align with schema and UI

app/src/main/java/com/orbit/app/diary/
├── ActionPreviewCardUI.kt              # typed preview for todo/list; no raw JSON
└── DiaryViewModel.kt                   # keep inline chip path working

app/src/main/java/com/orbit/app/orbit/
└── ...                                 # Action Drafts repository/view model/UI projection

app/src/debug/java/com/orbit/app/debug/
└── DebugDemoSeedReceiver.kt            # deterministic action proposal fixtures

app/src/test/java/com/orbit/app/
└── action, diary, orbit, data tests
```

**Structure Decision**: Use existing Android modules and process boundaries. Add projection DTOs and UI surfaces only where needed.

## Phase Order

1. **Spec Lock**: finish Spec Kit artifacts and current-state audit.
2. **Contract Reconciliation**: fix `tasks.createTodo` schema/handler/extractor fixtures before UI; runtime injects proposal id.
3. **Outcome Semantics**: remove misleading Calendar undo promise and define visible failure outcome projection.
4. **Pending Draft Projection**: add local `ActionDraft` query/Binder/repository path.
5. **Orbit Workspace UI**: show pending drafts with source evidence and open local captures.
6. **Typed Approval Sheets**: calendar and todo/list sheets; remove raw JSON fallback.
7. **Execution Hardening**: duplicate confirm, stale source, schema mismatch, missing handler, real undo/failure copy.
8. **Debug Demo + S24 Validation**: seed deterministic proposals through debug-only safe surface, install APK, verify calendar/list/dismiss/failure flows.
9. **Closeout**: update roadmap/handoff and commit.

## Complexity Tracking

No constitution violations planned.
