# Quickstart: Resolution Semantics

## Purpose

Validate that Orbit records compact local semantic receipts for duplicate, dismiss, not-now, snooze, done, reopened, stale, and invalidated states, and that Follow-ups/agent reads can use those receipts to avoid repeated queue noise.

## Local Validation

Run from repo root:

```bash
./gradlew :app:testDebugUnitTest --tests "com.orbit.app.resolution.*" --tests "com.orbit.app.data.*Resolution*" --tests "com.orbit.app.understanding.*Duplicate*"
./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin
git diff --check
```

## Demo Flow

### Duplicate Recapture

1. Save a capture.
2. Save the same capture again.
3. Verify the overlay says already saved.
4. Verify no duplicate Follow-up appears.
5. Verify a compact duplicate receipt exists through debug/test hooks.

### Follow-up Resolution

1. Seed or create an Active Intent follow-up.
2. Dismiss it and verify it disappears from active Follow-ups.
3. Create another follow-up and choose Not now; verify it hides from cleanup but remains in Diary/Library.
4. Snooze a follow-up and verify it is hidden until the requested time.

### Local List Done/Reopened

1. Approve or seed a local list.
2. Mark all checklist items done.
3. Verify a `DONE` receipt exists.
4. Uncheck one item.
5. Verify a `REOPENED` receipt or invalidation exists and future agent logic treats it as active again.

## Validation Log

2026-06-13:

- Created fresh Spec Kit artifacts for Spec 012 after repo-side Spec 011 validation.
- Added pure resolution domain models, `ResolutionVerdictResolver`, Room v11 `resolution_receipt` schema/DAO, v10-to-v11 migration test source, and `ResolutionRepository` validation/write/query helper.
- PASS: `./gradlew :app:testDebugUnitTest --tests "com.orbit.app.resolution.ResolutionVerdictResolverTest" --tests "com.orbit.app.data.ResolutionRepositoryTest" :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin`
- Physical phone/manual validation is deferred until device availability.

## Known Deferred Checks

- Full conflict-resolution UI for graph/profile facts may wait for a later KG branch.
- Cloud sync of resolution receipts is out of scope; local Room remains authoritative.
- AppFunctions/Spark/platform-agent interop is out of scope.
