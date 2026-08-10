package com.orbit.app.diary

import com.orbit.app.data.ipc.ActiveIntentParcel
import com.orbit.app.understanding.domain.CompletionKeyStatus
import com.orbit.app.understanding.domain.IntentCategory
import com.orbit.app.understanding.domain.ResolutionReason
import org.json.JSONObject

sealed interface ActiveIntentUiState {
    data object Loading : ActiveIntentUiState
    data object Empty : ActiveIntentUiState
    data class Error(val message: String) : ActiveIntentUiState
    data class Ready(
        val groups: List<ActiveIntentGroup>,
        val activeCount: Int,
        val missingContextCount: Int
    ) : ActiveIntentUiState

    companion object {
        fun from(parcels: List<ActiveIntentParcel>): ActiveIntentUiState {
            val items = parcels
                .filter { it.status == "ACTIVE" }
                .map { it.toItem() }
                .sortedWith(compareBy<ActiveIntentItem> { it.lifecycleSort }.thenByDescending { it.updatedAtMillis })
            if (items.isEmpty()) return Empty

            val groups = items
                .groupBy { ActiveIntentGroupKey(it.lifecycleStatus, it.category) }
                .map { (key, groupedItems) ->
                    ActiveIntentGroup(
                        lifecycleStatus = key.lifecycleStatus,
                        category = key.category,
                        title = key.title,
                        items = groupedItems
                    )
                }
                .sortedWith(compareBy<ActiveIntentGroup> { it.lifecycleSort }.thenBy { it.title })

            return Ready(
                groups = groups,
                activeCount = items.size,
                missingContextCount = items.count { it.needsEscalation }
            )
        }
    }
}

data class ActiveIntentGroup(
    val lifecycleStatus: String,
    val category: String,
    val title: String,
    val items: List<ActiveIntentItem>
) {
    val lifecycleSort: Int = when (lifecycleStatus) {
        "NEEDS_CONTEXT" -> 0
        "MAYBE_OLD" -> 1
        else -> 2
    }
}

data class ActiveIntentItem(
    val intentId: String,
    val captureId: String,
    val category: String,
    val categoryLabel: String,
    val lifecycleStatus: String,
    val completionKeyStatus: String,
    val evidenceLabel: String,
    val evidenceSource: String,
    val evidenceKind: String,
    val evidenceExcerpt: String?,
    val sourceLabel: String,
    val reasonLabel: String,
    val clueLabel: String?,
    val askOrbitActionLabel: String,
    val openCaptureActionLabel: String,
    val addContextActionLabel: String,
    val actionLabel: String,
    val whyLabel: String,
    val guidanceLabel: String,
    val resolveActionLabel: String,
    val archiveActionLabel: String,
    val primaryAction: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val needsEscalation: Boolean,
    val defaultResolutionReason: ResolutionReason
) {
    val lifecycleSort: Int = when (lifecycleStatus) {
        "NEEDS_CONTEXT" -> 0
        "MAYBE_OLD" -> 1
        else -> 2
    }
}

private data class ActiveIntentGroupKey(
    val lifecycleStatus: String,
    val category: String
) {
    private val categoryLabel: String = category.toIntentCategoryOrNull()?.displayLabel() ?: category.toDisplayLabel()

    val title: String = when (lifecycleStatus) {
        "NEEDS_CONTEXT" -> "Needs your input · $categoryLabel"
        "MAYBE_OLD" -> "Maybe old · $categoryLabel"
        else -> categoryLabel
    }
}

