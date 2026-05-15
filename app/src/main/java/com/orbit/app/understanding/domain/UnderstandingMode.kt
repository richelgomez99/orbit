package com.orbit.app.understanding.domain

/** Spec 004 — level of effort Orbit applies to understand a capture. */
enum class UnderstandingMode {
    /** Local-only: hashing, canonical URL, app/category resolution. No network calls. */
    BASIC,

    /** User-triggered escalation: fetches Open Graph metadata + readable text extract. */
    SMART,

    /** User-triggered deep analysis: uses cloud LLM; may incur cost (spec 005+). */
    DEEP
}
