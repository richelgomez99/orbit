package com.orbit.app.memory

import com.orbit.app.data.entity.CaptureUnderstandingEntity
import com.orbit.app.data.entity.ContinuationResultEntity
import com.orbit.app.data.entity.EnvelopeNoteEntity
import com.orbit.app.data.entity.EvidenceBundleEntity
import com.orbit.app.data.entity.IntentEnvelopeEntity
import com.orbit.app.data.entity.StateSnapshot
import com.orbit.app.data.model.ActivityState
import com.orbit.app.data.model.AppCategory
import com.orbit.app.data.model.ContentType
import com.orbit.app.data.model.EnvelopeKind
import com.orbit.app.data.model.Intent
import com.orbit.app.data.model.IntentSource
import com.orbit.app.understanding.domain.CompletionKeyStatus
import com.orbit.app.understanding.domain.IntentCategory
import com.orbit.app.understanding.domain.UnderstandingMode
import com.orbit.app.understanding.domain.UnderstandingStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompactMemoryIndexBuilderTest {

    private val builder = CompactMemoryIndexBuilder()
    private val json = Json { encodeDefaults = false }

    @Test
    fun buildsCompactItemFromUnderstandingAndHydration() {
        val item = builder.build(
            envelope = envelope(text = "Long saved body about demo day and startup event"),
            latestResult = result(),
            understanding = understanding(),
            note = note("Need to decide whether to attend after the pitch meeting."),
            evidenceBundles = listOf(
                evidence("""{"kind":"OCR_HINT","label":"Clue","source":"understanding","excerpt":"Demo day June 4","confidence":0.8}""")
            )
        )

        assertNotNull(item)
        item!!
        assertEquals("env-1", item.envelopeId)
        assertEquals("READ_OR_WATCH_LATER", item.intent)
        assertEquals("Startup event flyer", item.title)
        assertEquals("example.com", item.domain)
        assertTrue(item.tags.contains("read_or_watch_later"))
        assertTrue(item.compactText.contains("Demo day"))
        assertTrue(item.compactText.contains("Chrome"))
        assertTrue(item.compactText.contains("Need to decide whether to attend"))
    }

    @Test
    fun capsTextAndEvidence() {
        val item = builder.build(
            envelope = envelope(text = "x".repeat(1_000)),
            understanding = understanding(
                title = "t".repeat(200),
                summary = "s".repeat(800)
            ),
            evidenceBundles = listOf(
                evidence("""{"kind":"OCR_HINT","label":"Clue","source":"understanding","excerpt":"${"e".repeat(500)}"}""")
            )
        )!!

        assertEquals(MemoryPayloadCaps.TITLE_MAX, item.title!!.length)
        assertEquals(MemoryPayloadCaps.SUMMARY_MAX, item.summary!!.length)
        assertTrue(item.evidence.all { (it.excerpt?.length ?: 0) <= MemoryPayloadCaps.EVIDENCE_EXCERPT_MAX })
        assertTrue(item.compactText.length <= MemoryPayloadCaps.COMPACT_TEXT_MAX)
    }

    @Test
    fun dropsBannedEvidenceBundleKeys() {
        val item = builder.build(
            envelope = envelope(text = "safe compact text"),
            evidenceBundles = listOf(
                evidence("""{"kind":"OCR_HINT","label":"Bad","source":"understanding","rawOcr":"full raw body"}""")
            )
        )!!

        assertTrue(item.evidence.none { it.label == "Bad" })
        val encoded = json.encodeToString(item)
        MemoryPayloadCaps.bannedKeys.forEach { key ->
            assertFalse("banned key leaked: $key", encoded.contains(key))
        }
        assertFalse(encoded.contains("full raw body"))
    }

    @Test
    fun compactEmbeddingInputIncludesCappedNoteButExcludesRawPromptAndModelResponseFields() {
        val longNote = "context ".repeat(80) + "private tail that should be capped"
        val item = builder.build(
            envelope = envelope(text = "saved body about a QR code"),
            note = note(longNote),
            evidenceBundles = listOf(
                evidence("""{"kind":"MODEL","label":"Bad","source":"understanding","excerpt":"unsafe","prompt":"system prompt","modelResponse":"answer"}"""),
                evidence("""{"kind":"SAFE_HINT","label":"Safe","source":"understanding","excerpt":"QR code for customer check-in"}""")
            )
        )!!

        assertTrue(item.compactText.contains("context"))
        assertTrue(item.evidence.any { it.kind == "NOTE" && (it.excerpt?.length ?: 0) <= MemoryPayloadCaps.EVIDENCE_EXCERPT_MAX })
        assertTrue(item.evidence.any { it.kind == "SAFE_HINT" })
        assertTrue(item.evidence.none { it.kind == "MODEL" })
        val encoded = json.encodeToString(item)
        assertFalse(encoded.contains("system prompt"))
        assertFalse(encoded.contains("modelResponse"))
        assertFalse(encoded.contains("private tail that should be capped"))
    }

    @Test
    fun deletedEnvelopeDoesNotBuildCloudIndexItem() {
        assertNull(builder.build(envelope(text = "deleted", isDeleted = true)))
    }

    @Test
    fun systemResolverSourceLabelIsNotCopiedToCompactIndex() {
        val item = builder.build(envelope(text = "screenshot body", sourceAppLabel = "IntentResolver"))!!

        assertNull(item.sourceAppLabel)
        assertFalse(item.compactText.contains("IntentResolver"))
    }

    private fun envelope(
        text: String?,
        isDeleted: Boolean = false,
        sourceAppLabel: String? = "Chrome",
    ) = IntentEnvelopeEntity(
        id = "env-1",
        contentType = ContentType.TEXT,
        textContent = text,
        imageUri = null,
        textContentSha256 = "text-sha",
        intent = Intent.READ_LATER,
        intentConfidence = 0.8f,
        intentSource = IntentSource.USER_CHIP,
        intentHistoryJson = "[]",
        state = StateSnapshot(
            appCategory = AppCategory.BROWSER,
            activityState = ActivityState.UNKNOWN,
            tzId = "America/New_York",
            hourLocal = 10,
            dayOfWeekLocal = 6,
            sourceAppLabel = sourceAppLabel
        ),
        createdAt = NOW,
        dayLocal = "2026-05-30",
        isDeleted = isDeleted,
        kind = EnvelopeKind.REGULAR
    )

    private fun understanding(
        title: String = "Startup event flyer",
        summary: String = "Saved event details for later."
    ) = CaptureUnderstandingEntity(
        captureId = "env-1",
        mode = UnderstandingMode.BASIC,
        status = UnderstandingStatus.READY,
        category = IntentCategory.READ_OR_WATCH_LATER,
        categoryConfidence = 0.8f,
        title = title,
        summaryText = summary,
        completionKeyJson = null,
        completionKeyStatus = CompletionKeyStatus.FOUND,
        sourceIdentityJson = null,
        contentHashHex = "understanding-hash",
        canonicalUrl = "https://example.com/event",
        groundingConstraintsJson = "{}",
        createdAt = NOW,
        updatedAt = NOW,
        invalidatedAt = null
    )

    private fun result() = ContinuationResultEntity(
        id = "result-1",
        continuationId = "cont-1",
        envelopeId = "env-1",
        producedAt = NOW,
        title = "Hydrated title",
        domain = "example.com",
        canonicalUrl = "https://example.com/event",
        canonicalUrlHash = "url-hash",
        excerpt = "Event page excerpt",
        summary = "Hydrated event summary",
        summaryModel = "local"
    )

    private fun evidence(payload: String) = EvidenceBundleEntity(
        id = "ev-${payload.hashCode()}",
        captureId = "env-1",
        bundleType = "OCR_HINT",
        payloadJson = payload,
        createdAt = NOW
    )

    private fun note(text: String) = EnvelopeNoteEntity(
        id = "note-1",
        envelopeId = "env-1",
        text = text,
        createdAt = NOW,
        updatedAt = NOW
    )

    private companion object {
        const val NOW = 1_780_000_000_000L
    }
}