private fun ActiveIntentParcel.toItem(): ActiveIntentItem {
    val evidence = primaryEvidenceJson.parseEvidence()
    val completion = runCatching { CompletionKeyStatus.valueOf(completionKeyStatus) }
        .getOrDefault(CompletionKeyStatus.MISSING)
    val categoryEnum = runCatching { IntentCategory.valueOf(intentType) }
        .getOrDefault(IntentCategory.UNKNOWN)
    val lifecycle = when {
        categoryEnum == IntentCategory.MAYBE_OLD_OR_INACTIVE -> "MAYBE_OLD"
        completion.requiresMoreContext(categoryEnum) -> "NEEDS_CONTEXT"
        else -> status
    }

    return ActiveIntentItem(
        intentId = intentId,
        captureId = captureId,
        category = categoryEnum.name,
        categoryLabel = categoryEnum.displayLabel(),
        lifecycleStatus = lifecycle,
        completionKeyStatus = completion.name,
        evidenceLabel = evidence.displayLabel(categoryEnum),
        evidenceSource = evidence.source,
        evidenceKind = evidence.kind,
        evidenceExcerpt = evidence.excerpt,
        sourceLabel = evidence.sourceLabel(createdAtMillis),
        reasonLabel = categoryEnum.reasonLabel(evidence),
        clueLabel = evidence.clueLabel(),
        askOrbitActionLabel = if (evidence.kindRaw == "ORBIT_REVIEW") "Refresh review" else "Ask Orbit",
        openCaptureActionLabel = "View capture",
        addContextActionLabel = "Add context",
        actionLabel = primaryAction?.toDisplayLabel()
            ?: categoryEnum.defaultActionLabel(),
        whyLabel = categoryEnum.whyLabel(),
        guidanceLabel = categoryEnum.guidanceLabel(completion),
        resolveActionLabel = categoryEnum.resolveActionLabel(),
        archiveActionLabel = categoryEnum.archiveActionLabel(),
        primaryAction = primaryAction,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        needsEscalation = completion.requiresMoreContext(categoryEnum),
        defaultResolutionReason = categoryEnum.defaultResolutionReason()
    )
}

private fun CompletionKeyStatus.requiresMoreContext(category: IntentCategory): Boolean =
    category != IntentCategory.MAYBE_OLD_OR_INACTIVE &&
        (this == CompletionKeyStatus.MISSING || this == CompletionKeyStatus.NEEDS_ESCALATION)

private data class EvidenceDisplay(
    val kindRaw: String,
    val kind: String,
    val label: String,
    val source: String,
    val excerpt: String?,
    val reason: String?
)

private fun EvidenceDisplay.displayLabel(category: IntentCategory): String {
    val categoryLabel = category.displayLabel()
    val cleaned = label.trim().ifBlank { categoryLabel }
    if (kindRaw == "COMPLETION_KEY" || kindRaw == "ORBIT_REVIEW") {
        return when (category) {
            IntentCategory.CHAT_ACTION -> "Message follow-up"
            IntentCategory.UNKNOWN -> "Capture to review"
            else -> categoryLabel
        }
    }
    return if (cleaned == category.name || cleaned == category.name.toDisplayLabel() || cleaned == categoryLabel || cleaned.isInternalEvidenceLabel()) {
        when (category) {
            IntentCategory.CHAT_ACTION -> "Message follow-up"
            IntentCategory.UNKNOWN -> "Capture to review"
            else -> categoryLabel
        }
    } else {
        cleaned
    }
}

private fun String.isInternalEvidenceLabel(): Boolean = split(',')
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .let { parts ->
        parts.isNotEmpty() && parts.all {
            it == "source_app_label" || it == "app_category" || it == "canonical_url"
        }
    }

private fun String.parseEvidence(): EvidenceDisplay {
    val json = runCatching { JSONObject(this) }.getOrNull()
    val rawKind = json?.optString("kind")?.takeIf { it.isNotBlank() } ?: "LOCAL_CLUE"
    val label = json?.optString("label")?.takeIf { it.isNotBlank() } ?: ""
    return EvidenceDisplay(
        kindRaw = rawKind,
        kind = rawKind.toEvidenceKindLabel(label),
        label = label,
        source = json?.optString("source")?.takeIf { it.isNotBlank() }?.toEvidenceSourceLabel() ?: "Local evidence",
        excerpt = json?.optString("excerpt")?.takeIf { it.isNotBlank() },
        reason = json?.optString("reason")?.takeIf { it.isNotBlank() }
    )
}

