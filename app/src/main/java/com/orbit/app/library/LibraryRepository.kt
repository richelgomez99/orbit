package com.orbit.app.library

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.orbit.app.audit.BinderAuditLogClient
import com.orbit.app.data.ipc.EnvelopeRepositoryService
import com.orbit.app.data.ipc.EnvelopeViewParcel
import com.orbit.app.data.ipc.IEnvelopeRepository
import com.orbit.app.memory.MemoryAudit
import com.orbit.app.memory.MemoryDisplayText
import com.orbit.app.memory.MemoryGatewayRequest
import com.orbit.app.memory.MemoryGatewayResponse
import com.orbit.app.memory.MemorySearchFilters
import com.orbit.app.memory.MemorySearchResult
import com.orbit.app.net.NetworkGatewayService
import com.orbit.app.net.ipc.INetworkGateway
import com.orbit.app.net.ipc.MemoryGatewayRequestParcel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.UUID

interface LibraryRepository {
    suspend fun search(
        query: String,
        filters: MemorySearchFilters? = null,
        limit: Int = 10,
    ): List<MemorySearchResult>
}

interface LocalEnvelopeLookup {
    suspend fun exists(envelopeId: String): Boolean
    suspend fun search(query: String, limit: Int): List<MemorySearchResult> = emptyList()
}

class BinderLibraryRepository(
    private val context: Context,
    private val requestIdFactory: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val auditClient: BinderAuditLogClient = BinderAuditLogClient(context),
    private val memoryAudit: MemoryAudit = MemoryAudit(),
    private val localEnvelopeLookup: LocalEnvelopeLookup = BinderLocalEnvelopeLookup(context),
    private val jsonCodec: Json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = true
    },
) : LibraryRepository {
    private var binder: INetworkGateway? = null
    private var connection: ServiceConnection? = null

    override suspend fun search(
        query: String,
        filters: MemorySearchFilters?,
        limit: Int,
    ): List<MemorySearchResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()
        val request = MemoryGatewayRequest.SemanticSearch(
            requestId = requestIdFactory(),
            query = trimmed,
            filters = filters,
            limit = maxOf(limit, REMOTE_LIMIT_FOR_LOCAL_FILTERING).coerceIn(1, REMOTE_LIMIT_FOR_LOCAL_FILTERING),
            mode = "hybrid",
        )
        val startedAt = clock()
        val localLimit = limit.coerceIn(1, REMOTE_LIMIT_FOR_LOCAL_FILTERING)
        try {
            val gateway = connect()
            val response = gateway.callMemoryGateway(
                MemoryGatewayRequestParcel(
                    jsonCodec.encodeToString(MemoryGatewayRequest.serializer(), request),
                )
            )
            when (val decoded = jsonCodec.decodeFromString(MemoryGatewayResponse.serializer(), response.payloadJson)) {
                is MemoryGatewayResponse.SearchResponse -> {
                    val localResults = decoded.results
                        .filter { localEnvelopeLookup.exists(it.envelopeId) }
                        .filter { LibrarySearchText.resultMatchesQuery(it, trimmed) }
                        .take(localLimit) + localEnvelopeLookup.search(trimmed, localLimit)
                    val displayResults = localResults.dedupeForDisplay().take(localLimit)
                    auditSearch(
                        requestId = request.requestId,
                        query = trimmed,
                        resultCount = displayResults.size,
                        latencyMs = clock() - startedAt,
                        outcome = "success",
                    )
                    displayResults
                }
                is MemoryGatewayResponse.SemanticSearchResponse -> {
                    val localResults = LibrarySemanticResults.localBackedResults(
                        remoteResults = decoded.results,
                        localEnvelopeLookup = localEnvelopeLookup,
                        query = trimmed,
                        limit = localLimit,
                    )
                    auditSearch(
                        requestId = request.requestId,
                        query = trimmed,
                        resultCount = localResults.size,
                        latencyMs = clock() - startedAt,
                        outcome = "success",
                    )
                    localResults
                }
                is MemoryGatewayResponse.Error -> {
                    throw MemoryGatewayUnavailable(decoded.code, decoded.message)
                }
                else -> {
                    throw MemoryGatewayUnavailable("UNEXPECTED_RESPONSE", decoded::class.java.simpleName)
                }
            }
        } catch (t: Throwable) {
            val localResults = localEnvelopeLookup.search(trimmed, localLimit)
            if (localResults.isNotEmpty()) {
                auditSearch(
                    requestId = request.requestId,
                    query = trimmed,
                    resultCount = localResults.size,
                    latencyMs = clock() - startedAt,
                    outcome = "local_fallback:${t.memoryFailureCode()}",
                )
                return@withContext localResults.take(localLimit)
            }
            auditSearch(request.requestId, trimmed, 0, clock() - startedAt, "failed:${t.memoryFailureCode()}")
            throw t
        }
    }

    fun disconnect() {
        val conn = connection
        if (conn != null) {
            runCatching { context.unbindService(conn) }
            connection = null
            binder = null
        }
        auditClient.disconnect()
        (localEnvelopeLookup as? BinderLocalEnvelopeLookup)?.disconnect()
    }

    private suspend fun auditSearch(
        requestId: String,
        query: String,
        resultCount: Int,
        latencyMs: Long,
        outcome: String,
    ) {
        runCatching {
            auditClient.append(
                memoryAudit.searchRequested(
                    requestId = requestId,
                    query = query,
                    resultCount = resultCount,
                    latencyMs = latencyMs.coerceAtLeast(0L),
                    outcome = outcome,
                )
            )
        }
    }

    private suspend fun connect(): INetworkGateway = withContext(Dispatchers.Main) {
        binder?.let { return@withContext it }
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
        val intent = Intent(context, NetworkGatewayService::class.java)
        val bound = context.bindService(intent, conn, Context.BIND_AUTO_CREATE)
        if (!bound) {
            connection = null
            throw IOException("bindService(NetworkGatewayService) failed")
        }
        deferred.await()
    }

    companion object {
        private const val REMOTE_LIMIT_FOR_LOCAL_FILTERING = 20
    }
}

