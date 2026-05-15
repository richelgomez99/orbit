package com.orbit.app.net

import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder
import java.security.MessageDigest
import java.util.Locale

/**
 * T066a — pure URL canonicalizer + SHA-256 hasher used for continuation dedupe.
 *
 * `hash(rawUrl)` returns `sha256(canonicalize(rawUrl))` as lowercase hex.
 *
 * Canonicalization rules (see tasks.md T066a / spec.md Clarification Q2):
 * - scheme/host lowercased
 * - leading `www.` stripped from hosts
 * - fragment stripped
 * - query params named `utm_*`, `fbclid`, `gclid` stripped
 * - remaining query params sorted lexicographically by name, preserving value
 * - trailing `/` on the path stripped, including root `/`
 * - default ports (80/443) stripped
 *
 * The function is intentionally tolerant — malformed URLs still produce a stable
 * hash of the raw input, so the pure-function contract "same input ⇒ same output"
 * holds even when validation has already rejected the URL.
 */
object CanonicalUrlHasher {

    private val TRACKING_PARAM_EXACT: Set<String> = setOf("fbclid", "gclid")
    private const val TRACKING_PARAM_PREFIX: String = "utm_"
    private const val MAX_UNWRAP_DEPTH: Int = 3

    fun hash(rawUrl: String): String {
        val canonical = canonicalize(rawUrl)
        return sha256Hex(canonical)
    }

    fun unwrapKnownRedirect(rawUrl: String): String = unwrapKnownRedirect(rawUrl.trim(), depth = 0)

    internal fun canonicalize(rawUrl: String): String {
        val trimmed = unwrapKnownRedirect(rawUrl)
        val uri: URI = try {
            URI(trimmed)
        } catch (_: URISyntaxException) {
            return trimmed.lowercase(Locale.ROOT)
        }

        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return trimmed.lowercase(Locale.ROOT)
        val host = uri.host
            ?.lowercase(Locale.ROOT)
            ?.removePrefix("www.")
            ?: return trimmed.lowercase(Locale.ROOT)

        val port = uri.port
        val portPart = when {
            port == -1 -> ""
            scheme == "https" && port == 443 -> ""
            scheme == "http" && port == 80 -> ""
            else -> ":$port"
        }

        val rawPath = uri.rawPath.orEmpty()
        val path = when {
            rawPath.isEmpty() -> ""
            rawPath == "/" -> ""
            rawPath.endsWith('/') -> rawPath.trimEnd('/')
            else -> rawPath
        }

        val queryPart = canonicalQuery(uri.rawQuery)

        return buildString {
            append(scheme)
            append("://")
            append(host)
            append(portPart)
            append(path)
            if (queryPart.isNotEmpty()) {
                append('?')
                append(queryPart)
            }
        }
    }

    private fun unwrapKnownRedirect(rawUrl: String, depth: Int): String {
        if (rawUrl.isBlank() || depth >= MAX_UNWRAP_DEPTH) return rawUrl
        val uri = try {
            URI(rawUrl)
        } catch (_: URISyntaxException) {
            return rawUrl
        }
        val host = uri.host?.lowercase(Locale.ROOT)?.trimEnd('.') ?: return rawUrl
        val path = uri.rawPath.orEmpty()

        val target = when {
            isGoogleAmpPath(host, path) -> googleAmpTarget(path)
            isGoogleRedirectPath(host, path) -> firstHttpQueryTarget(uri.rawQuery, "url", "q", "u")
            isYouTubeRedirectPath(host, path) -> firstHttpQueryTarget(uri.rawQuery, "q", "url")
            else -> null
        } ?: return rawUrl

        if (target == rawUrl) return rawUrl
        return unwrapKnownRedirect(target, depth + 1)
    }

    private fun isGoogleRedirectPath(host: String, path: String): Boolean =
        isGoogleHost(host) && (path == "/url" || path == "/imgres")

    private fun isGoogleAmpPath(host: String, path: String): Boolean =
        isGoogleHost(host) && (path.startsWith("/amp/s/") || path.startsWith("/amp/"))

    private fun isGoogleHost(host: String): Boolean =
        host == "google.com" || host.endsWith(".google.com") || host.startsWith("www.google.")

    private fun isYouTubeRedirectPath(host: String, path: String): Boolean =
        (host == "youtube.com" || host.endsWith(".youtube.com")) && path == "/redirect"

    private fun googleAmpTarget(path: String): String? {
        val rawTarget = when {
            path.startsWith("/amp/s/") -> "https://" + path.removePrefix("/amp/s/")
            path.startsWith("/amp/") -> "http://" + path.removePrefix("/amp/")
            else -> null
        } ?: return null
        return rawTarget.takeIf { it.isHttpUrl() }
    }

    private fun firstHttpQueryTarget(rawQuery: String?, vararg names: String): String? {
        if (rawQuery.isNullOrEmpty()) return null
        val wanted = names.map { it.lowercase(Locale.ROOT) }.toSet()
        return rawQuery.split('&').firstNotNullOfOrNull { piece ->
            val idx = piece.indexOf('=')
            if (idx <= 0) return@firstNotNullOfOrNull null
            val name = piece.substring(0, idx).lowercase(Locale.ROOT)
            if (name !in wanted) return@firstNotNullOfOrNull null
            val decoded = decodeQueryComponent(piece.substring(idx + 1)).trim()
            decoded.takeIf { it.isHttpUrl() }
        }
    }

    private fun decodeQueryComponent(value: String): String =
        runCatching { URLDecoder.decode(value, Charsets.UTF_8.name()) }.getOrDefault(value)

    private fun String.isHttpUrl(): Boolean {
        val scheme = runCatching { URI(this).scheme?.lowercase(Locale.ROOT) }.getOrNull()
        return scheme == "http" || scheme == "https"
    }

    private fun canonicalQuery(rawQuery: String?): String {
        if (rawQuery.isNullOrEmpty()) return ""
        val pairs = rawQuery.split('&')
            .mapNotNull { piece ->
                if (piece.isEmpty()) return@mapNotNull null
                val idx = piece.indexOf('=')
                val name = if (idx < 0) piece else piece.substring(0, idx)
                val value = if (idx < 0) null else piece.substring(idx + 1)
                val lowerName = name.lowercase(Locale.ROOT)
                if (lowerName.startsWith(TRACKING_PARAM_PREFIX)) return@mapNotNull null
                if (lowerName in TRACKING_PARAM_EXACT) return@mapNotNull null
                name to value
            }
            .sortedWith(compareBy({ it.first }, { it.second ?: "" }))

        return pairs.joinToString("&") { (n, v) ->
            if (v == null) n else "$n=$v"
        }
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(digest.size * 2)
        for (b in digest) {
            val v = b.toInt() and 0xFF
            sb.append(HEX[v ushr 4])
            sb.append(HEX[v and 0x0F])
        }
        return sb.toString()
    }

    private val HEX = "0123456789abcdef".toCharArray()
}
