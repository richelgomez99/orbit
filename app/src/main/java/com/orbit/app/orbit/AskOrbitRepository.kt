package com.orbit.app.orbit

import android.content.Context
import com.orbit.app.library.BinderLocalEnvelopeLookup
import com.orbit.app.library.LocalEnvelopeLookup
import com.orbit.app.memory.AskOrbitAnswer
import com.orbit.app.memory.AskOrbitCitation
import com.orbit.app.memory.MemorySearchFilters
import com.orbit.app.memory.MemorySearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AskOrbitRepository {
    suspend fun ask(
        question: String,
        filters: MemorySearchFilters? = null,
        limit: Int = 5,
    ): AskOrbitAnswer
}

class BinderAskOrbitRepository(
    context: Context?,
    private val localEnvelopeLookup: LocalEnvelopeLookup = BinderLocalEnvelopeLookup(requireNotNull(context)),
) : AskOrbitRepository {

    override suspend fun ask(
        question: String,
        filters: MemorySearchFilters?,
        limit: Int,
    ): AskOrbitAnswer = withContext(Dispatchers.IO) {
        val trimmed = question.trim()
        if (trimmed.isBlank()) return@withContext insufficientEvidence(emptyList())
        val cappedLimit = limit.coerceIn(1, 5)
        val localResults = AskOrbitQueryText.queryVariants(trimmed)
            .flatMap { query -> localEnvelopeLookup.search(query, cappedLimit) }
            .distinctBy { it.envelopeId }
            .filter { AskOrbitQueryText.resultMatchesQuestion(it, trimmed) }
            .sortedByDescending { AskOrbitQueryText.matchScore(it, trimmed) }
            .take(cappedLimit)
        if (localResults.isNotEmpty()) {
            return@withContext buildLocalAnswer(localResults)
        }
        insufficientEvidence(emptyList())
    }

    fun disconnect() {
        (localEnvelopeLookup as? BinderLocalEnvelopeLookup)?.disconnect()
    }

    private fun buildLocalAnswer(results: List<MemorySearchResult>): AskOrbitAnswer {
        if (results.isEmpty()) return insufficientEvidence(emptyList())
        val top = results.take(5)
        val citations = top.mapIndexed { index, result ->
            AskOrbitCitation(
                citationId = "c${index + 1}",
                envelopeId = result.envelopeId,
                title = result.title,
                excerpt = result.matchedEvidence.firstOrNull()?.excerpt ?: result.summary,
                dayLocal = result.dayLocal,
                sourceAppLabel = result.sourceAppLabel,
            )
        }
        val subject = top.first().title ?: top.first().summary ?: "a saved memory"
        val evidence = citations.firstOrNull()?.excerpt
        return AskOrbitAnswer(
            status = "answered",
            answer = if (!evidence.isNullOrBlank()) {
                "Best match: $evidence"
            } else {
                "Best match: $subject."
            },
            citations = citations,
            candidates = emptyList(),
            modelLabel = "local/deterministic",
        )
    }

    private fun insufficientEvidence(candidates: List<MemorySearchResult>) = AskOrbitAnswer(
        status = "insufficient_evidence",
        answer = "I could not find enough saved evidence to answer that.",
        citations = emptyList(),
        candidates = candidates,
        modelLabel = "local/deterministic",
    )

}

internal object AskOrbitQueryText {
    private val stopwords = setOf(
        "a",
        "an",
        "and",
        "are",
        "did",
        "do",
        "for",
        "got",
        "i",
        "is",
        "me",
        "my",
        "of",
        "or",
        "recent",
        "recently",
        "save",
        "saved",
        "the",
        "to",
        "want",
        "wanted",
        "was",
        "what",
        "when",
        "where",
        "which",
        "who",
        "why",
    )

    fun queryVariants(question: String): List<String> {
        val tokens = meaningfulTokens(question)
        val variants = linkedSetOf<String>()
        if (tokens.size > 1) {
            variants += tokens.joinToString(" ")
        }
        tokens.forEach { token ->
            variants += token
            variants += normalizedStatusTokens(token)
            stemVariant(token)?.let { variants += it }
        }
        return variants.filter { it.isNotBlank() }
    }

    fun resultMatchesQuestion(result: MemorySearchResult, question: String): Boolean {
        val groups = matchGroups(question)
        if (groups.isEmpty()) return false
        val haystack = result.searchableText()
        return groups.all { group ->
            group.any { token -> haystack.contains(token) }
        }
    }

    fun matchScore(result: MemorySearchResult, question: String): Float {
        val groups = matchGroups(question)
        val haystack = result.searchableText()
        val title = result.title.orEmpty().lowercase()
        val evidence = result.matchedEvidence.joinToString(" ") { it.excerpt.orEmpty() }.lowercase()
        val groupHits = groups.count { group -> group.any { token -> haystack.contains(token) } }
        val titleHits = groups.count { group -> group.any { token -> title.contains(token) } }
        val evidenceHits = groups.count { group -> group.any { token -> evidence.contains(token) } }
        val exactPhraseBonus = if (meaningfulTokens(question).joinToString(" ").let { it.isNotBlank() && haystack.contains(it) }) {
            4f
        } else {
            0f
        }
        return result.score + groupHits * 5f + titleHits * 3f + evidenceHits * 2f + exactPhraseBonus
    }

    private fun meaningfulTokens(question: String): List<String> = question
        .lowercase()
        .split(Regex("[^a-z0-9]+"))
        .map { it.trim() }
        .filter { it.length >= 3 && it !in stopwords }
        .distinct()

    private fun matchGroups(question: String): List<Set<String>> = meaningfulTokens(question)
        .map { token ->
            (listOf(token) + normalizedStatusTokens(token) + listOfNotNull(stemVariant(token))).toSet()
        }

    private fun normalizedStatusTokens(token: String): List<String> = when {
        token.startsWith("cancel") -> listOf("cancel", "reschedul", "postpon", "moved")
        token.startsWith("reschedul") -> listOf("reschedul")
        token.startsWith("postpon") -> listOf("postpon")
        token.startsWith("delay") -> listOf("delay")
        token.startsWith("refund") -> listOf("refund")
        else -> emptyList()
    }
        .filter { it != token }

    private fun stemVariant(token: String): String? {
        val stem = when {
            token.length > 6 && token.endsWith("ing") -> token.dropLast(3)
            token.length > 5 && token.endsWith("ed") -> token.dropLast(2)
            token.length > 5 && token.endsWith("es") -> token.dropLast(2)
            token.length > 4 && token.endsWith("e") -> token.dropLast(1)
            token.length > 4 && token.endsWith("s") -> token.dropLast(1)
            else -> null
        }?.trim()
        return stem?.takeIf { it.length >= 4 && it != token }
    }

    private fun MemorySearchResult.searchableText(): String =
        listOfNotNull(
            title,
            summary,
            sourceAppLabel,
            domain,
            intent,
            matchedEvidence.joinToString(" ") { it.excerpt.orEmpty() },
        ).joinToString(" ").lowercase()
}
