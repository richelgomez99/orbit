package com.orbit.app.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * T070 — user-facing privacy toggles backed by [SharedPreferences].
 *
 * v1 scope: only the "Pause continuations" switch. When paused,
 * [com.orbit.app.continuation.ContinuationEngine.enqueueSingle] no-ops
 * so no outbound network fetches are scheduled, and any in-flight
 * continuations are cancelled by calling [cancelAll] on pause.
 *
 * Lives in the default (`:ui`) process. Written only from the UI thread
 * via the Settings screen; read from the `:ml` process inside the engine
 * through a tiny multi-process `SharedPreferences` backed file. Multi-
 * process access is safe for a single boolean key (no read-modify-write
 * sequences), though not transactional — acceptable for a user-facing
 * toggle where a 50ms stale read is harmless.
 */
class PrivacyPreferences(context: Context) {

    private val prefs: SharedPreferences = context
        .applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var continuationsPaused: Boolean
        get() = prefs.getBoolean(KEY_CONTINUATIONS_PAUSED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_CONTINUATIONS_PAUSED, value).apply()
        }

    var memoryIndexingEnabled: Boolean
        get() = prefs.getBoolean(KEY_MEMORY_INDEXING_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_MEMORY_INDEXING_ENABLED, value).apply()
        }

    var cloudAskSynthesisEnabled: Boolean
        get() = prefs.getBoolean(KEY_CLOUD_ASK_SYNTHESIS_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_CLOUD_ASK_SYNTHESIS_ENABLED, value).apply()
        }

    var cloudAiRoutingEnabled: Boolean
        get() = prefs.getBoolean(KEY_CLOUD_AI_ROUTING_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_CLOUD_AI_ROUTING_ENABLED, value).apply()
        }

    /**
     * Spec 022 — persistent, multi-process "prefer on-device AI" switch.
     * When true (and a local model is installed + hardware-eligible),
     * [com.orbit.app.ai.LlmProviderRouter] routes inference to the BYOM
     * provider instead of the cloud. Read from every process via this
     * SharedPreferences file, so the toggle survives process death — unlike
     * the in-memory [com.orbit.app.RuntimeFlags.useLocalAi] debug flag.
     */
    var localAiEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCAL_AI_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_LOCAL_AI_ENABLED, value).apply()
        }

    var dailyCloudBudgetCents: Long?
        get() = prefs.getLong(KEY_DAILY_CLOUD_BUDGET_CENTS, NO_BUDGET_CAP)
            .takeUnless { it == NO_BUDGET_CAP }
        set(value) {
            prefs.edit().putLong(KEY_DAILY_CLOUD_BUDGET_CENTS, value ?: NO_BUDGET_CAP).apply()
        }

    companion object {
        const val PREFS_NAME = "orbit_privacy_prefs"
        const val KEY_CONTINUATIONS_PAUSED = "continuations_paused"
        const val KEY_MEMORY_INDEXING_ENABLED = "memory_indexing_enabled"
        const val KEY_CLOUD_ASK_SYNTHESIS_ENABLED = "cloud_ask_synthesis_enabled"
        const val KEY_CLOUD_AI_ROUTING_ENABLED = "cloud_ai_routing_enabled"
        const val KEY_LOCAL_AI_ENABLED = "local_ai_enabled"
        const val KEY_DAILY_CLOUD_BUDGET_CENTS = "daily_cloud_budget_cents"
        private const val NO_BUDGET_CAP = Long.MIN_VALUE
    }
}
