package com.orbit.app.understanding.domain

enum class IntentCategory {
    BUY_LATER_PRODUCT,
    RECIPE,
    QR_OR_BARCODE,
    RECEIPT_OR_ORDER,
    EVENT_TICKET_RESERVATION,
    COUPON_OR_PROMO,
    READ_OR_WATCH_LATER,
    PLACE_OR_TRAVEL_IDEA,
    GIFT_IDEA,
    CHAT_ACTION,
    MAYBE_OLD_OR_INACTIVE,
    UNKNOWN
}

enum class ActiveIntentStatus {
    ACTIVE,
    RESOLVED,
    EXPIRED,
    ARCHIVED,
    INVALIDATED
}

enum class ActiveIntentResolutionReason {
    BOUGHT,
    NOT_INTERESTED,
    COOKED,
    READ_OR_WATCHED,
    VISITED,
    REPLIED_OR_DONE,
    USER_ARCHIVED,
    AUTO_EXPIRED,
    SUPERSEDED,
    SOURCE_DELETED,
    NO_LONGER_ACTIONABLE,
    CORRECTION_APPLIED
}
