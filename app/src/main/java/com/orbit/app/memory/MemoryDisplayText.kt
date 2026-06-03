package com.orbit.app.memory

import java.util.Locale

object MemoryDisplayText {
    private val statusKeywords = listOf(
        "cancel",
        "cancelled",
        "canceled",
        "reschedul",
        "postpon",
        "delayed",
        "conflict",
        "deadline",
        "due",
        "refund",
        "sold out",
        "waitlist",
        "closed",
        "expires",
        "need to",
        "remember to",
        "reply",
    )

    fun title(
        existingTitle: String?,
        text: String?,
        domain: String? = null,
        maxChars: Int = 96,
    ): String? {
        val status = text.statusSentence()
        if (!status.isNullOrBlank() && !existingTitle.containsMeaning(status)) {
            return status.compactInternal(maxChars)
        }
        return existingTitle
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.compactInternal(maxChars)
            ?: domain?.trim()?.takeIf { it.isNotBlank() }?.compactInternal(maxChars)
            ?: text.firstReadableLine()?.compactInternal(maxChars)
    }

    fun summary(
        existingSummary: String?,
        text: String?,
        maxChars: Int = 180,
    ): String? = existingSummary
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.compactInternal(maxChars)
        ?: text.salientSentence()?.compactInternal(maxChars)
        ?: text.firstReadableLine()?.compactInternal(maxChars)

    fun excerptForQuery(text: String?, query: String, maxChars: Int = 180): String {
        val trimmedQuery = query.trim()
        val candidate = text
            ?.lineSequence()
            ?.firstOrNull { line ->
                trimmedQuery.isNotBlank() && line.contains(trimmedQuery, ignoreCase = true)
            }
            ?: text.salientSentence()
            ?: text.orEmpty()
        return candidate.compactInternal(maxChars)
    }

    fun compact(value: String, maxChars: Int): String = value.compactInternal(maxChars)

    private fun String?.salientSentence(): String? {
        val sentences = sentences() ?: return null
        return sentences.firstOrNull { it.hasStatusKeyword() } ?: sentences.firstOrNull()
    }

    private fun String?.statusSentence(): String? = sentences()
        ?.firstOrNull { it.hasStatusKeyword() }

    private fun String?.sentences(): List<String>? {
        val cleaned = normalize() ?: return null
        return cleaned
            .split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun String.hasStatusKeyword(): Boolean {
        val lower = lowercase(Locale.ROOT)
        return statusKeywords.any(lower::contains)
    }

    private fun String?.firstReadableLine(): String? = this
        ?.lineSequence()
        ?.map { it.trim() }
        ?.firstOrNull { it.isNotBlank() }
        ?.normalize()

    private fun String?.normalize(): String? = this
        ?.replace(Regex("\\s+"), " ")
        ?.trim()
        ?.takeIf { it.isNotBlank() }

    private fun String?.containsMeaning(other: String): Boolean {
        val current = this.normalize()?.lowercase(Locale.ROOT) ?: return false
        val target = other.normalize()?.lowercase(Locale.ROOT) ?: return false
        return current.contains(target) || target.contains(current)
    }

    private fun String.compactInternal(maxChars: Int): String {
        val singleLine = replace(Regex("\\s+"), " ").trim()
        if (singleLine.length <= maxChars) return singleLine
        val clipped = singleLine.take(maxChars).trimEnd()
        val lastSpace = clipped.lastIndexOf(' ')
        val wordSafe = if (lastSpace >= maxChars / 2) clipped.take(lastSpace) else clipped
        return "$wordSafe..."
    }
}
