package com.orbit.app.capture

import com.orbit.app.net.CanonicalUrlHasher
import java.net.URI
import java.util.Locale

/** YouTube-family canonicalization wrapper for source identity and duplicate checks. */
object YoutubeUrlCanonicalizer {

    fun canonicalize(rawUrl: String?): String? {
        val trimmed = rawUrl?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        val uri = runCatching { URI(withScheme) }.getOrNull() ?: return null
        val host = uri.host?.lowercase(Locale.ROOT)?.removePrefix("www.") ?: return null
        val videoId = when {
            host == "youtu.be" -> uri.path.trim('/').takeIf { it.isNotBlank() }
            host == "youtube.com" || host.endsWith(".youtube.com") -> when {
                uri.path.startsWith("/shorts/") -> uri.path.removePrefix("/shorts/").substringBefore('/').takeIf { it.isNotBlank() }
                uri.path.startsWith("/embed/") -> uri.path.removePrefix("/embed/").substringBefore('/').takeIf { it.isNotBlank() }
                uri.path == "/watch" -> queryParam(uri.rawQuery, "v")
                else -> null
            }
            host == "youtube-nocookie.com" || host.endsWith(".youtube-nocookie.com") -> {
                uri.path.removePrefix("/embed/").substringBefore('/').takeIf { it.isNotBlank() }
            }
            else -> null
        }
        val canonical = if (videoId != null) {
            "https://youtube.com/watch?v=$videoId"
        } else {
            withScheme
        }
        return CanonicalUrlHasher.canonicalize(canonical)
    }

    private fun queryParam(rawQuery: String?, name: String): String? {
        if (rawQuery.isNullOrBlank()) return null
        return rawQuery.split('&').firstNotNullOfOrNull { part ->
            val key = part.substringBefore('=', missingDelimiterValue = "")
            if (key != name) return@firstNotNullOfOrNull null
            part.substringAfter('=', missingDelimiterValue = "").takeIf { it.isNotBlank() }
        }
    }
}