private fun Throwable.memoryFailureCode(): String = when (this) {
    is MemoryGatewayUnavailable -> code
    else -> javaClass.simpleName
}

private fun List<MemorySearchResult>.dedupeForDisplay(): List<MemorySearchResult> =
    LibrarySearchText.dedupeResults(this)

internal object LibrarySearchText {
    fun queryVariants(query: String): List<String> {
        val tokens = query
            .lowercase()
            .split(Regex("\\s+"))
            .map { it.trim().trim(',', '.', ':', ';', '!', '?', '"', '\'') }
            .filter { it.length >= 2 && it !in weakTokens }
        val variants = linkedSetOf(query)
        tokens.forEach { token ->
            variants += token
            token.stemVariant()?.let { variants += it }
        }
        return variants.filter { it.isNotBlank() }
    }

    fun dedupeResults(results: List<MemorySearchResult>): List<MemorySearchResult> =
        results.distinctBy { it.envelopeId }
            .distinctBy { result ->
                result.displayKey()
            }

    fun resultMatchesQuery(result: MemorySearchResult, query: String): Boolean {
        val tokens = significantTokens(query)
        if (tokens.isEmpty()) return true
        val haystack = result.searchableText()
        if (tokens.size == 1) return haystack.contains(tokens.single())
        return tokens.all { haystack.contains(it) }
    }

    private fun MemorySearchResult.displayKey(): String =
            listOf(
                dayLocal,
                sourceAppLabel.orEmpty(),
                title.orEmpty(),
                summary.orEmpty(),
            ).joinToString("|").lowercase().replace(Regex("\\s+"), " ").trim()

    private fun String.stemVariant(): String? {
        val stem = when {
            length > 6 && endsWith("ing") -> dropLast(3)
            length > 5 && endsWith("ed") -> dropLast(2)
            length > 5 && endsWith("es") -> dropLast(2)
            length > 4 && endsWith("e") -> dropLast(1)
            length > 4 && endsWith("s") -> dropLast(1)
            else -> null
        }?.trim()
        return stem?.takeIf { it.length >= 4 && it != this }
    }

    internal fun significantTokens(query: String): List<String> =
        query.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .map { it.trim() }
            .filter { it.length >= 2 && it !in weakTokens }
            .distinct()

    private fun MemorySearchResult.searchableText(): String =
        listOfNotNull(
            title,
            summary,
            sourceAppLabel,
            domain,
            intent,
            matchedEvidence.joinToString(" ") { it.excerpt.orEmpty() },
        ).joinToString(" ").lowercase()

    private val weakTokens = setOf(
        "a",
        "an",
        "and",
        "as",
        "at",
        "by",
        "for",
        "from",
        "in",
        "is",
        "it",
        "of",
        "on",
        "or",
        "the",
        "to",
        "was",
        "with",
    )
}

internal object LibrarySemanticResults {
    suspend fun localBackedResults(
        remoteResults: List<MemorySearchResult>,
        localEnvelopeLookup: LocalEnvelopeLookup,
        query: String,
        limit: Int,
    ): List<MemorySearchResult> {
        val boundedLimit = limit.coerceIn(1, 50)
        val localCloudResults = remoteResults
            .filter { localEnvelopeLookup.exists(it.envelopeId) }
            .take(boundedLimit)
        val localFallbackResults = localEnvelopeLookup.search(query, boundedLimit)
        return (localCloudResults + localFallbackResults)
            .dedupeForDisplay()
            .take(boundedLimit)
    }
}

