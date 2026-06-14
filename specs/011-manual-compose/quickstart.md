# Quickstart: Manual Compose And Capture Context

## Purpose

Validate that Orbit can capture explicit user context at save time and create deliberate manual memories without bypassing local-first storage or process boundaries.

## Local Validation

Run from repo root:

```bash
./gradlew :app:testDebugUnitTest --tests "com.orbit.app.overlay.*" --tests "com.orbit.app.diary.*"
./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin
git diff --check
```

## Demo Flow

### Capture-Time Context

1. Start Orbit overlay.
2. Save a clipboard or screenshot capture.
3. Tap the post-capture context affordance.
4. Enter a short note such as `reschedule this`.
5. Open the capture detail and verify the note appears under Context.
6. Search Library for `reschedule` and verify the capture appears with context evidence.

### Manual Compose

1. Open Diary.
2. Start manual compose.
3. Enter body text and optional context.
4. Save.
5. Verify the new envelope appears on the target day.
6. Open detail and verify the body and context are distinct.
7. Try the same body again and verify duplicate suppression.

## Validation Log

2026-06-13:

- Created Spec Kit artifacts for Spec 011 after closing Spec 010.
- PASS: `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin`
- Implemented post-capture `Add context` for new silent/undo capture states using the existing note-entry callback. Android test sources cover that the context action targets the saved envelope and does not dismiss the current undo/silent state.

## Known Deferred Checks

- Physical phone/manual validation waits for device availability.
- Voice compose, share sheet compose, attachment compose, and transparent pre-seal Clarify activity are deferred unless explicitly pulled into this branch after P1/P2 pass.
- BYOM/local model manager and A2UI are out of scope.
