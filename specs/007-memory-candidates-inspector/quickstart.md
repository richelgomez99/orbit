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
memory_viewmodel_tests=PASS 2026-06-12 `./gradlew :app:testDebugUnitTest --tests "com.orbit.app.diary.DiaryViewModelTest"` covers candidate observation and accept/reject notice delegation
android_lint=PASS 2026-06-05 `./gradlew :app:lintDebug`
android_compile=PASS 2026-06-12 `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin`
room_migration_tests=SOURCE_READY 2026-06-05 `OrbitDatabaseMigrationV8toV9Test` compiles; connected execution deferred until device/emulator available
memory_decision_tests=SOURCE_READY 2026-06-05 `MemoryRepositoryDelegateTest` compiles; connected execution deferred until device/emulator available
ask_action_candidate_exclusion=PASS 2026-06-12 `MemoryBoundaryPolicyTest.askAndActionSurfacesDoNotReadMemoryCandidateReviewTables` proves Ask/action code does not read `memory_candidate`/`promoted_memory` review tables or parcels as facts
cloud_payload_exclusion=PASS 2026-06-12 `MemoryBoundaryPolicyTest.compactMemoryIndexCodeDoesNotReadMemoryCandidateReviewTables` and `CompactMemoryIndexBuilderTest.dropsMemoryReviewEvidenceBundleKeysFromCloudPayload` prove compact memory/Atlas payloads do not read or serialize candidate review text
focused_memory_tests=PASS 2026-06-12 `./gradlew :app:testDebugUnitTest --tests "com.orbit.app.memory.*"`
full_non_phone_gate=PASS 2026-06-12 `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin`
diff_whitespace=PASS 2026-06-12 `git diff --check`
permission_scan=PASS 2026-06-12 no Android secret/client hits under `app/src`
network_boundary_scan=PASS 2026-06-12 no direct network constructors in scanned app source
apk_path=app/build/outputs/apk/debug/app-debug.apk
apk_sha256=2759e7fcfd75276680b1f3caf9d85b1327c9e497834c80fdd10522fca9139f5f
phone_validation=deferred until device available
known_limits=Memory Review UI is source-compiled and locally gated, but not yet manually validated on phone. Compose UI assertions still need explicit regression tests before closeout.
```
