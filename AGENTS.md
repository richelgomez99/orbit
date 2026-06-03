# AGENTS.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

Orbit is a local-first, cloud-augmented personal memory layer for Android. It captures screenshots, clipboard text, and other user activity, wraps each in an **IntentEnvelope** (what was captured + why), and surfaces them in a daily diary.

Do not treat this repository as if the strategic vision is already fully implemented. Current code still has the earlier `LlmProvider` abstraction with `CloudLlmProvider`, `NanoLlmProvider`, and AICore/Gemini Nano references. The May 22 vision changes the local-AI direction: AICore/Gemini Nano should be understood as current/legacy implementation, while the strategic target is a BYOM/local-model-manager architecture using MLC LLM or LiteRT-LM with tiered local models and a cloud gateway fallback.

The repo name is `capsule-app` but the project identity is **Orbit** (`com.orbit.app`).

## Product Vision vs Current Implementation

Read `VISION-2026-05-22.md` before major product or architecture work. It is the strategic direction, not a shipped-state document. Future agents must explicitly distinguish:

- **Current implementation** — what exists in Kotlin/Room/AIDL/Compose code and current specs/tasks.
- **Active Spec Kit work** — what the current branch is implementing or has just implemented.
- **Strategic vision** — what Orbit is becoming, even when no concrete spec or code exists yet.

When those differ, do not "fix" code to match the vision without a spec/plan. First identify the gap, find the relevant Spec Kit artifact, and if no artifact exists, create or update the spec before implementing.

Orbit is intended to be a **mobile attention memory system**: a quiet daybook that captures small things the user would otherwise lose and returns them as a narrative of the day. It is not primarily a productivity dashboard, chatbot, or cloud archive.

### UX Direction

Orbit is moving toward a **3-pillar information architecture**:
- **Diary (Time)** — chronological daybook of saved moments; pure memory, no queue pressure.
- **Library (Space & Search)** — semantic retrieval, fuzzy search, filtering by tags/types.
- **Orbit (Agent & Action)** — execution layer for Active Intent cleanup, Action Drafts, and dedicated chat sessions.

Near-term UX roadmap:
- **Clarify capture affordance** — lightweight button on the floating overlay that opens a transparent Android dialog so the user can add intent/context at capture time.
- **Active Intent cleanup** — the agent helps resolve accumulated intent patterns without turning the diary into a task queue.
- **Dedicated agent workspace** — bottom navigation separates memory browsing from agentic workflows.

### Agentic Direction

Orbit should become an agent that learns from captured attention rather than guessing blindly:
- Use the Knowledge Graph to ground actions and disambiguate references.
- Ask before acting when intent is ambiguous, using native choice surfaces.
- Surface "Curious Agent" profile questions only when background clusters reveal a high-confidence pattern.
- Support dedicated chat/workbench sessions where users can attach prior envelopes as context.

### Generative UI Direction

The long-term UI target is **"the chat IS the app"** using declarative, native-rendered agent UI:
- Strategic target: Google's A2UI (Agent-to-User Interface) JSON protocol plus a Jetpack Compose renderer.
- LLMs should output UI intent/data, not arbitrary visual styling or executable code.
- The Android app remains responsible for rendering using Orbit's graphite/cream Quiet Almanac design language.

### Local AI Direction

The vision explicitly supersedes the old assumption that Google's restricted AICore/Gemini Nano path is the long-term local-AI foundation:

- **Current code**: `NanoLlmProvider` exists and must remain functional where referenced. `LlmProviderRouter` currently chooses cloud by default unless local mode and hardware capability allow Nano.
- **Strategic target**: abandon reliance on AICore as the primary local engine and ship a BYOM/local-model-manager path using MLC LLM or LiteRT-LM via C++/JNI and Vulkan.
- **Model tiers**: a small "Speed" model for extraction/basic understanding, a larger "Intelligence" model for offline deep chat/A2UI generation, and cloud gateway as the zero-download default.
- **Invariant**: local mode remains a structural escape hatch. Cloud changes quality and latency, not feature scope.

Do not describe AICore/Gemini Nano as the final architecture. Describe it as the current local-provider implementation until the BYOM/local-model-manager spec lands.

### Android AI Landscape (from Google I/O 2025 transcripts)

The `transcripts/` directory contains Google I/O 2025 session transcripts. The following distills the durable technical facts future agents need when working on Orbit's AI layer. This is reference material — do not auto-implement any of it without a spec.

#### Google's On-Device AI Stack

