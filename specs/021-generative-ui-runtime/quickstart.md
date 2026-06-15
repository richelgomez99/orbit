# Quickstart: Generative UI Runtime

## Local Validation

```bash
./gradlew :app:testDebugUnitTest --tests "com.orbit.app.generativeui.*"
./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin
git diff --check
```

## Demo Flow

1. Open the Orbit tab.
2. Ask for a plan that has saved evidence.
3. Verify the result renders as native sections, question choices, steps, and evidence rows.
4. Verify evidence rows still open the cited capture.
5. Verify the same document can produce bounded plain text fallback.

## Validation Log

2026-06-14:

- Created Spec 021 artifacts. Scope is a safe internal typed renderer over existing agent-plan parcels, not external A2UI/provider JSON integration.
