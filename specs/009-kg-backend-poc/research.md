# Research: KG Backend POC

**Date**: 2026-06-12  
**Branch**: `feature/009-kg-backend-poc-20260612`

## Inputs Reviewed

- `VISION-2026-05-22.md`
- `docs/mvp-to-vision-execution-plan-2026-06-02.md`
- `docs/orbit-roadmap-queue-2026-06-02.md`
- `docs/product-roadmap-audit-2026-05-12.md`
- `docs/spec-branch-reorganization-plan-2026-05-13.md`
- `.specify/memory/constitution.md`
- Current Spec 007/008 outputs and branch commits.

## Current State

- Spec 007 provides candidate/promoted memory review, support/provenance junctions, audit rows, and explicit exclusion of pending/rejected candidates from Ask/action/cloud fact surfaces.
- Spec 008 provides cloud controls and local bounded receipts for compact indexing, cloud Ask synthesis, and cloud AI routing.
- No canonical KG tables exist yet.
- Atlas currently holds compact retrieval/search records only. It is not source of truth.

## Decisions

### D-009-001: Room Baseline First

**Decision**: Implement the first KG backend as local Room/SQLCipher tables behind an adapter contract.

**Rationale**: Local-first semantics, deletion propagation, and Binder boundaries are easier to prove with the existing storage owner in `:ml`.

**Rejected**: Starting with Graphiti/Zep/Mem0/remote graph storage. Those can be evaluated later only if they pass the same adapter contract.

### D-009-002: Provenance Is A Structural Write Requirement

**Decision**: Facts and relationships cannot be inserted active unless at least one source episode is attached in the same logical write.

**Rationale**: Constitution Principle XII says facts without provenance are structurally rejected.

**Rejected**: Allowing uncited facts and backfilling provenance later. That creates an untrustworthy graph and deletion bugs.

### D-009-003: Memory Candidates Feed KG Only After Promotion

**Decision**: Pending/rejected memory candidates are graph inputs only for review context. Active KG facts can be created from promoted memories, explicit confirmations, or future approved agent outputs.

**Rationale**: Spec 007 intentionally prevents silent profile facts.

**Rejected**: Treating all candidates as low-confidence graph facts. That would turn review suggestions into hidden profiling.

### D-009-004: Cloud Mirrors Are Optional Compact Projections

**Decision**: Any cloud/Atlas graph mirror must be a compact projection governed by Spec 008 controls. Local Room remains authoritative.

**Rationale**: Spec 008 was added specifically so deeper cloud/agent work has user-sovereign controls.

**Rejected**: Remote KG as default memory store or backup.

### D-009-005: Adapter Contract Evaluates Products, Not Product Semantics

**Decision**: Graphiti/Zep/Mem0/Supabase are adapter candidates only. They must conform to Orbit semantics rather than reshape Orbit around their model.

**Rationale**: The product promise is local-first provenance-backed memory, not use of a specific graph vendor.

## Open Questions For Implementation

- Which first entity taxonomy is narrow enough for MVP: `PERSON`, `ORG`, `PROJECT`, `PLACE`, `EVENT`, `PRODUCT`, `TOPIC`, `TASK`, `USER` is the likely starting set.
- Whether promoted-memory facts should write synchronously on accept or via a deterministic projection worker.
- Whether `GraphFact` should support confidence at v1 or only active/invalidated plus source count. Recommendation: include confidence but keep decisions deterministic.
- Whether UI for "why this?" belongs initially in Memory Review detail, Orbit tab, or a small repository projection test only. Recommendation: start repository/Binder projection first.

## Non-Goals

- Autonomous agent planning.
- AppFunctions/Spark/platform-agent sharing.
- BYOC/BYOK graph storage.
- External graph vendor production integration.
- LLM entity extraction as a hard dependency.
- Generative UI/A2UI.
