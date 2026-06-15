package com.orbit.app.curious

class CuriousQuestionGenerator(
    private val policy: CuriousQuestionPolicy = CuriousQuestionPolicy()
) {
    fun generate(
        signals: List<CuriousSignal>,
        suppressedQuestionIds: Set<String> = emptySet()
    ): List<CuriousQuestionCandidate> {
        if (policy.minSupportingSources < 2 || policy.maxQuestions <= 0) return emptyList()
        return signals
            .filter { it.topicKey.isNotBlank() && it.evidence.sourceId.isNotBlank() }
            .groupBy { it.topicKey.normalizedTopicKey() }
            .mapNotNull { (topicKey, topicSignals) ->
                val distinct = topicSignals.distinctBy { it.evidence.sourceType to it.evidence.sourceId }
                if (distinct.size < policy.minSupportingSources) return@mapNotNull null
                val questionId = "curious:$topicKey"
                if (questionId in suppressedQuestionIds) return@mapNotNull null
                val confidence = (distinct.sumOf { it.weight.toDouble() } / policy.minSupportingSources)
                    .coerceIn(0.1, 1.0)
                    .toFloat()
                CuriousQuestionCandidate(
                    id = questionId,
                    questionText = "How should Orbit treat these ${topicKey.toDisplayTopic()} saves?",
                    choices = defaultChoices(topicKey),
                    sourceRefs = distinct
                        .take(policy.maxSourceRefsPerQuestion)
                        .map { it.evidence.capped(policy.maxLabelChars) },
                    confidence = confidence
                )
            }
            .sortedWith(
                compareByDescending<CuriousQuestionCandidate> { it.confidence }
                    .thenBy { it.id }
            )
            .take(policy.maxQuestions)
    }

    private fun defaultChoices(topicKey: String): List<CuriousQuestionChoice> = listOf(
        CuriousQuestionChoice(
            id = "$topicKey:work",
            label = "Work",
            meaning = "Treat this topic as work-related context."
        ),
        CuriousQuestionChoice(
            id = "$topicKey:project",
            label = "Project",
            meaning = "Treat this topic as connected to something I am building."
        ),
        CuriousQuestionChoice(
            id = "$topicKey:research",
            label = "Research",
            meaning = "Treat this topic as research or learning material."
        ),
        CuriousQuestionChoice(
            id = "$topicKey:reference",
            label = "Reference",
            meaning = "Keep this as reference without assuming it is current."
        )
    )
}

private fun String.normalizedTopicKey(): String = trim()
    .lowercase()
    .replace(Regex("[^a-z0-9]+"), "_")
    .trim('_')

private fun String.toDisplayTopic(): String = replace('_', ' ')
    .ifBlank { "related" }

private fun CuriousEvidenceRef.capped(maxChars: Int): CuriousEvidenceRef = copy(
    label = label
        .replace(Regex("\\s+"), " ")
        .trim()
        .let { clean ->
            if (clean.length <= maxChars) clean else clean.take(maxChars).trimEnd()
        }
)
