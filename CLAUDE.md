# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Where the deeper context lives

`AGENTS.md` at the repo root is the authoritative brief for AI agents working here — read it first for the full strategic vision, current-vs-target architecture gaps, and Spec-Kit landscape. This file is the operational subset most useful during day-to-day Claude Code sessions; when the two disagree, AGENTS.md wins.

Before any non-trivial work, also read (in order):
1. `VISION-2026-05-22.md` — strategic destination, not shipped state.
2. `docs/spec-branch-reorganization-plan-2026-05-13.md` — branch/spec ordering and stale spec disposition.
3. The active spec folder (see current branch).
4. The code and tests you're about to touch.

Repo name is `capsule-app`; the product is **Orbit** (`com.orbit.app`). Do not treat AGENTS.md's vision as shipped — reconcile it against actual code, tests, and the active spec before making architectural claims.

## Build & test

Gradle-based Android project. **JDK 21 (Temurin) required.**

```
./gradlew :app:assembleDebug                       # debug APK
./gradlew :app:compileDebugKotlin                  # compile-only check
./gradlew :app:testDebugUnitTest                   # JVM unit tests, no device
./gradlew :build-logic:lint:test                   # custom lint module tests
./gradlew :app:lintDebug                           # Android lint (runs custom rules)
./gradlew :app:connectedDebugAndroidTest           # instrumented tests (device/emulator)
```

Single test class:
```
./gradlew :app:testDebugUnitTest --tests "com.orbit.app.ai.LlmProviderRouterTest"
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.orbit.app.data.OrbitDatabaseMigrationV7toV8Test
```

Full CI-equivalent pipeline (matches GitHub Actions):
```
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug
```

### local.properties

Not committed. Cloud features degrade gracefully if omitted — build still succeeds in offline/local-AI mode with a Gradle warning.

```
sdk.dir=/path/to/android/sdk
cloud.gateway.url=https://...
supabase.url=https://...
supabase.publishable.key=...
supabase.debug.email=...        # debug builds only
supabase.debug.password=...     # debug builds only
```

## Non-negotiable architectural constraints

### Multi-process privilege separation (Constitution Principle VI)

Orbit runs across four Android processes, enforced at the manifest level. **No single process holds both user content and network access.** This is the security model, not a suggestion.

| Process | Role | Restrictions |
|---|---|---|
| `:capture` | Clipboard/MediaStore observer, overlay service, action execution | No network, no corpus write |
| `:ml` | Encrypted Room (SQLCipher) owner, AI inference | **No network** |
| `:net` | Sole network egress: URL fetch, LLM gateway, Supabase auth | No corpus access |
| default (`:ui`) | Activities, ViewModels | No direct corpus access — reads via AIDL |

Cross-process communication is AIDL: `IEnvelopeRepository`, `INetworkGateway`, `IActionExecutor`, `IAuditLog`.

**Any code that constructs an HTTP client MUST live under `com.orbit.app.net.*` and run in the `:net` process.** The custom lint rule `OrbitNoHttpClientOutsideNet` in `build-logic/lint/` fails the build if `OkHttpClient`, `HttpURLConnection`, `Socket`, or Ktor `HttpClient` is constructed anywhere else. Don't try to disable it — respect the boundary or route through `INetworkGateway`.

Similarly, `NoAgentVoiceMarkOutsideAgentSurfaces` restricts the ✦ `AgentVoiceMark` glyph to an allow-list of agent-voice UI surfaces.

### Room / SQLCipher

- All user data in `OrbitDatabase` (currently v8), encrypted with an Android Keystore-wrapped passphrase. DB opens only in `:ml`.
- `EnvelopeStorageBackend` is the persistence abstraction; sole v1 impl is `LocalRoomBackend`. Future cloud/BYOC backends implement the same interface — don't bypass it.
- Migrations live in `OrbitMigrations.kt`. Each schema change gets a matching instrumented test (`OrbitDatabaseMigrationV*Test`). Room schemas exported to `app/schemas/` — commit them.

### LLM / AI layer

