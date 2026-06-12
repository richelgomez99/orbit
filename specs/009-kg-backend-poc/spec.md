# Feature Specification: KG Backend POC

**Feature Branch**: `feature/009-kg-backend-poc-20260612`
**Created**: 2026-06-12
**Status**: Draft
**Input**: Rebaselined Spec 009 placeholder, current Spec 005A-008 outputs, May 22 Orbit vision, and local-first constitution.

## User Scenarios & Testing

### User Story 1 - I Can See Why Orbit Believes A Memory Fact (Priority: P1)

As an Orbit user, I need every entity, fact, or relationship Orbit stores about me to cite the captures or explicit confirmations that produced it, so the graph is inspectable memory rather than hidden profiling.

**Why this priority**: The KG is only trustworthy if it can answer "why do you think this?" from local evidence. This must land before agent planning or curious profiling.

**Independent Test**: Create a graph fact from a promoted memory candidate with one support envelope; verify the fact cannot be written without provenance and its projection includes the source envelope id.

**Acceptance Scenarios**:

1. **Given** a promoted memory with source support, **When** it is projected into KG storage, **Then** the resulting fact stores source provenance and can render a "why this?" evidence list.
2. **Given** a fact write with no source envelope, user confirmation, or correction episode, **When** the repository validates it, **Then** the write is rejected and audited.
3. **Given** a user opens a KG-backed profile fact, **When** they ask why it exists, **Then** Orbit shows the original capture/user confirmation path without raw cloud data.

---

### User Story 2 - Deletion And Correction Propagate Through Derived Graph Memory (Priority: P2)

As an Orbit user, I need deleting or correcting a source capture to invalidate dependent graph facts when they lose all surviving provenance, so the KG does not preserve stale private derivatives.

**Why this priority**: The May 13 reorg explicitly says not to start KG persistence before deletion, invalidation, and provenance rules are defined.

**Independent Test**: Create a fact with one source envelope, soft-delete that envelope, run invalidation, and verify the fact becomes invalidated rather than remaining active.

**Acceptance Scenarios**:

1. **Given** a graph fact has one provenance source, **When** that source is deleted, **Then** the fact is marked invalidated with reason `lost_provenance`.
2. **Given** a graph fact has two provenance sources, **When** one source is deleted, **Then** the fact remains active and records the reduced support set.
3. **Given** a user rejects/corrects a graph fact, **When** similar future candidates are considered, **Then** the correction is available as negative provenance signal.

---

### User Story 3 - The KG Backend Is Behind An Adapter, Not A Product Commitment (Priority: P3)

As the Orbit builder, I need a small adapter contract that can compare a local Room baseline against future Graphiti/Zep/Mem0/Supabase candidates without changing product semantics, so backend experiments cannot leak into UX or privacy guarantees.

**Why this priority**: Spec 009 is a backend POC, but the product requires local-first semantics. Adapters must prove they obey provenance, tenant isolation, deletion, export, and invalidation before becoming real storage.

**Independent Test**: Run the same repository contract tests against the Room adapter and a fake external adapter; both must reject no-provenance writes and respect tenant/user scoping.

**Acceptance Scenarios**:

1. **Given** a graph adapter implementation, **When** contract tests write entities/facts/edges, **Then** it must preserve local provenance ids exactly.
2. **Given** a graph adapter implementation, **When** a user-scoped query runs, **Then** it returns only the requesting user's graph rows.
3. **Given** an adapter cannot support deletion/invalidation/export semantics, **When** evaluated, **Then** it remains a rejected candidate and is not wired into production.

---

### User Story 4 - Compact Graph Mirror Never Becomes Authoritative (Priority: P4)

As an Orbit user, I need any cloud graph/index mirror to be compact, optional, and subordinate to local Room, so cloud helps retrieval without becoming the memory owner.

**Why this priority**: Spec 008 gave cloud controls. Spec 009 must preserve those boundaries before any KG mirror is considered.

**Independent Test**: Disable compact indexing/cloud controls and verify local KG facts still exist and can be inspected; no cloud adapter write is required for local graph behavior.

**Acceptance Scenarios**:

1. **Given** all cloud controls are disabled, **When** KG facts are created locally, **Then** local graph inspection and provenance still work.
2. **Given** a compact graph mirror payload is built, **When** payload caps inspect it, **Then** raw screenshots, full OCR, raw prompts, model responses, and unsupported candidate text are excluded.
3. **Given** Atlas has a stale mirror row, **When** local Room invalidates the fact, **Then** UI trusts local invalidation over remote mirror state.

