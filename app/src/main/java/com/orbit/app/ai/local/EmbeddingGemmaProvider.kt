package com.orbit.app.ai.local

import android.util.Log
import com.orbit.app.ai.EmbeddingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.channels.FileChannel
import kotlin.math.sqrt

/**
 * Tokenizer seam for the EmbeddingGemma provider. Kept behind an interface
 * because the concrete Android tokenizer is still being chosen: the DJL
 * `ai.djl.huggingface:tokenizers` artifact ships only desktop (.dylib/.dll)
 * natives — no Android arm64 `.so` — so it can't be used here. Candidate Android
 * impls: a SentencePiece JNI over the model's `sentencepiece.model`, or a
 * verified DJL-Android native build. Verify the token id space matches
 * EmbeddingGemma's vocab on-device.
 */
interface GemmaTokenizer {
    /** Returns (token ids, attention mask of 1/0) for [text]. */
    fun encode(text: String): TokenizedText
}

data class TokenizedText(val ids: IntArray, val attentionMask: IntArray)

/** L2-normalize in place — required after Matryoshka truncation breaks unit norm. */
private fun l2Normalize(v: FloatArray) {
    var norm = 0f
    for (x in v) norm += x * x
    norm = sqrt(norm)
    if (norm > 0f) for (i in v.indices) v[i] /= norm
}

/**
 * Spec-020 Phase B — on-device text embeddings via EmbeddingGemma-300m on the
 * LiteRT `Interpreter` (raw TFLite). Unlike MediaPipe `LlmInference` this has no
 * single-engine-per-process constraint, so it coexists with the BYOM Gemma
 * engine in `:ml`. No network.
 *
 * The exact tensor signature of the `litert-community` `.tflite` is only knowable
 * by running it, so this provider **inspects the interpreter at load** and adapts:
 * 1 or 2 integer inputs (ids, optional attention mask), int32 or int64; a 2-D
 * `[1, dim]` pooled output used as-is, or a 3-D `[1, seq, dim]` token output that
 * is mean-pooled here. Actual shapes are logged at load so they can be confirmed
 * on-device. Anything it can't handle → `embed()` returns null (degrade, never
 * crash). Output is Matryoshka-truncated to [outDim] and re-normalized.
 *
 * Prompt prefixes are load-bearing for EmbeddingGemma quality — queries and
 * documents MUST use their distinct prefixes.
 */
class EmbeddingGemmaProvider private constructor(
    private val interpreter: Interpreter,
    private val tokenizer: GemmaTokenizer,
    private val seqLen: Int,
    private val inputCount: Int,
    private val inputIsLong: Boolean,
    private val outputRank: Int,
    private val nativeDim: Int,
) {

    suspend fun embed(
        text: String,
        prefix: String = QUERY_PREFIX,
        outDim: Int = DEFAULT_DIM,
    ): EmbeddingResult? = withContext(Dispatchers.Default) {
        runCatching {
            val enc = tokenizer.encode(prefix + text)
            val ids = enc.ids
            val mask = enc.attentionMask

            val idsArr = IntArray(seqLen)
            val maskArr = IntArray(seqLen)
            val n = minOf(ids.size, seqLen)
            for (i in 0 until n) {
                idsArr[i] = ids[i]
                maskArr[i] = if (i < mask.size) mask[i] else 1
            }

            val inputs: Array<Any> = buildInputs(idsArr, maskArr)
            val output: Any = allocateOutput()
            interpreter.runForMultipleInputsOutputs(inputs, mapOf(0 to output))

            val pooled = readPooled(output, maskArr)
            var v = if (outDim < pooled.size) pooled.copyOf(outDim) else pooled
            l2Normalize(v)
            EmbeddingResult(vector = v, modelLabel = MODEL_LABEL, dimensionality = v.size)
        }.onFailure { Log.w(TAG, "embed failed", it) }.getOrNull()
    }

    private fun buildInputs(ids: IntArray, mask: IntArray): Array<Any> {
        fun wrap(a: IntArray): Any =
            if (inputIsLong) arrayOf(LongArray(a.size) { a[it].toLong() }) else arrayOf(a)
        return if (inputCount >= 2) arrayOf(wrap(ids), wrap(mask)) else arrayOf(wrap(ids))
    }

    private fun allocateOutput(): Any =
        if (outputRank == 3) {
            Array(1) { Array(seqLen) { FloatArray(nativeDim) } }
        } else {
            Array(1) { FloatArray(nativeDim) }
        }

    @Suppress("UNCHECKED_CAST")
    private fun readPooled(output: Any, mask: IntArray): FloatArray {
        if (outputRank == 2) {
            return (output as Array<FloatArray>)[0]
        }
        // [1, seq, dim] token embeddings → masked mean-pool over the sequence.
        val tokens = (output as Array<Array<FloatArray>>)[0]
        val pooled = FloatArray(nativeDim)
        var count = 0
        for (t in tokens.indices) {
            if (t < mask.size && mask[t] == 0) continue
            val tok = tokens[t]
            for (d in 0 until nativeDim) pooled[d] += tok[d]
            count++
        }
        if (count > 0) for (d in pooled.indices) pooled[d] /= count
        return pooled
    }

    fun close() = runCatching { interpreter.close() }

    companion object {
        private const val TAG = "EmbeddingGemma"
        const val MODEL_LABEL = "embeddinggemma-300m-256"
        const val DEFAULT_DIM = 256
        /** EmbeddingGemma query prefix — required for retrieval quality. */
        const val QUERY_PREFIX = "task: search result | query: "
        /** EmbeddingGemma document prefix — required for retrieval quality. */
        const val DOCUMENT_PREFIX = "title: none | text: "

        /**
         * Load the interpreter + tokenizer and probe the tensor signature.
         * Returns null (logged) if the files are missing or the signature is
         * something this provider can't drive — so callers degrade gracefully.
         */
        fun createOrNull(modelFile: File, tokenizer: GemmaTokenizer): EmbeddingGemmaProvider? = runCatching {
            require(modelFile.exists()) { "model missing: ${modelFile.absolutePath}" }

            val buffer = FileInputStream(modelFile).channel.use { ch ->
                ch.map(FileChannel.MapMode.READ_ONLY, 0, modelFile.length())
            }
            val options = Interpreter.Options().apply {
                setNumThreads(Runtime.getRuntime().availableProcessors().coerceIn(2, 4))
            }
            val interpreter = Interpreter(buffer, options)

            val inputCount = interpreter.inputTensorCount
            val inTensor = interpreter.getInputTensor(0)
            val inShape = inTensor.shape()
            val seqLen = inShape.last()
            val inputIsLong = inTensor.dataType() == DataType.INT64
            val outShape = interpreter.getOutputTensor(0).shape()
            val outputRank = outShape.size
            val nativeDim = outShape.last()

            Log.i(
                TAG,
                "loaded: inputs=$inputCount inShape=${inShape.toList()} dtype=${inTensor.dataType()} " +
                    "seqLen=$seqLen outShape=${outShape.toList()} rank=$outputRank dim=$nativeDim",
            )

            EmbeddingGemmaProvider(
                interpreter = interpreter,
                tokenizer = tokenizer,
                seqLen = seqLen,
                inputCount = inputCount,
                inputIsLong = inputIsLong,
                outputRank = outputRank,
                nativeDim = nativeDim,
            )
        }.onFailure { Log.w(TAG, "createOrNull failed", it) }.getOrNull()
    }
}
