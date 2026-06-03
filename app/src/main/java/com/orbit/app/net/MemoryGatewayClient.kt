package com.orbit.app.net

import com.orbit.app.BuildConfig
import com.orbit.app.memory.MemoryGatewayRequest
import com.orbit.app.memory.MemoryGatewayResponse
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Spec 005 — `:net`-process HTTP client for Orbit's memory gateway.
 *
 * This client talks to Orbit's backend, not Atlas directly. Atlas credentials
 * stay server-side in `supabase/functions/memory_gateway/.env.local`.
 */
class MemoryGatewayClient(
    private val client: OkHttpClient = SafeOkHttpClient.build(),
    private val gatewayUrl: String = DEFAULT_GATEWAY_URL,
    private val authStateBinder: AuthStateBinder = NoSessionAuthStateBinder,
) {

    init {
        when (val v = UrlValidator(requireHttps = true).validate(gatewayUrl)) {
            is UrlValidator.Validation.Invalid -> error(
                "MemoryGatewayClient: invalid MEMORY_GATEWAY_URL (${v.errorKind}: ${v.reason})"
            )
            is UrlValidator.Validation.Valid -> Unit
        }
    }

    private val jsonCodec: Json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun call(request: MemoryGatewayRequest): MemoryGatewayResponse =
        withContext(Dispatchers.IO) {
            val jwt = authStateBinder.currentJwt()
            if (jwt.isNullOrBlank()) {
                return@withContext MemoryGatewayResponse.Error(
                    requestId = request.requestId,
                    code = "UNAUTHORIZED",
                    message = "no active Supabase session",
                )
            }

            val perCallClient = client.newBuilder()
                .callTimeout(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build()
            postOnce(
                client = perCallClient,
                body = wrapToHttpEnvelope(request),
                original = request,
                jwt = jwt,
            )
        }

    private fun postOnce(
        client: OkHttpClient,
        body: String,
        original: MemoryGatewayRequest,
        jwt: String,
    ): MemoryGatewayResponse {
        val httpRequest = Request.Builder()
            .url(gatewayUrl)
            .header("Content-Type", "application/json; charset=utf-8")
            .header("X-Orbit-Request-Id", original.requestId)
            .header("Authorization", "Bearer $jwt")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return try {
            client.newCall(httpRequest).execute().use { response ->
                val raw = response.body?.let { rb ->
                    val source = rb.source()
                    source.request((MAX_BODY_BYTES + 1).toLong())
                    val buffered = source.buffer
                    if (buffered.size > MAX_BODY_BYTES) {
                        return@use MemoryGatewayResponse.Error(
                            original.requestId,
                            "MALFORMED_RESPONSE",
                            "response body exceeded $MAX_BODY_BYTES bytes",
                        )
                    }
                    buffered.readUtf8()
                }.orEmpty()

                if (!response.isSuccessful) {
                    val code = if (response.code == 401 || response.code == 403) {
                        "UNAUTHORIZED"
                    } else {
                        "INTERNAL"
                    }
                    MemoryGatewayResponse.Error(original.requestId, code, "HTTP ${response.code}")
                } else {
                    unwrapHttpEnvelope(raw, original.requestId)
                }
            }
        } catch (e: SocketTimeoutException) {
            Log.w(TAG, "memory gateway timeout: ${e.message}")
            MemoryGatewayResponse.Error(original.requestId, "TIMEOUT", e.message ?: "timeout")
        } catch (e: UnknownHostException) {
            Log.w(TAG, "memory gateway dns failure: ${e.message}")
            MemoryGatewayResponse.Error(original.requestId, "NETWORK_UNAVAILABLE", e.message ?: "dns")
        } catch (e: IOException) {
            Log.w(TAG, "memory gateway io failure: ${e.javaClass.simpleName}: ${e.message}")
            MemoryGatewayResponse.Error(original.requestId, "NETWORK_UNAVAILABLE", e.message ?: "io")
        }
    }

    private fun wrapToHttpEnvelope(request: MemoryGatewayRequest): String {
        val flat = jsonCodec.encodeToJsonElement(MemoryGatewayRequest.serializer(), request).jsonObject
        val payload = stripNulls(JsonObject(flat.filterKeys { it != "type" && it != "requestId" })) as JsonObject
        val envelope = buildJsonObject {
            put("type", flat["type"]!!)
            put("requestId", request.requestId)
            put("payload", payload)
        }
        return jsonCodec.encodeToString(JsonObject.serializer(), envelope)
    }

    private fun stripNulls(element: JsonElement): JsonElement = when (element) {
        JsonNull -> JsonNull
        is JsonObject -> JsonObject(
            element.mapNotNull { (key, value) ->
                val stripped = stripNulls(value)
                if (stripped is JsonNull) null else key to stripped
            }.toMap()
        )
        is JsonArray -> JsonArray(
            element.mapNotNull { value ->
                val stripped = stripNulls(value)
                if (stripped is JsonNull) null else stripped
            }
        )
        else -> element
    }

    private fun unwrapHttpEnvelope(raw: String, requestId: String): MemoryGatewayResponse = try {
        val outer = jsonCodec.parseToJsonElement(raw).jsonObject
        val typeValue = outer["type"]
            ?: return MemoryGatewayResponse.Error(requestId, "MALFORMED_RESPONSE", "missing type")
        val ok = outer["ok"]?.toString()?.equals("true", ignoreCase = true) ?: false
        if (!ok || outer.containsKey("error")) {
            val err = outer["error"]?.jsonObject
            val code = err?.get("code")?.toString()?.trim('"') ?: "INTERNAL"
            val msg = err?.get("message")?.toString()?.trim('"') ?: "unknown"
            MemoryGatewayResponse.Error(requestId, code, msg)
        } else {
            val data = outer["data"]?.jsonObject ?: JsonObject(emptyMap())
            val flat = buildJsonObject {
                put("type", typeValue)
                put("requestId", requestId)
                data.forEach { (k, v) -> put(k, v) }
            }
            jsonCodec.decodeFromJsonElement(MemoryGatewayResponse.serializer(), flat)
        }
    } catch (e: Throwable) {
        MemoryGatewayResponse.Error(requestId, "MALFORMED_RESPONSE", e.message ?: "parse")
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS: Long = 30_000L
        const val MAX_BODY_BYTES: Int = 2 * 1024 * 1024
        private const val DEFAULT_GATEWAY_URL: String = BuildConfig.MEMORY_GATEWAY_URL
        private const val TAG: String = "MemoryGatewayClient"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
