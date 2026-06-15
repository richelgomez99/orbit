package com.orbit.app.curious

enum class CuriousEvidenceSourceType {
    ENVELOPE,
    PROMOTED_MEMORY,
    GRAPH_FACT,
    GRAPH_ENTITY
}

enum class CuriousQuestionStatus {
    ACTIVE,
    DISMISSED,
    ANSWERED,
    STALE
}

data class CuriousEvidenceRef(
    val sourceType: CuriousEvidenceSourceType,
    val sourceId: String,
    val label: String
)

data class CuriousSignal(
    val topicKey: String,
    val evidence: CuriousEvidenceRef,
    val weight: Float = 1f
)

data class CuriousQuestionChoice(
    val id: String,
    val label: String,
    val meaning: String
)

data class CuriousQuestionCandidate(
    val id: String,
    val questionText: String,
    val choices: List<CuriousQuestionChoice>,
    val sourceRefs: List<CuriousEvidenceRef>,
    val confidence: Float,
    val status: CuriousQuestionStatus = CuriousQuestionStatus.ACTIVE
)

data class CuriousQuestionPolicy(
    val minSupportingSources: Int = 3,
    val maxQuestions: Int = 2,
    val maxSourceRefsPerQuestion: Int = 4,
    val maxLabelChars: Int = 48
)
