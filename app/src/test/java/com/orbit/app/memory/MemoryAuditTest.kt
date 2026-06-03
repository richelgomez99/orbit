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
}
