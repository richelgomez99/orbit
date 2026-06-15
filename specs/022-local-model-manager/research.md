# Research: Local Model Manager

## Decision 1: Policy First, Native Engine Later

**Decision**: Implement the first Spec 022 slice as a pure model-selection/capability policy plus router seam. Do not add LiteRT-LM/MLC binaries or JNI in this branch.

**Rationale**: The native runtime and model packaging work is large and device-sensitive. Orbit needs a stable policy boundary first so Android UI, settings, and tests can reason about local capability without blocking on engine integration.

**Alternatives Considered**

- Import native engine immediately: rejected for blast radius, build complexity, APK size, and memory profiling risk.
- Keep using Nano-only local routing: rejected as strategic drift because the May 22 vision explicitly supersedes Nano as final architecture.

## Decision 2: Tier Capabilities Are Explicit

**Decision**: Speed, Intelligence, and Legacy Nano publish capabilities separately.

**Rationale**: Local-first should not mean every AI feature is equally safe on every model. Speed can support extraction/basic understanding; Intelligence can support deep Ask and typed/generative UI.

## Decision 3: Cloud Remains Zero-Download Default

**Decision**: Production defaults remain unchanged unless local-first mode and a usable local model/provider are explicitly available.

**Rationale**: The current cloud gateway is the working MVP path. Spec 022 adds the local escape hatch architecture without destabilizing shipped behavior.

## Decision 4: Legacy Nano Is A Compatibility Candidate

**Decision**: `NanoLlmProvider` remains a local candidate for current devices/tests, but documentation and policy label it as `LEGACY_NANO`.

**Rationale**: Existing code and tests rely on Nano. The strategic target is BYOM, not deleting the current provider prematurely.
