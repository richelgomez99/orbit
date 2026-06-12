package com.orbit.app.graph

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphBackendAdapterContractTest {

    @Test
    fun factWritesRequireProvenance() = runTest {
        val adapter = InMemoryGraphBackendAdapter()
        adapter.upsertEntity(userEntity())

        val result = adapter.writeFact(profileFact(), provenance = emptyList())

        assertEquals(GraphWriteResult.Rejected("missing_provenance"), result)
    }

    @Test
    fun relationshipWritesRequireProvenance() = runTest {
        val adapter = InMemoryGraphBackendAdapter()
        adapter.upsertEntity(userEntity())
        adapter.upsertEntity(projectEntity())

        val result = adapter.writeRelationship(projectRelationship(), provenance = emptyList())

        assertEquals(GraphWriteResult.Rejected("missing_provenance"), result)
    }

    @Test
    fun whyThisReturnsSourceEvidenceForActiveFact() = runTest {
        val adapter = InMemoryGraphBackendAdapter()
        adapter.upsertEntity(userEntity())
        adapter.writeFact(profileFact(), provenance = listOf(factSource("support-1", "env-1")))

        val whyThis = adapter.whyThis(
            userId = USER_ID,
            targetType = GraphTargetType.FACT,
            targetId = "fact-founder",
        )

        assertEquals("fact-founder", whyThis!!.targetId)
        assertEquals(listOf("env-1"), whyThis.sources.map { it.sourceId })
        assertEquals(listOf(GraphSourceType.ENVELOPE), whyThis.sources.map { it.sourceType })
    }

    @Test
    fun invalidatingOnlySourceInvalidatesFact() = runTest {
        val adapter = InMemoryGraphBackendAdapter()
        adapter.upsertEntity(userEntity())
        adapter.writeFact(profileFact(), provenance = listOf(factSource("support-1", "env-1")))

        val result = adapter.invalidateBySource(GraphSourceType.ENVELOPE, "env-1", "lost_provenance")

        assertEquals(setOf("fact-founder"), result.invalidatedTargetIds)
        assertTrue(result.preservedTargetIds.isEmpty())
        assertNull(adapter.whyThis(USER_ID, GraphTargetType.FACT, "fact-founder"))
    }

    @Test
    fun invalidatingOneOfMultipleSourcesPreservesFact() = runTest {
        val adapter = InMemoryGraphBackendAdapter()
        adapter.upsertEntity(userEntity())
        adapter.writeFact(
            profileFact(),
            provenance = listOf(
                factSource("support-1", "env-1"),
                factSource("support-2", "env-2"),
            ),
        )

        val result = adapter.invalidateBySource(GraphSourceType.ENVELOPE, "env-1", "source_deleted")
        val whyThis = adapter.whyThis(USER_ID, GraphTargetType.FACT, "fact-founder")

        assertTrue(result.invalidatedTargetIds.isEmpty())
        assertEquals(setOf("fact-founder"), result.preservedTargetIds)
        assertEquals(listOf("env-2"), whyThis!!.sources.map { it.sourceId })
    }

    @Test
    fun whyThisIsUserScoped() = runTest {
        val adapter = InMemoryGraphBackendAdapter()
        adapter.upsertEntity(userEntity())
        adapter.writeFact(profileFact(), provenance = listOf(factSource("support-1", "env-1")))

        assertNull(adapter.whyThis("other-user", GraphTargetType.FACT, "fact-founder"))
    }

    @Test
    fun feedbackRequiresLocalSourceEpisode() = runTest {
        val adapter = InMemoryGraphBackendAdapter()

        val result = adapter.writeFeedback(
            GraphFeedbackDraft(
                id = "feedback-1",
                userId = USER_ID,
                targetType = GraphTargetType.FACT,
                targetId = "fact-founder",
                feedbackType = GraphFeedbackType.CORRECTED,
                replacementText = "I am not a founder yet.",
                sourceType = GraphSourceType.USER_CORRECTION,
                sourceId = "",
            )
        )

        assertEquals(GraphWriteResult.Rejected("missing_source_episode"), result)
    }

    private class InMemoryGraphBackendAdapter : GraphBackendAdapter {
        private val entities = linkedMapOf<String, GraphEntityDraft>()
        private val facts = linkedMapOf<String, GraphFactDraft>()
        private val relationships = linkedMapOf<String, GraphRelationshipDraft>()
        private val provenance = linkedMapOf<String, GraphProvenanceDraft>()
        private val invalidatedTargets = mutableSetOf<Pair<GraphTargetType, String>>()
        private val invalidatedProvenance = mutableSetOf<String>()
        private val feedbackRows = mutableListOf<GraphFeedbackDraft>()

        override suspend fun upsertEntity(entity: GraphEntityDraft): GraphWriteResult {
            if (entity.userId.isBlank() || entity.id.isBlank()) {
                return GraphWriteResult.Rejected("invalid_entity")
            }
            entities[entity.id] = entity
            return GraphWriteResult.Written(entity.id)
        }

        override suspend fun writeFact(
            fact: GraphFactDraft,
            provenance: List<GraphProvenanceDraft>,
        ): GraphWriteResult {
            val rejection = validateProvenance(fact.userId, GraphTargetType.FACT, fact.id, provenance)
            if (rejection != null) return rejection
            facts[fact.id] = fact
            provenance.forEach { this.provenance[it.id] = it }
            return GraphWriteResult.Written(fact.id)
        }

        override suspend fun writeRelationship(
            relationship: GraphRelationshipDraft,
            provenance: List<GraphProvenanceDraft>,
        ): GraphWriteResult {
            val rejection = validateProvenance(
                relationship.userId,
                GraphTargetType.RELATIONSHIP,
                relationship.id,
                provenance,
            )
            if (rejection != null) return rejection
            relationships[relationship.id] = relationship
            provenance.forEach { this.provenance[it.id] = it }
            return GraphWriteResult.Written(relationship.id)
        }

        override suspend fun writeFeedback(feedback: GraphFeedbackDraft): GraphWriteResult {
            if (feedback.sourceId.isBlank()) return GraphWriteResult.Rejected("missing_source_episode")
            feedbackRows += feedback
            return GraphWriteResult.Written(feedback.id)
        }

        override suspend fun invalidateBySource(
            sourceType: GraphSourceType,
            sourceId: String,
            reason: String,
        ): GraphInvalidationResult {
            provenance.values
                .filter { it.sourceType == sourceType && it.sourceId == sourceId }
                .forEach { invalidatedProvenance += it.id }

            val invalidated = mutableSetOf<String>()
            val preserved = mutableSetOf<String>()
            listOf(GraphTargetType.FACT, GraphTargetType.RELATIONSHIP).forEach { targetType ->
                val targetIds = provenance.values
                    .filter { it.targetType == targetType }
                    .map { it.targetId }
                    .toSet()
                targetIds.forEach { targetId ->
                    val support = provenance.values.filter { it.targetType == targetType && it.targetId == targetId }
                    val surviving = support.any { it.id !in invalidatedProvenance }
                    if (surviving) {
                        preserved += targetId
                    } else {
                        invalidated += targetId
                        invalidatedTargets += targetType to targetId
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
            if (targetType to targetId in invalidatedTargets) return null
            val ownerId = when (targetType) {
                GraphTargetType.FACT -> facts[targetId]?.userId
                GraphTargetType.RELATIONSHIP -> relationships[targetId]?.userId
                GraphTargetType.ENTITY -> entities[targetId]?.userId
            }
            if (ownerId != userId) return null
            val sources = provenance.values
                .filter { it.targetType == targetType && it.targetId == targetId && it.id !in invalidatedProvenance }
                .map {
                    WhyThisSource(
                        sourceType = it.sourceType,
                        sourceId = it.sourceId,
                        label = "${it.sourceType.name.lowercase()}:${it.sourceId}",
                    )
                }
            if (targetType != GraphTargetType.ENTITY && sources.isEmpty()) return null
            return WhyThisProjection(
                targetType = targetType,
                targetId = targetId,
                title = targetId,
                summary = null,
                sources = sources,
            )
        }

        override suspend fun exportUserGraph(userId: String): GraphExport = GraphExport(
            userId = userId,
            entities = entities.values.filter { it.userId == userId },
            facts = facts.values.filter { it.userId == userId },
            relationships = relationships.values.filter { it.userId == userId },
            provenance = provenance.values.filter { it.userId == userId },
            feedback = feedbackRows.filter { it.userId == userId },
        )

        private fun validateProvenance(
            userId: String,
            targetType: GraphTargetType,
            targetId: String,
            provenance: List<GraphProvenanceDraft>,
        ): GraphWriteResult.Rejected? {
            if (provenance.isEmpty()) return GraphWriteResult.Rejected("missing_provenance")
            val invalid = provenance.any {
                it.userId != userId ||
                    it.targetType != targetType ||
                    it.targetId != targetId ||
                    it.sourceId.isBlank()
            }
            return if (invalid) GraphWriteResult.Rejected("invalid_provenance") else null
        }
    }

    private companion object {
        const val USER_ID = "local-user"

        fun userEntity() = GraphEntityDraft(
            id = "user",
            userId = USER_ID,
            type = GraphEntityType.USER,
            canonicalName = "You",
        )

        fun projectEntity() = GraphEntityDraft(
            id = "project-orbit",
            userId = USER_ID,
            type = GraphEntityType.PROJECT,
            canonicalName = "Orbit",
        )

        fun profileFact() = GraphFactDraft(
            id = "fact-founder",
            userId = USER_ID,
            subjectEntityId = "user",
            predicate = "works_on",
            objectText = "Orbit",
        )

        fun projectRelationship() = GraphRelationshipDraft(
            id = "rel-user-orbit",
            userId = USER_ID,
            fromEntityId = "user",
            toEntityId = "project-orbit",
            relationshipType = "works_on",
        )

        fun factSource(id: String, envelopeId: String) = GraphProvenanceDraft(
            id = id,
            userId = USER_ID,
            targetType = GraphTargetType.FACT,
            targetId = "fact-founder",
            sourceType = GraphSourceType.ENVELOPE,
            sourceId = envelopeId,
            supportKind = GraphSupportKind.ASSERTS,
        )
    }
}
