# Quickstart: Memory Candidates Inspector

## Local Validation

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest --tests "com.orbit.app.data.*Memory*"
./gradlew :app:testDebugUnitTest --tests "com.orbit.app.diary.*"
./gradlew :build-logic:lint:test :app:lintDebug
./gradlew :app:assembleDebug
./gradlew :app:compileDebugAndroidTestKotlin
```

Secret/network boundary scans:

```bash
rg -n "OPENAI_API_KEY|MONGODB_ATLAS_URI|mongodb\\+srv|MongoClient|ANTHROPIC_API_KEY|ZEROENTROPY" app/src || true
rg -n "OkHttpClient\\(|HttpURLConnection|Socket\\(|HttpClient\\(" app/src/main/java app/src/debug/java app/src/release/java || true
```

Expected: no Android secret/client hits and no direct network constructors outside approved net code.

## Demo Flow

1. Build/install debug APK.
2. Run debug seed for demo captures, action proposals, and memory candidates.
3. Open Orbit.
4. Verify Action Drafts still appear if seeded.
5. Verify Memory Review appears only when candidates exist.
6. Open a candidate source capture.
7. Accept one candidate.
8. Verify it disappears from pending review and appears in promoted memory projection/test surface.
9. Reject one candidate.
10. Verify it disappears and does not become promoted memory.
11. Edit one candidate, accept it, and verify promoted memory uses edited text.

## Negative Paths

- Candidate source capture deleted: candidate is stale/invalidated, not promoted silently.
- Sensitive/local-only candidate: explicit accept required; no cloud sync payload includes text.
- Duplicate accept/reject: no duplicate promoted memory or audit rows.
- Binder unavailable: visible failure copy, no partial state.

## Closeout Evidence

```text
android_unit_tests=PASS 2026-06-05 `./gradlew :app:testDebugUnitTest`; focused `MemoryParcelTest` passed
android_lint=PASS 2026-06-05 `./gradlew :app:lintDebug`
android_compile=PASS 2026-06-05 `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin`
room_migration_tests=SOURCE_READY 2026-06-05 `OrbitDatabaseMigrationV8toV9Test` compiles; connected execution deferred until device/emulator available
memory_decision_tests=SOURCE_READY 2026-06-05 `MemoryRepositoryDelegateTest` compiles; connected execution deferred until device/emulator available
cloud_payload_exclusion=PARTIAL 2026-06-05 no references to `memory_candidate`, `promoted_memory`, `MemoryCandidate`, `PromotedMemory`, or `MemoryRepository` under compact memory/Ask/action code paths; explicit assertion test still pending
permission_scan=PASS 2026-06-05 no Android secret/client hits under `app/src`
network_boundary_scan=PASS 2026-06-05 no direct network constructors in scanned app source
apk_path=app/build/outputs/apk/debug/app-debug.apk
apk_sha256=bb7ce6477f37d0ac3ff1d2e3d49292e365838e76120a004b41e91c0e937c3ea3
phone_validation=deferred until device available
known_limits=Memory Review UI is source-compiled and locally gated, but not yet manually validated on phone. Pending/rejected candidate exclusion from Ask/action/cloud needs explicit regression tests before closeout.
```
