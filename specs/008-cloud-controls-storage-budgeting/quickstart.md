# Quickstart: Cloud Controls, Storage, And Budgeting

## Purpose

Validate that Orbit's cloud augmentation is visible, reversible, bounded, and local-first.

## Local Validation

Run from repo root:

```bash
./gradlew :app:testDebugUnitTest --tests "com.orbit.app.cloud.*"
./gradlew :app:testDebugUnitTest --tests "com.orbit.app.memory.*"
./gradlew :app:testDebugUnitTest --tests "com.orbit.app.orbit.*"
./gradlew :app:testDebugUnitTest --tests "com.orbit.app.ai.*"
./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :build-logic:lint:test :app:lintDebug :app:assembleDebug
```

## Manual Demo Script

Device validation can wait until a phone/emulator is available.

1. Open Settings.
2. Confirm three distinct controls are visible:
   - Compact memory index.
   - Cloud Ask synthesis.
   - Cloud AI routing.
3. Disable compact memory index.
4. Capture or seed a memory.
5. Confirm Diary still shows the memory locally.
6. Search Library for a local term and confirm local fallback still opens the capture.
7. Disable cloud Ask synthesis.
8. Ask a question with local evidence, such as `what was rescheduled?`.
9. Confirm Ask returns a cited local answer or refusal and does not require cloud synthesis.
10. Ask an unsupported sensitive identifier question, such as `what is my passport number?`.
11. Confirm Orbit refuses with user-friendly saved-evidence copy.
12. Disable cloud AI routing.
13. Trigger an LLM-backed feature if available.
14. Confirm Orbit uses local fallback/unavailable copy, not a crash.
15. Open "What Orbit did today" or cloud activity copy.
16. Confirm receipts show capability/outcome/counts/digests only.

## Expected Results

- All controls persist after leaving and reopening Settings.
- Disabled compact indexing prevents upsert/tombstone gateway calls.
- Disabled cloud Ask prevents `GroundedAsk` gateway calls.
- Disabled cloud AI prevents `CloudLlmProvider` selection.
- Receipts never show raw questions, prompts, full OCR, screenshots, embeddings, model responses, tokens, API keys, or JWTs.
- Capture, Diary, Library local fallback, and Ask refusal remain usable with all cloud controls disabled.

## Known Deferred Checks

- Full phone/manual validation is deferred when no device is available.
- Remote tombstone-all for already-indexed Atlas records is not required unless implemented explicitly.
- Full provider cost reconciliation is deferred.
- BYOC/BYOK setup is deferred.
