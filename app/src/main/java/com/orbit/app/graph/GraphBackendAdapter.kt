package com.orbit.app.graph

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

    suspend fun writeFeedback(feedback: GraphFeedbackDraft): GraphWriteResult

    suspend fun invalidateBySource(
        sourceType: GraphSourceType,
        sourceId: String,
        reason: String,
    ): GraphInvalidationResult

    suspend fun whyThis(
        userId: String,
        targetType: GraphTargetType,
        targetId: String,
    ): WhyThisProjection?

    suspend fun exportUserGraph(userId: String): GraphExport
}
