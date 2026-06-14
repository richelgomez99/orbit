package com.orbit.app.understanding

import com.orbit.app.data.dao.ActiveIntentDao
import com.orbit.app.data.dao.AuditLogDao
import com.orbit.app.data.dao.CaptureUnderstandingDao
import com.orbit.app.data.dao.EvidenceBundleDao
import com.orbit.app.data.entity.ActiveIntentEntity
import com.orbit.app.data.entity.AuditLogEntryEntity
import com.orbit.app.data.entity.CaptureUnderstandingEntity
import com.orbit.app.data.entity.EvidenceBundleEntity
import com.orbit.app.data.model.AuditAction
import com.orbit.app.data.ResolutionReceiptSink
import com.orbit.app.resolution.ResolutionKind
import com.orbit.app.resolution.ResolutionReceipt
import com.orbit.app.understanding.domain.ActiveIntentStatus
import com.orbit.app.understanding.domain.CompletionKeyStatus
import com.orbit.app.understanding.domain.IntentCategory
import com.orbit.app.understanding.domain.SourceIdentity
import com.orbit.app.understanding.domain.UnderstandingMode
import com.orbit.app.understanding.domain.UnderstandingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BasicUnderstandingWriterTest {

    @Test
    fun persistWritesUnderstandingEvidenceAndActiveIntent() = runTest {
        val captureDao = FakeCaptureUnderstandingDao()
        val evidenceDao = FakeEvidenceBundleDao()
        val activeDao = FakeActiveIntentDao()
        val writer = BasicUnderstandingWriter(captureDao, evidenceDao, activeDao)

        val result = writer.persist(input())

        assertEquals(IntentCategory.COUPON_OR_PROMO, result.category)
        assertEquals("capture-1", captureDao.upserted?.captureId)
        assertEquals(CompletionKeyStatus.FOUND, captureDao.upserted?.completionKeyStatus)
        assertNotNull(captureDao.upserted?.completionKeyJson)
        assertEquals("https://example.com/item", captureDao.upserted?.canonicalUrl)
        assertTrue(evidenceDao.deletedCaptureIds.contains("capture-1"))
        assertTrue(evidenceDao.inserted.isNotEmpty())
        assertEquals("basic:capture-1", activeDao.upserted?.intentId)
        assertEquals("capture-1", activeDao.upserted?.captureId)
        assertEquals(ActiveIntentStatus.ACTIVE, activeDao.upserted?.status)
    }

    @Test
    fun persistDoesNotResurrectResolvedActiveIntent() = runTest {
        val activeDao = FakeActiveIntentDao(
            existing = activeIntent(status = ActiveIntentStatus.RESOLVED)
        )
        val writer = BasicUnderstandingWriter(
            captureUnderstandingDao = FakeCaptureUnderstandingDao(),
            evidenceBundleDao = FakeEvidenceBundleDao(),
            activeIntentDao = activeDao
        )

        writer.persist(input())

        assertNull(activeDao.upserted)
    }

    @Test
    fun persistSkipsActiveIntentForDuplicateUnderstandingContent() = runTest {
        val captureDao = FakeCaptureUnderstandingDao(
            contentHashMatches = listOf(
                captureUnderstanding(captureId = "existing-capture")
            )
        )
        val activeDao = FakeActiveIntentDao()
        val auditDao = FakeAuditLogDao()
        val receiptSink = FakeResolutionReceiptSink()
        val writer = BasicUnderstandingWriter(
            captureUnderstandingDao = captureDao,
            evidenceBundleDao = FakeEvidenceBundleDao(),
            activeIntentDao = activeDao,
            auditLogDao = auditDao,
            resolutionReceiptSink = receiptSink
        )

        val result = writer.persist(input())

        assertNotNull(result.contentHashHex)
        assertEquals("capture-1", captureDao.upserted?.captureId)
        assertNull(activeDao.upserted)
        val audit = auditDao.inserted.single()
        assertEquals(AuditAction.ACTIVE_INTENT_DUPLICATE_SUPPRESSED, audit.action)
        assertEquals("capture-1", audit.envelopeId)
        assertTrue(audit.extraJson?.contains("existing-capture") == true)
        assertTrue(audit.extraJson?.contains(result.contentHashHex!!) == true)
        assertTrue(audit.extraJson?.contains("Running shoes") != true)
        assertTrue(audit.extraJson?.contains("$42.00") != true)
        val receipt = receiptSink.receipts.single()
        assertEquals(ResolutionKind.DUPLICATE_RECAPTURE, receipt.kind)
        assertEquals("existing-capture", receipt.targetId)
        assertEquals("capture-1", receipt.relatedId)
        assertEquals("CONTENT_HASH", receipt.reason)
        assertTrue(receipt.metadataJson?.contains(result.contentHashHex!!) == true)
        assertTrue(receipt.metadataJson?.contains("Running shoes") != true)
        assertTrue(receipt.metadataJson?.contains("$42.00") != true)
    }

    @Test
    fun refreshActiveFromSidecarsRelabelsActiveBasicRows() = runTest {
        val captureDao = FakeCaptureUnderstandingDao(
            existing = captureUnderstanding(
                category = IntentCategory.EVENT_TICKET_RESERVATION,
                summaryText = "Okay. Chelsea has a scheduled appointment at 2pm",
                sourceIdentityJson = SourceIdentity.fromLocalSignals(
                    sourceAppLabel = "Messages",
                    appCategory = "MESSAGING",
                    canonicalUrl = null
                ).toCompactJson()
            )
        )
        val evidenceDao = FakeEvidenceBundleDao()
        val activeDao = FakeActiveIntentDao(
            existing = activeIntent(
                status = ActiveIntentStatus.ACTIVE,
                intentType = IntentCategory.EVENT_TICKET_RESERVATION,
                completionKeyStatus = CompletionKeyStatus.MISSING,
                updatedAt = NOW - 1_000
            )
        )
        val writer = BasicUnderstandingWriter(captureDao, evidenceDao, activeDao)

        val refreshed = writer.refreshActiveFromSidecars(nowMillis = NOW)

        assertEquals(1, refreshed)
        assertEquals(IntentCategory.CHAT_ACTION, activeDao.upserted?.intentType)
        assertEquals(CompletionKeyStatus.FOUND, activeDao.upserted?.completionKeyStatus)
        assertEquals(NOW - 1_000, activeDao.upserted?.createdAt)
        assertEquals(NOW, activeDao.upserted?.updatedAt)
    }

    @Test
    fun refreshActiveFromSidecarsPreservesOrbitDecisionBrief() = runTest {
        val activeDao = FakeActiveIntentDao(
            existing = activeIntent(
                status = ActiveIntentStatus.ACTIVE,
                primaryEvidenceJson = """{"kind":"ORBIT_REVIEW","label":"CHAT_ACTION","reason":"Orbit reviewed this."}""",
                primaryAction = "ask_orbit_review"
            )
        )
        val writer = BasicUnderstandingWriter(
            captureUnderstandingDao = FakeCaptureUnderstandingDao(existing = captureUnderstanding()),
            evidenceBundleDao = FakeEvidenceBundleDao(),
            activeIntentDao = activeDao
        )

        val refreshed = writer.refreshActiveFromSidecars(nowMillis = NOW)

        assertEquals(1, refreshed)
        assertEquals("ask_orbit_review", activeDao.upserted?.primaryAction)
        assertTrue(activeDao.upserted?.primaryEvidenceJson?.contains("ORBIT_REVIEW") == true)
    }

    @Test
    fun refreshActiveFromSidecarsSkipsResolvedRows() = runTest {
        val activeDao = FakeActiveIntentDao(
            existing = activeIntent(status = ActiveIntentStatus.RESOLVED)
        )
        val writer = BasicUnderstandingWriter(
            captureUnderstandingDao = FakeCaptureUnderstandingDao(existing = captureUnderstanding()),
            evidenceBundleDao = FakeEvidenceBundleDao(),
            activeIntentDao = activeDao
        )

        val refreshed = writer.refreshActiveFromSidecars(nowMillis = NOW)

        assertEquals(0, refreshed)
        assertNull(activeDao.upserted)
    }

    private fun input() = BasicUnderstandingInput(
        captureId = "capture-1",
        textContent = "Running shoes price $42.00 with promo code SAVE20",
        sourceAppLabel = "Chrome",
        appCategory = "BROWSER",
        canonicalUrl = "https://example.com/item",
        capturedAtMillis = NOW,
        nowMillis = NOW
    )

    private fun activeIntent(
        status: ActiveIntentStatus,
        intentType: IntentCategory = IntentCategory.BUY_LATER_PRODUCT,
        completionKeyStatus: CompletionKeyStatus = CompletionKeyStatus.FOUND,
        updatedAt: Long = NOW - 1_000,
        primaryEvidenceJson: String = "{}",
        primaryAction: String? = "buy_or_skip"
    ) = ActiveIntentEntity(
        intentId = "basic:capture-1",
        captureId = "capture-1",
        intentType = intentType,
        status = status,
        completionKeyJson = "{}",
        completionKeyStatus = completionKeyStatus,
        primaryEvidenceJson = primaryEvidenceJson,
        primaryAction = primaryAction,
        dueAt = null,
        expiresAt = null,
        resolutionReason = null,
        resolvedAt = if (status == ActiveIntentStatus.RESOLVED) NOW else null,
        userConfirmed = status == ActiveIntentStatus.RESOLVED,
        createdAt = NOW - 1_000,
        updatedAt = updatedAt
    )

    private fun captureUnderstanding(
        captureId: String = "capture-1",
        category: IntentCategory = IntentCategory.BUY_LATER_PRODUCT,
        summaryText: String? = "Running shoes price $42.00",
        sourceIdentityJson: String? = SourceIdentity.fromLocalSignals(
            sourceAppLabel = "Chrome",
            appCategory = "BROWSER",
            canonicalUrl = "https://example.com/item"
        ).toCompactJson()
    ) = CaptureUnderstandingEntity(
        captureId = captureId,
        mode = UnderstandingMode.BASIC,
        status = UnderstandingStatus.READY,
        category = category,
        categoryConfidence = 0.8f,
        title = "Stored title",
        summaryText = summaryText,
        completionKeyJson = null,
        completionKeyStatus = CompletionKeyStatus.MISSING,
        sourceIdentityJson = sourceIdentityJson,
        contentHashHex = null,
        canonicalUrl = null,
        groundingConstraintsJson = "{}",
        createdAt = NOW - 2_000,
        updatedAt = NOW - 2_000,
        invalidatedAt = null
    )

    private class FakeCaptureUnderstandingDao(
        private var existing: CaptureUnderstandingEntity? = null,
        private val contentHashMatches: List<CaptureUnderstandingEntity> = emptyList()
    ) : CaptureUnderstandingDao {
        var upserted: CaptureUnderstandingEntity? = null

        override suspend fun upsert(entity: CaptureUnderstandingEntity) {
            upserted = entity
            existing = entity
        }

        override fun observeByCaptureId(captureId: String): Flow<CaptureUnderstandingEntity?> = flowOf(existing)
        override suspend fun getByCaptureId(captureId: String): CaptureUnderstandingEntity? = existing
        override suspend fun getByContentHash(hex: String): List<CaptureUnderstandingEntity> = contentHashMatches
        override suspend fun getByCanonicalUrl(canonicalUrl: String): List<CaptureUnderstandingEntity> = emptyList()
        override suspend fun markInvalidated(captureId: String, invalidatedAt: Long): Int = 0
        override suspend fun deleteByCaptureId(captureId: String): Int = 0
    }

    private class FakeEvidenceBundleDao : EvidenceBundleDao {
        val deletedCaptureIds = mutableListOf<String>()
        val inserted = mutableListOf<EvidenceBundleEntity>()

        override suspend fun insert(entity: EvidenceBundleEntity) {
            inserted.add(entity)
        }

        override suspend fun insertAll(entities: List<EvidenceBundleEntity>) {
            inserted.addAll(entities)
        }

        override suspend fun getByCaptureId(captureId: String): List<EvidenceBundleEntity> =
            inserted.filter { it.captureId == captureId }

        override suspend fun deleteByCaptureId(captureId: String): Int {
            deletedCaptureIds.add(captureId)
            val before = inserted.size
            inserted.removeAll { it.captureId == captureId }
            return before - inserted.size
        }
    }

    private class FakeActiveIntentDao(
        private val existing: ActiveIntentEntity? = null
    ) : ActiveIntentDao {
        var upserted: ActiveIntentEntity? = null

        override suspend fun upsert(entity: ActiveIntentEntity) {
            upserted = entity
        }

        override suspend fun getById(intentId: String): ActiveIntentEntity? =
            existing?.takeIf { it.intentId == intentId } ?: upserted?.takeIf { it.intentId == intentId }

        override suspend fun getByCaptureId(captureId: String): List<ActiveIntentEntity> =
            listOfNotNull(existing, upserted).filter { it.captureId == captureId }

        override fun observeActive(): Flow<List<ActiveIntentEntity>> = flowOf(emptyList())
        override fun observeCleanupQueue(): Flow<List<ActiveIntentEntity>> = flowOf(emptyList())
        override suspend fun getActiveBasic(limit: Int): List<ActiveIntentEntity> =
            listOfNotNull(existing, upserted)
                .filter { it.status == ActiveIntentStatus.ACTIVE && it.intentId.startsWith("basic:") }
                .take(limit)

        override suspend fun markResolved(
            intentId: String,
            status: String,
            resolutionReason: String?,
            resolvedAt: Long?,
            userConfirmed: Boolean,
            updatedAt: Long
        ): Int = 0

        override suspend fun markInvalidatedForCapture(
            captureId: String,
            invalidatedAt: Long,
            resolutionReason: String
        ): Int = 0

        override suspend fun deleteByCaptureId(captureId: String): Int = 0
    }

    private class FakeAuditLogDao : AuditLogDao {
        val inserted = mutableListOf<AuditLogEntryEntity>()

        override suspend fun insert(entry: AuditLogEntryEntity) {
            inserted += entry
        }

        override suspend fun entriesForDay(startMillis: Long, endMillis: Long): List<AuditLogEntryEntity> = emptyList()
        override suspend fun entriesForEnvelope(envelopeId: String): List<AuditLogEntryEntity> = emptyList()
        override suspend fun countForDay(startMillis: Long, endMillis: Long, action: String): Int = 0
        override suspend fun deleteOlderThan(cutoffMillis: Long): Int = 0
        override suspend fun deleteByEnvelopeId(envelopeId: String) = Unit
        override suspend fun listAll(): List<AuditLogEntryEntity> = inserted
    }

    private class FakeResolutionReceiptSink : ResolutionReceiptSink {
        val receipts = mutableListOf<ResolutionReceipt>()

        override suspend fun record(receipt: ResolutionReceipt): Boolean {
            receipts += receipt
            return true
        }
    }

    private companion object {
        const val NOW = 1_779_216_200_000L
    }
}