**ML Kit GenAI APIs → AICore → Gemini Nano** is Google's managed on-device path:
- AICore is an Android system service that deploys and manages Gemini Nano. It handles model updates, hardware-specific optimizations, and per-app inference isolation (input/output are never stored).
- **Prompt API**: accepts text + images as input, emits text. This is what `NanoLlmProvider` will use once AICore SDK integration lands (all current methods are `TODO("AICore integration — US2")` stubs).
- **Task APIs** (no prompting required): summarization, proofreading, rewriting, image description, speech recognition.
- **Gemini Nano 4** (based on Gemma 4 architecture) ships in two variants: **Fast** (lower latency) and **Full** (better multimodal/image understanding). Preview available now; consumer devices later in 2025.
- **Prefix Caching**: stores intermediate LLM state for recurring prompt prefixes. 2–4× faster inference. Directly useful for Orbit's extraction/classification prompts that share system-prompt prefixes.
- **Structured Output API** (upcoming): annotate a Kotlin data class as `@Generable` with per-field `@Guide` annotations; call `generateTypedContent()` to get type-safe results. Will make `extractActions()` and `classifyIntent()` far more reliable than parsing raw JSON from prompts.
- **Automated Prompt Optimizer (APO)**: server-side tool that uses Gemini Pro/Flash to automatically find optimal prompts for on-device models given evaluation datasets.
- **Model Selection API**: target preview models (e.g., Nano 4) during development via `AICore Developer Preview` app.
- Nano is now on **140M+ devices**. It runs on flagship/upper-mid-range hardware only.

**LiteRT-LM** is Google's official BYOM (Bring Your Own Model) path for custom/fine-tuned models:
- **LiteRT-LM CLI**: converts and quantizes models into a format optimized for mobile hardware.
- **Hardware backends**: CPU, GPU, NPU — specified at engine initialization.
- **Kotlin API**: `LiteRtLmEngine` → load model → `Conversation` object (manages context window and chat history) → `sendMessageAsync()` returns a Kotlin Flow for streaming tokens.
- **Google AI Edge portal**: benchmarks your LiteRT-LM file across 100+ real physical Android devices before production.
- Works on broader device range than AICore. Good for: niche domains, offline-only regions, custom fine-tuned models (e.g., Gemma 270M fine-tuned for specific tasks).
- This is the path Orbit's spec-022 (local-model-manager) will use for the BYOM architecture.

#### Hybrid Inference

**Firebase AI Logic** provides managed hybrid routing with four inference modes:
- `preferOnDevice` → `preferCloud` → `onlyOnDevice` → `onlyCloud`
- Automatically falls back based on Nano availability and connectivity.
- Orbit's `LlmProviderRouter` performs similar routing manually. Firebase AI Logic could simplify this if Orbit adopts it, but the current custom routing preserves Orbit's process-boundary invariants (`:ml` ↔ `:net`).

