package com.orbit.app.ai.local

/**
 * Spec 022 — the catalog of BYOM models Orbit can offer to download.
 *
 * This is pure data: the "what could be installed" list that the model
 * manager (download + storage) and [LocalModelSelectionPolicy] (which
 * installed model to route to) build on. No engine or network here.
 *
 * v1 target is Google's MediaPipe LLM Inference (LiteRT-LM) path, which
 * runs Gemma `.task` bundles on-device via `com.google.mediapipe:
 * tasks-genai` — the most documented, dependency-only Android route (no
 * hand-rolled JNI). Weights MUST be memory-mapped at load (file-backed,
 * reclaimable) per docs/ml-process-memory-baseline-2026-07-06.md, so the
 * ~500MB–1.3GB working set doesn't sit as anonymous RAM in :ml.
 *
 * Architectural note (open decision, tracked): the download crosses the
 * network boundary that :ml must not — the file has to be fetched via
 * :net (or a dedicated download service) and written to a path :ml can
 * mmap. See the model-manager download slice.
 */
data class DownloadableModel(
    val id: String,
    val tier: LocalModelTier,
    val displayName: String,
    /** One-line "what it's for" shown on the model-store row. */
    val blurb: String,
    val approxDownloadBytes: Long,
    /** Minimum total device RAM to offer this tier (MB). */
    val minTotalRamMb: Int,
    /** Capabilities this model is trusted to power (intersected with tier caps). */
    val capabilities: Set<LocalAiCapability>,
    /** Engine that loads it — v1 is MediaPipe LLM Inference (LiteRT-LM). */
    val engine: LocalModelEngine,
    /** MediaPipe `.task` bundle filename once downloaded to app storage. */
    val assetFileName: String,
) {
    /** Human size, e.g. "529 MB". */
    val approxDownloadLabel: String
        get() = "%,d MB".format(approxDownloadBytes / (1024 * 1024))
}

enum class LocalModelEngine {
    /** Google MediaPipe LLM Inference / LiteRT-LM (`tasks-genai`). */
    MEDIAPIPE_LLM,
}

object LocalModelCatalog {

    /**
     * Speed tier — Gemma 3 1B, INT4 quantized. ~529 MB, runs on 4GB-class
     * devices. Trusted for extraction/basic-understanding + embeddings;
     * NOT deep Ask/generative UI (that is the Intelligence tier), matching
     * LocalModelCapabilities.SPEED.
     */
    val GEMMA_3_1B_INT4 = DownloadableModel(
        id = "gemma-3-1b-it-int4",
        tier = LocalModelTier.SPEED,
        displayName = "Gemma 3 1B (Speed)",
        blurb = "On-device extraction and basic understanding. Fast, ~529 MB.",
        approxDownloadBytes = 529L * 1024 * 1024,
        minTotalRamMb = 4_096,
        capabilities = LocalModelCapabilities.SPEED,
        engine = LocalModelEngine.MEDIAPIPE_LLM,
        assetFileName = "gemma-3-1b-it-int4.task",
    )

    /**
     * Intelligence tier — Gemma 3 4B, INT4. ~1.3 GB, for offline deep
     * Ask + generative-UI generation on higher-memory devices.
     */
    val GEMMA_3_4B_INT4 = DownloadableModel(
        id = "gemma-3-4b-it-int4",
        tier = LocalModelTier.INTELLIGENCE,
        displayName = "Gemma 3 4B (Intelligence)",
        blurb = "Offline deep Ask and generative UI. ~1.3 GB, needs 6GB+ RAM.",
        approxDownloadBytes = 1_300L * 1024 * 1024,
        minTotalRamMb = 6_144,
        capabilities = LocalModelCapabilities.INTELLIGENCE,
        engine = LocalModelEngine.MEDIAPIPE_LLM,
        assetFileName = "gemma-3-4b-it-int4.task",
    )

    val ALL: List<DownloadableModel> = listOf(GEMMA_3_1B_INT4, GEMMA_3_4B_INT4)

    /** Models this device has enough RAM to run, given a hardware profile. */
    fun offerableFor(hardware: DeviceAiHardwareProfile): List<DownloadableModel> =
        ALL.filter { hardware.totalRamMb >= it.minTotalRamMb }

    fun byId(id: String): DownloadableModel? = ALL.firstOrNull { it.id == id }
}
