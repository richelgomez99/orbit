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

    private fun okhttp3.RequestBody?.bodyString(): String {
        val buffer = okio.Buffer()
        requireNotNull(this).writeTo(buffer)
        return buffer.readUtf8()
    }

    private companion object {
        const val REQ_ID = "11111111-1111-4111-8111-111111111111"
    }
}
