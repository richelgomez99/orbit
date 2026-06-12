package com.orbit.app.graph

enum class GraphEntityType {
    USER,
    PERSON,
    ORGANIZATION,
    PROJECT,
    PLACE,
    EVENT,
    PRODUCT,
    TOPIC,
    TASK,
}

enum class GraphStatus {
    ACTIVE,
    INVALIDATED,
}

enum class GraphSourceType {
    ENVELOPE,
    USER_CONFIRMATION,
    USER_CORRECTION,
    ACTION_DRAFT,
    ASK_ANSWER,
    INTEGRATION_READ,
}

enum class GraphTargetType {
    ENTITY,
    FACT,
    RELATIONSHIP,
}

enum class GraphSupportKind {
    ASSERTS,
    MENTIONS,
    CORRECTS,
    REJECTS,
}

enum class GraphFeedbackType {
    REJECTED,
    CORRECTED,
    MERGED,
    CONFIDENCE_DOWN,
}

data class GraphEntityDraft(
    val id: String,
    val userId: String,
    val type: GraphEntityType,
    val canonicalName: String,
    val normalizedName: String = canonicalName.trim().lowercase(),
    val description: String? = null,
)

data class GraphFactDraft(
    val id: String,
    val userId: String,
    val subjectEntityId: String,
    val predicate: String,
    val objectText: String? = null,
    val objectEntityId: String? = null,
    val confidence: Float = 1f,
)

data class GraphRelationshipDraft(
    val id: String,
    val userId: String,
    val fromEntityId: String,
    val toEntityId: String,
    val relationshipType: String,
    val confidence: Float = 1f,
)

data class GraphProvenanceDraft(
    val id: String,
    val userId: String,
    val targetType: GraphTargetType,
    val targetId: String,
    val sourceType: GraphSourceType,
    val sourceId: String,
    val supportKind: GraphSupportKind,
)

data class GraphFeedbackDraft(
    val id: String,
    val userId: String,
    val targetType: GraphTargetType,
    val targetId: String,
    val feedbackType: GraphFeedbackType,
    val replacementText: String? = null,
    val replacementEntityId: String? = null,
    val sourceType: GraphSourceType,
    val sourceId: String,
)

data class WhyThisSource(
    val sourceType: GraphSourceType,
    val sourceId: String,
    val label: String,
    val dayLocal: String? = null,
    val createdAtMillis: Long? = null,
)

data class WhyThisProjection(
    val targetType: GraphTargetType,
    val targetId: String,
    val title: String,
    val summary: String?,
    val sources: List<WhyThisSource>,
)

data class GraphExport(
    val userId: String,
    val entities: List<GraphEntityDraft>,
    val facts: List<GraphFactDraft>,
    val relationships: List<GraphRelationshipDraft>,
    val provenance: List<GraphProvenanceDraft>,
    val feedback: List<GraphFeedbackDraft>,
)

sealed class GraphWriteResult {
    data class Written(val id: String) : GraphWriteResult()
    data class Rejected(val reason: String) : GraphWriteResult()
}

data class GraphInvalidationResult(
    val invalidatedTargetIds: Set<String>,
    val preservedTargetIds: Set<String>,
)
