package com.orbit.app.cloud

import com.orbit.app.audit.AuditLogWriter
import com.orbit.app.data.model.AuditAction
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudUsageReceiptTest {
    private val writer = CloudUsageReceiptWriter(
        writer = AuditLogWriter(
            clock = { 123L },
            idGen = { "audit-cloud-1" },
        )
    )

    @Test
    fun receiptStoresBoundedMetadataAndDigestsPrivateInputs() {
        val rawQuestion = "What is my passport number?"
        val rawPayload = """{"prompt":"summarize my private document"}"""
        val entry = writer.receipt(
            requestId = "req-1",
            capability = CloudCapability.ASK_GROUNDED_SYNTHESIS,
            outcome = CloudUsageOutcome.FALLBACK_USED,
            reason = BudgetDecisionReason.DISABLED_BY_USER,
            endpoint = "grounded_ask",
            resultCount = 0,
            provider = "mongodb_atlas",
            model = "openai:gpt-4.1-mini",
            inputForDigest = rawQuestion,
            payloadForDigest = rawPayload,
            estimatedInputTokens = 24,
            estimatedOutputTokens = 0,
            estimatedCostCents = 1,
        )

        assertEquals(AuditAction.CLOUD_USAGE_RECORDED, entry.action)
        val extras = JSONObject(entry.extraJson!!)
        assertEquals("req-1", extras.getString("requestId"))
        assertEquals("ASK_GROUNDED_SYNTHESIS", extras.getString("capability"))
        assertEquals("FALLBACK_USED", extras.getString("outcome"))
        assertEquals("DISABLED_BY_USER", extras.getString("reason"))
        assertEquals("grounded_ask", extras.getString("endpoint"))
        assertEquals(24, extras.getInt("estimatedInputTokens"))
        assertTrue(extras.has("inputDigest"))
        assertTrue(extras.has("payloadDigest"))
        assertFalse(entry.extraJson!!.contains(rawQuestion))
        assertFalse(entry.extraJson!!.contains("summarize my private document"))
    }

    @Test
    fun skippedDecisionMapsBudgetExhaustedToBudgetDeniedOutcome() {
        val decision = BudgetDecision(
            capability = CloudCapability.LLM_EXTRACT_ACTIONS,
            allowed = false,
            reason = BudgetDecisionReason.BUDGET_EXHAUSTED,
            requestId = "req-budget",
            estimatedCostCents = 9L,
            budgetRemainingCents = 0L,
            fallbackMode = FallbackMode.UNAVAILABLE_COPY,
        )

        val entry = writer.skippedDecision(decision, endpoint = "llm_gateway")
        val extras = JSONObject(entry.extraJson!!)

        assertEquals("BUDGET_DENIED", extras.getString("outcome"))
        assertEquals("BUDGET_EXHAUSTED", extras.getString("reason"))
        assertEquals(9L, extras.getLong("estimatedCostCents"))
    }

    @Test
    fun receiptJsonDoesNotContainForbiddenRawContentKeys() {
        val entry = writer.receipt(
            requestId = "req-forbidden",
            capability = CloudCapability.LLM_SUMMARIZE,
            outcome = CloudUsageOutcome.SUCCESS,
            endpoint = "llm_gateway",
            inputForDigest = "full OCR body should be digested",
            payloadForDigest = "prompt and model response should be digested",
        )
        val extras = JSONObject(entry.extraJson!!)
        val forbidden = listOf(
            "question",
            "prompt",
            "rawOcr",
            "ocrText",
            "screenshot",
            "imageBytes",
            "embedding",
            "modelResponse",
            "accessToken",
            "refreshToken",
            "apiKey",
            "jwt",
            "cookie",
        )

        forbidden.forEach { key ->
            assertFalse("receipt must not include key $key", extras.has(key))
            assertFalse("receipt must not include raw marker $key", entry.extraJson!!.contains(key))
        }
    }
}
