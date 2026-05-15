package com.orbit.app.understanding.domain

/**
 * The result of a completed understanding pass for a single capture.
 *
 * Consumers should check [status] before reading [title] / [summaryText].
 *
 * @param captureId       Matches the parent [com.orbit.app.data.entity.IntentEnvelopeEntity].
 * @param status          Outcome quality — [UnderstandingStatus.READY] means title+summary are valid.
 * @param mode            Which understanding engine produced this result.
 * @param title           Human-readable title; non-null when status is [UnderstandingStatus.READY].
 * @param summaryText     Short description; non-null when status is [UnderstandingStatus.READY].
 * @param groundingConstraints  Constraints applied to the generation process.
 * @param sourceIdentityJson    Serialised [com.orbit.app.understanding.domain.SourceIdentity] or null.
 * @param contentHashHex  SHA-256 hex of the canonical text content used for deduplication.
 * @param canonicalUrl    Deduplicated URL form (e.g. after YouTube canonicalisation).
 * @param evidenceBundleIds IDs of [com.orbit.app.data.entity.EvidenceBundleEntity] rows created.
 * @param duplicateMatch  Non-null when an existing understanding result matches this capture.
 */
data class UnderstandingResult(
    val captureId: String,
    val status: UnderstandingStatus,
    val mode: UnderstandingMode,
    val title: String?,
    val summaryText: String?,
    val groundingConstraints: GroundingConstraints,
    val sourceIdentityJson: String?,
    val contentHashHex: String?,
    val canonicalUrl: String?,
    val evidenceBundleIds: List<String>,
    val duplicateMatch: DuplicateMatch?
)

/**
 * Describes an exact or semantic match between two captures.
 *
 * @param existingCaptureId The ID of the capture already understood.
 * @param matchType         How the match was determined.
 */
data class DuplicateMatch(
    val existingCaptureId: String,
    val matchType: MatchType
)

/** How [DuplicateMatch] was detected. */
enum class MatchType {
    /** SHA-256 of the normalised content is identical. */
    EXACT_HASH,
    /** Canonical URL matches an existing capture's canonical URL. */
    CANONICAL_URL,
    /** Both hash and canonical URL match. */
    BOTH
}
