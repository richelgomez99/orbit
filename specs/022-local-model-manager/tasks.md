# Tasks: Local Model Manager

**Input**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/local-model-manager-contract.md`

**Tests**: Required. This branch changes local/cloud inference routing semantics.

## Phase 1: Setup

- [x] **T022-001** Create fresh Spec Kit artifacts in `specs/022-local-model-manager/`.
- [x] **T022-002** Update roadmap and handoff to mark Spec 022 active.

## Phase 2: Local Model Policy

- [x] **T022-003** Add local model domain models under `app/src/main/java/com/orbit/app/ai/local/`.
- [x] **T022-004** Add deterministic `LocalModelSelectionPolicy`.
- [x] **T022-005** Add JVM tests for Speed, Intelligence, legacy Nano, Cloud, and Unavailable outcomes.

## Phase 3: Router Seam

- [x] **T022-006** Add optional BYOM/local selection seam to `LlmProviderRouter.resolve` without changing production defaults.
- [x] **T022-007** Add router tests proving selected BYOM provider can win in local mode and unavailable fails closed when cloud is disabled.
- [x] **T022-008** Update comments/docs to label Nano as current/legacy, not final architecture.

## Phase 4: Validation And Closeout

- [x] **T022-009** Run focused local-model/router tests.
- [x] **T022-010** Run compile gates.
- [x] **T022-011** Run full non-phone gate.
- [x] **T022-012** Run `git diff --check`.
- [x] **T022-013** Update quickstart, roadmap, and handoff with validation evidence.
- [x] **T022-014** Commit without `dist/`, APK outputs, screenshots, `.gbrain-source`, or secrets.

## Phase 5: Native Engine, Download & Manager UI (delivered 2026-07-07)

> Second slice. Supersedes the original spec's Non-Goals "No model download UI"
> and "No native LiteRT-LM/MLC dependency" (user-directed — see spec.md Status
> Update 2026-07-07). Constitution boundaries held: download in `:net`,
> inference in `:ml`, no network at inference.

- [x] **T022-015** Model catalog (`LocalModelCatalog`: Gemma 3 1B/4B, sizes, capabilities, HF source URLs). `996706d`
- [x] **T022-016** BYOM download through `:net` (`INetworkGateway.startModelDownload` → `NetworkGatewayImpl.downloadModelFile` → `ModelDownloadStore`), incl. bearer-auth + redirect-following for gated Hugging Face weights. Verified: real 529 MB Gemma 3 1B `.task` downloaded byte-exact. `0fc6294`, `0b36cd6`
- [x] **T022-017** MediaPipe LLM engine (`com.google.mediapipe:tasks-genai`, `MediaPipeLlmProvider`) — mmaps the `.task`, runs `generateResponse` in `:ml`. Verified on device (~2.2s, coherent). `9a43872`
- [x] **T022-018** Router routes to the local provider when a model is installed + selected. `531c789`
- [x] **T022-019** Model-manager settings UI (`LocalModelsActivity` — download/progress/delete + persistent `localAiEnabled` toggle; in-memory-only HF token field). `c4b1f79`
- [x] **T022-020** Single `:ml` inference engine over AIDL (`ILocalInference` + `LocalInferenceService` + `RemoteLocalLlmProvider`) — one warm engine, other processes proxy. Unit-tested (`LocalInferenceDispatcherTest`) + validated on device (engine loads in `:ml` only; Binder round-trip). `2012adf`, `c00b972`
- [x] **T022-021** `NanoLlmProvider` fails safe (no more `TODO()` landmine). `e32965a` (M3)
- [x] **T022-022** M2 gate — measured on-device classifier quality; naive-prompt `classifyIntent`/`scanSensitivity` on the 1B are not production-quality, so the BYOM provider returns safe defaults for those and powers only `summarize`/`generateDayHeader`. `df3e4aa`

## Deferred / Follow-up

- [x] **T022-026** Verify the 4B bundle loads on device — **FINDING (2026-07-07): it does NOT.** The download works (2.56 GB byte-exact) but litert-community's 4B repo ships only `-web` variants, and that file is a raw `TFL3` LiteRT flatbuffer, not a zip-based MediaPipe `.task` — `LlmInference` rejects it ("Unable to open zip archive"). Encoded as `androidTaskAvailable=false` so the 4B is not offered for download or selected. `96ff037`+
- [ ] **T022-023** Reliable local classification — needs EITHER a genuinely loadable 4B on Android (proper `.task` via Google's converter, or the LiteRT-LM engine instead of tasks-genai — see T022-026) OR grammar-constrained decoding on the 1B. Note: even if a 4B loaded, per-capture classification on a 2.6 GB model is impractical (memory/latency); 4B is the on-demand INTELLIGENCE tier, not a per-capture classifier.
- [ ] **T022-024** On-device `extractActions` via constrained decoding (empty default until then).
- [ ] **T022-025** Session-level sampling (topK/temperature) — `LlmInferenceSession` hung with tasks-genai 0.10.35 + Gemma 1B; revisit on a version bump.
- [ ] Crash-telemetry Phase B (consent-gated upload) is tracked separately in spec-023.
