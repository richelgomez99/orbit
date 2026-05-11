# Implementation Plan: Capture Feedback Actions

**Branch**: `017-capture-feedback-actions` | **Date**: 2026-05-11 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/017-capture-feedback-actions/spec.md`

## Summary

Add envelope-level duplicate detection and a typed post-capture feedback result.
The existing URL hydration cache remains intact, but it is no longer mistaken
for user-visible dedupe. A second capture of the same canonical URL or exact
text returns `AlreadySaved(existingEnvelopeId, matchedBy)` and opens a compact
action surface for note, reclassify, or open existing capture.

## Technical Context

- **Language/Version**: Kotlin 2.x Android app.
- **Primary Dependencies**: Room/SQLCipher, Jetpack Compose, existing AIDL/Binder
  repository boundary, existing `ContinuationEngine` and `CanonicalUrlHasher`.
- **Storage**: SQLCipher Room database (`OrbitDatabase`). Expected schema change
  for `primaryCanonicalUrlHash`; `textContentSha256` already exists but is not
  currently populated in the seal path.
- **Testing**: JVM unit tests, Room migration/instrumented tests as needed,
  physical QA on S24 and Tab S9.
- **Target Platform**: Android 13+.
- **Project Type**: Mobile app.
- **Performance Goals**: Duplicate lookup adds no visible latency to capture;
  lookup should use indexed hashes, not full-text scans.
- **Constraints**: Preserve undo semantics; do not remove hydration-result reuse;
  typed seal result must cross process boundaries without stringly encoding.
- **Scale/Scope**: Capture seal path, repository/AIDL boundary, overlay feedback
  UI, duplicate audit row, and tests.

## Constitution Check

- **Intent Before Artifact**: `Already saved` gives the user a correction path
  before creating redundant artifacts. PASS.
- **Append-Only Mutation Trail**: Duplicate attempts receive their own audit
  event; reclassify from `Already saved` appends an intent history layer. PASS.
- **Reversible Capture**: Undo remains available for newly-created captures;
  duplicates are not newly created, so the action surface must not imply undo of
  a new row. PASS.
- **Stable Data Contracts**: AIDL/Binder surface changes must be additive or
  migrated atomically with callers. PASS with explicit contract work.

No constitutional violations to justify in Complexity Tracking.

## Project Structure

### Documentation

```text
specs/017-capture-feedback-actions/
├── spec.md
├── plan.md
├── data-model.md
└── tasks.md
```

Contracts should be added under `contracts/` if the AIDL shape becomes larger
than a simple parcel addition.

### Source Code

```text
app/src/main/java/com/capsule/app/
├── data/
│   ├── EnvelopeRepositoryImpl.kt
│   ├── dao/IntentEnvelopeDao.kt
│   ├── entity/IntentEnvelopeEntity.kt
│   └── model/AuditAction.kt
├── data/ipc/                         # AIDL/Binder typed seal result
├── overlay/                          # SealOutcome/PostCaptureUi + UI actions
└── service/CapsuleSealOrchestrator.kt

app/src/androidTest/java/com/capsule/app/data/   # migration/repository tests
app/src/test/java/com/capsule/app/               # pure unit tests where possible
```

## Phase Plan

### Phase 1 - Duplicate keys and migration

Add durable duplicate keys and DAO lookups. Prefer `primaryCanonicalUrlHash` for
URL captures and populated `textContentSha256` for exact text captures. Author a
Room migration and index before wiring user-facing behavior.

### Phase 2 - Typed seal result

Replace the current `seal(): String` assumption at the repository boundary with
a typed result that distinguishes `Created` from `AlreadySaved`. Update all
callers atomically so overlay copy is truthful.

### Phase 3 - Feedback actions

Add `Already saved` UI state with note, reclassify, and open-existing actions.
Do not add note persistence ad hoc; if notes need schema, include it in this
feature's data model and migration.

### Phase 4 - Verification and physical QA

Run unit, migration, lint, and physical-device QA. Confirm adjacent app icons are
tappable outside compact feedback pills in portrait and landscape.

## Risk And Rollback

- **Highest risk**: AIDL/Binder signature changes. Mitigation: additive parcel
  shape and compile all processes together.
- **Schema risk**: duplicate key migration. Mitigation: forward-only migration,
  exported schema, migration tests.
- **UX risk**: duplicate action copy implying a new save. Mitigation: separate
  `Already saved` state and no undo wording for duplicate attempts.

Rollback requires reverting schema/code together before release; once a schema
version ships to alpha devices, rollback must be a forward migration.