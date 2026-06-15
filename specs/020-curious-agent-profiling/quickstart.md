# Quickstart: Curious Agent Profiling

## Local Validation

```bash
./gradlew :app:testDebugUnitTest --tests "com.orbit.app.curious.*"
./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin
git diff --check
```

## Demo Flow

1. Seed or construct compact evidence about repeated saves.
2. Run the local curious question generator.
3. Verify one cited question appears only when evidence threshold is met.
4. Dismiss it and verify it does not reappear as active.

## Validation Log

2026-06-14:

- Created Spec 020 artifacts. Scope is conservative: question candidates before profile facts, deterministic local generator first.
- Added pure curious-agent domain models and a deterministic local question generator under `app/src/main/java/com/orbit/app/curious/`.
- The generator enforces minimum evidence threshold, max question cap, source refs, suppressed question ids, and capped source labels. Persistence/UI remains deferred until the pure model is integrated with repository-backed evidence.
- PASS: `./gradlew :app:testDebugUnitTest --tests "com.orbit.app.curious.*" :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin`
- PASS: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin`
- PASS: `git diff --check`
