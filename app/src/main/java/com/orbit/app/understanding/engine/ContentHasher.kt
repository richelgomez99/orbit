package com.orbit.app.understanding.engine

import java.security.MessageDigest

/**
 * Deterministic SHA-256 content hashing for deduplication.
 *
 * Lowercase hex output; stable across JVM and Dalvik/ART runtimes.
 */
object ContentHasher {

    /** SHA-256 hash of raw bytes, returned as lowercase hex. */
    fun hash(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).toHexString()
    }

    /**
     * Normalise text then hash.
     *
     * Normalisation lowercases, trims leading/trailing whitespace, and collapses
     * internal runs of whitespace to a single space so that trivially
     * re-formatted copies of the same content produce the same hash.
     */
    fun normalizedTextHash(text: String): String {
        val normalized = text.lowercase().trim().replace(Regex("\\s+"), " ")
        return hash(normalized.toByteArray(Charsets.UTF_8))
    }

    private fun ByteArray.toHexString(): String = joinToString("") { byte ->
        "%02x".format(byte)
    }
}
