package com.orbit.app.ai.ipc

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
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
import com.orbit.app.data.model.Intent as DomainIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Spec 022 M1 — an [LlmProvider] that proxies to the single BYOM engine in
 * `:ml` via [ILocalInference]. Used by the router in every process EXCEPT
 * `:ml` (which talks to the in-process engine directly). Keeps all local
 * generation on one warm engine and honours "inference in :ml".
 *
 * Only the four served methods cross Binder; [extractActions] (empty) and
 * [embed] (null) short-circuit locally, matching [com.orbit.app.ai.local.MediaPipeLlmProvider].
 */
class RemoteLocalLlmProvider(context: Context) : LlmProvider {

    private val appContext = context.applicationContext

    @Volatile
    private var service: ILocalInference? = null
    private val bindMutex = Mutex()

    private suspend fun binder(): ILocalInference {
        service?.let { return it }
        return bindMutex.withLock {
            service ?: suspendCancellableCoroutine { cont ->
                val conn = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, b: IBinder?) {
                        val s = ILocalInference.Stub.asInterface(b)
                        service = s
                        if (cont.isActive) cont.resume(s)
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        service = null
                    }
                }
                val bound = appContext.bindService(
                    Intent(appContext, LocalInferenceService::class.java),
                    conn,
                    Context.BIND_AUTO_CREATE,
                )
                if (!bound && cont.isActive) {
                    cont.resumeWithException(IOException("bind LocalInferenceService failed"))
                }
                // Binding held for the process lifetime (like the engine it fronts).
            }
        }
    }

    private suspend fun infer(request: LocalInferenceRequest): LocalInferenceResponse =
        withContext(Dispatchers.IO) {
            val parcel = binder().infer(
                LocalInferenceRequestParcel(
                    LocalInferenceJson.encodeToString(LocalInferenceRequest.serializer(), request),
                ),
            )
            LocalInferenceJson.decodeFromString(
                LocalInferenceResponse.serializer(), parcel.payloadJson,
            )
        }

    private fun fail(resp: LocalInferenceResponse.Error): Nothing =
        throw IOException("${resp.code}: ${resp.message}")

    override suspend fun summarize(text: String, maxTokens: Int): SummaryResult {
        val id = UUID.randomUUID().toString()
        return when (val r = infer(LocalInferenceRequest.Summarize(id, text, maxTokens))) {
            is LocalInferenceResponse.Summary ->
                SummaryResult(r.text, r.generationLocale, LlmProvenance.LocalByom(r.modelLabel))
            is LocalInferenceResponse.Error -> fail(r)
            else -> throw IOException("unexpected response ${r::class.simpleName}")
        }
    }

    override suspend fun generateDayHeader(
        dayIsoDate: String,
        envelopeSummaries: List<String>,
    ): DayHeaderResult {
        val id = UUID.randomUUID().toString()
        return when (
            val r = infer(LocalInferenceRequest.GenerateDayHeader(id, dayIsoDate, envelopeSummaries))
        ) {
            is LocalInferenceResponse.DayHeader ->
                DayHeaderResult(r.text, r.generationLocale, LlmProvenance.LocalByom(r.modelLabel))
            is LocalInferenceResponse.Error -> fail(r)
            else -> throw IOException("unexpected response ${r::class.simpleName}")
        }
    }

    override suspend fun classifyIntent(text: String, appCategory: String): IntentClassification {
        val id = UUID.randomUUID().toString()
        return when (val r = infer(LocalInferenceRequest.ClassifyIntent(id, text, appCategory))) {
            is LocalInferenceResponse.Intent -> IntentClassification(
                intent = runCatching { DomainIntent.valueOf(r.intent) }.getOrDefault(DomainIntent.AMBIGUOUS),
                confidence = r.confidence,
                provenance = LlmProvenance.LocalByom(r.modelLabel),
            )
            is LocalInferenceResponse.Error -> fail(r)
            else -> throw IOException("unexpected response ${r::class.simpleName}")
        }
    }

    override suspend fun scanSensitivity(text: String): SensitivityResult {
        val id = UUID.randomUUID().toString()
        return when (val r = infer(LocalInferenceRequest.ScanSensitivity(id, text))) {
            is LocalInferenceResponse.Sensitivity ->
                SensitivityResult(r.flagsJson, LlmProvenance.LocalByom(r.modelLabel))
            is LocalInferenceResponse.Error -> fail(r)
            else -> throw IOException("unexpected response ${r::class.simpleName}")
        }
    }

    // Short-circuited locally — no round-trip (matches MediaPipeLlmProvider).
    override suspend fun extractActions(
        text: String,
        contentType: String,
        state: StateSnapshot,
        registeredFunctions: List<AppFunctionSummary>,
        maxCandidates: Int,
    ): ActionExtractionResult =
        ActionExtractionResult(provenance = LlmProvenance.LocalByom("local"), candidates = emptyList())

    override suspend fun embed(text: String): EmbeddingResult? = null
}
