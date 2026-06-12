package com.orbit.app.orbit

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.orbit.app.audit.BinderAuditLogClient
import com.orbit.app.cloud.BudgetDecisionReason
import com.orbit.app.cloud.CloudCapability
import com.orbit.app.cloud.CloudUsageOutcome
import com.orbit.app.cloud.CloudUsageReceiptWriter
import com.orbit.app.data.entity.AuditLogEntryEntity
import com.orbit.app.library.BinderLocalEnvelopeLookup
import com.orbit.app.library.LocalEnvelopeLookup
import com.orbit.app.memory.AskOrbitAnswer
import com.orbit.app.memory.AskOrbitCitation
import com.orbit.app.memory.MemoryGatewayRequest
import com.orbit.app.memory.MemoryGatewayResponse
import com.orbit.app.memory.MemorySearchFilters
import com.orbit.app.memory.MemorySearchResult
import com.orbit.app.net.NetworkGatewayService
import com.orbit.app.net.ipc.INetworkGateway
import com.orbit.app.net.ipc.MemoryGatewayRequestParcel
import com.orbit.app.settings.PrivacyPreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.UUID

interface AskOrbitRepository {
    suspend fun ask(
        question: String,
        filters: MemorySearchFilters? = null,
        limit: Int = 5,
    ): AskOrbitAnswer
}

