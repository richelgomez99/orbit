package com.orbit.app.understanding.domain

/** Spec 004 — overall quality of the understanding result. */
enum class UnderstandingStatus {
    /** All requested evidence gathered; result is usable. */
    READY,

    /** Partial evidence only; result is usable but grounding constraints apply. */
    LIMITED,

    /** Engine could not produce any usable result (timeout, parse error, etc.). */
    FAILED
}

/** Spec 004 — what evidence types were actually used to build this result. */
enum class EvidenceLevel {
    /** Readable article text + metadata extracted successfully. */
    FULL,

    /** Some evidence gathered, but not the full readable content. */
    PARTIAL,

    /** Only HTTP headers / Open Graph tags available; no article body. */
    METADATA_ONLY,

    /** Screenshot or image only; no textual evidence. */
    VISUAL_ONLY
}
