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
    /**
     * Resolvable https source for the `.task` weights. Gemma repos are
     * license-gated on Hugging Face — the download needs a Read token
     * (the model manager supplies one). The download runs in :net.
     */
    val sourceUrl: String,
    /**
     * Whether [sourceUrl] is an Android-loadable MediaPipe `.task` (a zip
     * bundling model + tokenizer + metadata). false when the only available
     * source is an incompatible format (e.g. a raw `TFL3` LiteRT flatbuffer),
     * which `tasks-genai`'s LlmInference rejects with "Unable to open zip
     * archive". Non-loadable models are NOT offered for download.
     */
    val androidTaskAvailable: Boolean = true,
) {
    /** Human size, e.g. "529 MB". */
    val approxDownloadLabel: String
        get() = "%,d MB".format(approxDownloadBytes / (1024 * 1024))
}

enum class LocalModelEngine {
    /** Google MediaPipe LLM Inference (`tasks-genai`) — loads zip `.task`. Maintenance-only. */
    MEDIAPIPE_LLM,

    /** Google LiteRT-LM (`litertlm-android`) — loads `.litertlm`. Strategic (Slice 3). */
    LITERT_LM,
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
        sourceUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task",
    )

    /**
     * Intelligence tier — Gemma 3 4B, INT4. ~2.6 GB, for offline deep Ask +
     * generative-UI on higher-memory devices.
     *
     * NOT currently offerable on Android: litert-community's 4B repo ships
     * only `-web` bundles, and that file is a raw `TFL3` LiteRT flatbuffer,
     * not a zip-based MediaPipe `.task` — `tasks-genai`'s LlmInference rejects
     * it ("Unable to open zip archive"; verified on device 2026-07-07). A
     * usable 4B needs a proper Android `.task` (Google's converter) or the
     * LiteRT-LM engine. Kept for the tier record; `androidTaskAvailable=false`
     * keeps it out of the download UI until a loadable source exists.
     */
    val GEMMA_3_4B_INT4 = DownloadableModel(
        id = "gemma-3-4b-it-int4",
        tier = LocalModelTier.INTELLIGENCE,
        displayName = "Gemma 3 4B (Intelligence)",
        blurb = "Offline deep Ask and generative UI. ~2.6 GB, needs 6GB+ RAM.",
        approxDownloadBytes = 2_560L * 1024 * 1024,
        minTotalRamMb = 6_144,
        capabilities = LocalModelCapabilities.INTELLIGENCE,
        engine = LocalModelEngine.MEDIAPIPE_LLM,
        assetFileName = "gemma-3-4b-it-int4.task",
        sourceUrl = "https://huggingface.co/litert-community/Gemma3-4B-IT/resolve/main/gemma3-4b-it-int4-web.task",
        androidTaskAvailable = false,
    )

    /**
     * Intelligence tier via **LiteRT-LM** (Slice 3) — Gemma 4 E2B, `.litertlm`.
     * ~2.6 GB, Apache-2.0 (ungated, no token). Loads on the LiteRT-LM engine
     * (the 4B `.task` path is a dead end — T022-026). On-demand deep Ask /
     * generative UI on higher-memory devices; loaded in `:ml` (BIND_IMPORTANT
     * + largeHeap keep the low-memory killer off it).
     */
    val GEMMA_4_E2B_LITERTLM = DownloadableModel(
        id = "gemma-4-e2b-it",
        tier = LocalModelTier.INTELLIGENCE,
        displayName = "Gemma 4 E2B (Intelligence)",
        blurb = "Offline deep Ask and generative UI. ~2.6 GB, needs 6GB+ RAM.",
        approxDownloadBytes = 2_590L * 1024 * 1024,
        minTotalRamMb = 6_144,
        capabilities = LocalModelCapabilities.INTELLIGENCE,
        engine = LocalModelEngine.LITERT_LM,
        assetFileName = "gemma-4-e2b-it.litertlm",
        sourceUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
        androidTaskAvailable = true,
    )

    val ALL: List<DownloadableModel> = listOf(
        GEMMA_3_1B_INT4,
        GEMMA_4_E2B_LITERTLM,
        GEMMA_3_4B_INT4,
    )

    /**
     * Models this device can actually download AND load: enough RAM for the
     * tier, and an Android-loadable `.task` source. Excludes models whose only
     * source is an incompatible format (see [DownloadableModel.androidTaskAvailable]).
     */
    fun offerableFor(hardware: DeviceAiHardwareProfile): List<DownloadableModel> =
        ALL.filter { it.androidTaskAvailable && hardware.totalRamMb >= it.minTotalRamMb }

    fun byId(id: String): DownloadableModel? = ALL.firstOrNull { it.id == id }
}