class BinderAskOrbitRepository(
    private val context: Context?,
    private val localEnvelopeLookup: LocalEnvelopeLookup = BinderLocalEnvelopeLookup(requireNotNull(context)),
    private val requestIdFactory: () -> String = { UUID.randomUUID().toString() },
    private val jsonCodec: Json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = true
    },
    private val memoryGatewayCaller: (suspend (MemoryGatewayRequest) -> MemoryGatewayResponse)? = null,
    private val cloudAskSynthesisEnabled: () -> Boolean = {
        context?.let { PrivacyPreferences(it).cloudAskSynthesisEnabled } ?: true
    },
    private val cloudReceiptAppender: (suspend (AuditLogEntryEntity) -> Unit)? = null,
    private val cloudReceiptWriter: CloudUsageReceiptWriter = CloudUsageReceiptWriter(),
) : AskOrbitRepository {
    private var binder: INetworkGateway? = null
    private var connection: ServiceConnection? = null
    private val auditClient: BinderAuditLogClient? = context?.let { BinderAuditLogClient(it) }

    override suspend fun ask(
        question: String,
        filters: MemorySearchFilters?,
        limit: Int,
    ): AskOrbitAnswer = withContext(Dispatchers.IO) {
        val trimmed = question.trim()
        if (trimmed.isBlank()) return@withContext insufficientEvidence(emptyList(), trimmed)
        val cappedLimit = limit.coerceIn(1, 5)
        requestGroundedAsk(trimmed, filters, cappedLimit)?.let { return@withContext it }
        val localResults = AskOrbitQueryText.queryVariants(trimmed)
            .flatMap { query -> localEnvelopeLookup.search(query, cappedLimit) }
            .distinctBy { it.envelopeId }
            .filter { AskOrbitQueryText.resultMatchesQuestion(it, trimmed) }
            .sortedByDescending { AskOrbitQueryText.matchScore(it, trimmed) }
            .take(cappedLimit)
        if (localResults.isNotEmpty()) {
            return@withContext buildLocalAnswer(localResults)
        }
        insufficientEvidence(emptyList(), trimmed)
    }

    fun disconnect() {
        val conn = connection
        val appContext = context
        if (conn != null && appContext != null) {
            runCatching { appContext.unbindService(conn) }
            connection = null
            binder = null
        }
        auditClient?.disconnect()
        (localEnvelopeLookup as? BinderLocalEnvelopeLookup)?.disconnect()
    }

    private suspend fun requestGroundedAsk(
        question: String,
        filters: MemorySearchFilters?,
        limit: Int,
    ): AskOrbitAnswer? {
        if (!cloudAskSynthesisEnabled()) {
            recordCloudAskSkipped(question)
            return null
        }
        return runCatching {
            val request = MemoryGatewayRequest.GroundedAsk(
                requestId = requestIdFactory(),
                question = question,
                filters = filters,
                limit = limit,
                allowSynthesis = true,
            )
            val decoded = memoryGatewayCaller?.invoke(request)
                ?: run {
                    if (context == null) return null
                    val response = connect().callMemoryGateway(
                        MemoryGatewayRequestParcel(
                            jsonCodec.encodeToString(MemoryGatewayRequest.serializer(), request)
                        )
                    )
                    jsonCodec.decodeFromString(MemoryGatewayResponse.serializer(), response.payloadJson)
                }
            when (decoded) {
                is MemoryGatewayResponse.GroundedAskResponse -> decoded.answer.localBackedOrNull()
                is MemoryGatewayResponse.Error -> null
                else -> null
            }
        }.getOrNull()
    }

    private suspend fun recordCloudAskSkipped(question: String) {
        val entry = cloudReceiptWriter.receipt(
            requestId = requestIdFactory(),
            capability = CloudCapability.ASK_GROUNDED_SYNTHESIS,
            outcome = CloudUsageOutcome.SKIPPED,
            reason = BudgetDecisionReason.DISABLED_BY_USER,
            endpoint = "grounded_ask",
            inputForDigest = question,
        )
        runCatching {
            val appender = cloudReceiptAppender
            if (appender != null) {
                appender(entry)
            } else {
                auditClient?.append(entry)
            }
        }
    }

    private suspend fun AskOrbitAnswer.localBackedOrNull(): AskOrbitAnswer? =
        AskOrbitGrounding.localBackedOrNull(this, localEnvelopeLookup)

    private suspend fun connect(): INetworkGateway = withContext(Dispatchers.Main) {
        binder?.let { return@withContext it }
        val appContext = context ?: throw IOException("context unavailable")
        val deferred = CompletableDeferred<INetworkGateway>()
        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val stub = INetworkGateway.Stub.asInterface(service)
                binder = stub
                deferred.complete(stub)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                binder = null
            }
        }
        connection = conn
        val bound = appContext.bindService(Intent(appContext, NetworkGatewayService::class.java), conn, Context.BIND_AUTO_CREATE)
        if (!bound) {
            connection = null
            throw IOException("bindService(NetworkGatewayService) failed")
        }
        deferred.await()
    }

    private fun buildLocalAnswer(results: List<MemorySearchResult>): AskOrbitAnswer {
        if (results.isEmpty()) return insufficientEvidence(emptyList(), null)
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

    private fun insufficientEvidence(
        candidates: List<MemorySearchResult>,
        question: String?,
    ) = AskOrbitAnswer(
        status = "insufficient_evidence",
        answer = if (question?.let(AskOrbitQueryText::isSensitiveIdentifierQuestion) == true) {
            "I can answer sensitive identifier questions only when a saved capture explicitly contains the detail. I could not find that saved evidence."
        } else {
            "I could not find enough saved evidence to answer that."
        },
        citations = emptyList(),
        candidates = candidates,
        modelLabel = "local/deterministic",
    )

}

internal object AskOrbitGrounding {
    suspend fun localBackedOrNull(
        answer: AskOrbitAnswer,
        localEnvelopeLookup: LocalEnvelopeLookup,
    ): AskOrbitAnswer? {
        if (answer.status == "sensitive_refusal" || answer.status == "insufficient_evidence") return answer
        val localCitations = answer.citations.filter { localEnvelopeLookup.exists(it.envelopeId) }
        if (answer.status == "answered" && localCitations.isEmpty()) return null
        val localCandidateIds = answer.candidates
            .filter { localEnvelopeLookup.exists(it.envelopeId) }
            .map { it.envelopeId }
            .toSet()
        return answer.copy(
            citations = localCitations,
            candidates = answer.candidates.filter { it.envelopeId in localCandidateIds },
        )
    }
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

    fun isSensitiveIdentifierQuestion(question: String): Boolean {
        val normalized = question.lowercase()
        return listOf(
            "passport",
            "social security",
            "ssn",
            "tax id",
            "driver license",
            "driver's license",
            "bank account",
            "routing number",
            "credit card",
        ).any { normalized.contains(it) }
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
