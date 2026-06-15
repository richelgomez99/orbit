# Implementation Plan: Agent Workspace IA

**Branch**: `feature/019-agent-workspace-ia-20260614` | **Date**: 2026-06-14 | **Spec**: `specs/019-agent-workspace-ia/spec.md`

## Summary

Formalize the current three-pillar IA by hardening tab-exit reset behavior. The branch keeps the existing `DiaryActivity` shell and adds Library reset semantics plus tests.

## Technical Context

**Language/Version**: Kotlin, Android Gradle project, JDK 21  
**Primary Dependencies**: Jetpack Compose, ViewModel, Kotlin coroutines  
**Storage**: No schema change  
**Testing**: JVM unit tests, compile/lint/full non-phone gate  
**Target Platform**: Android  
**Project Type**: Mobile app  
**Constraints**: Do not clear durable repository-backed data on tab switch  

## Constitution Check

- **Principle I Local-First Supremacy**: PASS. No cloud dependency.
- **Principle III Intent Before Artifact**: PASS. IA separates memory, retrieval, and action.
- **Principle VI Privilege Separation**: PASS. UI state only; no process-boundary changes.
- **Principle VIII Collect Only What You Use**: PASS. No new collection.

## Project Structure

```text
specs/019-agent-workspace-ia/
├── spec.md
├── research.md
├── data-model.md
├── contracts/
│   └── agent-workspace-ia-contract.md
├── plan.md
├── quickstart.md
└── tasks.md

app/src/main/java/com/orbit/app/diary/DiaryActivity.kt
app/src/main/java/com/orbit/app/library/LibraryViewModel.kt
app/src/test/java/com/orbit/app/library/LibraryViewModelTest.kt
```

## Implementation Approach

1. Add `LibraryViewModel.reset()`.
2. Call Library reset when leaving the Library tab.
3. Keep existing Ask reset when leaving Orbit.
4. Add JVM coverage for Library reset and source-level shell reset contract.
5. Run focused and full gates.
