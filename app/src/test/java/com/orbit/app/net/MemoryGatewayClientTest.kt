package com.orbit.app.net

import com.orbit.app.memory.MemoryGatewayRequest
import com.orbit.app.memory.MemoryGatewayResponse
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class MemoryGatewayClientTest {

    @Test
    fun searchRequestOmitsNullFiltersForGatewaySchema() = runTest {
        val capturedBody = AtomicReference<String>()
        val mockClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                capturedBody.set(chain.request().body.bodyString())
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(
                        """
                        {"type":"memory_search_response","requestId":"$REQ_ID","ok":true,"data":{"results":[]}}
                        """.trimIndent().toResponseBody("application/json".toMediaType())
                    )
                    .build()
            })
            .build()
        val client = MemoryGatewayClient(
            client = mockClient,
            gatewayUrl = "https://gateway.test.invalid/memory",
            authStateBinder = FakeAuthStateBinder(initialJwt = "test.jwt"),
        )

        val response = client.call(
            MemoryGatewayRequest.Search(
                requestId = REQ_ID,
                query = "startup",
                filters = null,
            )
        )

        assertTrue(response is MemoryGatewayResponse.SearchResponse)
        val body = capturedBody.get()
        assertTrue(body.contains("\"type\":\"memory_search\""))
        assertTrue(body.contains("\"query\":\"startup\""))
        assertFalse(body.contains("\"filters\""))
        assertFalse(body.contains(":null"))
    }

    @Test
    fun semanticSearchAndGroundedAskRoundTripThroughGatewayEnvelope() = runTest {
        val capturedBodies = mutableListOf<String>()
        val mockClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val body = chain.request().body.bodyString()
                capturedBodies += body
                val responseBody = if (body.contains("\"memory_grounded_ask\"")) {
                    """
                    {"type":"memory_grounded_ask_response","requestId":"$REQ_ID","ok":true,"data":{"answer":{"status":"answered","answer":"You saved the flight receipt.","citations":[{"citationId":"c1","envelopeId":"env-flight","title":"Flight receipt","excerpt":"Flight receipt: NYC to San Francisco.","dayLocal":"2026-05-30","sourceAppLabel":"Gmail"}],"candidates":[],"modelLabel":"openai:gpt-4.1-mini","retrievalMode":"hybrid","confidence":0.9,"limitations":["Generated only from cited compact saved-memory evidence."]}}}
                    """.trimIndent()
                } else {
                    """
                    {"type":"memory_semantic_search_response","requestId":"$REQ_ID","ok":true,"data":{"results":[{"envelopeId":"env-flight","rank":1,"score":8.0,"semanticScore":0.8,"lexicalScore":2.0,"retrievalMode":"hybrid","embeddingModel":"openai:text-embedding-3-small","title":"Flight receipt","summary":"Receipt from last week","dayLocal":"2026-05-30","createdAtMillis":1780000000000,"intent":"REFERENCE","sourceAppLabel":"Gmail","matchedEvidence":[]}]}}
                    """.trimIndent()
                }
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(responseBody.toResponseBody("application/json".toMediaType()))
                    .build()
            })
            .build()
        val client = MemoryGatewayClient(
            client = mockClient,
            gatewayUrl = "https://gateway.test.invalid/memory",
            authStateBinder = FakeAuthStateBinder(initialJwt = "test.jwt"),
        )

        val search = client.call(
            MemoryGatewayRequest.SemanticSearch(
                requestId = REQ_ID,
                query = "flight receipt",
                filters = null,
            )
        )
        val ask = client.call(
            MemoryGatewayRequest.GroundedAsk(
                requestId = REQ_ID,
                question = "Which flight receipt did I save?",
                filters = null,
                allowSynthesis = true,
            )
        )

        assertTrue(search is MemoryGatewayResponse.SemanticSearchResponse)
        search as MemoryGatewayResponse.SemanticSearchResponse
        assertTrue(search.results.first().retrievalMode == "hybrid")
        assertTrue(ask is MemoryGatewayResponse.GroundedAskResponse)
        ask as MemoryGatewayResponse.GroundedAskResponse
        assertTrue(ask.answer.modelLabel.startsWith("openai:"))
        assertTrue(capturedBodies.any { it.contains("\"type\":\"memory_semantic_search\"") })
        assertTrue(capturedBodies.any { it.contains("\"type\":\"memory_grounded_ask\"") })
        assertFalse(capturedBodies.joinToString("\n").contains(":null"))
    }

    private fun okhttp3.RequestBody?.bodyString(): String {
        val buffer = okio.Buffer()
        requireNotNull(this).writeTo(buffer)
        return buffer.readUtf8()
    }

    private companion object {
        const val REQ_ID = "11111111-1111-4111-8111-111111111111"
    }
}
