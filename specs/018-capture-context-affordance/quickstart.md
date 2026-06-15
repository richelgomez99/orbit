# Quickstart: Capture Context Affordance

## Purpose

Validate that Orbit's capture-time context affordance feels lightweight while preserving Spec 011 storage and Spec 012 resolution semantics.

## Local Validation

Run from repo root:

```bash
./gradlew :app:testDebugUnitTest --tests "com.orbit.app.overlay.*" --tests "com.orbit.app.diary.*CaptureContext*"
./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin
git diff --check
```

## Demo Flow

### New Capture Context

1. Save a screenshot or clipboard capture from the overlay.
2. Tap the context icon in the saved/undo post-capture pill.
3. Enter a short reason such as "reschedule dentist".
4. Save.
5. Verify the context appears on the capture detail and Library can find the note-only term.

### Duplicate Capture Context

1. Save an already-saved item again.
2. In the Already Saved state, tap context.
3. Enter today's reason.
4. Save.
5. Verify no duplicate envelope is created and the existing capture receives the note.

### Failure/Cancel

1. Open context entry.
2. Try saving blank text; verify no Binder call.
3. Cancel with typed text; verify the capture remains saved and no note is written.
4. Simulate Binder failure in tests; verify copy is user-friendly and no raw data is logged.

## Validation Log

2026-06-14:

- Created Spec 018 artifacts after Spec 012 repo-side completion.
- Current implementation gap: Spec 011 has post-save `Add context` that opens full detail note entry; this branch scopes a focused post-save context surface and keeps true pre-seal Clarify deferred unless evidence changes.

## Known Deferred Checks

- Physical phone/manual validation is deferred while the device is unavailable.
- True pre-seal transparent Clarify activity is deferred unless implementation research proves it can be done without destabilizing capture.
