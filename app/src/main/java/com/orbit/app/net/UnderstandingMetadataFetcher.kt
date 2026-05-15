package com.orbit.app.net

import com.orbit.app.understanding.domain.UnderstandingMode
import com.orbit.app.understanding.engine.ContentHasher
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

data class UnderstandingMetadata(
    val finalUrl: String,
    val title: String?,
    val description: String?,
    val canonicalUrl: String?,
    val type: String?,
    val normalizedContentHashHex: String?,
    val evidenceJson: String
)

class UnderstandingMetadataFetcher(
    private val client: OkHttpClient = SafeOkHttpClient.build(),
    private val readabilityExtractor: ReadabilityExtractor = ReadabilityExtractor(),
    private val maxResponseBytes: Int = 128 * 1024
) {
    fun fetch(url: String, mode: UnderstandingMode): UnderstandingMetadata {
        require(mode != UnderstandingMode.BASIC) { "Network metadata fetching requires Smart or Deep escalation." }
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Range", "bytes=0-${maxResponseBytes - 1}")
            .header("Accept", "text/html,application/xhtml+xml")
            .removeHeader("Cookie")
            .removeHeader("Authorization")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("metadata fetch failed with HTTP ${response.code}")
            val finalUrl = response.request.url.toString()
            val html = response.body?.bytes()?.let { bytes ->
                String(bytes.copyOfRange(0, bytes.size.coerceAtMost(maxResponseBytes)), Charsets.UTF_8)
            }.orEmpty()
            val document = Jsoup.parse(html, finalUrl)
            val ogTitle = document.selectFirst("meta[property=og:title]")?.attr("content")?.trim().orEmpty()
            val ogDescription = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim().orEmpty()
            val ogUrl = document.selectFirst("meta[property=og:url]")?.attr("content")?.trim().orEmpty()
            val ogType = document.selectFirst("meta[property=og:type]")?.attr("content")?.trim().orEmpty()
            val canonical = document.selectFirst("link[rel=canonical]")?.attr("abs:href")?.trim().orEmpty()
            val readable = if (mode == UnderstandingMode.SMART || mode == UnderstandingMode.DEEP) {
                readabilityExtractor.extract(finalUrl, html).readableHtml
            } else {
                null
            }
            val normalizedHash = readable
                ?.let { Jsoup.parse(it).text() }
                ?.takeIf { it.isNotBlank() }
                ?.let(ContentHasher::normalizedTextHash)

            return UnderstandingMetadata(
                finalUrl = finalUrl,
                title = ogTitle.ifBlank { document.title().takeIf { it.isNotBlank() } },
                description = ogDescription.ifBlank { null },
                canonicalUrl = ogUrl.ifBlank { canonical.ifBlank { finalUrl } },
                type = ogType.ifBlank { null },
                normalizedContentHashHex = normalizedHash,
                evidenceJson = buildEvidenceJson(
                    title = ogTitle.ifBlank { document.title() },
                    description = ogDescription,
                    canonicalUrl = ogUrl.ifBlank { canonical },
                    type = ogType
                )
            )
        }
    }

    private fun buildEvidenceJson(
        title: String,
        description: String,
        canonicalUrl: String,
        type: String
    ): String = "{\"ogTitle\":\"${title.escapeJson()}\",\"ogDescription\":\"${description.escapeJson()}\",\"canonicalUrl\":\"${canonicalUrl.escapeJson()}\",\"ogType\":\"${type.escapeJson()}\"}"

    private fun String.escapeJson(): String = buildString {
        for (character in this@escapeJson) {
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(character)
            }
        }
    }
}
