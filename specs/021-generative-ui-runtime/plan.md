# Implementation Plan: Generative UI Runtime

**Branch**: `feature/021-generative-ui-runtime-20260614` | **Date**: 2026-06-14 | **Spec**: `specs/021-generative-ui-runtime/spec.md`

## Summary

Add Orbit's first safe generative UI runtime by mapping existing `AgentPlanParcel` outputs into app-defined typed UI components, rendering them in Compose, and providing a deterministic text fallback. This validates the "chat becomes native UI" direction without adding external A2UI dependencies, raw model JSON, or new persistence.

## Technical Context

**Language/Version**: Kotlin, Android Gradle project, JDK 21  
**Primary Dependencies**: Jetpack Compose, Material3, Kotlin/JVM tests  
**Storage**: No schema change  
**Testing**: JVM tests, compile/lint/full non-phone gate  
**Target Platform**: Android  
**Project Type**: Mobile app  
**Constraints**: No arbitrary UI/style execution; no new Binder/Room; no action execution bypass

## Constitution Check

- **Principle I Local-First Supremacy**: PASS. No new cloud dependency.
- **Principle III Intent Before Artifact**: PASS. Agent UI is grounded in plan/evidence intent.
- **Principle VI Privilege Separation**: PASS. Uses existing Binder parcels; no Room/network access.
- **Principle VIII Collect Only What You Use**: PASS. Presentation-only; no new collection.
- **Principle XII Provenance**: PASS if evidence refs are preserved in rich UI and fallback text.

## Project Structure

```text
specs/021-generative-ui-runtime/
├── spec.md
├── research.md
├── data-model.md
├── contracts/
│   └── generative-ui-runtime-contract.md
├── plan.md
├── quickstart.md
└── tasks.md

app/src/main/java/com/orbit/app/generativeui/
app/src/test/java/com/orbit/app/generativeui/
app/src/main/java/com/orbit/app/diary/ui/OrbitCleanupScreen.kt
```

## Implementation Approach

1. Add typed document/component models and safety caps.
2. Add `AgentPlanParcel` adapter and deterministic fallback renderer.
3. Add Compose renderer for the initial component set.
4. Replace the current ad hoc agent-plan result rendering with the typed renderer.
5. Add focused JVM tests for adapter/fallback guarantees.
6. Run focused and full non-phone gates.
