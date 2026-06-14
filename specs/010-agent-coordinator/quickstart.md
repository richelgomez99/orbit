# Quickstart: Agent Coordinator

## Purpose

Validate that Orbit can produce cited, approval-first agent plans without executing external actions or requiring cloud/model availability.

## Local Validation

Run from repo root:

```bash
./gradlew :app:testDebugUnitTest --tests "com.orbit.app.agent.*"
./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin
git diff --check
```

## Demo Flow

1. Seed or capture evidence for a loop-closing request, such as a cancelled/rescheduled appointment.
2. Ask Orbit to help close the loop.
3. Confirm the coordinator returns a cited plan, not an executed action.
4. Open the cited capture from the plan.
5. Confirm any action step opens the existing approval preview.
6. Ask an ambiguous request and confirm Orbit asks a focused question or presents choices.
7. Disable cloud AI routing and confirm deterministic local plan/refusal still works.
8. Enable model assistance and confirm unsupported/uncited model output is rejected or falls back.

## Validation Log

2026-06-12:

- PASS: `./gradlew :app:testDebugUnitTest --tests "com.orbit.app.agent.*" :app:compileDebugKotlin`

2026-06-13:

- PASS: `./gradlew :app:testDebugUnitTest --tests "com.orbit.app.agent.*" :app:compileDebugKotlin`
- PASS: `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin`
- PASS: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin`
- PASS: `git diff --check`
- PASS: `./gradlew :app:testDebugUnitTest --tests "com.orbit.app.diary.DiaryViewModelTest" --tests "com.orbit.app.agent.*" :app:compileDebugKotlin`
- PASS: `./gradlew :app:testDebugUnitTest --tests "com.orbit.app.agent.*" --tests "com.orbit.app.diary.DiaryViewModelTest" :app:compileDebugKotlin`
- PASS after model-assist wiring: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin`
- PASS after model-assist wiring: `git diff --check`

## Model Assistance Notes

- The Orbit tab now requests `allowModelAssist=true` for explicit user planning requests.
- Model assistance is advisory only: it can rewrite bounded display copy for an already-valid deterministic plan, but it cannot add steps, cite unknown evidence, change function ids, or execute actions.
- Android production wiring binds `:ml` to `:net` on demand and resolves the provider through `LlmProviderRouter`. If Spec 008 cloud AI routing is disabled, assistance returns `null` and the deterministic plan/refusal is displayed.

## Known Deferred Checks

- Physical phone/manual validation waits for device availability.
- Durable chat sessions are deferred.
- A2UI rendering is deferred.
- Local BYOM model planning is deferred to Spec 022.
