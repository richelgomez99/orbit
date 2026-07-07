package com.orbit.app.data

import com.orbit.app.data.entity.ContinuationEntity
import com.orbit.app.data.entity.ContinuationResultEntity
import com.orbit.app.data.entity.IntentEnvelopeEntity
import com.orbit.app.data.entity.StateSnapshot
import com.orbit.app.data.model.ActivityState
import com.orbit.app.data.model.AppCategory
import com.orbit.app.data.model.ContentType
import com.orbit.app.data.model.ContinuationStatus
import com.orbit.app.data.model.ContinuationType
import com.orbit.app.data.model.EnvelopeKind
import com.orbit.app.data.model.Intent
import com.orbit.app.data.model.IntentSource
import java.time.Instant
import java.time.ZoneId

/**
 * Debug-only realistic corpus seeder.
 *
 * Every populated UI/UX pattern — diary day-threading, hydrated link
 * cards, intent glyphs across all four intents, Library search hits,
 * and agent evidence lookup — is invisible against an empty database.
 * This produces a believable "day in the life" spanning several days so
 * those patterns can actually be evaluated on-device against the
 * `design.md` bar. DEBUG builds only; never wired into release.
 *
 * Idempotent: every row id is prefixed `demo-`, and re-seeding clears
 * the previous demo rows first so the corpus is stable across taps.
 */
object DebugCorpusSeeder {

    private const val ID_PREFIX = "demo-"

    data class Capture(
        val key: String,
        val text: String,
        val intent: Intent,
        val appCategory: AppCategory,
        val daysAgo: Int,
        val hourLocal: Int,
        /** When set, a hydrated ContinuationResult is attached (a URL capture). */
        val url: String? = null,
        val title: String? = null,
        val domain: String? = null,
        val summary: String? = null,
    )

    /** The believable corpus. Ordered oldest → newest by (daysAgo desc). */
    private val CORPUS: List<Capture> = listOf(
        Capture(
            key = "termsheet",
            text = "How to think about pre-seed valuation when the comparison set is thin — anchor the round at the top of your peer set.",
            intent = Intent.READ_LATER,
            appCategory = AppCategory.BROWSER,
            daysAgo = 6, hourLocal = 9,
            url = "https://example.com/pre-seed-valuation",
            title = "Pre-seed valuation when comps are thin",
            domain = "example.com",
            summary = "Anchor the round at the top of your peers, not the middle. Investors price off the comparison set; a thin set means you set the anchor. Keep dilution under 20% and leave room for a bridge.",
        ),
        Capture(
            key = "founder-events",
            text = "Founder office hours — Thursdays 4pm, RSVP link. Third one this month I've saved.",
            intent = Intent.WANT_IT,
            appCategory = AppCategory.SOCIAL,
            daysAgo = 5, hourLocal = 14,
            url = "https://example.com/founder-office-hours",
            title = "Founder Office Hours — weekly",
            domain = "example.com",
            summary = "Weekly founder office hours, Thursdays 4pm PT. Bring one specific blocker. RSVP required; capped at 20 seats.",
        ),
        Capture(
            key = "dentist",
            text = "Dentist appointment canceled. Need to reschedule — front desk said call back after the 10th.",
            intent = Intent.WANT_IT,
            appCategory = AppCategory.MESSAGING,
            daysAgo = 4, hourLocal = 11,
        ),
        Capture(
            key = "salmon-recipe",
            text = "Miso-glazed salmon recipe — salmon, white miso, mirin, ginger, rice, lemons. 20 min at 400F.",
            intent = Intent.WANT_IT,
            appCategory = AppCategory.BROWSER,
            daysAgo = 3, hourLocal = 19,
            url = "https://example.com/miso-salmon",
            title = "Miso-glazed salmon in 20 minutes",
            domain = "example.com",
            summary = "Whisk white miso with mirin and grated ginger, coat salmon fillets, roast at 400F for 12 minutes. Serve over rice with a squeeze of lemon.",
        ),
        Capture(
            key = "attention-essay",
            text = "\"Your attention is the most valuable thing you own, and it is being strip-mined.\" — worth re-reading.",
            intent = Intent.INTERESTING,
            appCategory = AppCategory.READING,
            daysAgo = 3, hourLocal = 22,
            url = "https://example.com/attention-essay",
            title = "The attention economy is strip-mining you",
            domain = "example.com",
            summary = "The essay argues attention is a non-renewable personal resource being extracted by engagement-optimized feeds, and proposes treating it like a budget you spend deliberately.",
        ),
        Capture(
            key = "concert",
            text = "Concert ticket saved — indie showcase, Sat the 12th, doors 8pm, will-call under my name.",
            intent = Intent.WANT_IT,
            appCategory = AppCategory.OTHER,
            daysAgo = 2, hourLocal = 13,
        ),
        Capture(
            key = "transformer-paper",
            text = "Attention is all you need — the encoder/decoder split and multi-head attention. Foundations for the RAG work.",
            intent = Intent.REFERENCE,
            appCategory = AppCategory.READING,
            daysAgo = 2, hourLocal = 16,
            url = "https://example.com/attention-paper",
            title = "Attention Is All You Need",
            domain = "example.com",
            summary = "Introduces the transformer: self-attention replaces recurrence, multi-head attention lets the model attend to multiple positions, positional encodings inject order. Basis for modern LLMs.",
        ),
        Capture(
            key = "solutions-engineer",
            text = "\"AI Solutions Engineer\" vs \"Forward Deployed Engineer\" — same role, different title. Worth applying.",
            intent = Intent.REFERENCE,
            appCategory = AppCategory.BROWSER,
            daysAgo = 1, hourLocal = 10,
        ),
        Capture(
            key = "gift-idea",
            text = "Mom's birthday — she mentioned wanting that ceramic pour-over set. Do NOT forget this time.",
            intent = Intent.WANT_IT,
            appCategory = AppCategory.MESSAGING,
            daysAgo = 1, hourLocal = 20,
        ),
        Capture(
            key = "today-note",
            text = "Standup takeaway: ship the crash journal, then the local model seam. Everything else waits.",
            intent = Intent.REFERENCE,
            appCategory = AppCategory.WORK_EMAIL,
            daysAgo = 0, hourLocal = 9,
        ),
    )

