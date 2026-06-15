# Implementation Plan: Curious Agent Profiling

**Branch**: `feature/020-curious-agent-profiling-20260614` | **Date**: 2026-06-14 | **Spec**: `specs/020-curious-agent-profiling/spec.md`

## Summary

Add a conservative Curious Agent foundation: deterministic, evidence-backed question candidates with dismiss/answer semantics. Do not silently infer profile facts.

## Technical Context

**Language/Version**: Kotlin, Android Gradle project, JDK 21  
**Primary Dependencies**: Room/Binder only if persistence is needed; otherwise pure domain plus existing Orbit UI  
**Storage**: Prefer pure/domain first; add Room only if answer/dismiss persistence is implemented in this branch  
**Testing**: JVM unit tests, compile/lint/full non-phone gate  
**Target Platform**: Android  
**Constraints**: Local-first, provenance-backed, no cloud/model requirement, no raw payloads  

## Constitution Check

- **Principle I Local-First Supremacy**: PASS.
- **Principle III Intent Before Artifact**: PASS. Questions ask why repeated saves matter.
- **Principle VI Privilege Separation**: PASS if persistence stays in `:ml`.
- **Principle VIII Collect Only What You Use**: PASS if questions are sparse and dismissible.
- **Principle XII Provenance Or It Didn't Happen**: PASS only if every question cites evidence.

## Project Structure

```text
specs/020-curious-agent-profiling/
├── spec.md
├── research.md
├── data-model.md
├── contracts/
│   └── curious-agent-contract.md
├── plan.md
├── quickstart.md
└── tasks.md

app/src/main/java/com/orbit/app/curious/
app/src/test/java/com/orbit/app/curious/
```

## Implementation Approach

1. Add pure curious-agent domain models and deterministic generator.
2. Add tests for insufficient evidence, capped output, no raw text, dismissed suppression, and source refs.
3. Decide whether persistence/UI belongs in this branch after pure generator passes.
4. Run focused and full gates before closing.
