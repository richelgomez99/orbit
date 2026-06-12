# Contract: Graph Backend Adapter

## Scope

Spec 009 defines the storage/query boundary for local KG memory. This is a local contract first; it does not authorize remote graph storage.

## Kotlin Contract Sketch

```kotlin
interface GraphBackendAdapter {
    suspend fun upsertEntity(entity: GraphEntityDraft): GraphWriteResult

    suspend fun writeFact(
        fact: GraphFactDraft,
        provenance: List<GraphProvenanceDraft>,
    ): GraphWriteResult

    suspend fun writeRelationship(
        relationship: GraphRelationshipDraft,
        provenance: List<GraphProvenanceDraft>,
    ): GraphWriteResult

    suspend fun invalidateBySource(
        sourceType: GraphSourceType,
        sourceId: String,
        reason: String,
    ): GraphInvalidationResult

    suspend fun whyThis(
        targetType: GraphTargetType,
        targetId: String,
    ): WhyThisProjection?

    suspend fun exportUserGraph(userId: String): GraphExport
}
```

## Required Behaviors

### Provenance

- `writeFact` and `writeRelationship` MUST reject empty provenance lists.
- Every provenance row MUST reference a local source episode id.
- Cloud-only ids are not acceptable source episodes.

### Deletion/Invalidation

- `invalidateBySource` marks matching provenance invalidated.
- If a fact/relationship loses all surviving provenance, it becomes invalidated.
- If surviving provenance remains, the fact/relationship stays active.

### Tenant/User Isolation

- Every adapter read/write must be scoped by user id or local single-user source.
- Contract tests must simulate two users and reject cross-user reads.

### Export

- Export must include entities, facts, relationships, provenance, feedback, statuses, and invalidation reasons.
- Export must not include raw screenshots, full OCR, prompts, embeddings, model responses, access tokens, or API keys.

### Adapter Candidate Evaluation

External candidates must pass the same contract as Room:

- No-provenance rejection.
- Invalidation by source.
- Surviving support preservation.
- User scoping.
- Export shape.
- Compact/no-raw cloud payload behavior.

Failing candidates remain research notes and must not be wired into production.
