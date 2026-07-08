package com.orbit.app.ai.local

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import java.io.File

/**
 * Spec-020 Phase B — [GemmaTokenizer] backed by the HuggingFace (Rust)
 * tokenizer, loading EmbeddingGemma's `tokenizer.json` directly. The native
 * arm64 `.so` comes from the `ai.djl.android:tokenizer-native` AAR (the plain
 * JVM `tokenizers` jar ships only desktop natives — see build.gradle packaging).
 *
 * Loads `tokenizer.json` from `google/embeddinggemma-300m` (the
 * `litert-community` `.tflite` repo ships only `sentencepiece.model`, not the
 * JSON). Constructed only in `:ml` when the model is installed; never on the
 * unit-test JVM (the native lib isn't present there).
 *
 * Padding/truncation to the model's sequence length is done downstream by
 * [EmbeddingGemmaProvider]; this just returns raw ids + attention mask with
 * Gemma special tokens (`<bos>`=2 / `<eos>`=1) added.
 */
class DjlGemmaTokenizer private constructor(
    private val tokenizer: HuggingFaceTokenizer,
) : GemmaTokenizer {

    override fun encode(text: String): TokenizedText {
        val enc = tokenizer.encode(text)
        return TokenizedText(
            ids = enc.ids.toIntArray(),
            attentionMask = enc.attentionMask.toIntArray(),
        )
    }

    companion object {
        /** Null (never throws) if the tokenizer.json is missing or the native lib won't load. */
        fun createOrNull(tokenizerJson: File): DjlGemmaTokenizer? = runCatching {
            require(tokenizerJson.exists()) { "tokenizer.json missing: ${tokenizerJson.absolutePath}" }
            DjlGemmaTokenizer(HuggingFaceTokenizer.newInstance(tokenizerJson.toPath()))
        }.getOrNull()

        private fun LongArray.toIntArray(): IntArray = IntArray(size) { this[it].toInt() }
    }
}
