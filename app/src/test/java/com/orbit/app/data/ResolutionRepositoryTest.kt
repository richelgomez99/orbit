package com.orbit.app.data

import com.orbit.app.data.dao.ResolutionReceiptDao
import com.orbit.app.data.entity.ResolutionReceiptEntity
import com.orbit.app.resolution.ResolutionActor
import com.orbit.app.resolution.ResolutionKind
import com.orbit.app.resolution.ResolutionReceipt
import com.orbit.app.resolution.ResolutionSurfacingVerdict
import com.orbit.app.resolution.ResolutionTargetType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolutionRepositoryTest {

    @Test
    fun recordPersistsValidReceipt() = runTest {
        val dao = FakeResolutionReceiptDao()
        val repository = ResolutionRepository(dao)

        val ok = repository.record(receipt(kind = ResolutionKind.DUPLICATE_RECAPTURE))

        assertTrue(ok)
        assertEquals(1, dao.receipts.size)
        assertEquals(ResolutionKind.DUPLICATE_RECAPTURE, dao.receipts.single().kind)
    }

    @Test
    fun rejectsInvalidSnoozeWindow() = runTest {
        val dao = FakeResolutionReceiptDao()
        val repository = ResolutionRepository(dao)

        val ok = repository.record(
            receipt(
                kind = ResolutionKind.SNOOZED,
                effectiveUntilMillis = NOW,
            )
        )

        assertFalse(ok)
        assertEquals(0, dao.receipts.size)
    }

    @Test
    fun rejectsBannedMetadataKeys() = runTest {
        val dao = FakeResolutionReceiptDao()
        val repository = ResolutionRepository(dao)

        val ok = repository.record(
            receipt(
                kind = ResolutionKind.DUPLICATE_RECAPTURE,
                metadataJson = """{"safe":"id-only","modelResponse":"raw answer"}""",
            )
        )

        assertFalse(ok)
        assertEquals(0, dao.receipts.size)
    }

    @Test
    fun rejectsModelOnlyDone() = runTest {
        val dao = FakeResolutionReceiptDao()
        val repository = ResolutionRepository(dao)

        val ok = repository.record(
            receipt(
                kind = ResolutionKind.DONE,
                actor = ResolutionActor.AGENT,
            )
        )

        assertFalse(ok)
        assertEquals(0, dao.receipts.size)
    }

    @Test
    fun verdictUsesPersistedReceipts() = runTest {
        val dao = FakeResolutionReceiptDao()
        val repository = ResolutionRepository(dao)
        repository.record(receipt(id = "dismissed", kind = ResolutionKind.DISMISSED))

        val verdict = repository.verdictFor(
            targetType = ResolutionTargetType.ACTIVE_INTENT,
            targetId = "target-1",
            nowMillis = NOW,
        )

        assertEquals(ResolutionSurfacingVerdict.DISMISSED, verdict.verdict)
        assertEquals("dismissed", verdict.receiptId)
    }

    private fun receipt(
        id: String = "receipt-1",
        kind: ResolutionKind,
        actor: ResolutionActor = if (kind == ResolutionKind.DUPLICATE_RECAPTURE) {
            ResolutionActor.DUPLICATE_DETECTOR
        } else {
            ResolutionActor.USER
        },
        effectiveUntilMillis: Long? = null,
        metadataJson: String? = """{"matchedBy":"EXACT_TEXT"}""",
    ) = ResolutionReceipt(
        id = id,
        targetType = ResolutionTargetType.ACTIVE_INTENT,
        targetId = "target-1",
        envelopeId = "env-1",
        relatedType = null,
        relatedId = null,
        kind = kind,
        actor = actor,
        reason = null,
        occurredAtMillis = NOW,
        effectiveUntilMillis = effectiveUntilMillis,
        invalidatesReceiptId = null,
        metadataJson = metadataJson,
    )

    private class FakeResolutionReceiptDao : ResolutionReceiptDao {
        val receipts = mutableListOf<ResolutionReceiptEntity>()

        override suspend fun insert(entity: ResolutionReceiptEntity): Long {
            if (receipts.any { it.id == entity.id }) return -1
            receipts += entity
            return receipts.size.toLong()
        }

        override suspend fun getForTarget(
            targetType: ResolutionTargetType,
            targetId: String,
        ): List<ResolutionReceiptEntity> =
            receipts.filter { it.targetType == targetType && it.targetId == targetId }

        override suspend fun getForEnvelope(
            envelopeId: String,
            limit: Int,
        ): List<ResolutionReceiptEntity> =
            receipts.filter { it.envelopeId == envelopeId }
                .sortedByDescending { it.occurredAtMillis }
                .take(limit)

        override suspend fun getRecentByKind(
            kind: ResolutionKind,
            limit: Int,
        ): List<ResolutionReceiptEntity> =
            receipts.filter { it.kind == kind }
                .sortedByDescending { it.occurredAtMillis }
                .take(limit)
    }

    private companion object {
        const val NOW = 1_781_000_000_000L
    }
}
