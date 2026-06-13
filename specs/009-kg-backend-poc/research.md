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

## Adapter Evaluation Verdicts

### Room/SQLCipher Baseline

**Verdict**: Accepted as the Spec 009 canonical backend.

**Evidence**:

- Room v10 graph tables are exported in `app/schemas/com.orbit.app.data.OrbitDatabase/10.json`.
- `RoomGraphBackendAdapter` implements provenance-required fact and relationship writes.
- `invalidateBySource` invalidates targets only when all provenance is gone and preserves targets with surviving support.
- `GraphRepositoryDelegate` projects only promoted memories; pending/rejected candidates remain outside active graph facts.
- `IEnvelopeRepository.getGraphWhyThis(targetType, targetId)` exposes a compact Binder projection without raw screenshots, OCR, prompts, embeddings, model responses, or cloud payloads.

**Tradeoff**: The first projection uses the current local-user bridge because Spec 007 promoted memories do not yet carry durable user ids. That is acceptable for this single-user local MVP branch, but a future auth/user-identity spec should thread the actual user id into promoted memory and graph projection.

### MongoDB Atlas / Compact Cloud Mirror

**Verdict**: Deferred. Do not implement a graph mirror in Spec 009.

**Reason**: Spec 008 cloud controls now distinguish compact index sync, cloud Ask synthesis, and cloud AI routing, but there is no graph-mirror control, receipt shape, or user-facing cloud activity row for KG mirrors yet. Any mirror must be compact, opt-in, bounded, and non-authoritative.

### Graphiti

**Verdict**: Deferred adapter candidate only.

**Reason**: Graphiti may be useful for temporal/entity graph experiments, but it cannot become canonical storage unless it passes Orbit's local-first adapter contract, provenance invalidation, deletion behavior, and no-raw-export rules. No production wiring in this branch.

### Zep / Mem0

**Verdict**: Deferred adapter candidates only.

**Reason**: These products optimize hosted memory workflows. Orbit's current requirement is private local graph memory in `:ml`; hosted memory products can only be evaluated as optional compact mirrors or test adapters after user controls and contract tests exist.

### Supabase Graph/Postgres

**Verdict**: Deferred.

**Reason**: Supabase remains useful for authenticated gateways and future cloud control surfaces, but a remote graph store would violate the current branch scope unless it is only a compact, opt-in mirror governed by Spec 008-style controls.

## Open Questions For Implementation

- Which UI should first expose `getGraphWhyThis`: Memory Review detail, Orbit tab, or a future agent workbench.
- Whether graph-specific audit rows are needed once a user-visible graph surface lands. Current branch relies on existing memory accepted/rejected audit rows and local provenance tables.
- When auth/user identity should replace the current `GraphRepositoryDelegate.LOCAL_USER_ID` bridge.

## Non-Goals

- Autonomous agent planning.
- AppFunctions/Spark/platform-agent sharing.
- BYOC/BYOK graph storage.
- External graph vendor production integration.
- LLM entity extraction as a hard dependency.
- Generative UI/A2UI.
