# Feature Specification: Local Model Manager

**Feature Branch**: `feature/022-local-model-manager-20260614`  
**Created**: 2026-06-14  
**Status**: Slice 1 (seam) done; Slice 2 (native engine + download + UI) delivered 2026-07-07  
**Input**: Roadmap row 13, `VISION-2026-05-22.md`, and current `LlmProviderRouter`/`NanoLlmProvider` implementation.

## Status Update (2026-07-07)

The original spec below scoped **Slice 1** — a pure, JVM-testable selection
policy + router seam — and deferred the native runtime, download, and UI to
Non-Goals. A user-directed **Slice 2** has since landed all of that on device
(see `tasks.md` Phase 5): MediaPipe LLM Inference (`tasks-genai`) running Gemma
3 1B in `:ml`, a `:net` download pipe for gated Hugging Face weights, a
model-manager settings UI with a persistent "prefer on-device AI" toggle, and a
single `:ml` inference engine fronted by the `ILocalInference` AIDL. Constitution
boundaries held throughout (download in `:net`, inference in `:ml`, no network at
inference). The Non-Goals "No model download UI" and "No native LiteRT-LM/MLC
dependency" are therefore **superseded**; the rest still hold. Known limitation:
on-device 1B classification (`classifyIntent`/`scanSensitivity`) is not
production-quality (M2 finding), so those return safe defaults — reliable local
classification (4B / constrained decoding) is deferred (T022-023/024).

## User Stories

### US1 - Local Mode Has A Real Provider Plan

As an Orbit user, when I enable local-first AI, Orbit should know whether this device can use a local model tier and what capability that tier supports.

**Acceptance Criteria**

1. Given a hardware profile and installed-model state, Orbit can select Speed, Intelligence, legacy Nano, Cloud, or Unavailable with an explicit reason.
2. Given no installed BYOM model, Orbit does not pretend local model capabilities are available.
3. Given cloud routing is disabled and no local model is usable, Orbit fails closed with an unavailable provider.

### US2 - Capability-Aware Routing

As Orbit, I need to know which AI capabilities are safe on each local tier.

**Acceptance Criteria**

1. Speed tier is eligible for extraction/basic understanding and embeddings only.
2. Intelligence tier is eligible for deeper Ask/generative UI capabilities.
3. Legacy Nano remains available as current implementation, not the strategic final architecture.

### US3 - Native Engine Integration Stays Isolated

As an engineer, I need the large native runtime work to be behind an interface so the current app remains shippable.

**Acceptance Criteria**

1. No LiteRT-LM/MLC native binary, model download, or JNI bridge is required in the first slice.
2. The selection policy and router seam are testable on the JVM.
3. `:ml` remains the only process that may host local inference; local providers must not touch network.

## Requirements

- **FR-022-001**: Add local model tier, capability, hardware profile, installed model, and selection domain models.
- **FR-022-002**: Add a deterministic selection policy for Speed/Intelligence/legacy Nano/Cloud/Unavailable.
- **FR-022-003**: Add tests for RAM/Vulkan/model-install/cloud-disabled routing outcomes.
- **FR-022-004**: Add a router seam that can accept a BYOM local provider without changing production default behavior.
- **FR-022-005**: Update comments/docs so AICore/Nano is current/legacy, while BYOM/local model manager is the strategic target.

## Non-Goals

- No model download UI.
- No native LiteRT-LM/MLC dependency.
- No C++/JNI/Vulkan integration.
- No persistent model registry table.
- No memory profiling automation.
- No removal of `NanoLlmProvider`.
- No change to cloud gateway defaults.

## Stop Signs

- Stop if implementation requires network access outside `:net`.
- Stop if local mode falls back silently to cloud while cloud routing is disabled.
- Stop if a not-installed model is treated as usable.
- Stop if Speed tier is allowed to power deep Ask/generative UI.
- Stop if native engine import expands the branch beyond a testable seam.
