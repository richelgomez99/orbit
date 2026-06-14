# Implementation Plan: Resolution Semantics

**Branch**: `feature/012-resolution-semantics-20260613` | **Date**: 2026-06-13 | **Spec**: `specs/012-resolution-semantics/spec.md`
**Input**: Feature specification from `specs/012-resolution-semantics/spec.md`

## Summary

Spec 012 adds durable local resolution semantics so Orbit can distinguish duplicate recapture, dismiss, not-now, snooze, done, reopened, stale, invalidated, and source-deleted states. The implementation adds a compact Room-backed receipt model and read-time surfacing resolver, then hooks existing duplicate, action, Active Intent, and derived todo paths into that model.

## Technical Context

**Language/Version**: Kotlin, Android Gradle project, JDK 21  
**Primary Dependencies**: Room/SQLCipher, AIDL Binder, Jetpack Compose, WorkManager  
**Storage**: Additive Room v11 `resolution_receipt`; encrypted SQLCipher remains local source of truth  
**Testing**: JVM unit tests, migration/source-ready tests, Android lint/custom lint, androidTest compile  
**Target Platform**: Android app across `:ui`, `:capture`, `:ml`, and `:net` processes  
**Project Type**: Mobile app  
**Performance Goals**: Receipt verdict lookup is bounded by indexed target id; no network dependency  
**Constraints**: Local-first, no direct Room outside `:ml`, no raw content in receipts, no cloud authority  
**Scale/Scope**: One schema migration, DAO/repository layer, hook existing lifecycle transitions, minimal Orbit UI affordances for resolution choices if needed

## Constitution Check

- **Principle I Local-First Supremacy**: PASS. Resolution state remains local Room source of truth.
- **Principle III Intent Before Artifact**: PASS. Receipts capture user's intent about whether a loop is done, dismissed, later, or still active.
- **Principle V Under-Deliver on Noise**: PASS. Not-now/snooze/dismiss reduce repeated queue pressure.
- **Principle VI Privilege Separation**: PASS if all receipt writes happen inside `:ml` and Binder carries only compact parcels.
- **Principle VIII Collect Only What You Use**: PASS. Receipts drive concrete surfacing/agent decisions.
- **Principle XII Provenance Or It Didn't Happen**: PASS. Every resolution must carry target/provenance ids.

No constitutional violations expected.

## Project Structure

### Documentation

```text
specs/012-resolution-semantics/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── contracts/
│   └── resolution-semantics-contract.md
├── quickstart.md
└── tasks.md
```

### Source Code

Expected implementation surfaces:

```text
app/src/main/java/com/orbit/app/data/entity/
└── ResolutionReceiptEntity.kt

app/src/main/java/com/orbit/app/data/dao/
└── ResolutionReceiptDao.kt

app/src/main/java/com/orbit/app/data/
├── OrbitDatabase.kt
├── OrbitMigrations.kt
├── EnvelopeRepositoryImpl.kt
├── ActionsRepositoryDelegate.kt
└── ResolutionRepository.kt

app/src/main/java/com/orbit/app/data/ipc/
├── ResolutionReceiptParcel.kt
└── ResolutionVerdictParcel.kt

app/src/main/java/com/orbit/app/understanding/
└── BasicUnderstandingWriter.kt

app/src/main/java/com/orbit/app/diary/
└── DiaryViewModel.kt

app/src/test/java/com/orbit/app/resolution/
app/src/test/java/com/orbit/app/data/
```

**Structure Decision**: Add one local semantic layer in `:ml` data code and keep UI changes narrow. Do not create cloud or model dependencies.

## Implementation Approach

1. Add Room v11 entity/DAO/migration for `resolution_receipt`.
2. Add domain enums and `ResolutionVerdictResolver`.
3. Add repository helper for validating/writing receipts and querying target verdicts.
4. Hook exact duplicate seal and Basic-understanding duplicate suppression.
5. Hook action proposal dismiss/invalidation.
6. Hook aggregate derived-list done/reopened transitions.
7. Add minimal Follow-up surfacing filters for dismissed/not-now/snoozed/resolved targets.
8. Add tests for schema, validation, surfacing rules, hook writes, and banned payload keys.

## Complexity Tracking

No complexity exceptions.
