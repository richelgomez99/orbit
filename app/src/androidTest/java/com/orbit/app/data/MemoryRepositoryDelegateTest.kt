package com.orbit.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.orbit.app.audit.AuditLogWriter
import com.orbit.app.data.entity.MemoryCandidateEntity
import com.orbit.app.data.entity.MemoryCandidateSupportEntity
import com.orbit.app.data.model.AuditAction
import com.orbit.app.data.model.MemoryCandidateKind
import com.orbit.app.data.model.MemoryCandidateSource
import com.orbit.app.data.model.MemoryCandidateState
import com.orbit.app.data.model.MemorySensitivity
import com.orbit.app.data.model.MemorySupportType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MemoryRepositoryDelegateTest {

    private lateinit var db: OrbitDatabase
    private lateinit var delegate: MemoryRepositoryDelegate
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var clock: Long = 1_780_000_000_000L
    private val now: () -> Long = { clock }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, OrbitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        delegate = MemoryRepositoryDelegate(
            database = db,
            auditWriter = AuditLogWriter(clock = now, idGen = { UUID.randomUUID().toString() }),
            scope = scope,
            clock = now
        )
        seedEnvelope("env-1")
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun acceptCandidate_promotesOnceAndWritesAudit() = runTest {
        seedCandidate("candidate-1")

        val first = delegate.acceptCandidate(
            candidateId = "candidate-1",
            editedLabel = null,
            editedFactText = null
        )
        assertEquals(true, first.ok)
        assertEquals("promoted", first.status)
        assertNotNull(first.memoryId)

        val candidate = db.memoryCandidateDao().getById("candidate-1")!!
        assertEquals(MemoryCandidateState.PROMOTED, candidate.state)
        assertEquals(1, db.promotedMemorySupportDao().countForMemory(first.memoryId!!))
        assertEquals(1, db.auditLogDao().listAll().count { it.action == AuditAction.MEMORY_CANDIDATE_ACCEPTED })

        val second = delegate.acceptCandidate(
            candidateId = "candidate-1",
            editedLabel = null,
            editedFactText = null
        )
        assertEquals(true, second.ok)
        assertEquals("already_promoted", second.status)
        assertEquals(first.memoryId, second.memoryId)
        assertEquals(1, db.auditLogDao().listAll().count { it.action == AuditAction.MEMORY_CANDIDATE_ACCEPTED })
    }

    @Test
    fun rejectCandidate_marksRejectedWithoutPromoting() = runTest {
        seedCandidate("candidate-1")

        val result = delegate.rejectCandidate("candidate-1", "wrong")

        assertEquals(true, result.ok)
        assertEquals("rejected", result.status)
        assertEquals(MemoryCandidateState.REJECTED, db.memoryCandidateDao().getById("candidate-1")!!.state)
        assertEquals(0, db.auditLogDao().listAll().count { it.action == AuditAction.MEMORY_CANDIDATE_ACCEPTED })
        assertEquals(1, db.auditLogDao().listAll().count { it.action == AuditAction.MEMORY_CANDIDATE_REJECTED })
    }

    @Test
    fun acceptCandidate_withEditedText_promotesUserEditedMemory() = runTest {
        seedCandidate("candidate-1")

        val result = delegate.acceptCandidate(
            candidateId = "candidate-1",
            editedLabel = "Tracks founder events",
            editedFactText = "founder events"
        )

        val promoted = db.promotedMemoryDao().getById(result.memoryId!!)!!
        assertEquals("Tracks founder events", promoted.displayLabel)
        assertEquals("founder events", promoted.objectValue)
    }

    private fun seedCandidate(id: String) {
        kotlinx.coroutines.runBlocking {
            val candidate = MemoryCandidateEntity(
                id = id,
                candidateKind = MemoryCandidateKind.INTEREST,
                state = MemoryCandidateState.PENDING,
                displayLabel = "Interested in startup events",
                subject = "user",
                predicate = "interested_in",
                objectValue = "startup events",
                confidence = 0.78f,
                sensitivity = MemorySensitivity.NORMAL,
                supportingEnvelopeIdsJson = JSONArray().put("env-1").toString(),
                supportingEvidenceIdsJson = null,
                supportingFeedbackIdsJson = null,
                askUserCopy = "Remember this?",
                createdAt = clock,
                updatedAt = clock,
                expiresAt = null,
                decidedAt = null,
                decisionReason = null,
                modelLabel = "debug_seed",
                promptVersion = null,
                source = MemoryCandidateSource.DEBUG_SEED
            )
            db.memoryCandidateDao().insert(candidate)
            db.memoryCandidateSupportDao().insertAll(
                listOf(
                    MemoryCandidateSupportEntity(
                        candidateId = id,
                        envelopeId = "env-1",
                        supportType = MemorySupportType.CAPTURE,
                        evidenceId = null,
                        createdAt = clock
                    )
                )
            )
        }
    }

    private fun seedEnvelope(id: String) {
        db.openHelper.writableDatabase.execSQL(
            """
            INSERT INTO intent_envelope(
                id, contentType, textContent, imageUri, textContentSha256,
                intent, intentConfidence, intentSource, intentHistoryJson,
                createdAt, day_local, isArchived, isDeleted, deletedAt,
                sharedContinuationResultId, appCategory, activityState,
                tzId, hourLocal, dayOfWeekLocal,
                kind, derivedFromEnvelopeIdsJson, todoMetaJson
            ) VALUES('$id', 'TEXT', 'Startup event ticket', NULL, NULL,
                'REFERENCE', NULL, 'USER_CHIP', '[]',
                $clock, '2026-06-05', 0, 0, NULL, NULL,
                'WORK_EMAIL', 'STILL', 'UTC', 21, 5,
                'REGULAR', NULL, NULL)
            """.trimIndent()
        )
    }
}
