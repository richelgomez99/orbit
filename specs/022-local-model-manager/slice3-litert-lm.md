# Spec 022 — Slice 3: LiteRT-LM Engine Migration

**Branch**: `feature/022-local-model-manager-20260614` · **Created**: 2026-07-07 · **Status**: Planning / de-risk

## Why

`com.google.mediapipe:tasks-genai` (our current on-device LLM engine) is
officially **maintenance-only**; Google says "new features and optimizations
will be focused on LiteRT-LM. We recommend migrating." Slice 2 ships the 1B on
tasks-genai and works, but the engine is a dead end and can't load a larger or
multimodal model (the 4B `-web` `.task` is an incompatible `TFL3` flatbuffer —
T022-026).

**LiteRT-LM** (`com.google.ai.edge.litertlm:litertlm-android`) is the strategic
runtime: GPU/NPU acceleration (speeds up even the 1B), multimodal (relevant —
Orbit captures images), tool use, and **ready-made, ungated `.litertlm` models**
(`litert-community/gemma-4-E2B/E4B-it-litert-lm`, Apache-2.0) that need no
converter, hosting, or license gate.

## Goals

1. Add the LiteRT-LM Android engine behind the existing `LlmProvider` interface
   as `LiteRtLmProvider`, hosted (like `MediaPipeLlmProvider`) only in `:ml`.
2. Prove it end-to-end with the **1B** `.litertlm` first (small, safe), then
   offer a bigger **on-demand INTELLIGENCE** model — **E2B** (not E4B: E4B is
   3.66 GB and this device OOM'd on 2.56 GB; E2B is the pragmatic ceiling).
3. Preserve all constitution boundaries (inference in `:ml`, no network at
   inference, download via `:net`).

## Non-Goals / scope guards

- Do NOT rip out `MediaPipeLlmProvider` yet — run LiteRT-LM alongside behind an
  engine selector so we can compare and fall back. Removal is a later step once
  LiteRT-LM is proven.
- No multimodal wiring in this slice (text only first); it's the follow-on that
  justifies the migration for Orbit's image captures.
- Bigger model is **on-demand INTELLIGENCE only** (deep Ask / generative UI),
  never per-capture classification (M2: memory/latency).

## Approach (staged, each independently committable)

1. **De-risk the dependency (FIRST, before any provider code)**: add
   `com.google.ai.edge.litertlm:litertlm-android:0.13.1`, `assembleDebug`,
   measure APK-size delta + confirm it resolves and the native libs pack
   cleanly. **Stop-and-report if it bloats absurdly or needs vendor-specific
   `.so` juggling** (a known issue: some dispatch backends ship `.so`
   separately).
2. `LiteRtLmProvider : LlmProvider` — `Engine(EngineConfig(modelPath, backend))`
   → `initialize()` (≤10 s, off-main) → `createConversation().sendMessageAsync()`
   Flow, collected into the text methods. Same safe-default posture as
   `MediaPipeLlmProvider` for classify/sensitivity/extractActions/embed.
   Backend = GPU with CPU fallback.
3. Engine selection: `LocalModelEngine.LITERT_LM` in the catalog; the `:ml`
   `ByomLocalProviderHolder` builds the right provider per the selected model's
   engine. `RemoteLocalLlmProvider`/AIDL path is unchanged (engine-agnostic).
4. Catalog: add the 1B `.litertlm` (validation) and gemma-4-E2B `.litertlm`
   (INTELLIGENCE), both with real `.litertlm` source URLs + `androidTaskAvailable`
   semantics generalized to "loadable".
5. Validate on device (app foreground; `:ml`): 1B `.litertlm` loads + generates;
   then E2B loads + a real on-demand Ask. Confirm engine loads in `:ml` only.

## Risks

- **APK size / native `.so`** — the AAR bundles GPU/NPU backends; could be large
  and/or need per-ABI or vendor dispatch libs. This is the gate-1 de-risk.
- **Memory** — even E2B is ~2 GB+; this device is memory-pressured. Validate in
  `:ml` (lean) with the app foreground; keep the OOM lesson in mind.
- **Two engines in the tree** — transitional weight until tasks-genai is removed.
- **API maturity** — LiteRT-LM is pre-1.0 (0.13.1); API may shift.

## Validation log (2026-07-07)

- **Gate 1 (dependency)**: ✅ `litertlm-android:0.13.1` resolves + builds, no
  duplicate-class conflict with tasks-genai. APK +49.3 MB (arm64-only ≈23 MB).
- **Engine runs in Orbit**: ✅ downloaded the 1B `.litertlm` (584 MB byte-exact,
  magic bytes `LITERTLM`) and ran a prompt via `LiteRtLmProvider` →
  `litertlm OK (8223ms)`, coherent output. Findings:
  - **Extension matters**: LiteRT-LM dispatches its loader by file extension.
    A `.task`-named `.litertlm` fails with "Unable to open zip archive" — the
    file MUST end in `.litertlm`. → `ModelDownloadStore` needs a per-model
    extension (currently hardcodes `.task`); wire in the catalog stage.
  - **GPU init failed → CPU fallback worked** (`LiteRtLmJniException` on
    `Backend.GPU()`). CPU is solid; GPU acceleration (the perf win) needs
    follow-up — likely a missing OpenCL/dispatch `.so` or backend config.
  - 8.2 s includes cold load + the failed GPU attempt; steady-state CPU will
    be faster, GPU faster still once fixed.

- **E2B (bigger model) VALIDATED on device (2026-07-07)**: ✅ downloaded Gemma 4
  E2B `.litertlm` (2.59 GB byte-exact, ungated Apache-2.0), routed via `:ml`,
  ran on **GPU** (no CPU fallback), `routed inference OK (10534ms)` incl. cold
  load, coherent answer. **Anti-OOM held**: `:ml` at ~2.26 GB PSS with ~2 GB
  free — no LMK kill (vs the earlier 2.56 GB OOM in `:ui`). Levers that worked:
  load in lean `:ml` (not `:ui`) + `BIND_IMPORTANT` + `largeHeap` + mmap.
  Catalog/extension/engine-selection all wired; download wrote the `.litertlm`
  path directly (no manual rename).

## Remaining (next stages)

- `ModelDownloadStore` per-model file extension (`.litertlm` vs `.task`).
- Catalog: add `LocalModelEngine.LITERT_LM`; entries for the 1B `.litertlm`
  and **E2B** `.litertlm` (INTELLIGENCE). `ByomLocalProviderHolder` builds
  `LiteRtLmProvider` vs `MediaPipeLlmProvider` per the model's engine.
- Download + device-validate E2B (memory-risky: ~2 GB on a pressured device).
- Investigate GPU backend (`libLiteRtDispatch_*` / OpenCL) for acceleration.
- Once LiteRT-LM proven across models, plan tasks-genai removal (reclaims its
  APK share).

## Acceptance

- Dependency resolves + `assembleDebug` green + APK-size delta recorded (gate 1).
- `LiteRtLmProvider` runs the 1B `.litertlm` on device via the existing router/
  AIDL path (engine loads in `:ml`).
- E2B loads and answers one on-demand Ask on device (or a documented memory
  finding if it doesn't fit).
- Full non-phone gate green; constitution boundaries intact.
