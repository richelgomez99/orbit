package com.orbit.app.data.model

enum class MemoryCandidateKind {
    PROFILE_FACT,
    PREFERENCE,
    INTEREST,
    PATTERN,
    RELATIONSHIP,
    CORRECTION_RULE
}

enum class MemoryCandidateState {
    PENDING,
    ASKED,
    PROMOTED,
    REJECTED,
    EXPIRED,
    INVALIDATED
}

enum class MemorySensitivity {
    NORMAL,
    SENSITIVE,
    LOCAL_ONLY
}

enum class MemoryCandidateSource {
    DEBUG_SEED,
    USER_DECLARATION,
    ACTION_PATTERN,
    CAPTURE_PATTERN,
    MODEL_PROPOSED
}

enum class MemorySupportType {
    CAPTURE,
    ACTION,
    NOTE,
    UNDERSTANDING,
    USER_CONFIRMATION
}

enum class PromotedMemoryKind {
    DECLARED_PROFILE_FACT,
    CONFIRMED_PREFERENCE,
    RECURRING_INTEREST,
    BEHAVIOR_PATTERN,
    CORRECTION_RULE,
    RELATIONSHIP
}

enum class PromotedMemoryState {
    ACTIVE,
    DISABLED,
    FORGOTTEN,
    INVALIDATED
}

enum class PromotedMemoryConfidence {
    DECLARED,
    CONFIRMED,
    HIGH,
    MEDIUM,
    LOW
}

enum class PromotedMemorySource {
    USER_DECLARED,
    USER_CONFIRMED,
    REPEATED_BEHAVIOR,
    CORRECTION
}