Custom routing criteria (from Google's guidance): device CPU/battery health, network latency thresholds, query complexity (use a lightweight LLM like Gemma 270M as a router), model/language support on device.

#### Agentic Architecture on Android

- **AppFunctions API** (Jetpack library): makes apps behave as on-device MCP servers. Agents like Gemini discover and invoke annotated `@AppFunction` suspend functions. Orbit already has its own internal `AppFunctionRegistry` + `BuiltInAppFunctionSchemas` — these are conceptually aligned but not yet using the Jetpack API.
- **ADK for Android** (v0.1): Agent Development Kit for on-device multi-agent orchestration. Lets you define agents with models, instructions, tools, and subagents. Supports cloud orchestrator + on-device subagents pattern (privacy-preserving: sensitive data processed locally, only stripped results go to cloud). Relevant to spec-010 (agent-coordinator).
- **AG-UI protocol**: standardized bidirectional event-driven communication between agents and UI clients. Predefined message types for lifecycle events, text messages, tool calls, state management.
- **A2UI protocol**: agents output declarative JSON component trees; client renders natively. Supports basic components (column, text, etc.) and custom components. Variables bound separately from structure. Google is building a **Jetpack Compose renderer**. This validates Vision's "chat IS the app" target (spec-021).

#### How This Maps to Orbit's Architecture

- `NanoLlmProvider` → ML Kit GenAI Prompt API path. Once AICore SDK lands, wire `classifyIntent()`, `summarize()`, `extractActions()`, `embed()` to Prompt API calls.
- `LlmProviderRouter` → Orbit's custom hybrid inference. Analogous to Firebase AI Logic's 4 modes but respects Orbit's `:ml`/`:net` process boundaries.
- `AppFunctionRegistry` / `BuiltInAppFunctionSchemas` → Orbit's internal function registry. Future: consider adopting Jetpack `@AppFunction` annotations so Gemini and other system agents can discover Orbit's capabilities.
- `:ml` process (no network) + `:net` process (sole egress) → already implements the privacy-preserving pattern Google recommends for ADK on-device subagents.
- Spec-021 (generative-ui-runtime) → A2UI + Compose renderer.
- Spec-022 (local-model-manager) → LiteRT-LM / MLC LLM with hardware-tiered models.
- Spec-010 (agent-coordinator) → ADK for Android as a potential framework.

#### Memory Pressure Warning

Android 17 introduces per-app memory limits that kill apps with runaway resource usage. On-device AI models compete for the same RAM budget. Orbit's tiered model approach (Speed ~900MB / Intelligence ~1.2GB / Cloud 0MB) must account for this. The `:ml` process hosts both the encrypted Room database and AI inference — memory profiling is critical when the local model manager lands.

Android 17's `ProfilingManager` API adds **Anomaly** and **Out of Memory** event triggers that automatically capture heap dumps when memory exceeds system thresholds or an OOM occurs. When spec-022 lands, wire these triggers in the `:ml` process to catch model-loading memory spikes in the field. Use Perfetto UI (`ui.perfetto.dev`) to analyze the captured heap dumps.

### Current Active Product Slice

The active branch is `feature/004-active-intent-cleanup-20260518`. `specs/004-capture-understanding/` is currently implemented as **Screenshot Cleanup + Active Intent**, not the older broad "capture understanding" plan from the May 13 roadmap. It adds Room v8 sidecars, Basic local understanding, Active Intent projection/resolution, compact Binder payloads, and a cleanup UI.

Important current constraints from Spec 004:
- Basic mode is deterministic and local. It must not call network, cloud LLMs, public fetch, Readability, browser automation, or VLM.
- Active Intent sidecars store compact evidence only. Raw screenshots, raw HTML, full OCR bodies, embeddings, prompts, and model responses must not cross Binder.
- Smart/Deep escalation is explicit, audited, and still routed through existing network boundaries.
- Ask Orbit chat, KG backend, autonomous agent planning, external writes, and BYOM local models are still future work.

Deferred Spec 004 items include Android Active Intent review dispatch through `:net`, action drafting from reviewed captures, backup/data-extraction cleanup, direct DB access audit, KG/backend POC, and AppFunctions dependency upgrade.

## Build & Development Commands

This is a Gradle-based Android project. JDK 21 (Temurin) is required.

### Build
```
./gradlew :app:assembleDebug
./gradlew :app:compileDebugKotlin          # compile-only check
```

### Unit Tests (JVM, no device needed)
```
./gradlew :app:testDebugUnitTest
```

### Custom Lint Module Tests
```
./gradlew :build-logic:lint:test
```

### Android Lint
```
./gradlew :app:lintDebug
```

### Instrumented Tests (requires connected device or emulator)
```
./gradlew :app:connectedDebugAndroidTest
```

### Run a single unit test class
```
./gradlew :app:testDebugUnitTest --tests "com.orbit.app.ai.LlmProviderRouterTest"
```

### Run a single instrumented test class
```
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.orbit.app.data.OrbitDatabaseMigrationV7toV8Test
```

### Full CI pipeline (matches GitHub Actions)
```
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug
```

### local.properties

Required for cloud features (not committed to git):
```
sdk.dir=/path/to/android/sdk
cloud.gateway.url=https://...
supabase.url=https://...
supabase.publishable.key=...
supabase.debug.email=...        # debug builds only
supabase.debug.password=...     # debug builds only
```

Missing cloud properties produce Gradle warnings but the app builds and runs in offline/local-AI mode.

## Architecture

### Multi-Process Model (Principle VI — Privilege Separation)

This is the most important architectural concept. Orbit runs across **four Android processes**, enforced at the manifest level. No single process holds both user content and network access:

- **`:capture`** — Clipboard observation, screenshot MediaStore observer, overlay service, action execution. **No network permission, no corpus write.**
- **`:ml`** — Encrypted Room database (SQLCipher) owner, AI inference (Gemini Nano / cloud routing). **No network permission.** This is where `OrbitDatabase`, all DAOs, and `EnvelopeRepositoryService` live.
- **`:net`** — Sole network egress process. URL fetching, LLM gateway proxy, Supabase auth. **No corpus access.** Enforced by a custom lint rule (`OrbitNoHttpClientOutsideNet`).
- **default (`:ui`)** — User-facing Activities and ViewModels. **No direct corpus access** — reads go through `:ml` via AIDL binder.

All cross-process communication uses **AIDL interfaces**:
- `IEnvelopeRepository` — `:ui`/`:capture` → `:ml` (seal, read, mutate envelopes)
- `INetworkGateway` — `:ml` → `:net` (URL fetch, LLM gateway calls)
- `IActionExecutor` — `:ui` → `:capture` (dispatch confirmed action proposals)
- `IAuditLog` — `:ui` → `:ml` (audit log reads)

**When adding code that touches the network: it MUST go in `com.orbit.app.net.*` and run in the `:net` process.** The lint rule `OrbitNoHttpClientOutsideNet` enforces this at build time — OkHttpClient, HttpURLConnection, Socket, and Ktor HttpClient constructors outside `com.orbit.app.net` are compilation errors.

### Database & Storage

- **Room + SQLCipher** — All user data in `OrbitDatabase` (currently version 8), encrypted with Android Keystore-wrapped passphrase. Database opens only in `:ml` process.
- **`EnvelopeStorageBackend`** interface — Abstraction layer over persistence. Sole v1 impl is `LocalRoomBackend`. Future backends (Orbit Cloud, BYOC) will implement this same interface.
- **Schema migrations** live in `OrbitMigrations.kt`. Each migration has a corresponding instrumented test (`OrbitDatabaseMigrationV*Test`). Room schemas are exported to `app/schemas/` for migration testing.

### AI / LLM Layer

- **`LlmProvider`** interface — All AI inference goes through this. Two production implementations:
  - `NanoLlmProvider` — Existing on-device provider path with AICore/Gemini Nano assumptions. Treat this as current/legacy local implementation, not the long-term vision.
  - `CloudLlmProvider` — Routes through `:net` process via `INetworkGateway.callLlmGateway()`.
- **`LlmProviderRouter`** — Single resolution point. Checks `RuntimeFlags.useLocalAi` and hardware capability to select the provider.
- **`RuntimeFlags`** — In-memory volatile flags that gate features: `useLocalAi`, `clusterEmitEnabled`, `devClusterForceEmit`, `useNewVisualLanguage`, `clusterModelLabelLock`.

Future local inference work should add a new local model manager/provider behind `LlmProvider` rather than bypassing it. Preserve the process boundary: inference code can run in `:ml`, but all network egress still goes through `:net`.

### Cloud LLM Gateway

The Edge Function lives at `supabase/functions/llm_gateway/` (TypeScript). It proxies AI requests from the Android app to Anthropic/OpenAI, authenticated via Supabase Auth JWT. Handlers exist for each AI capability (classify_intent, summarize, extract_actions, embed, etc.).

### Key Domain Concepts

- **IntentEnvelope** — The core data unit. Captures *what* (text/image) and *why* (intent classification). Has kinds: `REGULAR`, `DIGEST`, `DERIVED`.
- **Continuation** — Background enrichment of an envelope (URL hydration, OCR extraction, action extraction). Managed by WorkManager.
- **Cluster** — Groups of related envelopes detected by embedding similarity. Managed by `ClusterDetector` + `ClusterDetectionWorker`.
- **Active Intent** — Higher-order understanding layer that tracks user intent patterns across envelopes (spec 004).
- **Action Proposal / Execution** — AI-extracted actionable items (calendar events, todos) that the user confirms before dispatch (spec 003).
- **AppFunction** — Agent-callable function registry. Built-in schemas registered at `:ml` process boot.

### Spec-Driven Development

Features are organized in numbered specs under `specs/`. Before coding, read the relevant spec folder in this order:

1. `spec.md` — user stories, requirements, non-goals, stop signs.
2. `plan.md` — implementation approach, architecture constraints, validation gates.
3. `data-model.md` and `contracts/` — schema/API/Binder contracts.
4. `tasks.md` — implementation checklist and current status.
5. `quickstart.md` — expected validation or demo flow.

Do not rely on task checkboxes alone. Reconcile them against the code, tests, git history, and current branch. Several specs have historical drift.

### Existing Spec Landscape

- `001-core-capture-overlay` — Floating capture bubble (shipped)
- `002-intent-envelope-and-diary` — Envelope model + daily diary (v1 target)
- `003-orbit-actions` — Calendar/todo extraction + weekly digest (v1.1)
- `004-capture-understanding` — Current active branch: Screenshot Cleanup + Active Intent, Room v8 sidecars, Basic local understanding
- `005-retrieval-and-ask-citations` — Rebaselined placeholder for search/Ask with citations
- `006-approval-action-runtime` — Rebaselined placeholder for user-approved action execution
- `007-memory-candidates-inspector` — Rebaselined placeholder for memory promotion/inspection/correction
- `008-cloud-controls-storage-budgeting` — Rebaselined placeholder for cloud controls, budgets, fallback policy
- `009-kg-backend-poc` — Rebaselined placeholder for graph/memory backend proof of concept
- `010-agent-coordinator` — Rebaselined placeholder for approval-first agent planning
- `011-manual-compose` — Rebaselined placeholder for deliberate/manual capture
- `012-resolution-semantics` — Rebaselined placeholder for duplicate/conflict/stale fact semantics
- `013-cloud-llm-routing` — Cloud LLM routing skeleton
- `014-edge-function-llm-gateway` — Supabase Edge Function gateway
- `015-visual-refit` — "Quiet Almanac" visual language

`018-capture-context-affordance`, `019-agent-workspace-ia`, and `020-curious-agent-profiling` exist only as empty directories in this checkout. `021-generative-ui-runtime` and `022-local-model-manager` are named in `VISION-2026-05-22.md` but do not yet exist as concrete spec folders here. Treat all of these as future roadmap slots until Spec Kit artifacts are created.

### Spec Kit Workflow

The repository uses Spec Kit conventions and git extension helpers under `.specify/`:

- Full feature flow: `/speckit.specify` → `/speckit.plan` → `/speckit.tasks` → implementation.
- Git extension commands are documented in `.specify/extensions/git/README.md`.
- Feature branches use sequential/spec naming, usually `feature/<spec-number>-<description>-<date>`.
- Specs should be independently testable by user story. `tasks.md` should map tasks to user stories and exact file paths.

Important workflow rule from `docs/spec-branch-reorganization-plan-2026-05-13.md`: do not pre-generate detailed plans/tasks for every future branch. Keep lightweight placeholders for future slots, then rerun the full Spec Kit loop for the next branch after rereading the latest code, docs, and previous branch outputs.

### Roadmap Reconciliation Before Work

Before starting any feature, check:

1. `VISION-2026-05-22.md` for the strategic destination.
2. `docs/product-roadmap-audit-2026-05-12.md` for product thesis, contradictions, and source-of-truth hierarchy.
3. `docs/spec-branch-reorganization-plan-2026-05-13.md` for branch/spec ordering and stale spec disposition.
4. The active spec folder for the branch.
5. Current code and tests.

If vision and code conflict, document the gap in the spec/plan rather than silently changing architecture. If an existing spec conflicts with newer vision, update the spec first.

### Constitution

The twelve principles in `.specify/memory/constitution.md` govern all design decisions. Key ones:
- **Principle I** — Local-first supremacy: device is source of truth; cloud augments, never overrides
- **Principle III** — Intent before artifact: organize by *why*, not *what*
- **Principle VI** — Privilege separation: process boundaries are the security model (see Multi-Process Model above)
- **Principle VIII** — Collect only what you use: no speculative data collection
- **Principle IX** — User-sovereign cloud escape hatch: every feature must work in local mode; current constitutional/code wording preserves Nano, while the strategic direction generalizes this to a BYOM local model manager
- **Principle XII** — Provenance or it didn't happen: derived facts must trace to source episodes

### Custom Lint Rules

Two custom lint rules in `build-logic/lint/`:
1. **`OrbitNoHttpClientOutsideNet`** — Blocks HTTP client construction outside `com.orbit.app.net.*` (Principle VI).
2. **`NoAgentVoiceMarkOutsideAgentSurfaces`** — Restricts the ✦ `AgentVoiceMark` glyph to an allow-list of agent-voice surfaces.

### WorkManager Scheduled Tasks

Scheduled from `OrbitApplication.onCreate()` in the default process:
- `SoftDeleteRetentionWorker` — Daily, purges envelopes soft-deleted >30 days
- `AuditLogRetentionWorker` — Daily, prunes old audit entries
- `WeeklyDigestWorker` — Weekly (Sunday 06:00), requires charging + WiFi
- `ClusterDetectionWorker` — Daily (03:00), requires charging + WiFi

### UI

- **Jetpack Compose** throughout
- **DiaryActivity** is the launcher entry point
- Theme tokens in `ui/theme/` and `ui/tokens/`
- "Quiet Almanac" design language gated by `RuntimeFlags.useNewVisualLanguage`

## Branch Naming Convention

Feature branches follow: `feature/<spec-number>-<description>-<date>` (e.g., `feature/004-active-intent-cleanup-20260518`). Hygiene branches use `hygiene/` prefix.

## Commit Conventions

Conventional-commit style prefixes: `feat(spec)`, `fix(spec)`, `docs(spec)`, `chore`. Task IDs (e.g., T025, T132) are referenced in commit messages and code comments to trace back to spec task lists.