### Edge Cases

- A memory candidate is rejected: it must not become a graph fact.
- A promoted memory loses all support envelopes: dependent facts must invalidate.
- Duplicate entities are detected later: merge must preserve provenance from both entities.
- An inferred relationship conflicts with an explicit user correction: correction wins and future similar inferences are down-weighted.
- Cloud index disabled: local KG still works; remote mirror writes are skipped.
- No phone/device available: repository contracts and migration tests must still compile/pass locally.
- External adapter produces cross-user rows: contract tests fail and the adapter is rejected.

## Requirements

### Functional Requirements

- **FR-009-001**: System MUST define local KG entities, mentions, facts, relationships, and provenance support before any external adapter is production-wired.
- **FR-009-002**: System MUST reject KG facts and relationships that lack at least one source episode.
- **FR-009-003**: Source episodes MUST include local envelope ids, explicit user confirmations/corrections, or future integration-read ids; cloud-only evidence is not sufficient.
- **FR-009-004**: System MUST store KG canonical state in local Room/SQLCipher in the `:ml` process.
- **FR-009-005**: UI/default process MUST access KG projections through existing Binder boundaries, not direct Room.
- **FR-009-006**: System MUST support invalidating facts/edges when all surviving provenance is deleted.
- **FR-009-007**: System MUST preserve facts/edges when at least one surviving provenance source remains.
- **FR-009-008**: System MUST represent user corrections/rejections as graph feedback with provenance, not silent overwrites.
- **FR-009-009**: System MUST expose a compact "why this?" projection for facts/edges with source envelope ids and short evidence labels.
- **FR-009-010**: System MUST define a `GraphBackendAdapter` contract for Room baseline and future external candidates.
- **FR-009-011**: Adapter contract tests MUST cover provenance rejection, tenant isolation, deletion/invalidation, export shape, and no raw-content cloud payloads.
- **FR-009-012**: Atlas/cloud graph mirrors, if introduced, MUST be optional compact mirrors controlled by Spec 008 preferences and never authoritative.
- **FR-009-013**: System MUST NOT introduce AppFunctions/Spark/platform-agent sharing in this branch.
- **FR-009-014**: System MUST NOT create autonomous agent plans or actions in this branch.
- **FR-009-015**: System MUST NOT store rejected/pending memory candidates as active KG facts.

### Key Entities

- **GraphEntity**: Canonical local node for a person, organization, project, place, event, product, topic, task, or user-profile subject.
- **GraphMention**: Evidence that an entity appears in an envelope, note, Ask answer, action draft, or user confirmation.
- **GraphFact**: A typed claim about an entity or the user, always provenance-backed and editable.
- **GraphRelationship**: A typed edge between entities/facts, always provenance-backed and invalidatable.
- **GraphProvenance**: Junction/support row connecting facts/edges/entities to source episodes.
- **GraphFeedback**: User rejection, correction, merge, or confidence adjustment with provenance.
- **GraphBackendAdapter**: Storage/query boundary for local Room baseline and future candidate backends.

## Success Criteria

- **SC-009-001**: No-provenance fact and edge writes fail in unit tests.
- **SC-009-002**: Deleting the only source envelope invalidates dependent facts/edges in repository tests.
- **SC-009-003**: Deleting one of multiple sources preserves active facts/edges with surviving support.
- **SC-009-004**: Pending/rejected memory candidates are excluded from active KG projections.
- **SC-009-005**: `why this?` projection returns source envelope/user-confirmation ids for every active fact/edge.
- **SC-009-006**: Adapter contract tests pass for the local Room baseline and a fake adapter.
- **SC-009-007**: Full non-phone gate passes: `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:build-logic:lint:test`, `:app:lintDebug`, `:app:assembleDebug`, `:app:compileDebugAndroidTestKotlin`.

## Assumptions

- Spec 007 memory candidates/promoted memories are available as an input, but pending/rejected candidates are not facts.
- Spec 008 cloud controls are available and govern any compact graph mirror.
- The first implementation should prefer a local Room baseline adapter; external graph products are evaluation targets only.
- Graph extraction can start from deterministic/promoted memory data; LLM/model extraction can be layered later.
- Connected/manual validation can be deferred while phone access is unavailable.
