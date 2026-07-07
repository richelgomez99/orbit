package com.orbit.app.ai.ipc

import com.orbit.app.ai.EmbeddingResult
import com.orbit.app.ai.LlmProvider
import com.orbit.app.ai.model.ActionExtractionResult
import com.orbit.app.ai.model.AppFunctionSummary
import com.orbit.app.ai.model.DayHeaderResult
import com.orbit.app.ai.model.IntentClassification
import com.orbit.app.ai.model.LlmProvenance
import com.orbit.app.ai.model.SensitivityResult
import com.orbit.app.ai.model.SummaryResult
import com.orbit.app.data.entity.StateSnapshot
import com.orbit.app.data.model.Intent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec 022 M1 — covers the [ILocalInference] wire contract without Android:
 *  - every [LocalInferenceRequest]/[LocalInferenceResponse] variant survives a
 *    JSON encode→decode round-trip (the payload that crosses Binder), and
 *  - [LocalInferenceDispatcher] maps each request to the right response,
 *    stamping the caller-supplied model label.
 */
class LocalInferenceDispatcherTest {

    private val label = "gemma-3-1b-it-int4"

    private class FakeProvider : LlmProvider {
        override suspend fun classifyIntent(text: String, appCategory: String) =
            IntentClassification(Intent.WANT_IT, 0.6f, LlmProvenance.LocalByom("ignored"))

        override suspend fun summarize(text: String, maxTokens: Int) =
            SummaryResult("summary of $text", "en-US", LlmProvenance.LocalByom("ignored"))

        override suspend fun scanSensitivity(text: String) =
            SensitivityResult("""["financial"]""", LlmProvenance.LocalByom("ignored"))

        override suspend fun generateDayHeader(dayIsoDate: String, envelopeSummaries: List<String>) =
            DayHeaderResult("header for $dayIsoDate", "en-US", LlmProvenance.LocalByom("ignored"))

        override suspend fun extractActions(
            text: String,
            contentType: String,
            state: StateSnapshot,
            registeredFunctions: List<AppFunctionSummary>,
            maxCandidates: Int,
        ): ActionExtractionResult = error("unused")

        override suspend fun embed(text: String): EmbeddingResult? = error("unused")
    }

    private fun requestRoundTrip(req: LocalInferenceRequest): LocalInferenceRequest =
        LocalInferenceJson.decodeFromString(
            LocalInferenceRequest.serializer(),
            LocalInferenceJson.encodeToString(LocalInferenceRequest.serializer(), req),
        )

    private fun responseRoundTrip(resp: LocalInferenceResponse): LocalInferenceResponse =
        LocalInferenceJson.decodeFromString(
            LocalInferenceResponse.serializer(),
            LocalInferenceJson.encodeToString(LocalInferenceResponse.serializer(), resp),
        )

    @Test
    fun requestVariantsSurviveRoundTrip() {
        val requests = listOf(
            LocalInferenceRequest.Summarize("r1", "hello", 64),
            LocalInferenceRequest.GenerateDayHeader("r2", "2026-07-07", listOf("a", "b")),
            LocalInferenceRequest.ClassifyIntent("r3", "buy this", "OTHER"),
            LocalInferenceRequest.ScanSensitivity("r4", "my ssn is..."),
        )
        requests.forEach { assertEquals(it, requestRoundTrip(it)) }
    }

    @Test
    fun responseVariantsSurviveRoundTrip() {
        val responses = listOf(
            LocalInferenceResponse.Summary("r1", "sum", "en-US", label),
            LocalInferenceResponse.DayHeader("r2", "hdr", "en-US", label),
            LocalInferenceResponse.Intent("r3", "WANT_IT", 0.6f, label),
            LocalInferenceResponse.Sensitivity("r4", "[]", label),
            LocalInferenceResponse.Error("r5", "NO_LOCAL_MODEL", "none"),
        )
        responses.forEach { assertEquals(it, responseRoundTrip(it)) }
    }

    @Test
    fun dispatchSummarize() = runTest {
        val r = LocalInferenceDispatcher.dispatch(
            LocalInferenceRequest.Summarize("id", "doc", 64), FakeProvider(), label,
        )
        assertTrue(r is LocalInferenceResponse.Summary)
        r as LocalInferenceResponse.Summary
        assertEquals("summary of doc", r.text)
        assertEquals(label, r.modelLabel)
        assertEquals("id", r.requestId)
    }

    @Test
    fun dispatchClassifyIntent() = runTest {
        val r = LocalInferenceDispatcher.dispatch(
            LocalInferenceRequest.ClassifyIntent("id", "buy it", "OTHER"), FakeProvider(), label,
        )
        assertTrue(r is LocalInferenceResponse.Intent)
        r as LocalInferenceResponse.Intent
        assertEquals("WANT_IT", r.intent)
        assertEquals(0.6f, r.confidence, 0.0001f)
        assertEquals(label, r.modelLabel)
    }

    @Test
    fun dispatchScanSensitivity() = runTest {
        val r = LocalInferenceDispatcher.dispatch(
            LocalInferenceRequest.ScanSensitivity("id", "text"), FakeProvider(), label,
        )
        assertTrue(r is LocalInferenceResponse.Sensitivity)
        assertEquals("""["financial"]""", (r as LocalInferenceResponse.Sensitivity).flagsJson)
    }

    @Test
    fun dispatchDayHeader() = runTest {
        val r = LocalInferenceDispatcher.dispatch(
            LocalInferenceRequest.GenerateDayHeader("id", "2026-07-07", listOf("x")),
            FakeProvider(), label,
        )
        assertTrue(r is LocalInferenceResponse.DayHeader)
        assertEquals("header for 2026-07-07", (r as LocalInferenceResponse.DayHeader).text)
    }
}