private fun String.toEvidenceKindLabel(label: String): String = when (trim().uppercase()) {
    "COMPLETION_KEY" -> label.trim().takeIf { it.isNotBlank() }
        ?.toDisplayLabel()
        ?.lowercase()
        ?.let { "Found $it" }
        ?: "Found detail"
    "ORBIT_REVIEW" -> "Decision brief"
    "CATEGORY" -> "Category clue"
    "APP_CONTEXT" -> "App context"
    "LIMITED" -> "Limited clue"
    else -> toDisplayLabel()
}

private fun String.toEvidenceSourceLabel(): String = when (this.trim().lowercase()) {
    "local", "basic" -> "Local signals"
    "local_regex" -> "Local text"
    "foreground_app", "source_app_label", "app_context" -> "App context"
    "timestamp" -> "Capture age"
    "messaging_source" -> "Messages"
    "event_ticket_text" -> "Event text"
    "orbit_review" -> "Orbit review"
    "active_intent" -> "Cleanup"
    else -> toDisplayLabel()
}

private fun EvidenceDisplay.sourceLabel(createdAtMillis: Long): String =
    "From: $source · $kind · ${createdAtMillis.toShortTimeLabel()}"

private fun EvidenceDisplay.clueLabel(): String? = excerpt
    ?.trim()
    ?.takeIf { it.isNotBlank() }
    ?.let { "Capture clue: $it" }

private fun Long.toShortTimeLabel(): String = java.time.Instant.ofEpochMilli(this)
    .atZone(java.time.ZoneId.systemDefault())
    .format(java.time.format.DateTimeFormatter.ofPattern("h:mma", java.util.Locale.US))
    .lowercase(java.util.Locale.US)

private fun String.toDisplayLabel(): String = lowercase()
    .replace('_', ' ')
    .split(' ')
    .filter { it.isNotBlank() }
    .joinToString(" ") { token -> token.replaceFirstChar { it.titlecase() } }

private fun String.toIntentCategoryOrNull(): IntentCategory? =
    runCatching { IntentCategory.valueOf(this) }.getOrNull()

private fun IntentCategory.displayLabel(): String = when (this) {
    IntentCategory.BUY_LATER_PRODUCT -> "Buy later"
    IntentCategory.RECIPE -> "Recipe"
    IntentCategory.QR_OR_BARCODE -> "QR or barcode"
    IntentCategory.RECEIPT_OR_ORDER -> "Receipt or order"
    IntentCategory.EVENT_TICKET_RESERVATION -> "Event or reservation"
    IntentCategory.COUPON_OR_PROMO -> "Coupon or promo"
    IntentCategory.READ_OR_WATCH_LATER -> "Read or watch later"
    IntentCategory.PLACE_OR_TRAVEL_IDEA -> "Place or trip idea"
    IntentCategory.GIFT_IDEA -> "Gift idea"
    IntentCategory.CHAT_ACTION -> "Message follow-up"
    IntentCategory.MAYBE_OLD_OR_INACTIVE -> "Maybe old"
    IntentCategory.UNKNOWN -> "Review"
}

private fun IntentCategory.defaultActionLabel(): String = when (this) {
    IntentCategory.BUY_LATER_PRODUCT -> "Buy or skip"
    IntentCategory.RECIPE -> "Cook or archive"
    IntentCategory.QR_OR_BARCODE -> "Use or archive"
    IntentCategory.RECEIPT_OR_ORDER -> "Check order"
    IntentCategory.EVENT_TICKET_RESERVATION -> "Add or archive"
    IntentCategory.COUPON_OR_PROMO -> "Use or ignore"
    IntentCategory.READ_OR_WATCH_LATER -> "Read or archive"
    IntentCategory.PLACE_OR_TRAVEL_IDEA -> "Plan or skip"
    IntentCategory.GIFT_IDEA -> "Pick or skip"
    IntentCategory.CHAT_ACTION -> "Reply or dismiss"
    IntentCategory.MAYBE_OLD_OR_INACTIVE -> "Keep or archive"
    IntentCategory.UNKNOWN -> "Review"
}