- All inference goes through the `LlmProvider` interface. Current impls: `NanoLlmProvider` (on-device AICore/Gemini Nano — treat as current/legacy, not the long-term target) and `CloudLlmProvider` (routes through `:net` via `INetworkGateway.callLlmGateway()`).
- `LlmProviderRouter` is the single resolution point — it reads `RuntimeFlags.useLocalAi` and hardware capability. Don't call providers directly.
- `RuntimeFlags` are in-memory volatile gates: `useLocalAi`, `clusterEmitEnabled`, `devClusterForceEmit`, `useNewVisualLanguage`, `clusterModelLabelLock`.
- Cloud Edge Function lives at `supabase/functions/llm_gateway/` (TypeScript, Supabase Auth JWT).
- New local-inference work (spec-022 territory) adds providers behind `LlmProvider` — do not bypass the interface or the `:ml`/`:net` boundary.

## Current active work

Active branch (start-of-session snapshot): **`feature/022-local-model-manager-20260614`** — the BYOM / local-model-manager path (LiteRT-LM / MLC LLM direction). Recent commits closed the local-model-selection seam and shipped its artifacts. Always confirm the current branch with `git branch --show-current` — the snapshot goes stale.

Spec folders that are *named* in the vision but exist as placeholders or are still being populated: `018-capture-context-affordance`, `019-agent-workspace-ia`, `020-curious-agent-profiling`, `021-generative-ui-runtime`, `022-local-model-manager`. Treat all pre-022 rebaselined placeholders (`005`-`012`) as roadmap slots until real Spec Kit artifacts land — checkboxes in `tasks.md` do not equal shipped code.

## Domain vocabulary (used everywhere)

- **IntentEnvelope** — core capture unit; carries *what* (text/image) and *why* (intent). Kinds: `REGULAR`, `DIGEST`, `DERIVED`.
- **Continuation** — background enrichment of an envelope (URL hydration, OCR, action extraction) via WorkManager.
- **Cluster** — envelopes grouped by embedding similarity (`ClusterDetector` + `ClusterDetectionWorker`).
- **Active Intent** — spec-004's higher-order pattern layer; compact evidence sidecars only, never raw content across Binder.
- **Action Proposal / Execution** — spec-003 AI-extracted actionables that require user confirmation before dispatch.
- **AppFunction** — internal agent-callable registry (`AppFunctionRegistry` + `BuiltInAppFunctionSchemas`). Not yet on Jetpack `@AppFunction`.

## BYOM local inference (spec-022) — proven working

