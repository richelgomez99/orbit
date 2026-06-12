package com.orbit.app.orbit

import android.content.Context
import com.orbit.app.cloud.CloudCapability
import com.orbit.app.data.entity.AuditLogEntryEntity
import com.orbit.app.data.model.AuditAction
import com.orbit.app.library.LocalEnvelopeLookup
import com.orbit.app.memory.AskOrbitAnswer
import com.orbit.app.memory.AskOrbitCitation
import com.orbit.app.memory.MemoryEvidenceSnippet
import com.orbit.app.memory.MemoryGatewayRequest
import com.orbit.app.memory.MemoryGatewayResponse
import com.orbit.app.memory.MemorySearchResult
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AskOrbitRepositoryTest {

    @Test
    fun demoQuestionsReturnCitedAnswersAndUnsupportedQuestionRefuses() = runTest {
        val repository = BinderAskOrbitRepository(
            context = null,
            localEnvelopeLookup = DemoLocalEnvelopeLookup(),
        )

        val startup = repository.ask("What startup event did I save?")
        val flight = repository.ask("Which flight receipt did I save recently?")
        val recipe = repository.ask("What recipe did I want to try?")
        val unsupported = repository.ask("What is my passport number?")

        listOf(startup, flight, recipe).forEach { answer ->
            assertEquals("answered", answer.status)
            assertTrue("answered Ask result must cite saved memory", answer.citations.isNotEmpty())
            assertTrue("answered Ask result must expose source envelope", answer.citations.all { it.envelopeId.isNotBlank() })
        }
        assertEquals("env-startup", startup.citations.first().envelopeId)
        assertEquals("env-flight", flight.citations.first().envelopeId)
        assertTrue(recipe.citations.map { it.envelopeId }.contains("env-recipe"))

        assertEquals("insufficient_evidence", unsupported.status)
        assertTrue(unsupported.citations.isEmpty())
    }

    @Test
    fun groundedGatewayAnswerFiltersToLocalBackedCitationsAndCandidates() = runTest {
        val lookup = RecordingLocalEnvelopeLookup(existingIds = setOf("env-local"))
        val repository = BinderAskOrbitRepository(
            context = null,
            localEnvelopeLookup = lookup,
            requestIdFactory = { "req-grounded" },
            memoryGatewayCaller = { request ->
                assertTrue(request is MemoryGatewayRequest.GroundedAsk)
                MemoryGatewayResponse.GroundedAskResponse(
                    requestId = request.requestId,
                    answer = AskOrbitAnswer(
                        status = "answered",
                        answer = "You saved the flight receipt.",
                        citations = listOf(
                            citation("env-local", "Flight receipt"),
                            citation("env-cloud-only", "Deleted cloud result")
                        ),
                        candidates = listOf(
                            result("env-local", "Flight receipt", "Flight receipt: NYC to San Francisco.", "Gmail"),
                            result("env-cloud-only", "Deleted cloud result", "Stale cloud-only result.", "Gmail")
                        ),
                        modelLabel = "openai:gpt-4.1-mini",
                        retrievalMode = "hybrid",
                        confidence = 0.9f
                    )
                )
            }
        )

        val answer = repository.ask("Which flight receipt did I save recently?")

        assertEquals("answered", answer.status)
        assertEquals(listOf("env-local"), answer.citations.map { it.envelopeId })
        assertEquals(listOf("env-local"), answer.candidates.map { it.envelopeId })
        assertEquals(listOf("env-local", "env-cloud-only", "env-local", "env-cloud-only"), lookup.existsCalls)
    }

    @Test
    fun groundedGatewayAnswerWithNoLocalCitationFallsBackToLocalSearch() = runTest {
        val localFallback = result("env-local-fallback", "Recipe to try", "Recipe to try: miso salmon.", "Chrome")
        val lookup = RecordingLocalEnvelopeLookup(
            existingIds = emptySet(),
            searchResults = listOf(localFallback)
        )
        val repository = BinderAskOrbitRepository(
            context = null,
            localEnvelopeLookup = lookup,
            memoryGatewayCaller = { request ->
                MemoryGatewayResponse.GroundedAskResponse(
                    requestId = request.requestId,
                    answer = AskOrbitAnswer(
                        status = "answered",
                        answer = "Cloud says stale recipe.",
                        citations = listOf(citation("env-stale", "Stale recipe")),
                        candidates = emptyList(),
                        modelLabel = "openai:gpt-4.1-mini"
                    )
                )
            }
        )

        val answer = repository.ask("What recipe did I want to try?")

        assertEquals("answered", answer.status)
        assertEquals(listOf("env-local-fallback"), answer.citations.map { it.envelopeId })
        assertTrue(answer.answer.contains("miso salmon"))
    }

    @Test
    fun cloudAskDisabledSkipsGroundedGatewayAndUsesLocalSearch() = runTest {
        val localFallback = result("env-local-fallback", "Dentist rescheduled", "Dentist appointment was rescheduled.", "Calendar")
        val lookup = RecordingLocalEnvelopeLookup(
            existingIds = emptySet(),
            searchResults = listOf(localFallback)
        )
        var gatewayCalled = false
        val receipts = mutableListOf<AuditLogEntryEntity>()
        val repository = BinderAskOrbitRepository(
            context = null,
            localEnvelopeLookup = lookup,
            requestIdFactory = { "req-skip-ask" },
            cloudAskSynthesisEnabled = { false },
            cloudReceiptAppender = { receipts += it },
            memoryGatewayCaller = {
                gatewayCalled = true
                error("GroundedAsk must not be called when cloud Ask synthesis is disabled")
            }
        )

        val answer = repository.ask("What was rescheduled?")

        assertEquals("answered", answer.status)
        assertEquals("local/deterministic", answer.modelLabel)
        assertEquals(listOf("env-local-fallback"), answer.citations.map { it.envelopeId })
        assertTrue(!gatewayCalled)
        assertEquals(AuditAction.CLOUD_USAGE_RECORDED, receipts.single().action)
        val extras = JSONObject(receipts.single().extraJson!!)
        assertEquals(CloudCapability.ASK_GROUNDED_SYNTHESIS.name, extras.getString("capability"))
        assertEquals("SKIPPED", extras.getString("outcome"))
        assertEquals("DISABLED_BY_USER", extras.getString("reason"))
        assertTrue(extras.has("inputDigest"))
        assertTrue(!receipts.single().extraJson!!.contains("What was rescheduled?"))
    }

    @Test
    fun unsupportedSensitiveIdentifierQuestionUsesUserFriendlyRefusalCopy() = runTest {
        val repository = BinderAskOrbitRepository(
            context = null,
            localEnvelopeLookup = RecordingLocalEnvelopeLookup(existingIds = emptySet()),
            cloudAskSynthesisEnabled = { false },
        )

        val answer = repository.ask("What is my passport number?")

        assertEquals("insufficient_evidence", answer.status)
        assertTrue(answer.answer.contains("saved capture explicitly contains"))
        assertTrue(answer.citations.isEmpty())
    }

    @Test
    fun groundedSensitiveRefusalIsPreservedWithoutLocalEvidenceLookup() = runTest {
        val lookup = RecordingLocalEnvelopeLookup(existingIds = emptySet())
        val repository = BinderAskOrbitRepository(
            context = null,
            localEnvelopeLookup = lookup,
            memoryGatewayCaller = { request ->
                MemoryGatewayResponse.GroundedAskResponse(
                    requestId = request.requestId,
                    answer = AskOrbitAnswer(
                        status = "sensitive_refusal",
                        answer = "I do not have a saved capture that explicitly contains that, so I will not guess.",
                        citations = emptyList(),
                        candidates = emptyList(),
                        modelLabel = "policy",
                        limitations = listOf("Capture or add the document first if you want Orbit to recall that detail later.")
                    )
                )
            }
        )

        val answer = repository.ask("What is my passport number?")

        assertEquals("sensitive_refusal", answer.status)
        assertTrue(answer.citations.isEmpty())
        assertTrue(lookup.existsCalls.isEmpty())
    }

    private class DemoLocalEnvelopeLookup : LocalEnvelopeLookup {
        override suspend fun exists(envelopeId: String): Boolean = true

        override suspend fun search(query: String, limit: Int): List<MemorySearchResult> {
            val normalized = query.lowercase()
            val matches = when {
                normalized.contains("startup event") -> listOf(startupEvent)
                normalized.contains("flight receipt") -> listOf(flightReceipt)
                normalized.contains("recipe") -> listOf(recipeShoppingList, recipeClip, recipe)
                normalized.contains("startup") || normalized.contains("event") -> listOf(ramenNearEvent, raizyEventReply, startupEvent)
                normalized.contains("flight") || normalized.contains("receipt") -> listOf(momFlightArrival, dentistTravelConflict, flightReceipt)
                normalized.contains("number") -> listOf(tailoringNumberNoise)
                else -> emptyList()
            }
            return matches.take(limit)
        }
    }

    private class RecordingLocalEnvelopeLookup(
        private val existingIds: Set<String>,
        private val searchResults: List<MemorySearchResult> = emptyList()
    ) : LocalEnvelopeLookup {
        val existsCalls = mutableListOf<String>()

        override suspend fun exists(envelopeId: String): Boolean {
            existsCalls += envelopeId
            return envelopeId in existingIds
        }

        override suspend fun search(query: String, limit: Int): List<MemorySearchResult> =
            searchResults.take(limit)
    }

    private companion object {
        fun citation(
            envelopeId: String,
            title: String,
        ) = AskOrbitCitation(
            citationId = "c-$envelopeId",
            envelopeId = envelopeId,
            title = title,
            excerpt = title,
            dayLocal = "2026-05-30",
            sourceAppLabel = "Gmail",
        )

        val startupEvent = result(
            envelopeId = "env-startup",
            title = "Startup event ticket",
            excerpt = "Startup event ticket for Monday founder office hours.",
            sourceAppLabel = "Gmail",
        )
        val flightReceipt = result(
            envelopeId = "env-flight",
            title = "Flight receipt",
            excerpt = "Flight receipt: NYC to San Francisco, confirmation ORB123.",
            sourceAppLabel = "Gmail",
        )
        val recipe = result(
            envelopeId = "env-recipe",
            title = "Recipe to try",
            excerpt = "Recipe to try: miso salmon with ginger rice.",
            sourceAppLabel = "Chrome",
        )
        val ramenNearEvent = result(
            envelopeId = "env-ramen",
            title = "Ramen near startup event",
            excerpt = "Ramen restaurant near the hotel, open late after startup event.",
            sourceAppLabel = "Maps",
        )
        val raizyEventReply = result(
            envelopeId = "env-raizy",
            title = "Reply after startup event",
            excerpt = "Raizy asked if Monday afternoon works after the startup event.",
            sourceAppLabel = "Messages",
        )
        val momFlightArrival = result(
            envelopeId = "env-mom-flight",
            title = "Mom asked for flight arrival time",
            excerpt = "Mom asked for flight arrival time and hotel address.",
            sourceAppLabel = "WhatsApp",
        )
        val dentistTravelConflict = result(
            envelopeId = "env-dentist",
            title = "Dentist appointment travel conflict",
            excerpt = "Dentist appointment reminder conflicts with Monday travel.",
            sourceAppLabel = "Calendar",
        )
        val recipeShoppingList = result(
            envelopeId = "env-recipe-shopping",
            title = "Shopping list for recipe night",
            excerpt = "Shopping list for recipe night: salmon, miso, ginger, rice.",
            sourceAppLabel = "Notes",
        )
        val recipeClip = result(
            envelopeId = "env-recipe-clip",
            title = "Recipe clip",
            excerpt = "Recipe clip: lemon pasta with parmesan.",
            sourceAppLabel = "Instagram",
        )
        val tailoringNumberNoise = result(
            envelopeId = "env-number-noise",
            title = "Tailoring time",
            excerpt = "Tailoring should take 15 minutes, not two hours.",
            sourceAppLabel = "Browser",
        )

        fun result(
            envelopeId: String,
            title: String,
            excerpt: String,
            sourceAppLabel: String,
        ) = MemorySearchResult(
            envelopeId = envelopeId,
            rank = 1,
            score = 1.0f,
            title = title,
            summary = excerpt,
            dayLocal = "2026-05-30",
            createdAtMillis = 1_780_000_000_000L,
            intent = "REFERENCE",
            sourceAppLabel = sourceAppLabel,
            matchedEvidence = listOf(
                MemoryEvidenceSnippet(
                    kind = "LOCAL_TEXT",
                    label = "Local capture",
                    excerpt = excerpt,
                    source = "envelope",
                )
            ),
        )
    }
}
