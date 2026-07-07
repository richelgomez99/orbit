package com.orbit.app.ai.local

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.orbit.app.ai.EmbeddingResult
import com.orbit.app.ai.LlmProvider
import com.orbit.app.ai.model.ActionExtractionResult
import com.orbit.app.ai.model.AppFunctionSummary
import com.orbit.app.ai.model.DayHeaderResult
import com.orbit.app.ai.model.IntentClassification
import com.orbit.app.ai.model.LlmProvenance
import com.orbit.app.ai.model.SensitivityResult
import com.orbit.app.ai.model.SummaryResult
import com.orbit.app.data.entity.StateSnapshot
import com.orbit.app.data.model.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.util.Locale

/**
 * Spec 022 Slice 3 — BYOM [LlmProvider] backed by Google's LiteRT-LM engine
 * (`com.google.ai.edge.litertlm`), the strategic successor to the
 * maintenance-mode MediaPipe `tasks-genai`. Loads a `.litertlm` bundle and
 * runs generation on-device with GPU acceleration (CPU fallback).
 *
 * Same process/boundary contract as [MediaPipeLlmProvider]: runs in `:ml`,
 * no network. Same capability posture: real `summarize`/`generateDayHeader`;
 * classify/sensitivity/extractActions return safe defaults (M2), `embed`
 * returns null. Generation is serialized on [genLock]; the engine is built
 * once (lazy) and released via [close].
 */
class LiteRtLmProvider(
    private val appContext: Context,
    private val modelPath: String,
    private val modelLabel: String,
) : LlmProvider, Closeable {

    private val provenance = LlmProvenance.LocalByom(modelLabel)
    private val genLock = Mutex()

    @Volatile
    private var engine: Engine? = null

    private fun engineOrCreate(): Engine {
        engine?.let { return it }
        synchronized(this) {
            engine?.let { return it }
            if (!File(modelPath).exists()) {
                throw IOException("LiteRT-LM model file missing at $modelPath")
            }
            // Prefer GPU; fall back to CPU if GPU init fails (older/odd GPUs).
            val built = runCatching { buildEngine(Backend.GPU()) }
                .getOrElse {
                    Log.w(TAG, "GPU engine init failed (${it.javaClass.simpleName}); falling back to CPU")
                    buildEngine(Backend.CPU())
                }
            engine = built
            return built
        }
    }

    private fun buildEngine(backend: Backend): Engine {
        val config = EngineConfig(
            modelPath = modelPath,
            backend = backend,
            cacheDir = appContext.cacheDir.path,
        )
        return Engine(config).also { it.initialize() }
    }

    /** Serialized, blocking one-shot generation off the main thread. */
    private suspend fun generate(prompt: String): String = withContext(Dispatchers.Default) {
        genLock.withLock {
            try {
                val reply = engineOrCreate().createConversation().use { conversation ->
                    conversation.sendMessage(prompt)
                }
                // Message carries a Contents list; concatenate the Text parts.
                reply.contents.contents
                    .filterIsInstance<Content.Text>()
                    .joinToString(" ") { it.text }
                    .trim()
            } catch (t: Throwable) {
                Log.w(TAG, "sendMessage failed: ${t.javaClass.simpleName}: ${t.message}")
                throw IOException("LITERTLM_GENERATION_FAILED: ${t.message}", t)
            }
        }
    }

    override suspend fun summarize(text: String, maxTokens: Int): SummaryResult {
        val prompt = buildString {
            append("Summarize the following in at most ")
            append(maxTokens.coerceAtLeast(16))
            append(" tokens. Reply with only the summary, no preamble.\n\n")
            append(text)
        }
        return SummaryResult(
            text = generate(prompt),
            generationLocale = Locale.getDefault().toLanguageTag(),
            provenance = provenance,
        )
    }

    override suspend fun generateDayHeader(
        dayIsoDate: String,
        envelopeSummaries: List<String>,
    ): DayHeaderResult {
        val prompt = buildString {
            append("Write a short, calm one-line header (max 8 words) for a ")
            append("personal daily journal on ").append(dayIsoDate).append(". ")
            append("Base it on these captured notes; reply with only the header.\n\n")
            envelopeSummaries.take(20).forEach { append("- ").append(it).append('\n') }
        }
        return DayHeaderResult(
            text = generate(prompt),
            generationLocale = Locale.getDefault().toLanguageTag(),
            provenance = provenance,
        )
    }

    // Safe defaults — same rationale as MediaPipeLlmProvider (M2): local
    // classification/extraction is not production-quality yet.
    override suspend fun classifyIntent(text: String, appCategory: String): IntentClassification =
        IntentClassification(Intent.AMBIGUOUS, confidence = 0f, provenance = provenance)

    override suspend fun scanSensitivity(text: String): SensitivityResult =
        SensitivityResult(flagsJson = "[]", provenance = provenance)

    override suspend fun extractActions(
        text: String,
        contentType: String,
        state: StateSnapshot,
        registeredFunctions: List<AppFunctionSummary>,
        maxCandidates: Int,
    ): ActionExtractionResult =
        ActionExtractionResult(provenance = provenance, candidates = emptyList())

    override suspend fun embed(text: String): EmbeddingResult? = null

    /** Debug-only (Slice 3 validation): raw one-shot generation. */
    internal suspend fun debugGenerate(prompt: String): String = generate(prompt)

    override fun close() {
        synchronized(this) {
            runCatching { engine?.close() }
            engine = null
        }
    }

    companion object {
        private const val TAG = "LiteRtLmProvider"
    }
}
