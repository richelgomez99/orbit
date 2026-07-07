package com.orbit.app.ai.ipc

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Process
import android.util.Log
import com.orbit.app.ai.LlmProvider
import com.orbit.app.ai.local.DeviceAiHardware
import com.orbit.app.ai.local.LocalModelSelectionPolicy
import com.orbit.app.ai.local.byomProviderForSelection
import com.orbit.app.ai.local.installedLocalModels
import com.orbit.app.settings.PrivacyPreferences
import kotlinx.coroutines.runBlocking

/**
 * Spec 022 M1 — the ONLY place a BYOM engine loads. Runs in `:ml`
 * (manifest `android:process=":ml"`), owns the single
 * [com.orbit.app.ai.local.MediaPipeLlmProvider] via the in-process
 * [com.orbit.app.ai.local.ByomLocalProviderHolder] singleton, and serves
 * generation to every other process over [ILocalInference]. No network
 * (Principle II/VI); the model file was fetched by `:net` and is mmap'd here.
 */
class LocalInferenceService : Service() {

    private val binder = object : ILocalInference.Stub() {
        override fun infer(request: LocalInferenceRequestParcel): LocalInferenceResponseParcel {
            // Same-app UID gate, mirroring the :net gateway.
            if (Binder.getCallingUid() != Process.myUid()) {
                return errorParcel("", "UNAUTHORIZED", "caller uid not allowed")
            }
            val decoded = try {
                LocalInferenceJson.decodeFromString(
                    LocalInferenceRequest.serializer(), request.payloadJson,
                )
            } catch (t: Throwable) {
                return errorParcel("", "MALFORMED_REQUEST", t.message ?: "decode failed")
            }

            // Being invoked at all means the caller's router already chose
            // local, so force localFirstEnabled=true — the per-process
            // useLocalAi flag / pref need not be true in :ml specifically.
            val selection = LocalModelSelectionPolicy.select(
                localFirstEnabled = true,
                cloudRoutingEnabled = PrivacyPreferences(applicationContext).cloudAiRoutingEnabled,
                hardware = DeviceAiHardware.probe(applicationContext),
                installedModels = installedLocalModels(applicationContext),
            )
            val provider: LlmProvider = byomProviderForSelection(applicationContext, selection)
                ?: return errorParcel(decoded.requestId, "NO_LOCAL_MODEL", "no installed local model")
            val label = selection.modelLabel ?: "local"

            val response: LocalInferenceResponse = try {
                runBlocking { dispatch(decoded, provider, label) }
            } catch (t: Throwable) {
                Log.w(TAG, "infer failed: ${t.javaClass.simpleName}: ${t.message}")
                LocalInferenceResponse.Error(
                    decoded.requestId, "INFERENCE_FAILED", t.message ?: "inference threw",
                )
            }
            return LocalInferenceResponseParcel(
                LocalInferenceJson.encodeToString(LocalInferenceResponse.serializer(), response),
            )
        }
    }

    private suspend fun dispatch(
        request: LocalInferenceRequest,
        provider: LlmProvider,
        label: String,
    ): LocalInferenceResponse = when (request) {
        is LocalInferenceRequest.Summarize -> {
            val r = provider.summarize(request.text, request.maxTokens)
            LocalInferenceResponse.Summary(request.requestId, r.text, r.generationLocale, label)
        }
        is LocalInferenceRequest.GenerateDayHeader -> {
            val r = provider.generateDayHeader(request.dayIsoDate, request.envelopeSummaries)
            LocalInferenceResponse.DayHeader(request.requestId, r.text, r.generationLocale, label)
        }
        is LocalInferenceRequest.ClassifyIntent -> {
            val r = provider.classifyIntent(request.text, request.appCategory)
            LocalInferenceResponse.Intent(request.requestId, r.intent.name, r.confidence, label)
        }
        is LocalInferenceRequest.ScanSensitivity -> {
            val r = provider.scanSensitivity(request.text)
            LocalInferenceResponse.Sensitivity(request.requestId, r.flagsJson, label)
        }
    }

    private fun errorParcel(requestId: String, code: String, message: String) =
        LocalInferenceResponseParcel(
            LocalInferenceJson.encodeToString(
                LocalInferenceResponse.serializer(),
                LocalInferenceResponse.Error(requestId, code, message),
            ),
        )

    override fun onBind(intent: Intent?): IBinder = binder

    companion object {
        const val TAG = "LocalInferenceService"
        const val ACTION_BIND = "com.orbit.app.action.BIND_LOCAL_INFERENCE"
    }
}
