package com.orbit.app.curious

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CuriousQuestionGeneratorTest {

    @Test
    fun emitsNoQuestionWithInsufficientEvidence() {
        val generator = CuriousQuestionGenerator(
            CuriousQuestionPolicy(minSupportingSources = 3)
        )

        val questions = generator.generate(
            listOf(
                signal("coding_agents", "env-1"),
                signal("coding_agents", "env-2")
            )
        )

        assertTrue(questions.isEmpty())
    }

    @Test
    fun emitsQuestionWithSourceRefsAndChoices() {
        val generator = CuriousQuestionGenerator(
            CuriousQuestionPolicy(minSupportingSources = 3)
        )

        val question = generator.generate(
            listOf(
                signal("coding_agents", "env-1", label = "Claude Code notes"),
                signal("coding_agents", "env-2", label = "GStack plan"),
                signal("coding_agents", "memory-1", sourceType = CuriousEvidenceSourceType.PROMOTED_MEMORY)
            )
        ).single()

        assertEquals("curious:coding_agents", question.id)
        assertEquals("How should Orbit treat these coding agents saves?", question.questionText)
        assertEquals(3, question.sourceRefs.size)
        assertEquals(4, question.choices.size)
        assertTrue(question.sourceRefs.any { it.sourceId == "memory-1" })
    }

    @Test
    fun capsOutputAndSortsByConfidence() {
        val generator = CuriousQuestionGenerator(
            CuriousQuestionPolicy(minSupportingSources = 2, maxQuestions = 1)
        )

        val questions = generator.generate(
            listOf(
                signal("recipes", "env-1", weight = 0.5f),
                signal("recipes", "env-2", weight = 0.5f),
                signal("agent_tools", "env-3", weight = 1f),
                signal("agent_tools", "env-4", weight = 1f)
            )
        )

        assertEquals(1, questions.size)
        assertEquals("curious:agent_tools", questions.single().id)
    }

    @Test
    fun suppressedQuestionsDoNotReturnActive() {
        val generator = CuriousQuestionGenerator(
            CuriousQuestionPolicy(minSupportingSources = 2)
        )

        val questions = generator.generate(
            listOf(
                signal("agent_tools", "env-1"),
                signal("agent_tools", "env-2")
            ),
            suppressedQuestionIds = setOf("curious:agent_tools")
        )

        assertTrue(questions.isEmpty())
    }

    @Test
    fun questionCopyDoesNotEchoRawLongSourceText() {
        val generator = CuriousQuestionGenerator(
            CuriousQuestionPolicy(minSupportingSources = 2, maxLabelChars = 20)
        )
        val raw = "passport 123456789 private raw screenshot text that should not be copied"

        val question = generator.generate(
            listOf(
                signal("private_docs", "env-1", label = raw),
                signal("private_docs", "env-2", label = raw)
            )
        ).single()

        assertFalse(question.questionText.contains("123456789"))
        assertFalse(question.questionText.contains("passport"))
        assertTrue(question.sourceRefs.all { it.label.length <= 20 })
    }

    private fun signal(
        topic: String,
        sourceId: String,
        label: String = sourceId,
        weight: Float = 1f,
        sourceType: CuriousEvidenceSourceType = CuriousEvidenceSourceType.ENVELOPE
    ): CuriousSignal = CuriousSignal(
        topicKey = topic,
        evidence = CuriousEvidenceRef(
            sourceType = sourceType,
            sourceId = sourceId,
            label = label
        ),
        weight = weight
    )
}