On-device Gemma runs end-to-end as of `9a43872`. The path:
- **Download** (`:net`): `INetworkGateway.startModelDownload(id, url, expectedBytes, authToken?)` → `NetworkGatewayImpl.downloadModelFile` streams to `filesDir/models/<id>.task` (see `ModelDownloadStore`). Uses a download-tuned OkHttp client (redirects on, no 15s cap) — the *shared* client disables redirects and caps calls at 15s, wrong for large files. `authToken` is sent only as `Authorization: Bearer` on the first request; OkHttp strips it on the cross-host CDN redirect. Gated Gemma weights live at HF `litert-community/Gemma3-1B-IT` / `Gemma3-4B-IT` (accept the license per-repo, then a Read token works).
- **Inference** (`:ml`): `MediaPipeLlmProvider` (`com.google.mediapipe:tasks-genai`) mmaps the `.task` and runs `LlmInference.generateResponse` one-shot (no session for single turn). Engine builder takes only `setModelPath`/`setMaxTokens`; topK/temperature are session-level. `LlmInferenceOptions` is the nested `LlmInference.LlmInferenceOptions`. First slice: `summarize`/`generateDayHeader` generate for real; `classifyIntent`/`scanSensitivity`/`extractActions` return safe defaults; `embed` returns null.
- **Router** (done, `531c789`): both `LlmProviderRouter.create()` and `createPreferLocal()` run the pure `LocalModelSelectionPolicy` against `DeviceAiHardware.probe()` + `installedLocalModels()` and return a `MediaPipeLlmProvider` (via the per-process `ByomLocalProviderHolder` singleton) when a local model is selected. Gated on `RuntimeFlags.useLocalAi` (default false; in-memory volatile, per-process, until the Block 10 SharedPreferences surface). So real consumers (DiaryActivity day headers, `:ml` `EnvelopeRepositoryService` enrichment/digest) use local Gemma once the flag is on.
- **Model manager UI** (done, `c4b1f79`): Settings → "On-device AI" (`LocalModelsActivity`). Download (with optional HF token, in-memory only), progress, delete, and the persistent "Prefer on-device AI" toggle (`PrivacyPreferences.localAiEnabled`). Reached via the debug-only `SettingsActivity` (non-exported; `am start` is blocked on Android 16 — navigate Diary gear → Settings, or tap through the UI).
- **Classifiers — M2 verdict: local classification deferred**: naive-prompt `classifyIntent`/`scanSensitivity` on Gemma 3 1B were measured on device and are NOT production-quality (intent 0/3 on clear cases; sensitivity returned all 5 tags for benign text — the 1B echoes the category list). So the BYOM provider returns SAFE defaults for both (AMBIGUOUS / []) and only does what a 1B is good at: `summarize` / `generateDayHeader`. `extractActions` also stays empty. Reliable local classification needs a 4B model or grammar-constrained decoding (follow-up).
- **On-device findings (important):**
  - **One `LlmInference` per process.** The native lib appears to tolerate only a single engine per process lifetime — a second (even after `close()`) deadlocks generation. So local consumers MUST share the `ByomLocalProviderHolder` singleton; never construct a second `MediaPipeLlmProvider`. This is the concrete driver for the **next slice: route all local inference through one `:ml` engine over AIDL** (today `:ui` day-headers and other processes each build their own engine — mmap shares weights but not the engine, and cross-process this multiplies engines).
  - **Session sampling hangs.** `LlmInferenceSession` (topK/temperature) never returned with tasks-genai 0.10.35 + Gemma 3 1B. Using engine `generateResponse` (default sampling). Revisit on a tasks-genai bump.
  - Base `generate()` is proven on device (~2.2s, coherent). Iterative debug testing is hostile because of the one-engine constraint + flaky runtime-broadcast delivery — force-stop between engine tests.
- **`:ml` single-engine over AIDL** (`2012adf`, **VALIDATED on device**): `ILocalInference` AIDL + `LocalInferenceService` (`:ml`, owns the one engine) + `RemoteLocalLlmProvider` (proxy used by every process except `:ml`). Router returns the in-process engine on `:ml`, the proxy elsewhere. Gated behind `localAiEnabled` (default off). Confirmed: `DEBUG_TEST_ROUTED` from `:ui` → `routed provider = RemoteLocalLlmProvider` → `creating BYOM engine` logs from the **`:ml` pid only** → Binder round-trip to `ILocalInference` (296B req / 828B reply) → `routed inference OK (~2.7s)`. Wire contract also unit-tested (`LocalInferenceDispatcherTest`).
- **M1 test coverage** (`c00b972`): `LocalInferenceDispatcher` (extracted from the service) + `LocalInferenceDispatcherTest` give the AIDL wire contract real CI coverage (request/response JSON round-trip + dispatch mapping) without the device. Binder plumbing itself still needs a fresh-device run.
- **`NanoLlmProvider` fails safe** (`e32965a`, M3): the four `TODO()` stubs now degrade like `UnavailableLlmProvider` (AMBIGUOUS / [] / "" / typed `NanoUnavailableException`) instead of throwing `NotImplementedError`. Removes a latent crash on the `createPreferLocal` fallback path.
- **Debug broadcast harness — ACTUAL ROOT CAUSE (adb quoting)**: the "flaky/vanishing" model-action broadcasts were a *test-harness* bug, not app or device. `adb shell am broadcast … --es text "multi word value" -p com.orbit.app` — the local shell strips the quotes, so device-side `am` sees loose words that scramble arg parsing and **clobber `-p`** (e.g. `pkg=headphones`), sending the broadcast to a nonexistent package. `DEBUG_DUMP` always worked because it has NO `--es` extra. **Fix: wrap the whole remote command in double quotes and single-quote the value, and put `-p` first**: `adb shell "am broadcast -p com.orbit.app -a <ACTION> --es text 'multi word'"`. (`onReceive`/`coroutine entered` logging in `DebugDumpReceiver` confirmed onReceive simply never fired for the mis-targeted sends.)
- **Next**: validate M1 Binder plumbing via the real UI flow or an instrumented bind test; confirm classifier quality end-to-end; `extractActions` via constrained decoding.

