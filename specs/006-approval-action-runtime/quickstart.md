# Quickstart: Approval Action Runtime

## Preconditions

- Start from validated Spec 005A/005B MVP commit.
- Use branch `feature/006-approval-action-runtime-20260603`.
- Do not add generated APKs under `dist/` to git.

## Local Validation

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest --tests "com.orbit.app.action.*" --tests "com.orbit.app.diary.*" --tests "com.orbit.app.orbit.*"
./gradlew :build-logic:lint:test :app:lintDebug
./gradlew :app:assembleDebug
```

Secret/network boundary scans:

```bash
rg -n "OPENAI_API_KEY|MONGODB_ATLAS_URI|mongodb\\+srv|MongoClient|ANTHROPIC_API_KEY|ZEROENTROPY" app/src || true
rg -n "OkHttpClient\\(|HttpURLConnection|Socket\\(|HttpClient\\(" app/src/main/java app/src/debug/java app/src/release/java || true
```

Expected: no Android secret/client hits and no direct network constructors outside approved net code.

## S24 Demo Flow

1. Build and install the debug APK on the S24.
2. Run the debug seed that creates demo captures and deterministic action proposals.
3. Open Orbit.
4. Verify Action Drafts show at least:
   - Calendar draft for startup event or concert ticket.
   - Local list/todo draft for recipe shopping list.
   - A delegated/share/reply-like draft is either absent or clearly disabled if not supported.
5. Open the calendar draft.
6. Verify the approval sheet shows typed fields, source evidence, and no raw JSON.
7. Confirm.
8. Verify the Android Calendar insert screen opens with the expected fields.
9. Return to Orbit and verify the draft is no longer pending and does not show a misleading Orbit undo for Calendar.
10. Open the local list/todo draft.
11. Confirm.
12. Verify one derived list envelope appears in Diary and is searchable in Library.
13. Verify the envelope contains multiple checklist items, then toggle one item done.
14. Check no crash and no network-boundary violation.

## Negative Paths

- Dismiss a draft: it should disappear and not execute.
- Corrupt args in a test seam: proposal invalidates and no external intent fires.
- Disable or remove target handler in a test seam: visible failure and audit row.
- Airplane/local-only mode: existing drafts remain reviewable; new cloud extraction is not required.
- Calendar external managed action: no Orbit undo promise.

## Closeout Evidence

Record before closing the branch:

```text
android_unit_tests=PASS 2026-06-04 `./gradlew :app:testDebugUnitTest`
android_lint=PASS 2026-06-04 `./gradlew :app:lintDebug`
android_compile=PASS 2026-06-04 `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin`
action_contract_tests=PASS 2026-06-04 focused JVM action/ViewModel regressions; `ActionsRepositoryDelegateTest` passed on S24
permission_scan=PASS 2026-06-04 `ActionPermissionRegressionTest`; no calendar/contact/storage-write permissions
network_boundary_scan=PASS 2026-06-04 no direct network constructors in scanned app source
apk_path=dist/orbit-mvp-debug-20260603-006.apk
apk_sha256=05cb7026b5738b7c55f3cba7a0d168a4dabcb182270b2f18ad05863d7fad71c5
s24_install_seed=PASS 2026-06-04 installed and seeded on SM-S928U1 / R5CWC2KX4GK
s24_calendar_approval=PARTIAL/PASS by uploaded screenshots and user report before final grouped-list rebuild; final closeout still needs one concise S24 demo pass.
s24_local_todo_approval=PASS 2026-06-05 user confirmed S24 now shows one envelope with multiple checklist items. Earlier screenshot showed the pre-fix bad behavior where one shopping list exploded into ingredient rows.
s24_dismiss_failure_paths=PENDING final manual confirmation.
s24_no_calendar_undo_promise=PARTIAL/PASS by code/tests; final S24 demo should confirm no misleading Orbit undo appears after Calendar handoff.
known_limits=Existing ingredient rows created by older APK builds remain in the local phone database unless app data is reset. The fixed APK only changes newly approved list drafts: one derived list envelope with multiple checklist items in `todoMetaJson.items[]`.
```
