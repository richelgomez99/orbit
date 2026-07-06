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
