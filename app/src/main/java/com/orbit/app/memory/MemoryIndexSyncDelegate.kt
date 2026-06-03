package com.orbit.app.memory

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.orbit.app.data.OrbitDatabase
import com.orbit.app.net.NetworkGatewayService
import com.orbit.app.net.ipc.INetworkGateway
import com.orbit.app.settings.PrivacyPreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class MemoryIndexSyncDelegate(
    private val context: Context,
    private val database: OrbitDatabase,
    private val preferences: PrivacyPreferences = PrivacyPreferences(context),
) {
    suspend fun sync(envelopeId: String, mode: String, reason: String?): MemoryIndexSyncCoordinator.Outcome {
        return withGateway(context) { gateway ->
            val coordinator = MemoryIndexSyncCoordinator(
                source = RoomMemoryIndexSource(database),
                gateway = gateway,
                indexingEnabled = { preferences.memoryIndexingEnabled },
            )
            when (mode) {
                MemoryIndexSyncWorker.MODE_TOMBSTONE -> coordinator.tombstoneEnvelope(
                    envelopeId = envelopeId,
                    reason = reason?.takeIf { it.isNotBlank() } ?: "local_removed",
                )
                else -> coordinator.syncEnvelope(envelopeId)
            }
        }
    }

    private suspend fun withGateway(
        context: Context,
        block: suspend (INetworkGateway) -> MemoryIndexSyncCoordinator.Outcome,
    ): MemoryIndexSyncCoordinator.Outcome {
        val bound = bindGateway(context) ?: return MemoryIndexSyncCoordinator.Outcome.Failed("GATEWAY_BIND_FAILED")
        return try {
            block(bound.gateway)
        } finally {
            withContext(Dispatchers.Main) {
                runCatching { context.unbindService(bound.connection) }
            }
        }
    }

    private suspend fun bindGateway(context: Context): BoundGateway? =
        withTimeoutOrNull(BIND_TIMEOUT_MS) {
            val deferred = CompletableDeferred<INetworkGateway?>()
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    deferred.complete(INetworkGateway.Stub.asInterface(service))
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    if (!deferred.isCompleted) deferred.complete(null)
                }
            }
            val bound = withContext(Dispatchers.Main) {
                context.bindService(
                    Intent(context, NetworkGatewayService::class.java),
                    connection,
                    Context.BIND_AUTO_CREATE,
                )
            }
            if (!bound) return@withTimeoutOrNull null
            val gateway = deferred.await() ?: return@withTimeoutOrNull null
            BoundGateway(gateway = gateway, connection = connection)
        }

    private data class BoundGateway(
        val gateway: INetworkGateway,
        val connection: ServiceConnection,
    )

    companion object {
        const val RESULT_UPSERTED: String = "UPSERTED"
        const val RESULT_TOMBSTONED: String = "TOMBSTONED"
        const val RESULT_SKIPPED: String = "SKIPPED"
        const val RESULT_FAILED_PREFIX: String = "FAILED:"

        private const val BIND_TIMEOUT_MS: Long = 5_000L

        fun encodeOutcome(outcome: MemoryIndexSyncCoordinator.Outcome): String = when (outcome) {
            is MemoryIndexSyncCoordinator.Outcome.Upserted -> RESULT_UPSERTED
            is MemoryIndexSyncCoordinator.Outcome.Tombstoned -> RESULT_TOMBSTONED
            is MemoryIndexSyncCoordinator.Outcome.Skipped -> RESULT_SKIPPED
            is MemoryIndexSyncCoordinator.Outcome.Failed -> RESULT_FAILED_PREFIX + outcome.errorKind
        }
    }
}
