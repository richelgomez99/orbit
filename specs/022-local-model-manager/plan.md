# Implementation Plan: Local Model Manager

**Branch**: `feature/022-local-model-manager-20260614` | **Date**: 2026-06-14 | **Spec**: `specs/022-local-model-manager/spec.md`

## Summary

Add the strategic local-model-manager foundation as pure capability and selection policy. Preserve current cloud/Nano behavior while creating a tested seam for future LiteRT-LM/MLC providers.

## Technical Context

**Language/Version**: Kotlin, Android Gradle project, JDK 21  
**Primary Dependencies**: Existing `LlmProvider` abstraction  
**Storage**: No schema change  
**Testing**: JVM tests, router tests, compile/lint/full non-phone gate  
**Target Platform**: Android  
**Project Type**: Mobile app  
**Constraints**: No native engine import; no network outside `:net`; no production default change

## Constitution Check

- **Principle I Local-First Supremacy**: PASS. Adds a real local-first routing foundation.
- **Principle VI Privilege Separation**: PASS if providers stay behind `LlmProvider` and `:ml`.
- **Principle VIII Collect Only What You Use**: PASS. Capability metadata only.
- **Principle IX User-Sovereign Cloud Escape Hatch**: PASS if disabled-cloud paths fail closed.

## Project Structure

```text
specs/022-local-model-manager/
├── spec.md
├── research.md
├── data-model.md
├── contracts/
│   └── local-model-manager-contract.md
├── plan.md
├── quickstart.md
└── tasks.md

app/src/main/java/com/orbit/app/ai/local/
app/src/test/java/com/orbit/app/ai/local/
app/src/main/java/com/orbit/app/ai/LlmProviderRouter.kt
app/src/test/java/com/orbit/app/ai/LlmProviderRouterTest.kt
```

## Implementation Approach

1. Add pure local model domain models and selection policy.
2. Add JVM tests for tier/capability/cloud-disabled behavior.
3. Add an optional BYOM local provider seam to `LlmProviderRouter.resolve` without changing production `create`.
4. Update router tests for BYOM/local unavailable outcomes.
5. Run focused and full non-phone gates.
