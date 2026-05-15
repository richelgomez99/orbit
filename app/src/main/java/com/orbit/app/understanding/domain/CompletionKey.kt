package com.orbit.app.understanding.domain

enum class CompletionKeyStatus {
    FOUND,
    MISSING,
    NOT_ACTIONABLE
}

data class CompletionKeyResult(
    val category: IntentCategory,
    val status: CompletionKeyStatus,
    val key: CompletionKey?,
    val missingFields: List<String> = emptyList(),
    val evidenceSummary: String? = null,
    val confidence: Float = 0f
) {
    fun toStorageJson(): String = buildString {
        append('{')
        append("\"category\":\"").append(category.name).append("\",")
        append("\"status\":\"").append(status.name).append("\",")
        append("\"confidence\":").append(confidence).append(',')
        append("\"missingFields\":[")
        append(missingFields.joinToString(",") { "\"${it.escapeJson()}\"" })
        append("],")
        append("\"evidenceSummary\":")
        append(evidenceSummary?.let { "\"${it.escapeJson()}\"" } ?: "null")
        append(',')
        append("\"key\":")
        append(key?.toStorageJson() ?: "null")
        append('}')
    }
}

sealed interface CompletionKey {
    val type: IntentCategory
    fun toStorageJson(): String

    data class Product(
        val productName: String,
        val merchantOrSource: String?,
        val visiblePrice: String?,
        val urlOrDeepLink: String?
    ) : CompletionKey {
        override val type: IntentCategory = IntentCategory.BUY_LATER_PRODUCT
        override fun toStorageJson(): String = jsonObject(
            "type" to type.name,
            "productName" to productName,
            "merchantOrSource" to merchantOrSource,
            "visiblePrice" to visiblePrice,
            "urlOrDeepLink" to urlOrDeepLink
        )
    }

    data class Recipe(
        val recipeTitle: String,
        val source: String?,
        val ingredients: List<String>
    ) : CompletionKey {
        override val type: IntentCategory = IntentCategory.RECIPE
        override fun toStorageJson(): String = jsonObject(
            "type" to type.name,
            "recipeTitle" to recipeTitle,
            "source" to source,
            "ingredients" to ingredients
        )
    }

    data class Code(
        val decodedPayload: String,
        val expiry: String?,
        val context: String?
    ) : CompletionKey {
        override val type: IntentCategory = IntentCategory.QR_OR_BARCODE
        override fun toStorageJson(): String = jsonObject(
            "type" to type.name,
            "decodedPayload" to decodedPayload,
            "expiry" to expiry,
            "context" to context
        )
    }

    data class ReceiptOrder(
        val merchant: String,
        val orderId: String?,
        val total: String?,
        val date: String?,
        val trackingOrReturnClue: String?
    ) : CompletionKey {
        override val type: IntentCategory = IntentCategory.RECEIPT_OR_ORDER
        override fun toStorageJson(): String = jsonObject(
            "type" to type.name,
            "merchant" to merchant,
            "orderId" to orderId,
            "total" to total,
            "date" to date,
            "trackingOrReturnClue" to trackingOrReturnClue
        )
    }

    data class EventTicket(
        val dateTime: String,
        val location: String?,
        val confirmationId: String?,
        val ticketIdentity: String?
    ) : CompletionKey {
        override val type: IntentCategory = IntentCategory.EVENT_TICKET_RESERVATION
        override fun toStorageJson(): String = jsonObject(
            "type" to type.name,
            "dateTime" to dateTime,
            "location" to location,
            "confirmationId" to confirmationId,
            "ticketIdentity" to ticketIdentity
        )
    }

    data class Coupon(
        val code: String,
        val merchant: String?,
        val expiresAt: String?,
        val terms: String?
    ) : CompletionKey {
        override val type: IntentCategory = IntentCategory.COUPON_OR_PROMO
        override fun toStorageJson(): String = jsonObject(
            "type" to type.name,
            "code" to code,
            "merchant" to merchant,
            "expiresAt" to expiresAt,
            "terms" to terms
        )
    }

    data class ReadWatch(
        val title: String,
        val source: String?,
        val canonicalUrlOrAppIdentity: String?
    ) : CompletionKey {
        override val type: IntentCategory = IntentCategory.READ_OR_WATCH_LATER
        override fun toStorageJson(): String = jsonObject(
            "type" to type.name,
            "title" to title,
            "source" to source,
            "canonicalUrlOrAppIdentity" to canonicalUrlOrAppIdentity
        )
    }

    data class Place(
        val placeName: String,
        val addressOrMapIdentity: String?,
        val source: String?
    ) : CompletionKey {
        override val type: IntentCategory = IntentCategory.PLACE_OR_TRAVEL_IDEA
        override fun toStorageJson(): String = jsonObject(
            "type" to type.name,
            "placeName" to placeName,
            "addressOrMapIdentity" to addressOrMapIdentity,
            "source" to source
        )
    }

    data class Gift(
        val item: String,
        val recipientOrOccasion: String?,
        val source: String?,
        val price: String?
    ) : CompletionKey {
        override val type: IntentCategory = IntentCategory.GIFT_IDEA
        override fun toStorageJson(): String = jsonObject(
            "type" to type.name,
            "item" to item,
            "recipientOrOccasion" to recipientOrOccasion,
            "source" to source,
            "price" to price
        )
    }

    data class ChatAction(
        val personOrSource: String?,
        val requestedAction: String,
        val deadline: String?
    ) : CompletionKey {
        override val type: IntentCategory = IntentCategory.CHAT_ACTION
        override fun toStorageJson(): String = jsonObject(
            "type" to type.name,
            "personOrSource" to personOrSource,
            "requestedAction" to requestedAction,
            "deadline" to deadline
        )
    }
}

private fun jsonObject(vararg fields: Pair<String, Any?>): String = buildString {
    append('{')
    fields.forEachIndexed { index, (key, value) ->
        if (index > 0) append(',')
        append("\"").append(key.escapeJson()).append("\":")
        append(value.toJsonValue())
    }
    append('}')
}

private fun Any?.toJsonValue(): String = when (this) {
    null -> "null"
    is String -> "\"${escapeJson()}\""
    is Number, is Boolean -> toString()
    is Iterable<*> -> joinToString(prefix = "[", postfix = "]") { it.toJsonValue() }
    else -> "\"${toString().escapeJson()}\""
}

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
