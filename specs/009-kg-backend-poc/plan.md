# Implementation Plan: KG Backend POC

**Branch**: `feature/009-kg-backend-poc-20260612` | **Date**: 2026-06-12 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/009-kg-backend-poc/spec.md`

## Summary

Spec 009 introduces Orbit's local-first knowledge graph foundation behind an adapter contract. The first implementation should prove canonical Room/SQLCipher graph storage, provenance-required writes, deletion/correction invalidation, and "why this?" projections. External graph products remain adapter candidates until they pass the same contract.

## Technical Context

**Language/Version**: Kotlin, Android Room/SQLCipher  
**Primary Dependencies**: Room, AIDL Binder, existing Spec 007 memory repository, Spec 008 cloud controls  
**Storage**: Local Room/SQLCipher canonical graph tables in `:ml`; optional compact mirrors deferred  
**Testing**: JVM repository/unit tests, migration/androidTest source, custom lint, Android lint, assemble  
**Target Platform**: Android app with four-process architecture  
**Project Type**: Mobile app local storage/backend adapter foundation  
**Performance Goals**: Write/why-this repository operations under local UI latency budgets; no Binder payload bloat  
**Constraints**: Local source of truth, no no-provenance facts, no raw cloud graph payloads, no direct Room from UI/default process  
**Scale/Scope**: Local KG baseline, adapter contract, focused UI/Binder projections only

## Constitution Check

- **Principle I - Local-First Supremacy**: Pass if Room is canonical and cloud mirrors are optional.
- **Principle III - Intent Before Artifact**: Pass if graph rows cite IntentEnvelopes/promoted memories rather than raw artifacts alone.
- **Principle VI - Privilege Separation**: Pass if graph DAOs live in `:ml` and UI uses Binder.
- **Principle VIII - Collect Only What You Use**: Pass if graph extraction starts from promoted/confirmed facts only.
- **Principle X - Storage Sovereignty**: Pass if graph rows carry local/cloud category and cloud mirrors are controlled by Spec 008.
- **Principle XII - Provenance**: Gate. No active fact/edge without source episode provenance.

## Project Structure

```text
specs/009-kg-backend-poc/
├── spec.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── graph-backend-adapter-contract.md
└── tasks.md

app/src/main/java/com/orbit/app/graph/
├── GraphModels.kt
├── GraphBackendAdapter.kt
├── RoomGraphBackendAdapter.kt
└── GraphRepositoryDelegate.kt

app/src/main/java/com/orbit/app/data/entity/
app/src/main/java/com/orbit/app/data/dao/
app/src/main/java/com/orbit/app/data/ipc/

app/src/test/java/com/orbit/app/graph/
app/src/androidTest/java/com/orbit/app/data/
```

**Structure Decision**: Add `com.orbit.app.graph` for domain/adapter logic, keep Room entities/DAOs in existing data packages, and expose compact projections through existing repository Binder patterns.

## Implementation Strategy

### Phase A - Contract And Models

Add pure graph enums/drafts/results and adapter contract tests with fake adapters before Room implementation.

### Phase B - Room Baseline

Add Room v10 graph tables, DAOs, migration, exported schema, and migration test source. Enforce no active fact/edge without provenance in repository code.

### Phase C - Provenance And Invalidation

Implement `writeFact`, `writeRelationship`, `invalidateBySource`, and support-preserving invalidation semantics.

### Phase D - Spec 007 Projection

Project promoted memory into KG facts only after acceptance. Pending/rejected candidates remain excluded.

### Phase E - Binder/Why This

Expose compact `whyThis` projections through `IEnvelopeRepository` or a narrow graph Binder surface, preserving payload caps.

### Phase F - Adapter Evaluation Notes

Document Room baseline status and explicitly reject/defer external adapters until contract tests can run against them.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| New Room tables | KG needs durable local canonical state with invalidation support | JSON blobs cannot enforce provenance, deletion, or relationship queries safely |
