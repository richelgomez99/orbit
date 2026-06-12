package com.orbit.app.memory

import android.os.IBinder
import com.orbit.app.data.entity.AuditLogEntryEntity
import com.orbit.app.data.entity.IntentEnvelopeEntity
import com.orbit.app.data.entity.StateSnapshot
import com.orbit.app.data.model.ActivityState
import com.orbit.app.data.model.AppCategory
import com.orbit.app.data.model.AuditAction
import com.orbit.app.data.model.ContentType
import com.orbit.app.data.model.EnvelopeKind
import com.orbit.app.data.model.Intent
import com.orbit.app.data.model.IntentSource
import com.orbit.app.net.ipc.INetworkGateway
import com.orbit.app.net.ipc.LlmGatewayRequestParcel
import com.orbit.app.net.ipc.LlmGatewayResponseParcel
import com.orbit.app.net.ipc.MemoryGatewayRequestParcel
import com.orbit.app.net.ipc.MemoryGatewayResponseParcel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryIndexSyncCoordinatorTest {
    private val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun disabledIndexingSkipsBeforeGatewayCallAndAudits() = runTest {
        val source = FakeSource(snapshot = snapshot(envelope()))
        val gateway = FakeGateway(json)
        val coordinator = coordinator(source, gateway, enabled = false)

        val outcome = coordinator.syncEnvelope("env-1")

        assertTrue(outcome is MemoryIndexSyncCoordinator.Outcome.Skipped)
        assertEquals(0, gateway.requests.size)
        assertEquals(AuditAction.MEMORY_SYNC_SKIPPED, source.audit.single().action)
        val extras = JSONObject(source.audit.single().extraJson!!)
        assertEquals("COMPACT_INDEX_UPSERT", extras.getString("capability"))
        assertEquals("SKIPPED", extras.getString("cloudOutcome"))
        assertEquals("DISABLED_BY_USER", extras.getString("cloudReason"))
    }

    @Test
    fun disabledIndexingSkipsTombstoneBeforeGatewayCallAndAudits() = runTest {
        val source = FakeSource(snapshot = null)
        val gateway = FakeGateway(json)
        val coordinator = coordinator(source, gateway, enabled = false)

        val outcome = coordinator.tombstoneEnvelope("env-1", reason = "user_deleted")

        assertTrue(outcome is MemoryIndexSyncCoordinator.Outcome.Skipped)
        assertEquals(0, gateway.requests.size)
        assertEquals(AuditAction.MEMORY_SYNC_SKIPPED, source.audit.single().action)
        val extras = JSONObject(source.audit.single().extraJson!!)
        assertEquals("COMPACT_INDEX_TOMBSTONE", extras.getString("capability"))
        assertEquals("SKIPPED", extras.getString("cloudOutcome"))
    }

    @Test
    fun syncEnvelopeUpsertsCompactPayloadAndAuditsSuccess() = runTest {
        val source = FakeSource(snapshot = snapshot(envelope()))
        val gateway = FakeGateway(json)
        val coordinator = coordinator(source, gateway, enabled = true)

        val outcome = coordinator.syncEnvelope("env-1")

        assertTrue(outcome is MemoryIndexSyncCoordinator.Outcome.Upserted)
        val request = json.decodeFromString(MemoryGatewayRequest.serializer(), gateway.requests.single().payloadJson)
        assertTrue(request is MemoryGatewayRequest.Upsert)
        request as MemoryGatewayRequest.Upsert
        assertEquals("env-1", request.item.envelopeId)
        assertEquals("Saved article about Atlas search", request.item.title)
        assertEquals(AuditAction.MEMORY_INDEX_UPSERTED, source.audit.single().action)
        val extras = JSONObject(source.audit.single().extraJson!!)
        assertEquals("COMPACT_INDEX_UPSERT", extras.getString("capability"))
        assertEquals("SUCCESS", extras.getString("cloudOutcome"))
    }

    @Test
    fun deletedEnvelopeTombstonesRemoteIndex() = runTest {
        val source = FakeSource(snapshot = snapshot(envelope(isDeleted = true)))
        val gateway = FakeGateway(json)
        val coordinator = coordinator(source, gateway, enabled = true)

        val outcome = coordinator.syncEnvelope("env-1")

        assertTrue(outcome is MemoryIndexSyncCoordinator.Outcome.Tombstoned)
        val request = json.decodeFromString(MemoryGatewayRequest.serializer(), gateway.requests.single().payloadJson)
        assertTrue(request is MemoryGatewayRequest.Tombstone)
        request as MemoryGatewayRequest.Tombstone
        assertEquals("local_envelope_deleted", request.reason)
        assertEquals(AuditAction.MEMORY_INDEX_TOMBSTONED, source.audit.single().action)
        val extras = JSONObject(source.audit.single().extraJson!!)
        assertEquals("COMPACT_INDEX_TOMBSTONE", extras.getString("capability"))
        assertEquals("SUCCESS", extras.getString("cloudOutcome"))
    }

    @Test
    fun gatewayErrorAuditsFailure() = runTest {
        val source = FakeSource(snapshot = snapshot(envelope()))
        val gateway = FakeGateway(
            json = json,
            response = MemoryGatewayResponse.Error("req-1", "NETWORK_UNAVAILABLE", "offline"),
        )
        val coordinator = coordinator(source, gateway, enabled = true)

        val outcome = coordinator.syncEnvelope("env-1")

        assertTrue(outcome is MemoryIndexSyncCoordinator.Outcome.Failed)
        assertEquals(AuditAction.MEMORY_GATEWAY_FAILED, source.audit.single().action)
        assertTrue(source.audit.single().extraJson!!.contains("NETWORK_UNAVAILABLE"))
        val extras = JSONObject(source.audit.single().extraJson!!)
        assertEquals("COMPACT_INDEX_UPSERT", extras.getString("capability"))
        assertEquals("FAILED", extras.getString("cloudOutcome"))
    }

    private fun coordinator(
        source: FakeSource,
        gateway: FakeGateway,
        enabled: Boolean,
    ) = MemoryIndexSyncCoordinator(
        source = source,
        gateway = gateway,
        indexingEnabled = { enabled },
        clock = { NOW },
        requestIdFactory = { "req-1" },
        jsonCodec = json,
    )

    private class FakeSource(
        private val snapshot: MemoryIndexSnapshot?,
    ) : MemoryIndexSource {
        val audit = mutableListOf<AuditLogEntryEntity>()

        override suspend fun load(envelopeId: String): MemoryIndexSnapshot? = snapshot

        override suspend fun insertAudit(entry: AuditLogEntryEntity) {
            audit += entry
        }
    }

    private class FakeGateway(
        private val json: Json,
        private val response: MemoryGatewayResponse? = null,
    ) : INetworkGateway {
        val requests = mutableListOf<MemoryGatewayRequestParcel>()

        override fun fetchPublicUrl(url: String?, timeoutMs: Long) =
            error("not used")

        override fun callLlmGateway(request: LlmGatewayRequestParcel): LlmGatewayResponseParcel =
            error("not used")

        override fun callMemoryGateway(request: MemoryGatewayRequestParcel): MemoryGatewayResponseParcel {
            requests += request
            val decoded = json.decodeFromString(MemoryGatewayRequest.serializer(), request.payloadJson)
            val typed = response ?: when (decoded) {
                is MemoryGatewayRequest.Upsert -> MemoryGatewayResponse.UpsertResponse(
                    requestId = decoded.requestId,
                    envelopeId = decoded.item.envelopeId,
                    upserted = true,
                    indexedAtMillis = NOW,
                )
                is MemoryGatewayRequest.Tombstone -> MemoryGatewayResponse.TombstoneResponse(
                    requestId = decoded.requestId,
                    envelopeId = decoded.envelopeId,
                    matched = true,
                    modified = true,
                )
                else -> MemoryGatewayResponse.Error(decoded.requestId, "UNEXPECTED", "not tested")
            }
            return MemoryGatewayResponseParcel(
                json.encodeToString(MemoryGatewayResponse.serializer(), typed),
            )
        }

        override fun asBinder(): IBinder =
            throw UnsupportedOperationException("test fake")
    }

    private fun snapshot(envelope: IntentEnvelopeEntity) = MemoryIndexSnapshot(
        envelope = envelope,
        latestResult = null,
        note = null,
        understanding = null,
        evidenceBundles = emptyList(),
    )

    private fun envelope(isDeleted: Boolean = false) = IntentEnvelopeEntity(
        id = "env-1",
        contentType = ContentType.TEXT,
        textContent = "Saved article about Atlas search",
        imageUri = null,
        textContentSha256 = "sha",
        intent = Intent.READ_LATER,
        intentConfidence = 0.9f,
        intentSource = IntentSource.USER_CHIP,
        intentHistoryJson = "[]",
        state = StateSnapshot(
            appCategory = AppCategory.BROWSER,
            activityState = ActivityState.UNKNOWN,
            tzId = "America/New_York",
            hourLocal = 10,
            dayOfWeekLocal = 6,
            sourceAppLabel = "Chrome",
        ),
        createdAt = NOW,
        dayLocal = "2026-05-30",
        isDeleted = isDeleted,
        kind = EnvelopeKind.REGULAR,
    )

    private companion object {
        const val NOW = 1_780_000_000_000L
    }
}
