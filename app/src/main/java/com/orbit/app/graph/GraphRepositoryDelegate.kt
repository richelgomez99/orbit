package com.orbit.app.graph

import com.orbit.app.data.dao.PromotedMemorySupportDao
import com.orbit.app.data.entity.PromotedMemoryEntity
import com.orbit.app.data.model.MemorySupportType
import java.util.Locale

class GraphRepositoryDelegate(
    private val adapter: GraphBackendAdapter,
    private val promotedMemorySupportDao: PromotedMemorySupportDao,
    private val userId: String = LOCAL_USER_ID,
) {

    suspend fun projectPromotedMemory(memory: PromotedMemoryEntity): GraphWriteResult {
        if (memory.state.name != "ACTIVE") return GraphWriteResult.Rejected("memory_not_active")

        val subjectEntityId = entityId(memory.subject)
        adapter.upsertEntity(
            GraphEntityDraft(
                id = subjectEntityId,
                userId = userId,
                type = if (memory.subject.equals("user", ignoreCase = true)) {
                    GraphEntityType.USER
                } else {
                    GraphEntityType.TOPIC
                },
                canonicalName = memory.subject.ifBlank { "user" },
            )
        )

        val support = promotedMemorySupportDao.listForMemory(memory.id)
        if (support.isEmpty()) return GraphWriteResult.Rejected("missing_provenance")

        return adapter.writeFact(
            fact = GraphFactDraft(
                id = factId(memory.id),
                userId = userId,
                subjectEntityId = subjectEntityId,
                predicate = memory.predicate,
                objectText = memory.objectValue,
                objectEntityId = null,
                confidence = 1f,
            ),
            provenance = support.mapIndexed { index, row ->
                GraphProvenanceDraft(
                    id = provenanceId(memory.id, row.envelopeId, row.supportType, index),
                    userId = userId,
                    targetType = GraphTargetType.FACT,
                    targetId = factId(memory.id),
                    sourceType = row.supportType.toGraphSourceType(),
                    sourceId = row.envelopeId,
                    supportKind = GraphSupportKind.ASSERTS,
                )
            },
        )
    }

    private fun MemorySupportType.toGraphSourceType(): GraphSourceType = when (this) {
        MemorySupportType.CAPTURE,
        MemorySupportType.UNDERSTANDING -> GraphSourceType.ENVELOPE
        MemorySupportType.ACTION -> GraphSourceType.ACTION_DRAFT
        MemorySupportType.NOTE,
        MemorySupportType.USER_CONFIRMATION -> GraphSourceType.USER_CONFIRMATION
    }

    private fun entityId(value: String): String =
        "graph-entity-${normalize(value.ifBlank { "user" })}"

    private fun factId(memoryId: String): String = "promoted-memory-fact-$memoryId"

    private fun provenanceId(
        memoryId: String,
        envelopeId: String,
        supportType: MemorySupportType,
        index: Int,
    ): String = "promoted-memory-prov-${normalize(memoryId)}-${normalize(envelopeId)}-${supportType.name}-$index"

    private fun normalize(value: String): String =
        value.trim()
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "blank" }

    companion object {
        const val LOCAL_USER_ID = "local-user"
    }
}
