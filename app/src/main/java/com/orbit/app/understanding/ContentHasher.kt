package com.orbit.app.understanding

import java.security.MessageDigest
import java.util.Locale

object ContentHasher {

    fun hashArtifact(bytes: ByteArray): String = sha256Hex(bytes)

    fun hashNormalizedText(text: String): String = sha256Hex(normalizeText(text).toByteArray(Charsets.UTF_8))

    internal fun normalizeText(text: String): String = text
        .trim()
        .replace(WHITESPACE, " ")
        .lowercase(Locale.ROOT)

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val builder = StringBuilder(digest.size * 2)
        for (byte in digest) {
            val value = byte.toInt() and 0xFF
            builder.append(HEX[value ushr 4])
            builder.append(HEX[value and 0x0F])
        }
        return builder.toString()
    }

    private val WHITESPACE = Regex("\\s+")
    private val HEX = "0123456789abcdef".toCharArray()
}