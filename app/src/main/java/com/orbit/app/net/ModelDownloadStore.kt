package com.orbit.app.net

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Spec 022 — shared on-disk state for BYOM model downloads.
 *
 * The download runs in :net (sole network egress) and writes the model
 * file plus a small progress JSON into the app's private files dir,
 * which every Orbit process shares by path. :ml later mmaps the model
 * file; the UI polls the progress JSON. No large bytes cross Binder.
 *
 * Layout under `filesDir/models/`:
 *   <id>.task          — the finished model bundle (present iff COMPLETE)
 *   <id>.task.part     — in-flight partial (renamed to .task on success)
 *   <id>.download.json — progress: { state, bytesWritten, totalBytes, error }
 */
object ModelDownloadStore {

    enum class State { IDLE, DOWNLOADING, COMPLETE, FAILED }

    data class Progress(
        val modelId: String,
        val state: State,
        val bytesWritten: Long,
        val totalBytes: Long,
        val error: String? = null,
    ) {
        val fraction: Float
            get() = if (totalBytes > 0) (bytesWritten.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    }

    fun modelsDir(context: Context): File =
        File(context.filesDir, "models").apply { mkdirs() }

    fun modelFile(context: Context, modelId: String): File =
        File(modelsDir(context), "$modelId.task")

    fun partFile(context: Context, modelId: String): File =
        File(modelsDir(context), "$modelId.task.part")

    private fun progressFile(context: Context, modelId: String): File =
        File(modelsDir(context), "$modelId.download.json")

    /** True once the finished bundle exists on disk. */
    fun isInstalled(context: Context, modelId: String): Boolean =
        modelFile(context, modelId).exists()

    fun writeProgress(context: Context, progress: Progress) {
        val json = JSONObject().apply {
            put("modelId", progress.modelId)
            put("state", progress.state.name)
            put("bytesWritten", progress.bytesWritten)
            put("totalBytes", progress.totalBytes)
            put("error", progress.error ?: JSONObject.NULL)
        }
        progressFile(context, progress.modelId).writeText(json.toString())
    }

    fun readProgress(context: Context, modelId: String): Progress {
        // A present bundle wins even if the progress file is missing/stale.
        if (isInstalled(context, modelId)) {
            val f = modelFile(context, modelId)
            return Progress(modelId, State.COMPLETE, f.length(), f.length())
        }
        val file = progressFile(context, modelId)
        if (!file.exists()) return Progress(modelId, State.IDLE, 0, 0)
        return runCatching {
            val o = JSONObject(file.readText())
            Progress(
                modelId = o.optString("modelId", modelId),
                state = runCatching { State.valueOf(o.optString("state")) }.getOrDefault(State.IDLE),
                bytesWritten = o.optLong("bytesWritten"),
                totalBytes = o.optLong("totalBytes"),
                error = o.optString("error").takeIf { it.isNotBlank() && it != "null" },
            )
        }.getOrDefault(Progress(modelId, State.IDLE, 0, 0))
    }

    /** Remove the bundle + partial + progress for a model (user-initiated delete). */
    fun delete(context: Context, modelId: String) {
        modelFile(context, modelId).delete()
        partFile(context, modelId).delete()
        progressFile(context, modelId).delete()
    }
}