Debug broadcasts (debug build, registered in `OrbitApplication`; package is `com.orbit.app`, no `.debug` suffix):
```
adb shell am broadcast -a com.orbit.app.DEBUG_DOWNLOAD_MODEL --es url <URL> --es id <id> [--es token <hf_...>] -p com.orbit.app
adb shell am broadcast -a com.orbit.app.DEBUG_TEST_INFERENCE --es id <id> [--es prompt "<text>"] -p com.orbit.app
adb shell am broadcast -a com.orbit.app.DEBUG_TEST_ROUTED [--es prompt "<text>"] -p com.orbit.app   # flips useLocalAi, proves router → MediaPipeLlmProvider
adb shell am broadcast -a com.orbit.app.DEBUG_TEST_CLASSIFY --es text "<capture>" -p com.orbit.app   # classifyIntent + scanSensitivity via the singleton
```
Note: only ONE engine-creating debug action per process — force-stop between them (one-engine-per-process constraint above).
Results log under tag `OrbitDebugDump`. Downloaded models survive `adb install -r`. Progress/state: `filesDir/models/<id>.download.json`. S24 Ultra ran the 1B INT4 in ~5s.

**Device note:** the test S24 Ultra has a **user 150 = "Secure Folder"** (Samsung always assigns Secure Folder user id 150). Orbit installs in **user 0** (`u0_a929`), NOT Secure Folder. `run-as com.orbit.app` works; `pm list packages` may throw `SecurityException` trying to enumerate user 150 — harmless. The sideloaded debug build occasionally gets culled (Play Protect / auto-remove-unused), unrelated to Secure Folder.

## Spec Kit workflow

Feature work follows Spec Kit: `/speckit.specify` → `/speckit.plan` → `/speckit.tasks` → implementation. Read the spec folder in this order: `spec.md` → `plan.md` → `data-model.md` + `contracts/` → `tasks.md` → `quickstart.md`. Reconcile task checkboxes against code, tests, and git history — several specs have drift.

Do not pre-generate detailed plans/tasks for future branches. Keep lightweight placeholders, rerun the loop when the branch begins.

If vision and code conflict, update the spec first — do not silently refactor architecture to match VISION-2026-05-22.md.

## WorkManager schedule (started in `OrbitApplication.onCreate`, default process)

- `SoftDeleteRetentionWorker` — daily, purges soft-deleted envelopes >30 days
- `AuditLogRetentionWorker` — daily, prunes old audit entries
- `WeeklyDigestWorker` — Sunday 06:00, requires charging + WiFi
- `ClusterDetectionWorker` — daily 03:00, requires charging + WiFi

## Conventions

- Kotlin + Jetpack Compose throughout. Launcher entry point is `DiaryActivity`.
- Theme tokens in `ui/theme/` and `ui/tokens/`; "Quiet Almanac" visual language gated by `RuntimeFlags.useNewVisualLanguage`.
- Branches: `feature/<spec-number>-<description>-<yyyymmdd>` (e.g., `feature/004-active-intent-cleanup-20260518`); hygiene work uses `hygiene/` prefix.
- Commits: conventional-commit style with a spec prefix — `feat(spec-021)`, `fix(spec-004)`, `docs(spec-022)`, `chore`. Task IDs (`T025`, `T132`) referenced in messages and code comments to trace back to `tasks.md`.
