package com.orbit.app.ai.local

import android.content.Context
import android.util.Log
import com.orbit.app.ai.EmbeddingResult
import com.orbit.app.net.ModelDownloadStore

/**
 * Spec-020 Phase B — per-process singleton that lazily loads the EmbeddingGemma
 * `Interpreter` + tokenizer from the installed model files and serves
 * embeddings. Loaded once and reused (the `Interpreter` is cheap to keep). Runs
 * in whatever process calls it; the intended home is `:ml` (where the DB and
 * other inference live) — the LiteRT `Interpreter` has no single-engine hazard,
 * so it coexists with the BYOM LLM engine.
 *
 * Everything degrades to null: not installed, tokenizer/interpreter load failure,
 * or an unhandled tensor signature all return null rather than throw, so callers
 * (clustering, grouping, recall) simply fall back to their non-semantic path.
 */
object EmbeddingGemmaHolder {

    const val MODEL_ID = "embeddinggemma-300m"
    private const val TAG = "EmbeddingGemma"

    @Volatile private var cached: EmbeddingGemmaProvider? = null
    @Volatile private var loadFailed = false

    fun isInstalled(context: Context): Boolean =
        ModelDownloadStore.isFullyInstalled(context, MODEL_ID)

    /** Query-side embedding (for a recall/search question). */
    suspend fun embedQuery(context: Context, text: String): EmbeddingResult? =
        provider(context)?.embed(text, EmbeddingGemmaProvider.QUERY_PREFIX)

    /** Document-side embedding (for a stored capture/fact). */
    suspend fun embedDocument(context: Context, text: String): EmbeddingResult? =
        provider(context)?.embed(text, EmbeddingGemmaProvider.DOCUMENT_PREFIX)

    @Synchronized
    private fun provider(context: Context): EmbeddingGemmaProvider? {
        cached?.let { return it }
        if (loadFailed) return null
        val ctx = context.applicationContext
        if (!isInstalled(ctx)) return null // not a failure — a later call retries after install

        val modelFile = ModelDownloadStore.modelFile(ctx, MODEL_ID)
        val tokenizerFile = ModelDownloadStore.tokenizerFile(ctx, MODEL_ID)
        if (tokenizerFile == null) {
            loadFailed = true
            Log.w(TAG, "no tokenizer file configured for $MODEL_ID")
            return null
        }
        val tokenizer = DjlGemmaTokenizer.createOrNull(tokenizerFile)
        val built = tokenizer?.let { EmbeddingGemmaProvider.createOrNull(modelFile, it) }
        if (built == null) {
            loadFailed = true // genuine load failure — don't hammer it every call
            Log.w(TAG, "EmbeddingGemma load failed (tokenizer=${tokenizer != null})")
            return null
        }
        cached = built
        return built
    }

    /** Test/reset seam — drops the cached engine so a re-download reloads. */
    @Synchronized
    fun reset() {
        runCatching { cached?.close() }
        cached = null
        loadFailed = false
    }
}
