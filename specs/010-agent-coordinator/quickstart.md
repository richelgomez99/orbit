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

Pending implementation.

## Known Deferred Checks

- Physical phone/manual validation waits for device availability.
- Durable chat sessions are deferred.
- A2UI rendering is deferred.
- Local BYOM model planning is deferred to Spec 022.
