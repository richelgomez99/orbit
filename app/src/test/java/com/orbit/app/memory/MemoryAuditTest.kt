package com.orbit.app.memory

import com.orbit.app.audit.AuditLogWriter
import com.orbit.app.data.model.AuditAction
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryAuditTest {
    private val audit = MemoryAudit(
        writer = AuditLogWriter(
            clock = { 123L },
            idGen = { "audit-1" },
        ),
    )

    @Test
    fun searchAuditStoresDigestNotRawQuery() {
        val rawQuery = "where did I put the bank transfer screenshot"
        val entry = audit.searchRequested(
            requestId = "req-1",
            query = rawQuery,
            resultCount = 3,
            latencyMs = 42L,
            outcome = "success",
        )

        assertEquals(AuditAction.MEMORY_SEARCH_REQUESTED, entry.action)
        assertEquals("audit-1", entry.id)
        assertEquals(123L, entry.at)

        val extras = JSONObject(entry.extraJson!!)
        assertEquals(MemoryAudit.PROVIDER, extras.getString("provider"))
        assertEquals("search", extras.getString("endpoint"))
        assertEquals("req-1", extras.getString("requestId"))
        assertEquals(3, extras.getInt("resultCount"))
        assertEquals(42L, extras.getLong("latencyMs"))
        assertEquals("success", extras.getString("outcome"))
        assertTrue(extras.has("queryDigest"))
        assertFalse(entry.extraJson!!.contains(rawQuery))
    }

    @Test
    fun payloadDigestChangesWhenPayloadChanges() {
        val first = audit.upserted(
            requestId = "req-1",
            envelopeId = "env-1",
            payloadForDigest = """{"title":"A"}""",
            latencyMs = 10L,
        )
        val second = audit.upserted(
            requestId = "req-2",
            envelopeId = "env-1",
            payloadForDigest = """{"title":"B"}""",
            latencyMs = 10L,
        )

        val firstDigest = JSONObject(first.extraJson!!).getString("payloadDigest")
        val secondDigest = JSONObject(second.extraJson!!).getString("payloadDigest")
        assertNotEquals(firstDigest, secondDigest)
    }

    @Test
    fun semanticSearchAuditStoresModeAndDigestNotRawQuery() {
        val rawQuery = "qr code for first 1000 customers"
        val entry = audit.semanticSearchRequested(
            requestId = "req-semantic",
            query = rawQuery,
            resultCount = 2,
            latencyMs = 25L,
            outcome = "success",
            retrievalMode = "hybrid",
        )

        assertEquals(AuditAction.MEMORY_SEARCH_REQUESTED, entry.action)
        val extras = JSONObject(entry.extraJson!!)
        assertEquals("semantic_search", extras.getString("endpoint"))
        assertEquals("hybrid", extras.getString("retrievalMode"))
        assertEquals(2, extras.getInt("resultCount"))
        assertTrue(extras.has("queryDigest"))
        assertFalse(entry.extraJson!!.contains(rawQuery))
    }

    @Test
    fun groundedAskAuditStoresStatusCountsAndDigestNotRawQuestion() {
        val rawQuestion = "What is my passport number?"
        val entry = audit.groundedAskRequested(
            requestId = "req-ask",
            question = rawQuestion,
            citationCount = 0,
            candidateCount = 3,
            latencyMs = 31L,
            outcome = "refused",
            answerStatus = "sensitive_refusal",
        )

        assertEquals(AuditAction.MEMORY_ASK_REQUESTED, entry.action)
        val extras = JSONObject(entry.extraJson!!)
        assertEquals("grounded_ask", extras.getString("endpoint"))
        assertEquals("sensitive_refusal", extras.getString("answerStatus"))
        assertEquals(3, extras.getInt("candidateCount"))
        assertEquals(0, extras.getInt("resultCount"))
        assertTrue(extras.has("queryDigest"))
        assertFalse(entry.extraJson!!.contains(rawQuestion))
    }

    @Test
    fun embeddingAuditStoresCompactInputDigestAndProviderMetadata() {
        val compactInput = "TITLE: Flight receipt\nSUMMARY: NYC to San Francisco"
        val entry = audit.embeddingUpdated(
            requestId = "req-embed",
            envelopeId = "env-flight",
            compactInputForDigest = compactInput,
            embeddingModel = "openai:text-embedding-3-small",
            dimensions = 1536,
            latencyMs = 50L,
        )

        assertEquals(AuditAction.MEMORY_INDEX_UPSERTED, entry.action)
        assertEquals("env-flight", entry.envelopeId)
        val extras = JSONObject(entry.extraJson!!)
        assertEquals("embed", extras.getString("endpoint"))
        assertEquals("openai:text-embedding-3-small", extras.getString("embeddingModel"))
        assertEquals(1536, extras.getInt("dimensions"))
        assertTrue(extras.has("payloadDigest"))
        assertFalse(entry.extraJson!!.contains("Flight receipt"))
    }

    @Test
    fun fallbackAuditStoresReasonWithoutQueryText() {
        val entry = audit.fallbackUsed(
            requestId = "req-fallback",
            endpoint = "semantic_search",
            reason = "provider_unavailable",
            resultCount = 1,
            latencyMs = 6L,
        )

        assertEquals(AuditAction.MEMORY_SEARCH_REQUESTED, entry.action)
        val extras = JSONObject(entry.extraJson!!)
        assertEquals("semantic_search", extras.getString("endpoint"))
        assertEquals("local_fallback", extras.getString("outcome"))
        assertEquals("provider_unavailable", extras.getString("reason"))
        assertEquals(1, extras.getInt("resultCount"))
    }
}
