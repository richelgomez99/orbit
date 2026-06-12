# Quickstart: KG Backend POC

## Purpose

Validate that Orbit can store local, provenance-backed graph memory without weakening local-first, deletion, or cloud-control guarantees.

## Local Validation

Run from repo root:

```bash
./gradlew :app:testDebugUnitTest --tests "com.orbit.app.graph.*"
./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin
git diff --check
```

## Validation Log

2026-06-12:

- PASS: `./gradlew :app:testDebugUnitTest --tests "com.orbit.app.graph.*" :app:compileDebugKotlin`

## Required Demo Evidence

1. Promote or seed a memory candidate with source envelope support.
2. Project it into a KG fact.
3. Ask `why this?` for that fact.
4. Confirm the projection returns source evidence ids.
5. Delete the only source and run invalidation.
6. Confirm the fact invalidates with `lost_provenance`.
7. Repeat with two sources and confirm one surviving source preserves the fact.
8. Confirm pending/rejected candidates never appear as active facts.
9. Confirm all cloud controls disabled still permits local KG inspection.

## Known Deferred Checks

- Connected migration execution waits for device/emulator availability.
- External adapter comparison remains research unless a candidate can run contract tests locally.
- LLM entity extraction is deferred; deterministic/promoted-memory projection is enough for this branch.
