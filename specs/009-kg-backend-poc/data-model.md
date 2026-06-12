# Data Model: KG Backend POC

## GraphEntity

Canonical local node.

Fields:

- `id: String`
- `userId: String`
- `type: GraphEntityType`
- `canonicalName: String`
- `normalizedName: String`
- `description: String?`
- `createdAtMillis: Long`
- `updatedAtMillis: Long`
- `invalidatedAtMillis: Long?`
- `invalidatedReason: String?`

Types:

- `USER`
- `PERSON`
- `ORGANIZATION`
- `PROJECT`
- `PLACE`
- `EVENT`
- `PRODUCT`
- `TOPIC`
- `TASK`

## GraphMention

Entity appearance in an episode.

Fields:

- `id: String`
- `entityId: String`
- `sourceType: GraphSourceType`
- `sourceId: String`
- `excerptDigest: String?`
- `label: String?`
- `createdAtMillis: Long`

## GraphFact

Typed claim about an entity or user.

Fields:

- `id: String`
- `subjectEntityId: String`
- `predicate: String`
- `objectText: String?`
- `objectEntityId: String?`
- `confidence: Float`
- `status: GraphStatus`
- `createdAtMillis: Long`
- `updatedAtMillis: Long`
- `invalidatedAtMillis: Long?`
- `invalidatedReason: String?`

Invariant:

- Active facts require at least one surviving `GraphProvenance` row.

## GraphRelationship

Typed edge between entities/facts.

Fields:

- `id: String`
- `fromEntityId: String`
- `toEntityId: String`
- `relationshipType: String`
- `confidence: Float`
- `status: GraphStatus`
- `createdAtMillis: Long`
- `updatedAtMillis: Long`
- `invalidatedAtMillis: Long?`
- `invalidatedReason: String?`

Invariant:

- Active relationships require at least one surviving `GraphProvenance` row.

## GraphProvenance

Support row connecting a graph object to source episodes.

Fields:

- `id: String`
- `targetType: GraphTargetType`
- `targetId: String`
- `sourceType: GraphSourceType`
- `sourceId: String`
- `supportKind: GraphSupportKind`
- `createdAtMillis: Long`
- `invalidatedAtMillis: Long?`
- `invalidatedReason: String?`

Source types:

- `ENVELOPE`
- `USER_CONFIRMATION`
- `USER_CORRECTION`
- `ACTION_DRAFT`
- `ASK_ANSWER`
- `INTEGRATION_READ`

## GraphFeedback

User correction/rejection/merge signal.

Fields:

- `id: String`
- `targetType: GraphTargetType`
- `targetId: String`
- `feedbackType: GraphFeedbackType`
- `replacementText: String?`
- `replacementEntityId: String?`
- `sourceType: GraphSourceType`
- `sourceId: String`
- `createdAtMillis: Long`

## WhyThisProjection

Compact Binder-safe projection.

Fields:

- `targetType: GraphTargetType`
- `targetId: String`
- `title: String`
- `summary: String?`
- `sources: List<WhyThisSource>`

`WhyThisSource`:

- `sourceType: GraphSourceType`
- `sourceId: String`
- `label: String`
- `dayLocal: String?`
- `createdAtMillis: Long?`

## Adapter Contract

`GraphBackendAdapter` exposes:

- `upsertEntity(entity)`
- `writeFact(fact, provenance)`
- `writeRelationship(relationship, provenance)`
- `invalidateBySource(sourceType, sourceId)`
- `whyThis(targetType, targetId)`
- `exportUserGraph(userId)`

Contract invariants:

- No active fact/relationship without provenance.
- All reads are user-scoped.
- Invalidated provenance removes support from active projections.
- Local Room adapter is canonical; external adapters are candidates only.
