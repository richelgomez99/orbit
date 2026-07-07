package com.orbit.app.ai.local

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.orbit.app.ai.LlmProvider
import com.orbit.app.net.ModelDownloadStore

/**
 * Spec 022 — production glue between the (pure) [LocalModelSelectionPolicy]
 * and the (heavy) [MediaPipeLlmProvider]. Kept out of [LocalModelCatalog] and
 * the policy so those stay pure/data-only and unit-testable without Android.
 *
 * Probes real device hardware, enumerates which catalog models are actually
 * downloaded, and hands the router a ready-to-use BYOM provider when the
 * policy selects a local route.
 */
object DeviceAiHardware {
    /** Best-effort device profile for [LocalModelSelectionPolicy]. */
    fun probe(context: Context): DeviceAiHardwareProfile {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val pm = context.packageManager
        val vulkan = pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
        return DeviceAiHardwareProfile(
            totalRamMb = (mi.totalMem / (1024L * 1024L)).toInt(),
            availableRamMb = (mi.availMem / (1024L * 1024L)).toInt(),
            apiLevel = Build.VERSION.SDK_INT,
            supportsVulkan = vulkan,
            // Real Nano hardware detection is a separate spec; the router's
            // legacy-Nano fallback stays off here, matching its Day-1 stub.
            legacyNanoCapable = false,
        )
    }
}

/** Map the catalog to install state on disk (via [ModelDownloadStore]). */
fun installedLocalModels(context: Context): List<InstalledLocalModel> =
    LocalModelCatalog.ALL.map { model ->
        InstalledLocalModel(
            tier = model.tier,
            modelLabel = model.id,
            downloaded = ModelDownloadStore.isInstalled(context, model.id),
            capabilities = model.capabilities,
        )
    }

/**
 * Per-process singleton for the BYOM engine. Building a [MediaPipeLlmProvider]
 * loads a multi-hundred-MB model and costs several seconds, so every consumer
 * in a process shares one instance keyed by model file path.
 *
 * Multi-process note: several Orbit processes call the router (e.g. `:ml` for
 * enrichment/digest, `:ui` for day headers). Each that selects local builds its
 * own engine here. The `.task` weights are mmap'd read-only, so the OS page
 * cache shares the ~500MB across processes — only the per-process working set
 * (KV cache/activations) is duplicated. Routing all local inference through a
 * single `:ml` engine over AIDL is the proper next refinement.
 */
object ByomLocalProviderHolder {
    private const val TAG = "ByomLocalProvider"

    @Volatile
    private var cached: Pair<String, MediaPipeLlmProvider>? = null

    @Synchronized
    fun get(context: Context, modelId: String): MediaPipeLlmProvider {
        val path = ModelDownloadStore.modelFile(context, modelId).absolutePath
        cached?.let { (cachedPath, provider) ->
            if (cachedPath == path) return provider
            runCatching { provider.close() } // model switched — release the old engine
        }
        Log.i(TAG, "creating BYOM engine for $modelId")
        val provider = MediaPipeLlmProvider(
            appContext = context.applicationContext,
            modelPath = path,
            modelLabel = modelId,
        )
        cached = path to provider
        return provider
    }
}

/**
 * Returns a ready BYOM [LlmProvider] when [selection] routes to an installed,
 * hardware-eligible local model — else `null` (caller falls back to Nano/cloud).
 */
fun byomProviderForSelection(context: Context, selection: LocalModelSelection): LlmProvider? {
    if (selection.route != LocalModelRoute.LOCAL) return null
    if (selection.tier != LocalModelTier.SPEED && selection.tier != LocalModelTier.INTELLIGENCE) {
        return null
    }
    val modelId = selection.modelLabel ?: return null
    if (!ModelDownloadStore.isInstalled(context, modelId)) return null
    return ByomLocalProviderHolder.get(context, modelId)
}