private fun IntentCategory.guidanceLabel(completion: CompletionKeyStatus): String {
    if (completion == CompletionKeyStatus.NEEDS_ESCALATION) {
        return when (this) {
            IntentCategory.EVENT_TICKET_RESERVATION -> "Open the capture, add the date or venue if it matters, or clear it."
            IntentCategory.CHAT_ACTION -> "Open the capture, add who needs a reply if it matters, or mark it handled."
            IntentCategory.UNKNOWN -> "Open the capture, add why it matters, or clear it."
            else -> "Open the capture, add the missing context, or clear it."
        }
    }
    return when (this) {
        IntentCategory.BUY_LATER_PRODUCT -> "Buy it, save it for later, or clear it if you are no longer considering it."
        IntentCategory.RECIPE -> "Cook it, save it for later, or clear it if you do not need it."
        IntentCategory.QR_OR_BARCODE -> "Open or use the code, then mark it done."
        IntentCategory.RECEIPT_OR_ORDER -> "Check the order, return window, or receipt detail."
        IntentCategory.EVENT_TICKET_RESERVATION -> "Save the details, attend it, or clear it if you do not need it."
        IntentCategory.COUPON_OR_PROMO -> "Use the promo before it expires, or clear it if it is not useful."
        IntentCategory.READ_OR_WATCH_LATER -> "Read or watch it, then mark it done."
        IntentCategory.PLACE_OR_TRAVEL_IDEA -> "Plan the visit, save it, or clear it if it is not useful."
        IntentCategory.GIFT_IDEA -> "Buy it, keep it as an idea, or clear it if it is not useful."
        IntentCategory.CHAT_ACTION -> "Reply or mark the message handled."
        IntentCategory.MAYBE_OLD_OR_INACTIVE -> "Keep it if it still matters, otherwise clear it."
        IntentCategory.UNKNOWN -> "Review the screenshot and decide whether to keep it."
    }
}

private fun IntentCategory.reasonLabel(evidence: EvidenceDisplay): String =
    evidence.reason?.let { "Why it appears: $it" } ?: defaultReasonLabel(evidence)

private fun IntentCategory.defaultReasonLabel(evidence: EvidenceDisplay): String = when (this) {
    IntentCategory.BUY_LATER_PRODUCT -> "Why it appears: this looks like a product or price you may want to buy, save, or skip."
    IntentCategory.RECIPE -> "Why it appears: this looks like recipe text worth cooking, saving, or clearing."
    IntentCategory.QR_OR_BARCODE -> "Why it appears: this looks like a code that may still need to be opened or used."
    IntentCategory.RECEIPT_OR_ORDER -> "Why it appears: this looks like order or receipt information that may need checking."
    IntentCategory.EVENT_TICKET_RESERVATION -> "Why it appears: this looks like event, ticket, reservation, or booking information."
    IntentCategory.COUPON_OR_PROMO -> "Why it appears: this looks like a promo or discount that may expire."
    IntentCategory.READ_OR_WATCH_LATER -> "Why it appears: this looks like something saved to read or watch later."
    IntentCategory.PLACE_OR_TRAVEL_IDEA -> "Why it appears: this looks like a place or trip idea worth planning or clearing."
    IntentCategory.GIFT_IDEA -> "Why it appears: this looks like a gift idea that may need a buy-or-skip decision."
    IntentCategory.CHAT_ACTION -> when {
        evidence.kindRaw == "COMPLETION_KEY" -> "Why it appears: this looks like a message and Orbit found a date or time in the text."
        evidence.source == "Messages" -> "Why it appears: this came from a message-like source and may need a reply."
        else -> "Why it appears: this looks like a message that may need a reply or follow-up."
    }
    IntentCategory.MAYBE_OLD_OR_INACTIVE -> "Why it appears: this looks old enough that it may be safe to clear from follow-up."
    IntentCategory.UNKNOWN -> "Why it appears: Orbit has local clues, but not enough context to know the next action yet."
}