    suspend fun seed(db: OrbitDatabase, nowMillis: Long): Int {
        clear(db)
        val envelopeDao = db.intentEnvelopeDao()
        val continuationDao = db.continuationDao()
        val continuationResultDao = db.continuationResultDao()
        val zone = ZoneId.systemDefault()
        var seeded = 0

        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        for (c in CORPUS) {
            // Anchor to local midnight `daysAgo` days back, then set the wall
            // clock to hourLocal — so captures land on the intended calendar
            // day (naive now-minus-hours math drifts across day boundaries).
            val day = today.minusDays(c.daysAgo.toLong())
            val created = day.atTime(c.hourLocal, 3)
                .atZone(zone).toInstant().toEpochMilli()
            val dayLocal = day.toString()
            val id = "$ID_PREFIX${c.key}"

            envelopeDao.insert(
                IntentEnvelopeEntity(
                    id = id,
                    contentType = ContentType.TEXT,
                    textContent = c.text,
                    imageUri = null,
                    textContentSha256 = null,
                    intent = c.intent,
                    intentConfidence = 0.9f,
                    intentSource = IntentSource.USER_CHIP,
                    intentHistoryJson = "[]",
                    state = StateSnapshot(
                        appCategory = c.appCategory,
                        activityState = ActivityState.STILL,
                        tzId = zone.id,
                        hourLocal = c.hourLocal,
                        dayOfWeekLocal = 3,
                    ),
                    createdAt = created,
                    dayLocal = dayLocal,
                    kind = EnvelopeKind.REGULAR,
                )
            )

            if (c.url != null) {
                val contId = "$ID_PREFIX${c.key}-cont"
                continuationDao.insert(
                    ContinuationEntity(
                        id = contId,
                        envelopeId = id,
                        type = ContinuationType.URL_HYDRATE,
                        status = ContinuationStatus.SUCCEEDED,
                        inputUrl = c.url,
                        scheduledAt = created,
                        startedAt = created,
                        completedAt = created + 2_000L,
                        failureReason = null,
                    )
                )
                continuationResultDao.insert(
                    ContinuationResultEntity(
                        id = "$ID_PREFIX${c.key}-res",
                        continuationId = contId,
                        envelopeId = id,
                        producedAt = created + 2_000L,
                        title = c.title,
                        domain = c.domain,
                        canonicalUrl = c.url,
                        canonicalUrlHash = "$ID_PREFIX${c.key}-hash",
                        excerpt = c.summary?.take(120),
                        summary = c.summary,
                        summaryModel = "demo-seed",
                    )
                )
            }
            seeded++
        }
        return seeded
    }

    suspend fun clear(db: OrbitDatabase) {
        val sql = db.openHelper.writableDatabase
        // Children first (FKs), then envelopes. All demo rows share the prefix.
        sql.execSQL("DELETE FROM continuation_result WHERE id LIKE '$ID_PREFIX%'")
        sql.execSQL("DELETE FROM continuation WHERE id LIKE '$ID_PREFIX%'")
        sql.execSQL("DELETE FROM intent_envelope WHERE id LIKE '$ID_PREFIX%'")
    }
}
