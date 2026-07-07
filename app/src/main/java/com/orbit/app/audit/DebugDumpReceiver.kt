package com.orbit.app.audit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.orbit.app.BuildConfig
import com.orbit.app.data.OrbitDatabase
import com.orbit.app.data.model.AuditAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * T105 / T106 — dev-only broadcast-driven counter dump.
 *
 * Triggered by `adb shell am broadcast -a com.orbit.app.DEBUG_DUMP`
 * (see quickstart.md §Debug Dump). Writes to logcat under tag
 * "OrbitDebugDump". No-op on release builds — both the [BuildConfig.DEBUG]
 * guard here and the register-only-in-debug path in
 * [com.orbit.app.OrbitApplication] prevent any production exposure.
 *
 * Counters pulled at dump time:
 *  - envelopes: total / archived / soft-deleted
 *  - continuations: per-status from `countByStatus()`
 *  - continuation success rate: completed / (completed + failed_*)
 *  - audit: last-24h row counts grouped by action
 *  - diary opens: persistent counter from [DebugCounters]
 */
class DebugDumpReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (!BuildConfig.DEBUG) return
        val action = intent?.action
        if (action !in setOf(
                ACTION, ACTION_SEED, ACTION_CLEAR_SEED, ACTION_DOWNLOAD_MODEL,
                ACTION_TEST_INFERENCE, ACTION_TEST_ROUTED, ACTION_TEST_CLASSIFY,
            )
        ) {
            return
        }
        val pending = goAsync()
        val appCtx = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_SEED -> {
                        val n = com.orbit.app.data.DebugCorpusSeeder.seed(
                            OrbitDatabase.getInstance(appCtx),
                            System.currentTimeMillis(),
                        )
                        Log.i(TAG, "seeded demo corpus: $n envelopes")
                    }
                    ACTION_CLEAR_SEED -> {
                        com.orbit.app.data.DebugCorpusSeeder.clear(OrbitDatabase.getInstance(appCtx))
                        Log.i(TAG, "cleared demo corpus")
                    }
                    ACTION_DOWNLOAD_MODEL -> {
                        // Debug: exercise the :net model-download pipe end to
                        // end. Pass --es url <URL> (any https file works for
                        // testing the pipe) and optional --es id <modelId>.
                        val url = intent.getStringExtra("url")
                        val id = intent.getStringExtra("id") ?: "gemma-3-1b-it-int4"
                        // Optional bearer token for gated hosts (e.g. HF). Never
                        // logged — only its presence is noted.
                        val token = intent.getStringExtra("token")?.takeIf { it.isNotBlank() }
                        if (url.isNullOrBlank()) {
                            Log.w(TAG, "download: missing --es url")
                        } else {
                            com.orbit.app.net.ModelDownloadTrigger.start(appCtx, id, url, authToken = token)
                            Log.i(TAG, "requested model download id=$id url=$url auth=${token != null}")
                        }
                    }
                    ACTION_TEST_INFERENCE -> {
                        // Debug: prove BYOM on-device generation end to end.
                        // Loads the downloaded .task and runs one real prompt.
                        // --es id <modelId> (default gemma-3-1b-it-int4)
                        // --es prompt "<text>" (default sample)
                        val id = intent.getStringExtra("id") ?: "gemma-3-1b-it-int4"
                        val prompt = intent.getStringExtra("prompt")
                            ?: "In one sentence, what is a good reason to keep a personal journal?"
                        val modelFile = com.orbit.app.net.ModelDownloadStore.modelFile(appCtx, id)
                        if (!modelFile.exists()) {
                            Log.w(TAG, "inference: model not installed at ${modelFile.absolutePath}")
                        } else {
                            val provider = com.orbit.app.ai.local.MediaPipeLlmProvider(
                                appContext = appCtx,
                                modelPath = modelFile.absolutePath,
                                modelLabel = id,
                            )
                            val startedAt = System.currentTimeMillis()
                            Log.i(TAG, "inference: loading $id and generating…")
                            try {
                                val result = provider.summarize(prompt, maxTokens = 64)
                                val ms = System.currentTimeMillis() - startedAt
                                Log.i(TAG, "inference OK (${ms}ms): ${result.text}")
                            } finally {
                                provider.close()
                            }
                        }
                    }
                    ACTION_TEST_ROUTED -> {
                        // Debug: prove the PRODUCTION router picks BYOM local.
                        // Flips useLocalAi in this process, then resolves via
                        // the same LlmProviderRouter.createPreferLocal path real
                        // consumers use, logs the concrete provider class, and
                        // runs one generation to confirm it's the local engine.
                        com.orbit.app.RuntimeFlags.useLocalAi = true
                        val prompt = intent.getStringExtra("prompt")
                            ?: "In one sentence, why keep a personal journal?"
                        val provider = com.orbit.app.ai.LlmProviderRouter.createPreferLocal(appCtx)
                        Log.i(TAG, "routed provider = ${provider.javaClass.simpleName}")
                        try {
                            val startedAt = System.currentTimeMillis()
                            val result = provider.summarize(prompt, maxTokens = 64)
                            val ms = System.currentTimeMillis() - startedAt
                            Log.i(TAG, "routed inference OK (${ms}ms): ${result.text}")
                        } catch (t: Throwable) {
                            Log.w(TAG, "routed inference failed: ${t.javaClass.simpleName}: ${t.message}")
                        }
                    }
                    ACTION_TEST_CLASSIFY -> {
                        // Debug: exercise the prompt-and-parse paths on the
                        // installed local model via the PRODUCTION router
                        // singleton (ByomLocalProviderHolder) — NOT a fresh
                        // engine: a second LlmInference on the same model in one
                        // process deadlocks. --es text "<capture text>".
                        com.orbit.app.RuntimeFlags.useLocalAi = true
                        val text = intent.getStringExtra("text")
                            ?: "Order the new noise-cancelling headphones before the sale ends Friday"
                        val provider = com.orbit.app.ai.LlmProviderRouter.createPreferLocal(appCtx)
                        // Off-:ml this is a RemoteLocalLlmProvider proxying to the
                        // single :ml engine; the NanoLlmProvider fallback means no
                        // local model is installed/selected.
                        if (provider is com.orbit.app.ai.NanoLlmProvider) {
                            Log.w(TAG, "classify: not local (NanoLlmProvider); install a model first")
                        } else {
                            Log.i(TAG, "classify via ${provider.javaClass.simpleName}")
                            // Shared singleton — must NOT be closed here.
                            val intentResult = provider.classifyIntent(text, appCategory = "OTHER")
                            Log.i(TAG, "classify intent=${intentResult.intent} conf=${intentResult.confidence}")
                            val sens = provider.scanSensitivity(text)
                            Log.i(TAG, "classify sensitivity=${sens.flagsJson}")
                        }
                    }
                    else -> dump(appCtx)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "debug action '$action' failed", t)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun dump(appCtx: Context) {
        val db = OrbitDatabase.getInstance(appCtx)
        val envelopes = db.intentEnvelopeDao()
        val continuations = db.continuationDao()
        val audit = db.auditLogDao()

        val total = envelopes.countAll()
        val archived = envelopes.countArchived()
        val deleted = envelopes.countDeleted()

        val statuses = continuations.countByStatus()
        val succeeded = statuses.firstOrNull { it.status == "COMPLETED" }?.n ?: 0
        val failed = statuses.filter { it.status.startsWith("FAILED") }.sumOf { it.n }
        val denom = succeeded + failed
        val rate = if (denom == 0) "n/a" else String.format("%.1f%%", 100.0 * succeeded / denom)

        val dayMillis = 24L * 60L * 60L * 1000L
        val now = System.currentTimeMillis()
        val last24 = audit.entriesForDay(now - dayMillis, now)
            .groupingBy { it.action }
            .eachCount()

        val diaryOpens = DebugCounters.diaryOpens(appCtx)

        val sb = StringBuilder()
        sb.appendLine("=== Orbit debug dump ===")
        sb.appendLine("envelopes: total=$total archived=$archived soft_deleted=$deleted")
        sb.append("continuations: ")
        if (statuses.isEmpty()) sb.append("(none)") else
            sb.append(statuses.joinToString(" ") { "${it.status}=${it.n}" })
        sb.appendLine()
        sb.appendLine("continuation_success_rate: $rate ($succeeded/$denom)")
        sb.appendLine("diary_opens: $diaryOpens")
        sb.appendLine("audit_last_24h:")
        if (last24.isEmpty()) sb.appendLine("  (none)") else
            AuditAction.entries.forEach { a ->
                val n = last24[a] ?: 0
                if (n > 0) sb.appendLine("  ${a.name}=$n")
            }
        Log.i(TAG, sb.toString())
    }

    companion object {
        const val ACTION = "com.orbit.app.DEBUG_DUMP"
        const val ACTION_SEED = "com.orbit.app.DEBUG_SEED_CORPUS"
        const val ACTION_CLEAR_SEED = "com.orbit.app.DEBUG_CLEAR_CORPUS"
        const val ACTION_DOWNLOAD_MODEL = "com.orbit.app.DEBUG_DOWNLOAD_MODEL"
        const val ACTION_TEST_INFERENCE = "com.orbit.app.DEBUG_TEST_INFERENCE"
        const val ACTION_TEST_ROUTED = "com.orbit.app.DEBUG_TEST_ROUTED"
        const val ACTION_TEST_CLASSIFY = "com.orbit.app.DEBUG_TEST_CLASSIFY"
        private const val TAG = "OrbitDebugDump"
    }
}
