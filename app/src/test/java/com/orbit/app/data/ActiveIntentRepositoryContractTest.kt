package com.orbit.app.data

import com.orbit.app.data.dao.ActiveIntentDao
import com.orbit.app.data.dao.AuditLogDao
import com.orbit.app.data.entity.ActiveIntentEntity
import com.orbit.app.data.entity.AuditLogEntryEntity
import com.orbit.app.data.ipc.ActiveIntentParcel
import com.orbit.app.data.model.AuditAction
import com.orbit.app.understanding.domain.ActiveIntentStatus
import com.orbit.app.understanding.domain.CompletionKeyStatus
import com.orbit.app.understanding.domain.IntentCategory
import com.orbit.app.understanding.domain.ResolutionReason
import com.orbit.app.understanding.domain.UnderstandingMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveIntentRepositoryContractTest {

    @Test
    fun activeIntentParcelDropsForbiddenPayloadKeys() {
        val entity = activeIntent(
            completionKeyJson = """{"kind":"PRICE","label":"42 USD","modelResponse":"verbatim model output"}""",
            primaryEvidenceJson = """{"kind":"OCR","label":"bad","rawHtml":"<html>secret</html>"}"""
        )

        val parcel = ActiveIntentParcel.fromEntity(entity)
        val rendered = parcel.toString()

        assertNull(parcel.completionKeyJson)
        assertTrue(parcel.primaryEvidenceJson.contains("LIMITED"))
        assertFalse(rendered.contains("rawHtml"))
        assertFalse(rendered.contains("fullOcrText"))
        assertFalse(rendered.contains("embedding"))
        assertFalse(rendered.contains("prompt"))
        assertFalse(rendered.contains("modelResponse"))
        assertFalse(rendered.contains("<html>secret</html>"))
    }

    @Test
    fun activeIntentParcelPreservesCompactAllowedPayloads() {
        val entity = activeIntent(
            completionKeyJson = """{"kind":"PRICE","label":"42 USD","source":"local_regex","confidence":0.8}""",
            primaryEvidenceJson = """{"kind":"CATEGORY","label":"BUY_LATER_PRODUCT","source":"local_regex","excerpt":"short"}"""
        )

        val parcel = ActiveIntentParcel.fromEntity(entity)

        assertEquals(entity.intentId, parcel.intentId)
        assertEquals("BUY_LATER_PRODUCT", parcel.intentType)
        assertEquals("ACTIVE", parcel.status)
        assertEquals(entity.completionKeyJson, parcel.completionKeyJson)
        assertEquals(entity.primaryEvidenceJson, parcel.primaryEvidenceJson)
    }

    @Test
    fun reviewContextDropsForbiddenPayloadKeys() {
        val entity = activeIntent(
            completionKeyJson = """{"kind":"PRICE","label":"42 USD","modelResponse":"verbatim model output"}""",
            primaryEvidenceJson = """{"kind":"OCR","label":"bad","rawHtml":"<html>secret</html>"}"""
        )

        val rendered = ActiveIntentReviewContext.build(entity, UnderstandingMode.SMART).toString()

        assertTrue(rendered.contains("\"evidence\""))
        assertTrue(rendered.contains("LIMITED"))
        assertFalse(rendered.contains("rawHtml"))
        assertFalse(rendered.contains("fullOcrText"))
        assertFalse(rendered.contains("embedding"))
        assertFalse(rendered.contains("prompt"))
        assertFalse(rendered.contains("modelResponse"))
        assertFalse(rendered.contains("<html>secret</html>"))
    }

    @Test
    fun repositoryResolveUpdatesRowThroughDaoOnly() = runTest {
        val dao = FakeActiveIntentDao(activeIntent())
        val repo = ActiveIntentRepository(
            activeIntentDao = dao,
            scope = TestScope(testScheduler) as CoroutineScope,
            clock = { NOW }
        )

        val changed = repo.resolveActiveIntent(
            intentId = "intent-1",
            resolutionReason = ResolutionReason.BOUGHT.name,
            userConfirmed = true
        )

        assertTrue(changed)
        assertEquals("RESOLVED", dao.lastStatus)
        assertEquals("BOUGHT", dao.lastResolutionReason)
        assertEquals(true, dao.lastUserConfirmed)
        assertEquals(NOW, dao.lastResolvedAt)
    }

    @Test
    fun repositoryRejectsUnknownResolutionReason() = runTest {
        val dao = FakeActiveIntentDao(activeIntent())
        val repo = ActiveIntentRepository(
            activeIntentDao = dao,
            scope = TestScope(testScheduler) as CoroutineScope,
            clock = { NOW }
        )

        val changed = repo.resolveActiveIntent(
            intentId = "intent-1",
            resolutionReason = "DELETE_RAW_SCREENSHOT",
            userConfirmed = true
        )

        assertFalse(changed)
        assertNull(dao.lastStatus)
    }

    @Test
    fun repositoryEscalationWritesAuditBeforeAnyDispatch() = runTest {
        val dao = FakeActiveIntentDao(activeIntent(completionKeyStatus = CompletionKeyStatus.NEEDS_ESCALATION))
        val auditDao = FakeAuditLogDao()
        val repo = ActiveIntentRepository(
            activeIntentDao = dao,
            auditLogDao = auditDao,
            scope = TestScope(testScheduler) as CoroutineScope,
            clock = { NOW }
        )

        val recorded = repo.requestEscalation(intentId = "intent-1", mode = "SMART")

        assertTrue(recorded)
        val audit = auditDao.inserted.single()
        assertEquals(AuditAction.ACTIVE_INTENT_ESCALATION_REQUESTED, audit.action)
        assertEquals("capture-1", audit.envelopeId)
        assertTrue(audit.extraJson!!.contains("\"mode\":\"SMART\""))
        assertTrue(audit.extraJson.contains("\"intentId\":\"intent-1\""))
        assertTrue(audit.extraJson.contains("\"reviewContext\""))
        assertTrue(requireNotNull(dao.upserted).primaryEvidenceJson.contains("ORBIT_REVIEW"))
        assertTrue(requireNotNull(dao.upserted).primaryEvidenceJson.contains("Decide"))
        assertEquals("ask_orbit_review", dao.upserted?.primaryAction)
    }

    @Test
    fun repositoryEscalationRejectsBasicMode() = runTest {
        val dao = FakeActiveIntentDao(activeIntent())
        val auditDao = FakeAuditLogDao()
        val repo = ActiveIntentRepository(
            activeIntentDao = dao,
            auditLogDao = auditDao,
            scope = TestScope(testScheduler) as CoroutineScope,
            clock = { NOW }
        )

        val recorded = repo.requestEscalation(intentId = "intent-1", mode = "BASIC")

        assertFalse(recorded)
        assertEquals(0, auditDao.inserted.size)
    }

    private class FakeActiveIntentDao(
        private val row: ActiveIntentEntity?
    ) : ActiveIntentDao {
        private val flow = MutableStateFlow(row?.let { listOf(it) } ?: emptyList())
        var lastStatus: String? = null
        var lastResolutionReason: String? = null
        var lastResolvedAt: Long? = null
        var lastUserConfirmed: Boolean? = null
        var upserted: ActiveIntentEntity? = null

        override suspend fun upsert(entity: ActiveIntentEntity) {
            upserted = entity
        }
        override suspend fun getById(intentId: String): ActiveIntentEntity? = row?.takeIf { it.intentId == intentId }
        override suspend fun getByCaptureId(captureId: String): List<ActiveIntentEntity> = row?.let { listOf(it) } ?: emptyList()
        override fun observeActive(): Flow<List<ActiveIntentEntity>> = flow
        override fun observeCleanupQueue(): Flow<List<ActiveIntentEntity>> = flow
        override suspend fun getActiveBasic(limit: Int): List<ActiveIntentEntity> = emptyList()
        override suspend fun markResolved(
            intentId: String,
            status: String,
            resolutionReason: String?,
            resolvedAt: Long?,
            userConfirmed: Boolean,
            updatedAt: Long
        ): Int {
            lastStatus = status
            lastResolutionReason = resolutionReason
            lastResolvedAt = resolvedAt
            lastUserConfirmed = userConfirmed
            return 1
        }
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
        override suspend fun listAll(): List<AuditLogEntryEntity> = emptyList()
    }

    private companion object {
        const val NOW = 1_779_216_200_000L

        fun activeIntent(
            completionKeyJson: String? = "{}",
            primaryEvidenceJson: String = "{}",
            completionKeyStatus: CompletionKeyStatus = CompletionKeyStatus.FOUND
        ): ActiveIntentEntity = ActiveIntentEntity(
            intentId = "intent-1",
            captureId = "capture-1",
            intentType = IntentCategory.BUY_LATER_PRODUCT,
            status = ActiveIntentStatus.ACTIVE,
            completionKeyJson = completionKeyJson,
            completionKeyStatus = completionKeyStatus,
            primaryEvidenceJson = primaryEvidenceJson,
            primaryAction = "buy_or_skip",
            dueAt = null,
            expiresAt = null,
            resolutionReason = null,
            resolvedAt = null,
            userConfirmed = false,
            createdAt = NOW,
            updatedAt = NOW
        )
    }
}