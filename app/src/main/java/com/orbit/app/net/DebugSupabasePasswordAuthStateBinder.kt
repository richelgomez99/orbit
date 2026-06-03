package com.orbit.app.net

import android.util.Log
import com.orbit.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Debug-only fallback for MVP devices where the Supabase SDK/session manager
 * cannot initialize. It stays inside :net and uses the normal Supabase Auth
 * password endpoint with the debug user from local.properties.
 */
class DebugSupabasePasswordAuthStateBinder(
    private val client: OkHttpClient,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : AuthStateBinder {
    @Volatile private var cachedJwt: String? = null
    @Volatile private var expiresAtMillis: Long = 0L
    private val mutex = Mutex()

    override suspend fun currentJwt(): String? = withContext(Dispatchers.IO) {
        if (!BuildConfig.DEBUG) return@withContext null
        val url = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        val key = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()
        val email = BuildConfig.DEBUG_SUPABASE_EMAIL.trim()
        val password = BuildConfig.DEBUG_SUPABASE_PASSWORD
        if (url.isBlank() || key.isBlank() || email.isBlank() || password.isBlank()) {
            return@withContext null
        }
        cachedJwt?.takeIf { it.isNotBlank() && clock() < expiresAtMillis }?.let {
            return@withContext it
        }

        mutex.withLock {
            cachedJwt?.takeIf { it.isNotBlank() && clock() < expiresAtMillis }?.let {
                return@withLock it
            }
            fetchTokenWithRetry(url = url, key = key, email = email, password = password)
        }
    }

    private suspend fun fetchTokenWithRetry(
        url: String,
        key: String,
        email: String,
        password: String,
    ): String? {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url("$url/auth/v1/token?grant_type=password")
            .header("apikey", key)
            .header("Content-Type", "application/json; charset=utf-8")
            .post(body)
            .build()

        repeat(AUTH_ATTEMPTS) { attempt ->
            val token = runCatching {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "debug password auth failed http=${response.code}")
                        return null
                    }
                    val json = JSONObject(response.body?.string().orEmpty())
                    val token = json.optString("access_token").takeIf { it.isNotBlank() }
                        ?: return null
                    val expiresInSeconds = json.optLong("expires_in", 3600L).coerceAtLeast(120L)
                    cachedJwt = token
                    expiresAtMillis = clock() + (expiresInSeconds - 60L) * 1000L
                    Log.i(TAG, "debug password auth session seeded")
                    token
                }
            }.getOrElse { error ->
                Log.w(TAG, "debug password auth threw ${error.javaClass.simpleName}: ${error.message}")
                null
            }
            if (!token.isNullOrBlank()) return token
            if (attempt < AUTH_ATTEMPTS - 1) delay(AUTH_RETRY_DELAY_MS)
        }
        return null
    }

    private companion object {
        const val TAG = "DebugSupabaseAuth"
        const val AUTH_ATTEMPTS = 3
        const val AUTH_RETRY_DELAY_MS = 750L
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
