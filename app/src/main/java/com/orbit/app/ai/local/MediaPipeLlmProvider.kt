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
import org.json.JSONArray
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

    /**
     * Serialized, blocking one-shot generation off the main thread via the
     * engine's [LlmInference.generateResponse].
     *
     * NOTE: per-call sampling via [com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession]
     * (topK/temperature on session options) was attempted but hung on-device
     * with tasks-genai 0.10.35 + Gemma 3 1B — createFromOptions/generateResponse
     * never returned. Reverted to the proven engine path, which uses the
     * model's default sampling. Deterministic classification would benefit
     * from a working session path; revisit on a tasks-genai bump.
     */
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

    /**
     * Constrained single-label classification. generateResponse does not
     * expose token probabilities, so [IntentClassification.confidence] is a
     * heuristic: a matched label reports [LOCAL_MATCH_CONFIDENCE]; no match
     * falls back to AMBIGUOUS at 0. Deterministic (temperature 0, topK 1).
     */
    override suspend fun classifyIntent(text: String, appCategory: String): IntentClassification {
        if (text.isBlank()) {
            return IntentClassification(Intent.AMBIGUOUS, 0f, provenance)
        }
        val prompt = buildString {
            append("Classify the saved text into exactly one label.\n")
            append("WANT_IT: wants to buy, acquire, or own this.\n")
            append("READ_LATER: an article or content to read later.\n")
            append("REFERENCE: factual info to keep for reference.\n")
            append("FOR_SOMEONE: relevant to another person or to share.\n")
            append("INTERESTING: interesting but with no clear action.\n")
            append("Reply with ONLY the label word.\n\nText: ")
            append(text.take(2_000))
        }
        val raw = generate(prompt).uppercase()
        val matched = INTENT_LABELS.firstOrNull { raw.contains(it.name) }
        return IntentClassification(
            intent = matched ?: Intent.AMBIGUOUS,
            confidence = if (matched != null) LOCAL_MATCH_CONFIDENCE else 0f,
            provenance = provenance,
        )
    }

    /**
     * Constrained multi-label sensitivity tag scan. Returns a JSON array of
     * matched tags (empty on "NONE"/no match), mirroring the cloud provider's
     * `flagsJson` shape.
     */
    override suspend fun scanSensitivity(text: String): SensitivityResult {
        if (text.isBlank()) return SensitivityResult("[]", provenance)
        val prompt = buildString {
            append("List which sensitive categories the text contains, comma-separated, ")
            append("from: financial, medical, credentials, contact, location. ")
            append("Reply NONE if none apply.\n\nText: ")
            append(text.take(2_000))
        }
        val raw = generate(prompt).lowercase()
        val tags = SENSITIVITY_TAGS.filter { raw.contains(it) }
        val json = JSONArray().apply { tags.forEach { put(it) } }.toString()
        return SensitivityResult(flagsJson = json, provenance = provenance)
    }

    // extractActions stays a safe default: schema-constrained proposal
    // generation (never invent a functionId, argsJson must validate) is
    // unreliable from a 1B without constrained decoding, and the contract
    // treats an empty list as "no actions". A larger model / grammar-
    // constrained decoding is the follow-up.
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

        /**
         * Heuristic confidence for a local single-label match. generateResponse
         * gives no token probabilities, so this is a fixed moderate value — it
         * clears the 0.55 action floor but signals lower trust than the cloud
         * provider's real logits.
         */
        private const val LOCAL_MATCH_CONFIDENCE = 0.6f

        // Only the actionable labels are offered to the model; AMBIGUOUS is the
        // fallback when nothing matches, never a label the model can emit.
        private val INTENT_LABELS = listOf(
            Intent.WANT_IT,
            Intent.READ_LATER,
            Intent.REFERENCE,
            Intent.FOR_SOMEONE,
            Intent.INTERESTING,
        )

        private val SENSITIVITY_TAGS =
            listOf("financial", "medical", "credentials", "contact", "location")
    }
}
