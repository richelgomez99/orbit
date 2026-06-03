package com.orbit.app.memory

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MemoryIndexSyncScheduler(
    private val workManager: WorkManager,
) {
    fun enqueueEnvelope(envelopeId: String) {
        enqueue(envelopeId = envelopeId, mode = MemoryIndexSyncWorker.MODE_UPSERT, reason = null)
    }

    fun enqueueTombstone(envelopeId: String, reason: String) {
        enqueue(envelopeId = envelopeId, mode = MemoryIndexSyncWorker.MODE_TOMBSTONE, reason = reason)
    }

    private fun enqueue(envelopeId: String, mode: String, reason: String?) {
        if (envelopeId.isBlank()) return
        val data = Data.Builder()
            .putString(MemoryIndexSyncWorker.KEY_ENVELOPE_ID, envelopeId)
            .putString(MemoryIndexSyncWorker.KEY_MODE, mode)
            .apply { if (!reason.isNullOrBlank()) putString(MemoryIndexSyncWorker.KEY_REASON, reason) }
            .build()
        val request = OneTimeWorkRequestBuilder<MemoryIndexSyncWorker>()
            .setConstraints(CONSTRAINTS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_BASE_SECONDS, TimeUnit.SECONDS)
            .addTag(TAG_MEMORY_INDEX)
            .addTag(tagForEnvelope(envelopeId))
            .setInputData(data)
            .build()
        workManager.enqueueUniqueWork(
            uniqueNameForEnvelope(envelopeId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    companion object {
        const val TAG_MEMORY_INDEX: String = "memory-index"
        const val BACKOFF_BASE_SECONDS: Long = 60L

        val CONSTRAINTS: Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun create(context: Context): MemoryIndexSyncScheduler =
            MemoryIndexSyncScheduler(WorkManager.getInstance(context))

        fun uniqueNameForEnvelope(envelopeId: String): String = "memory-index:$envelopeId"
        fun tagForEnvelope(envelopeId: String): String = "memory-index-envelope:$envelopeId"
    }
}
