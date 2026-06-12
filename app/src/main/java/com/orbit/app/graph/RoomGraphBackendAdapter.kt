package com.orbit.app.graph

import androidx.room.withTransaction
import com.orbit.app.data.OrbitDatabase
import com.orbit.app.data.entity.GraphEntityEntity
import com.orbit.app.data.entity.GraphFactEntity
import com.orbit.app.data.entity.GraphFeedbackEntity
import com.orbit.app.data.entity.GraphProvenanceEntity
import com.orbit.app.data.entity.GraphRelationshipEntity

class RoomGraphBackendAdapter(
    private val database: OrbitDatabase,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : GraphBackendAdapter {

    private val graphDao = database.graphDao()

    override suspend fun upsertEntity(entity: GraphEntityDraft): GraphWriteResult {
        if (entity.id.isBlank() || entity.userId.isBlank() || entity.canonicalName.isBlank()) {
            return GraphWriteResult.Rejected("invalid_entity")
        }
        val now = clock()
        graphDao.upsertEntity(
            GraphEntityEntity(
                id = entity.id,
                userId = entity.userId,
                type = entity.type,
                canonicalName = entity.canonicalName,
                normalizedName = entity.normalizedName.ifBlank {
                    entity.canonicalName.trim().lowercase()
                },
                description = entity.description,
                status = GraphStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
                invalidatedAt = null,
                invalidatedReason = null,
            )
        )
        return GraphWriteResult.Written(entity.id)
    }

    override suspend fun writeFact(
        fact: GraphFactDraft,
        provenance: List<GraphProvenanceDraft>,
    ): GraphWriteResult {
        validateProvenance(fact.userId, GraphTargetType.FACT, fact.id, provenance)?.let {
            return it
        }
        if (fact.id.isBlank() || fact.userId.isBlank() || fact.subjectEntityId.isBlank()) {
            return GraphWriteResult.Rejected("invalid_fact")
        }
        val now = clock()
        database.withTransaction {
            graphDao.insertFact(
                GraphFactEntity(
                    id = fact.id,
                    userId = fact.userId,
                    subjectEntityId = fact.subjectEntityId,
                    predicate = fact.predicate,
                    objectText = fact.objectText,
                    objectEntityId = fact.objectEntityId,
                    confidence = fact.confidence,
                    status = GraphStatus.ACTIVE,
                    createdAt = now,
                    updatedAt = now,
                    invalidatedAt = null,
                    invalidatedReason = null,
                )
            )
            provenance.forEach { graphDao.insertProvenance(it.toEntity(now)) }
        }
        return GraphWriteResult.Written(fact.id)
    }

    override suspend fun writeRelationship(
        relationship: GraphRelationshipDraft,
        provenance: List<GraphProvenanceDraft>,
    ): GraphWriteResult {
        validateProvenance(
            relationship.userId,
            GraphTargetType.RELATIONSHIP,
            relationship.id,
            provenance,
        )?.let {
            return it
        }
        if (
            relationship.id.isBlank() ||
            relationship.userId.isBlank() ||
            relationship.fromEntityId.isBlank() ||
            relationship.toEntityId.isBlank()
        ) {
            return GraphWriteResult.Rejected("invalid_relationship")
        }
        val now = clock()
        database.withTransaction {
            graphDao.insertRelationship(
                GraphRelationshipEntity(
                    id = relationship.id,
                    userId = relationship.userId,
                    fromEntityId = relationship.fromEntityId,
                    toEntityId = relationship.toEntityId,
                    relationshipType = relationship.relationshipType,
                    confidence = relationship.confidence,
                    status = GraphStatus.ACTIVE,
                    createdAt = now,
                    updatedAt = now,
                    invalidatedAt = null,
                    invalidatedReason = null,
                )
            )
            provenance.forEach { graphDao.insertProvenance(it.toEntity(now)) }
        }
        return GraphWriteResult.Written(relationship.id)
    }

    override suspend fun writeFeedback(feedback: GraphFeedbackDraft): GraphWriteResult {
        if (feedback.id.isBlank() || feedback.userId.isBlank() || feedback.sourceId.isBlank()) {
            return GraphWriteResult.Rejected("missing_source_episode")
        }
        graphDao.insertFeedback(
            GraphFeedbackEntity(
                id = feedback.id,
                userId = feedback.userId,
                targetType = feedback.targetType,
                targetId = feedback.targetId,
                feedbackType = feedback.feedbackType,
                replacementText = feedback.replacementText,
                replacementEntityId = feedback.replacementEntityId,
                sourceType = feedback.sourceType,
                sourceId = feedback.sourceId,
                createdAt = clock(),
            )
        )
        return GraphWriteResult.Written(feedback.id)
    }

    override suspend fun invalidateBySource(
        sourceType: GraphSourceType,
        sourceId: String,
        reason: String,
    ): GraphInvalidationResult {
        if (sourceId.isBlank()) {
            return GraphInvalidationResult(emptySet(), emptySet())
        }
        val now = clock()
        val factIds = graphDao.targetIdsForSource(GraphTargetType.FACT, sourceType, sourceId)
        val relationshipIds = graphDao.targetIdsForSource(
            GraphTargetType.RELATIONSHIP,
            sourceType,
            sourceId,
        )

        val invalidated = linkedSetOf<String>()
        val preserved = linkedSetOf<String>()
        database.withTransaction {
            graphDao.invalidateProvenanceBySource(sourceType, sourceId, reason, now)
            factIds.forEach { factId ->
                val fact = graphDao.getFactById(factId) ?: return@forEach
                if (graphDao.activeProvenanceCount(fact.userId, GraphTargetType.FACT, factId) == 0) {
                    graphDao.invalidateFact(fact.userId, factId, reason, now)
                    invalidated += factId
                } else {
                    preserved += factId
                }
            }
            relationshipIds.forEach { relationshipId ->
                val relationship = graphDao.getRelationshipById(relationshipId) ?: return@forEach
                if (
                    graphDao.activeProvenanceCount(
                        relationship.userId,
                        GraphTargetType.RELATIONSHIP,
                        relationshipId,
                    ) == 0
                ) {
                    graphDao.invalidateRelationship(relationship.userId, relationshipId, reason, now)
                    invalidated += relationshipId
                } else {
                    preserved += relationshipId
                }
            }
        }
        return GraphInvalidationResult(
            invalidatedTargetIds = invalidated,
            preservedTargetIds = preserved,
        )
    }

    override suspend fun whyThis(
        userId: String,
        targetType: GraphTargetType,
        targetId: String,
    ): WhyThisProjection? {
        val sources = graphDao.activeProvenanceForTarget(userId, targetType, targetId)
        val targetExists = when (targetType) {
            GraphTargetType.ENTITY -> graphDao.getEntity(userId, targetId) != null
            GraphTargetType.FACT -> graphDao.getFact(userId, targetId)?.status == GraphStatus.ACTIVE
            GraphTargetType.RELATIONSHIP -> {
                graphDao.getRelationship(userId, targetId)?.status == GraphStatus.ACTIVE
            }
        }
        if (!targetExists) return null
        if (targetType != GraphTargetType.ENTITY && sources.isEmpty()) return null

        return WhyThisProjection(
            targetType = targetType,
            targetId = targetId,
            title = targetId,
            summary = null,
            sources = sources.map {
                WhyThisSource(
                    sourceType = it.sourceType,
                    sourceId = it.sourceId,
                    label = "${it.sourceType.name.lowercase()}:${it.sourceId}",
                )
            },
        )
    }

    override suspend fun exportUserGraph(userId: String): GraphExport = GraphExport(
        userId = userId,
        entities = graphDao.exportEntities(userId).map {
            GraphEntityDraft(
                id = it.id,
                userId = it.userId,
                type = it.type,
                canonicalName = it.canonicalName,
                normalizedName = it.normalizedName,
                description = it.description,
            )
        },
        facts = graphDao.exportFacts(userId).map {
            GraphFactDraft(
                id = it.id,
                userId = it.userId,
                subjectEntityId = it.subjectEntityId,
                predicate = it.predicate,
                objectText = it.objectText,
                objectEntityId = it.objectEntityId,
                confidence = it.confidence,
            )
        },
        relationships = graphDao.exportRelationships(userId).map {
            GraphRelationshipDraft(
                id = it.id,
                userId = it.userId,
                fromEntityId = it.fromEntityId,
                toEntityId = it.toEntityId,
                relationshipType = it.relationshipType,
                confidence = it.confidence,
            )
        },
        provenance = graphDao.exportProvenance(userId).map {
            GraphProvenanceDraft(
                id = it.id,
                userId = it.userId,
                targetType = it.targetType,
                targetId = it.targetId,
                sourceType = it.sourceType,
                sourceId = it.sourceId,
                supportKind = it.supportKind,
            )
        },
        feedback = graphDao.exportFeedback(userId).map {
            GraphFeedbackDraft(
                id = it.id,
                userId = it.userId,
                targetType = it.targetType,
                targetId = it.targetId,
                feedbackType = it.feedbackType,
                replacementText = it.replacementText,
                replacementEntityId = it.replacementEntityId,
                sourceType = it.sourceType,
                sourceId = it.sourceId,
            )
        },
    )

    private fun validateProvenance(
        userId: String,
        targetType: GraphTargetType,
        targetId: String,
        provenance: List<GraphProvenanceDraft>,
    ): GraphWriteResult.Rejected? {
        if (provenance.isEmpty()) return GraphWriteResult.Rejected("missing_provenance")
        val invalid = provenance.any {
            it.id.isBlank() ||
                it.userId != userId ||
                it.targetType != targetType ||
                it.targetId != targetId ||
                it.sourceId.isBlank()
        }
        return if (invalid) GraphWriteResult.Rejected("invalid_provenance") else null
    }

    private fun GraphProvenanceDraft.toEntity(now: Long): GraphProvenanceEntity =
        GraphProvenanceEntity(
            id = id,
            userId = userId,
            targetType = targetType,
            targetId = targetId,
            sourceType = sourceType,
            sourceId = sourceId,
            supportKind = supportKind,
            createdAt = now,
            invalidatedAt = null,
            invalidatedReason = null,
        )
}
