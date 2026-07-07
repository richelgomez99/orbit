package com.orbit.app.ai.local

import android.content.Context
import android.util.Log
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
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.util.Locale

/**
 * Spec 022 — BYOM on-device [LlmProvider] backed by Google's MediaPipe LLM
 * Inference (LiteRT-LM). Loads a downloaded Gemma `.task` bundle (mmap'd,
 * file-backed) and runs generation entirely on-device.
 *
 * Process/boundary contract (Principle II/VI): this runs in `:ml` and MUST
 * NOT touch the network — MediaPipe only reads the local model file. The
 * `.task` was fetched by `:net` and written to a path all processes share
 * (see [com.orbit.app.net.ModelDownloadStore]); `:ml` mmaps it here.
 *
 * Scope of this first engine slice:
 *  - [summarize] and [generateDayHeader] run real generation.
 *  - [classifyIntent], [scanSensitivity], [extractActions] return safe,
 *    conservative defaults (no fabricated proposals/labels) until the
 *    prompt-and-parse slice lands — same posture [NanoLlmProvider] takes.
 *  - [embed] returns `null`: MediaPipe LLM Inference does not expose an
 *    embedding API, and [LlmProvider.embed]'s contract is graceful `null`.
 *
 * Concurrency: [LlmInference.generateResponse] is not safe to call
 * concurrently on one engine, so calls are serialized with [genLock]. The
 * engine is created lazily on first use (loading is multi-hundred-MB) and
 * released via [close].
 */
class MediaPipeLlmProvider(
    private val appContext: Context,
    private val modelPath: String,
    private val modelLabel: String,
    private val maxTokens: Int = 1024,
) : LlmProvider, Closeable {

    private val provenance = LlmProvenance.LocalByom(modelLabel)
    private val genLock = Mutex()

    @Volatile
    private var engine: LlmInference? = null

    /** Build (once) or return the mmap-backed inference engine. */
    private fun engineOrCreate(): LlmInference {
        engine?.let { return it }
        synchronized(this) {
            engine?.let { return it }
            val file = File(modelPath)
            if (!file.exists()) {
                throw IOException("BYOM model file missing at $modelPath")
            }
            // Engine-level options in current MediaPipe take modelPath + token
            // budget; sampling params (topK/temperature) live on session options
            // and default sensibly for one-shot generateResponse.
            val options = LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(maxTokens)
                .build()
            return LlmInference.createFromOptions(appContext, options)
                .also { engine = it }
        }
    }

    /** Serialized, blocking one-shot generation off the main thread. */
    private suspend fun generate(prompt: String): String = withContext(Dispatchers.Default) {
        genLock.withLock {
            try {
                engineOrCreate().generateResponse(prompt).trim()
            } catch (t: Throwable) {
                Log.w(TAG, "generateResponse failed: ${t.javaClass.simpleName}: ${t.message}")
                throw IOException("BYOM_GENERATION_FAILED: ${t.message}", t)
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

    // --- Conservative defaults until the prompt-and-parse slice (documented above) ---

    override suspend fun classifyIntent(text: String, appCategory: String): IntentClassification =
        IntentClassification(intent = Intent.AMBIGUOUS, confidence = 0f, provenance = provenance)

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

    override fun close() {
        synchronized(this) {
            runCatching { engine?.close() }
            engine = null
        }
    }

    companion object {
        private const val TAG = "MediaPipeLlmProvider"
    }
}