private fun IntentCategory.whyLabel(): String = when (this) {
    IntentCategory.BUY_LATER_PRODUCT -> "Orbit found a product that may need a buy-or-skip decision."
    IntentCategory.RECIPE -> "Orbit found recipe details that may be worth saving or trying."
    IntentCategory.QR_OR_BARCODE -> "Orbit found a code that may still need to be opened or used."
    IntentCategory.RECEIPT_OR_ORDER -> "Orbit found receipt or order details that may need checking."
    IntentCategory.EVENT_TICKET_RESERVATION -> "Orbit found event or reservation details in this capture."
    IntentCategory.COUPON_OR_PROMO -> "Orbit found a promo that may expire or need a decision."
    IntentCategory.READ_OR_WATCH_LATER -> "Orbit found something you may have saved to read or watch later."
    IntentCategory.PLACE_OR_TRAVEL_IDEA -> "Orbit found a place or trip idea that may need a decision."
    IntentCategory.GIFT_IDEA -> "Orbit found a gift idea that may need a buy-or-skip decision."
    IntentCategory.CHAT_ACTION -> "Orbit found a message that may still need a reply or follow-up."
    IntentCategory.MAYBE_OLD_OR_INACTIVE -> "Orbit thinks this may be old enough to clear from follow-up."
    IntentCategory.UNKNOWN -> "Orbit found a capture that may need a quick keep-or-clear decision."
}

private fun IntentCategory.resolveActionLabel(): String = when (this) {
    IntentCategory.BUY_LATER_PRODUCT,
    IntentCategory.GIFT_IDEA -> "Bought or saved"
    IntentCategory.RECIPE -> "Cooked or saved"
    IntentCategory.QR_OR_BARCODE -> "Used code"
    IntentCategory.RECEIPT_OR_ORDER -> "Checked"
    IntentCategory.EVENT_TICKET_RESERVATION -> "Saved or attended"
    IntentCategory.COUPON_OR_PROMO -> "Used promo"
    IntentCategory.READ_OR_WATCH_LATER -> "Read or watched"
    IntentCategory.PLACE_OR_TRAVEL_IDEA -> "Planned or saved"
    IntentCategory.CHAT_ACTION -> "Replied or handled"
    IntentCategory.MAYBE_OLD_OR_INACTIVE -> "Still useful"
    IntentCategory.UNKNOWN -> "Mark handled"
}

private fun IntentCategory.archiveActionLabel(): String = when (this) {
    IntentCategory.MAYBE_OLD_OR_INACTIVE -> "No longer needed"
    else -> "Not needed"
}

private fun IntentCategory.defaultResolutionReason(): ResolutionReason = when (this) {
    IntentCategory.BUY_LATER_PRODUCT,
    IntentCategory.GIFT_IDEA -> ResolutionReason.BOUGHT
    IntentCategory.RECIPE -> ResolutionReason.COOKED
    IntentCategory.READ_OR_WATCH_LATER -> ResolutionReason.READ_OR_WATCHED
    IntentCategory.PLACE_OR_TRAVEL_IDEA,
    IntentCategory.EVENT_TICKET_RESERVATION -> ResolutionReason.VISITED
    IntentCategory.CHAT_ACTION -> ResolutionReason.REPLIED_OR_DONE
    else -> ResolutionReason.NOT_INTERESTED
}
