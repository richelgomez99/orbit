package com.orbit.app.memory

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.orbit.app.data.ipc.EnvelopeRepositoryService
import com.orbit.app.data.ipc.IEnvelopeRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class MemoryIndexSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val envelopeId = inputData.getString(KEY_ENVELOPE_ID) ?: return Result.failure()
        val mode = inputData.getString(KEY_MODE) ?: MODE_UPSERT
        val encodedOutcome = runCatching {
            withRepository(applicationContext) { repository ->
                repository.syncMemoryIndex(
                    envelopeId,
                    mode,
                    inputData.getString(KEY_REASON),
                )
            }
        }.getOrElse { t ->
            Log.w(TAG, "memory index sync binder call failed", t)
            return Result.retry()
        }

        return when {
            encodedOutcome == MemoryIndexSyncDelegate.RESULT_UPSERTED ||
                encodedOutcome == MemoryIndexSyncDelegate.RESULT_TOMBSTONED ||
                encodedOutcome == MemoryIndexSyncDelegate.RESULT_SKIPPED -> Result.success()
            encodedOutcome.startsWith(MemoryIndexSyncDelegate.RESULT_FAILED_PREFIX) -> {
                val errorKind = encodedOutcome.removePrefix(MemoryIndexSyncDelegate.RESULT_FAILED_PREFIX)
                Log.w(TAG, "memory index sync failed kind=$errorKind envelopeId=$envelopeId")
                if (errorKind in PERMANENT_ERRORS) Result.failure() else Result.retry()
            }
            else -> {
                Log.w(TAG, "memory index sync unexpected outcome=$encodedOutcome envelopeId=$envelopeId")
                Result.retry()
            }
        }
    }

    companion object {
        const val KEY_ENVELOPE_ID: String = "envelopeId"
        const val KEY_MODE: String = "mode"
        const val KEY_REASON: String = "reason"
        const val MODE_UPSERT: String = "upsert"
        const val MODE_TOMBSTONE: String = "tombstone"

        private const val TAG: String = "MemoryIndexSyncWorker"
        private const val BIND_TIMEOUT_MS: Long = 5_000L
        private val PERMANENT_ERRORS = setOf("UNAUTHORIZED", "MALFORMED_RESPONSE", "INVALID_REQUEST")

        internal var repositoryBinder: suspend (Context) -> BoundRepository? = ::bindRepositoryDefault

        private suspend fun <T> withRepository(
            context: Context,
            block: (IEnvelopeRepository) -> T,
        ): T {
            val bound = repositoryBinder(context) ?: error("bindService(EnvelopeRepositoryService) failed")
            return try {
                block(bound.repository)
            } finally {
                withContext(Dispatchers.Main) {
                    runCatching { context.unbindService(bound.connection) }
                }
            }
        }

        private suspend fun bindRepositoryDefault(context: Context): BoundRepository? =
            withTimeoutOrNull(BIND_TIMEOUT_MS) {
                val deferred = CompletableDeferred<IEnvelopeRepository?>()
                val connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        deferred.complete(IEnvelopeRepository.Stub.asInterface(service))
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        if (!deferred.isCompleted) deferred.complete(null)
                    }
                }
                val bound = withContext(Dispatchers.Main) {
                    context.bindService(
                        Intent(context, EnvelopeRepositoryService::class.java),
                        connection,
                        Context.BIND_AUTO_CREATE,
                    )
                }
                if (!bound) return@withTimeoutOrNull null
                val repository = deferred.await() ?: return@withTimeoutOrNull null
                BoundRepository(repository = repository, connection = connection)
            }

        internal data class BoundRepository(
            val repository: IEnvelopeRepository,
            val connection: ServiceConnection,
        )
    }
}
