package com.orbit.app.understanding.engine

import com.orbit.app.data.entity.ActiveIntentEntity
import com.orbit.app.understanding.domain.ActiveIntentStatus
import com.orbit.app.understanding.domain.CompletionKeyStatus
import com.orbit.app.understanding.domain.IntentCategory
import com.orbit.app.understanding.domain.UnderstandingResult
import java.util.UUID

class ActiveIntentResolver(
    private val classifier: IntentCategoryClassifier = IntentCategoryClassifier(),
    private val completionKeyExtractor: CompletionKeyExtractor = CompletionKeyExtractor(),
    private val sourceEvidenceRanker: SourceEvidenceRanker = SourceEvidenceRanker(),
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
    private val idProvider: () -> String = { UUID.randomUUID().toString() }
) {
    fun resolve(input: BasicCaptureInput, understandingResult: UnderstandingResult): ActiveIntentEntity? {
        val evidenceInput = IntentEvidenceInput(
            text = input.textContent,
            canonicalUrl = understandingResult.canonicalUrl ?: input.rawUrl,
            foregroundPackageName = input.foregroundPackageName,
            foregroundAppLabel = input.foregroundAppLabel,
            fallbackCategory = input.fallbackCategory
        )
        val categoryResult = classifier.classify(evidenceInput)
        if (categoryResult.category == IntentCategory.UNKNOWN) return null

        val completionKey = completionKeyExtractor.extract(categoryResult.category, evidenceInput)
        if (completionKey.status == CompletionKeyStatus.NOT_ACTIONABLE) return null

        val sourceEvidence = sourceEvidenceRanker.rank(
            canonicalUrl = evidenceInput.canonicalUrl,
            foregroundPackageName = evidenceInput.foregroundPackageName,
            foregroundAppLabel = evidenceInput.foregroundAppLabel,
            visibleText = evidenceInput.text
        )
        val now = clockMillis()
        return ActiveIntentEntity(
            intentId = "active-${understandingResult.captureId}-${categoryResult.category.name}".stableIntentId(),
            captureId = understandingResult.captureId,
            intentType = categoryResult.category.name,
            status = ActiveIntentStatus.ACTIVE.name,
            primaryEvidenceJson = primaryEvidenceJson(categoryResult, completionKey, sourceEvidence),
            primaryAction = primaryAction(categoryResult.category, completionKey.status),
            dueAt = inferDueAtMillis(input.textContent, now),
            expiresAt = inferExpiresAtMillis(categoryResult.category, input.textContent, now),
            resolutionReason = null,
            resolvedAt = null,
            userConfirmed = false,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun primaryAction(category: IntentCategory, status: CompletionKeyStatus): String = when (status) {
        CompletionKeyStatus.MISSING -> "Review missing evidence"
        CompletionKeyStatus.FOUND -> when (category) {
            IntentCategory.BUY_LATER_PRODUCT -> "Decide whether to buy"
            IntentCategory.RECIPE -> "Cook or save recipe"
            IntentCategory.QR_OR_BARCODE -> "Open or use code"
            IntentCategory.RECEIPT_OR_ORDER -> "Check order status"
            IntentCategory.EVENT_TICKET_RESERVATION -> "Prepare for event"
            IntentCategory.COUPON_OR_PROMO -> "Use coupon"
            IntentCategory.READ_OR_WATCH_LATER -> "Read or watch"
            IntentCategory.PLACE_OR_TRAVEL_IDEA -> "Plan visit"
            IntentCategory.GIFT_IDEA -> "Decide on gift"
            IntentCategory.CHAT_ACTION -> "Reply or act"
            IntentCategory.MAYBE_OLD_OR_INACTIVE -> "Review"
            IntentCategory.UNKNOWN -> "Review"
        }
        CompletionKeyStatus.NOT_ACTIONABLE -> "Review"
    }

    private fun inferDueAtMillis(text: String?, now: Long): Long? =
        if (text.orEmpty().contains(Regex("\\b(today|tomorrow|deadline|due)\\b", RegexOption.IGNORE_CASE))) {
            now + ONE_DAY_MILLIS
        } else {
            null
        }

    private fun inferExpiresAtMillis(category: IntentCategory, text: String?, now: Long): Long? = when {
        text.orEmpty().contains(Regex("\\b(expires|valid until|return by)\\b", RegexOption.IGNORE_CASE)) -> now + THIRTY_DAYS_MILLIS
        category == IntentCategory.COUPON_OR_PROMO -> now + THIRTY_DAYS_MILLIS
        category == IntentCategory.EVENT_TICKET_RESERVATION -> now + THIRTY_DAYS_MILLIS
        else -> null
    }

    private fun primaryEvidenceJson(
        categoryResult: IntentCategoryResult,
        completionKey: com.orbit.app.understanding.domain.CompletionKeyResult,
        sourceEvidence: SourceEvidenceRank
    ): String = buildString {
        append('{')
        append("\"category\":\"").append(categoryResult.category.name.escapeJson()).append("\",")
        append("\"categoryConfidence\":").append(categoryResult.confidence).append(',')
        append("\"categoryEvidence\":\"").append(categoryResult.evidenceSummary.escapeJson()).append("\",")
        append("\"completionKey\":").append(completionKey.toStorageJson()).append(',')
        append("\"source\":{")
        append("\"trustLevel\":\"").append(sourceEvidence.trustLevel.name).append("\",")
        append("\"evidenceBasis\":\"").append(sourceEvidence.evidenceBasis.name).append("\",")
        append("\"confidence\":").append(sourceEvidence.confidence).append(',')
        append("\"label\":\"").append(sourceEvidence.label.escapeJson()).append("\"")
        append('}')
        append('}')
    }

    private fun String.stableIntentId(): String = UUID.nameUUIDFromBytes(toByteArray(Charsets.UTF_8)).toString()

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

    private companion object {
        const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L
        const val THIRTY_DAYS_MILLIS = 30L * ONE_DAY_MILLIS
    }
}
