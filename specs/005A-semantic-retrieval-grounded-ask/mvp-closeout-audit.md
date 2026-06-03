# MVP Closeout Audit: Spec 005A/005B

Date: 2026-06-03
Branch: `feature/005a-semantic-retrieval-grounded-ask-20260603`

This file is the requirement-by-requirement evidence ledger for the current MVP gate. It exists to prevent emulator, backend, or unit-test proof from being mistaken for the final S24 demo proof.

## Current APK

```text
path=dist/orbit-mvp-debug-20260603-005b.apk
sha256=7a80f2dc94b00766177eec6f80393bf4289c39c8252cd6f3a44d925c1369c885
```

## Evidence Matrix

Aggregate non-S24 verification command:

```bash
specs/005A-semantic-retrieval-grounded-ask/scripts/verify-non-s24-closeout.sh
```

Latest result: pass on 2026-06-03. This command does not satisfy the final S24 proof.

| Requirement | Evidence | Status |
| --- | --- | --- |
| Atlas-backed compact memory index exists and is backend-only. | `memory_gateway_deploy=pass`; production alias `https://orbit-memory-gateway.vercel.app`; Atlas vector smoke/eval recorded in `quickstart.md`; Android secret scan has no Atlas/OpenAI client/secret hits under `app/src`. | Proven locally/backend. |
| Memory gateway deploy/config works. | `memory_gateway_deploy=pass`; deployment id `dpl_6inFrjavbbapv6HF8Kg3o6VKKehX`; live semantic smoke maps `qr code`, `reschedule`, and `flight receipt` to expected demo records and refuses passport question. | Proven backend/live smoke. |
| Backend implementation still passes after closeout changes. | 2026-06-03 command passed: `npm run typecheck && npm run test:unit && npm run eval:retrieval` in `supabase/functions/memory_gateway`; 39 unit tests and 6/6 retrieval eval. | Proven locally. |
| Android implementation still passes after closeout changes. | 2026-06-03 command passed: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:compileDebugAndroidTestKotlin :app:assembleDebug`. | Proven locally. |
| Room/SQLCipher remains source of truth. | Android code routes memory gateway through compact DTOs; Library and Ask filter cloud results to local-backed envelope IDs; Android still opens local capture detail through repository/binder paths. | Proven by code/tests; final phone UX still pending. |
| Process-boundary/privacy invariants hold. | `android_secret_scan=pass`; `android_network_boundary_scan=pass`; `:app:lintDebug` and `:build-logic:lint:test` pass; no direct Atlas/OpenAI secrets in Android. | Proven locally. |
| Demo memories can be seeded for validation. | `install-and-seed-device.sh --reset-data` on Pixel 10 Pro Android 17 emulator seeded 20 demo envelopes; repeat seed broadcast logged 20 without `DebugDemoSeedReceiver` failure. The same helper installed/launched and seeded APK SHA `7a80f2dc94b00766177eec6f80393bf4289c39c8252cd6f3a44d925c1369c885` on S24 `SM-S928U1`. | Proven on emulator and S24. |
| Diary/Library/Orbit are installable and launchable. | Rebuilt APK installed/launched on Pixel 10 Pro Android 17 emulator; connected smoke passed `OrbitHomeNavigationTest`, `LibraryScreenTest`, and `AskOrbitPanelTest`. S24 helper installed and launched the same APK. | Proven on emulator and S24. |
| Library search opens cited local captures. | Android connected test verifies search result opens local capture callback; backend live smoke verifies semantic ranking for expected records. User reported S24 Library searches/actions worked, including QR code and reschedule flows from prior screenshots. | Proven by tests/backend and user-reported S24 flow. |
| Orbit grounded Ask cites saved captures and refuses unsupported sensitive identifiers. | Backend live smoke answers/refuses correctly; Android connected test verifies citation open and sensitive refusal rendering. Uploaded S24 screenshots show grounded Ask recipe/startup-event answers with cited captures, and user reported the actions worked. | Proven by tests/backend and S24 screenshots/report. |
| 005B screenshot/source wording and link rehydration context do not leak raw content. | Unit tests cover prompt/source context and banned fields; detail fallback copy avoids `unknown`; user reported link/action flows worked. One S24 screenshot showed `from IntentResolver`; fixed APK filters non-user-facing source labels in card/detail/compact-memory paths, and user confirmed no more `from IntentResolver` after reinstall. | Proven by tests and S24 recheck. |
| Final MVP phone proof. | APK SHA above installed/launched/seeded on S24; user reported Library/Orbit actions worked; S24 screenshots uploaded for grounded Ask citations; post-fix source-label recheck passed. | Proven. |

## Remaining Non-Deferred Work

None for Spec 005A/005B.

Spec 006 can proceed from this validated MVP baseline.