class BinderLocalEnvelopeLookup(
    private val context: Context,
) : LocalEnvelopeLookup {
    private var binder: IEnvelopeRepository? = null
    private var connection: ServiceConnection? = null

    override suspend fun exists(envelopeId: String): Boolean {
        if (envelopeId.isBlank()) return false
        return runCatching {
            withContext(Dispatchers.IO) {
                connect().getEnvelope(envelopeId) != null
            }
        }.getOrDefault(false)
    }

    override suspend fun search(query: String, limit: Int): List<MemorySearchResult> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return emptyList()
        return runCatching {
            withContext(Dispatchers.IO) {
                val repository = connect()
                LibrarySearchText.queryVariants(trimmed)
                    .flatMap { variant -> repository.searchLocalEnvelopes(variant, limit.coerceIn(1, 50)) }
                    .distinctBy { it.id }
                    .mapNotNull { envelope ->
                        val note = runCatching { repository.getLatestNote(envelope.id) }.getOrNull()
                        LocalEnvelopeMemoryResultMapper.toMemorySearchResult(
                            envelope = envelope,
                            query = trimmed,
                            note = note,
                        )
                    }
                    .filter { LibrarySearchText.resultMatchesQuery(it, trimmed) }
                    .mapIndexed { index, result ->
                        result.copy(rank = index + 1)
                    }
                    .dedupeForDisplay()
                    .take(limit.coerceIn(1, 50))
            }
        }.getOrDefault(emptyList())
    }

    fun disconnect() {
        val conn = connection
        if (conn != null) {
            runCatching { context.unbindService(conn) }
            connection = null
            binder = null
        }
    }

    private suspend fun connect(): IEnvelopeRepository = withContext(Dispatchers.Main) {
        binder?.let { return@withContext it }
        val deferred = CompletableDeferred<IEnvelopeRepository>()
        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val stub = IEnvelopeRepository.Stub.asInterface(service)
                binder = stub
                deferred.complete(stub)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                binder = null
            }
        }
        connection = conn
        val bound = context.bindService(
            Intent(context, EnvelopeRepositoryService::class.java),
            conn,
            Context.BIND_AUTO_CREATE,
        )
        if (!bound) {
            connection = null
            throw IOException("bindService(EnvelopeRepositoryService) failed")
        }
        deferred.await()
    }

}

internal object LocalEnvelopeMemoryResultMapper {
    fun toMemorySearchResult(
        envelope: EnvelopeViewParcel,
        query: String,
        note: String?,
    ): MemorySearchResult {
        with(envelope) {
        val text = textContent.orEmpty()
        val noteMatch = note?.takeIf { it.containsQuery(query) }
        val excerpt = noteMatch?.let { MemoryDisplayText.compact(it, maxChars = 180) } ?: excerptFor(query)
        return MemorySearchResult(
            envelopeId = id,
            rank = 0,
            score = localScore(query, note),
            title = noteMatch?.let { MemoryDisplayText.compact(it, maxChars = 64) } ?: title ?: displayTitle(note),
            summary = noteMatch?.let { "Context: ${MemoryDisplayText.compact(it, maxChars = 160)}" }
                ?: MemoryDisplayText.summary(summary, text, maxChars = 180)
                ?: excerpt,
            dayLocal = dayLocal,
            createdAtMillis = createdAtMillis,
            intent = intent,
            sourceAppLabel = sourceAppLabel,
            domain = domain,
            matchedEvidence = if (excerpt.isNotBlank()) {
                listOf(
                    com.orbit.app.memory.MemoryEvidenceSnippet(
                        kind = if (noteMatch != null) "NOTE" else "LOCAL_TEXT",
                        label = if (noteMatch != null) "Context" else "Local capture",
                        excerpt = excerpt,
                        source = if (noteMatch != null) "note" else "envelope",
                    )
                )
            } else {
                emptyList()
            },
        )
        }
    }

    private fun EnvelopeViewParcel.localScore(query: String, note: String?): Float {
        val q = query.lowercase()
        var score = 0f
        if (note?.lowercase()?.contains(q) == true) score += 8f
        if (title?.lowercase()?.contains(q) == true) score += 5f
        if (summary?.lowercase()?.contains(q) == true) score += 3f
        if (textContent?.lowercase()?.contains(q) == true) score += 1f
        if (sourceAppLabel?.lowercase()?.contains(q) == true) score += 0.5f
        return score.coerceAtLeast(0.1f)
    }

    private fun EnvelopeViewParcel.displayTitle(note: String?): String {
        return MemoryDisplayText.title(
            existingTitle = title ?: note?.lineSequence()?.firstOrNull(),
            text = textContent,
            domain = domain,
            maxChars = 64,
        ) ?: contentType.lowercase().replaceFirstChar { it.uppercase() }
    }

    private fun EnvelopeViewParcel.excerptFor(query: String): String {
        return MemoryDisplayText.excerptForQuery(textContent, query, maxChars = 180)
    }

    private fun String.containsQuery(query: String): Boolean {
        val haystack = lowercase()
        val tokens = LibrarySearchText.significantTokens(query)
        if (tokens.isEmpty()) return false
        return if (tokens.size == 1) {
            haystack.contains(tokens.single())
        } else {
            tokens.all { haystack.contains(it) }
        }
    }
}

class MemoryGatewayUnavailable(
    val code: String,
    message: String,
) : IOException("$code: $message")
