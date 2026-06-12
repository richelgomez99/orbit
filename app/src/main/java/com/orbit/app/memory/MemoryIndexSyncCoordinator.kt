package com.orbit.app.memory

import com.orbit.app.net.ipc.INetworkGateway
import com.orbit.app.net.ipc.MemoryGatewayRequestParcel
import com.orbit.app.cloud.CloudCapability
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class MemoryIndexSyncCoordinator(
    private val source: MemoryIndexSource,
    private val gateway: INetworkGateway,
    private val builder: CompactMemoryIndexBuilder = CompactMemoryIndexBuilder(),
    private val audit: MemoryAudit = MemoryAudit(),
    private val indexingEnabled: () -> Boolean,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val requestIdFactory: () -> String = { UUID.randomUUID().toString() },
    private val jsonCodec: Json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = true
    },
) {
    suspend fun syncEnvelope(envelopeId: String): Outcome {
        val requestId = requestIdFactory()
        if (!indexingEnabled()) {
            source.insertAudit(
                audit.syncSkipped(
                    requestId = requestId,
                    reason = "memory_indexing_disabled",
                    envelopeId = envelopeId,
                    capability = CloudCapability.COMPACT_INDEX_UPSERT,
                )
            )
            return Outcome.Skipped("memory_indexing_disabled")
        }

        val snapshot = source.load(envelopeId)
        if (snapshot == null) {
            return tombstoneEnvelope(envelopeId, "missing_local_envelope", requestId)
        }

        val item = builder.build(
            envelope = snapshot.envelope,
            latestResult = snapshot.latestResult,
            note = snapshot.note,
            understanding = snapshot.understanding,
            evidenceBundles = snapshot.evidenceBundles,
        )
        if (item == null) {
            return tombstoneEnvelope(envelopeId, "local_envelope_deleted", requestId)
        }

        val request = MemoryGatewayRequest.Upsert(requestId = requestId, item = item)
        val startedAt = clock()
        val response = callGateway(request)
        val latencyMs = (clock() - startedAt).coerceAtLeast(0L)
        return when (response) {
            is MemoryGatewayResponse.UpsertResponse -> {
                val payload = jsonCodec.encodeToString(request)
                source.insertAudit(
                    audit.upserted(
                        requestId = requestId,
                        envelopeId = item.envelopeId,
                        payloadForDigest = payload,
                        latencyMs = latencyMs,
                        capability = CloudCapability.COMPACT_INDEX_UPSERT,
                    )
                )
                Outcome.Upserted(item.envelopeId)
            }
            is MemoryGatewayResponse.Error -> {
                source.insertAudit(
                    audit.gatewayFailed(
                        requestId = requestId,
                        endpoint = "upsert",
                        errorKind = response.code,
                        latencyMs = latencyMs,
                        envelopeId = item.envelopeId,
                        capability = CloudCapability.COMPACT_INDEX_UPSERT,
                    )
                )
                Outcome.Failed(response.code)
            }
            else -> {
                source.insertAudit(
                    audit.gatewayFailed(
                        requestId = requestId,
                        endpoint = "upsert",
                        errorKind = "UNEXPECTED_RESPONSE",
                        latencyMs = latencyMs,
                        envelopeId = item.envelopeId,
                        capability = CloudCapability.COMPACT_INDEX_UPSERT,
                    )
                )
                Outcome.Failed("UNEXPECTED_RESPONSE")
            }
        }
    }

    suspend fun tombstoneEnvelope(
        envelopeId: String,
        reason: String,
        requestId: String = requestIdFactory(),
    ): Outcome {
        if (!indexingEnabled()) {
            source.insertAudit(
                audit.syncSkipped(
                    requestId = requestId,
                    reason = "memory_indexing_disabled",
                    envelopeId = envelopeId,
                    capability = CloudCapability.COMPACT_INDEX_TOMBSTONE,
                )
            )
            return Outcome.Skipped("memory_indexing_disabled")
        }

        val request = MemoryGatewayRequest.Tombstone(
            requestId = requestId,
            envelopeId = envelopeId,
            reason = reason,
            tombstonedAtMillis = clock(),
        )
        val startedAt = clock()
        val response = callGateway(request)
        val latencyMs = (clock() - startedAt).coerceAtLeast(0L)
        return when (response) {
            is MemoryGatewayResponse.TombstoneResponse -> {
                source.insertAudit(
                    audit.tombstoned(
                        requestId = requestId,
                        envelopeId = envelopeId,
                        reason = reason,
                        latencyMs = latencyMs,
                        capability = CloudCapability.COMPACT_INDEX_TOMBSTONE,
                    )
                )
                Outcome.Tombstoned(envelopeId)
            }
            is MemoryGatewayResponse.Error -> {
                source.insertAudit(
                    audit.gatewayFailed(
                        requestId = requestId,
                        endpoint = "tombstone",
                        errorKind = response.code,
                        latencyMs = latencyMs,
                        envelopeId = envelopeId,
                        capability = CloudCapability.COMPACT_INDEX_TOMBSTONE,
                    )
                )
                Outcome.Failed(response.code)
            }
            else -> {
                source.insertAudit(
                    audit.gatewayFailed(
                        requestId = requestId,
                        endpoint = "tombstone",
                        errorKind = "UNEXPECTED_RESPONSE",
                        latencyMs = latencyMs,
                        envelopeId = envelopeId,
                        capability = CloudCapability.COMPACT_INDEX_TOMBSTONE,
                    )
                )
                Outcome.Failed("UNEXPECTED_RESPONSE")
            }
        }
    }

    private fun callGateway(request: MemoryGatewayRequest): MemoryGatewayResponse {
        val payload = jsonCodec.encodeToString(MemoryGatewayRequest.serializer(), request)
        val parcel = gateway.callMemoryGateway(MemoryGatewayRequestParcel(payload))
        return jsonCodec.decodeFromString(MemoryGatewayResponse.serializer(), parcel.payloadJson)
    }

    sealed class Outcome {
        data class Upserted(val envelopeId: String) : Outcome()
        data class Tombstoned(val envelopeId: String) : Outcome()
        data class Skipped(val reason: String) : Outcome()
        data class Failed(val errorKind: String) : Outcome()
    }
}
