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
import com.orbit.app.data.model.PromotedMemoryState
import com.orbit.app.graph.GraphRepositoryDelegate
import com.orbit.app.graph.GraphTargetType
import com.orbit.app.graph.RoomGraphBackendAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
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
            graphRepositoryDelegate = GraphRepositoryDelegate(
                adapter = RoomGraphBackendAdapter(db, clock = now),
                promotedMemorySupportDao = db.promotedMemorySupportDao()
            ),
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

        val second = delegate.rejectCandidate("candidate-1", "wrong")
        assertEquals(true, second.ok)
        assertEquals("already_rejected", second.status)
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

    @Test
    fun acceptCandidate_projectsPromotedMemoryIntoGraphFact() = runTest {
        seedCandidate("candidate-1")

        val result = delegate.acceptCandidate("candidate-1", null, null)

        val graph = db.graphDao().exportFacts(GraphRepositoryDelegate.LOCAL_USER_ID)
        assertEquals(1, graph.size)
        assertEquals("promoted-memory-fact-${result.memoryId}", graph.single().id)
        assertEquals("interested_in", graph.single().predicate)
        assertEquals("startup events", graph.single().objectText)

        val whyThis = RoomGraphBackendAdapter(db, clock = now).whyThis(
            userId = GraphRepositoryDelegate.LOCAL_USER_ID,
            targetType = GraphTargetType.FACT,
            targetId = graph.single().id
        )
        assertEquals(listOf("env-1"), whyThis!!.sources.map { it.sourceId })
    }

    @Test
    fun rejectCandidate_doesNotProjectGraphFact() = runTest {
        seedCandidate("candidate-1")

        delegate.rejectCandidate("candidate-1", "wrong")

        assertEquals(0, db.graphDao().exportFacts(GraphRepositoryDelegate.LOCAL_USER_ID).size)
    }

    @Test
    fun pendingProjection_ordersOldestFirstAndHidesTerminalCandidates() = runTest {
        seedEnvelope("env-2", text = "Recipe night note")
        seedCandidate("candidate-new", envelopeId = "env-2", createdAt = clock + 2_000, objectValue = "recipe nights")
        seedCandidate("candidate-old", envelopeId = "env-1", createdAt = clock + 1_000, objectValue = "startup events")
        delegate.rejectCandidate("candidate-new", "wrong")

        val rows = db.memoryCandidateDao().observePendingProjections(limit = 10).first()

        assertEquals(listOf("candidate-old"), rows.map { it.candidateId })
        assertEquals(1, rows.single().sourceCount)
        assertEquals("env-1", rows.single().primarySourceEnvelopeId)
        assertEquals("Startup event ticket", rows.single().primarySourceTitle)
    }

    @Test
    fun duplicateActiveFactKeyIsSuppressedUntilOriginalIsTerminal() = runTest {
        seedCandidate("candidate-1")

        val duplicate = candidateEntity("candidate-dup")
        assertEquals(-1L, db.memoryCandidateDao().insert(duplicate))
        assertEquals(
            "candidate-1",
            db.memoryCandidateDao()
                .findActiveDuplicate("INTEREST", "user", "interested_in", "startup events")
                ?.id
        )

        delegate.rejectCandidate("candidate-1", "wrong")

        assertEquals(
            null,
            db.memoryCandidateDao()
                .findActiveDuplicate("INTEREST", "user", "interested_in", "startup events")
        )
        assertEquals(1L, db.memoryCandidateDao().insert(duplicate.copy(id = "candidate-after-terminal")))
    }

    @Test
    fun sourceLessPendingAndPromotedMemoriesInvalidateConservatively() = runTest {
        seedEnvelope("env-2", text = "Recurring founder meetup")
        seedCandidate("candidate-pending", envelopeId = "env-2", objectValue = "founder meetups")
        seedCandidate("candidate-promoted", envelopeId = "env-1", objectValue = "startup events")
        val promoted = delegate.acceptCandidate("candidate-promoted", null, null)

        db.intentEnvelopeDao().hardDelete("env-2")
        val invalidatedPending = db.memoryCandidateDao()
            .invalidateSourceLessPending(reason = "source_deleted", at = clock + 5_000)
        assertEquals(1, invalidatedPending)
        assertEquals(
            MemoryCandidateState.INVALIDATED,
            db.memoryCandidateDao().getById("candidate-pending")!!.state
        )

        db.intentEnvelopeDao().hardDelete("env-1")
        val invalidatedPromoted = db.promotedMemoryDao().invalidateSourceLessActive(at = clock + 6_000)
        assertEquals(1, invalidatedPromoted)
        assertEquals(
            PromotedMemoryState.INVALIDATED,
            db.promotedMemoryDao().getById(promoted.memoryId!!)!!.state
        )
        assertEquals(0, db.promotedMemorySupportDao().countForMemory(promoted.memoryId!!))
    }

    private fun seedCandidate(
        id: String,
        envelopeId: String = "env-1",
        createdAt: Long = clock,
        objectValue: String = "startup events"
    ) {
        kotlinx.coroutines.runBlocking {
            val candidate = candidateEntity(
                id = id,
                envelopeId = envelopeId,
                createdAt = createdAt,
                objectValue = objectValue
            )
            db.memoryCandidateDao().insert(candidate)
            db.memoryCandidateSupportDao().insertAll(
                listOf(
                    MemoryCandidateSupportEntity(
                        candidateId = id,
                        envelopeId = envelopeId,
                        supportType = MemorySupportType.CAPTURE,
                        evidenceId = null,
                        createdAt = createdAt
                    )
                )
            )
        }
    }

    private fun candidateEntity(
        id: String,
        envelopeId: String = "env-1",
        createdAt: Long = clock,
        objectValue: String = "startup events"
    ) = MemoryCandidateEntity(
        id = id,
        candidateKind = MemoryCandidateKind.INTEREST,
        state = MemoryCandidateState.PENDING,
        displayLabel = "Interested in $objectValue",
        subject = "user",
        predicate = "interested_in",
        objectValue = objectValue,
        confidence = 0.78f,
        sensitivity = MemorySensitivity.NORMAL,
        supportingEnvelopeIdsJson = JSONArray().put(envelopeId).toString(),
        supportingEvidenceIdsJson = null,
        supportingFeedbackIdsJson = null,
        askUserCopy = "Remember this?",
        createdAt = createdAt,
        updatedAt = createdAt,
        expiresAt = null,
        decidedAt = null,
        decisionReason = null,
        modelLabel = "debug_seed",
        promptVersion = null,
        source = MemoryCandidateSource.DEBUG_SEED
    )

    private fun seedEnvelope(id: String, text: String = "Startup event ticket") {
        db.openHelper.writableDatabase.execSQL(
            """
            INSERT INTO intent_envelope(
                id, contentType, textContent, imageUri, textContentSha256,
                intent, intentConfidence, intentSource, intentHistoryJson,
                createdAt, day_local, isArchived, isDeleted, deletedAt,
                sharedContinuationResultId, appCategory, activityState,
                tzId, hourLocal, dayOfWeekLocal,
                kind, derivedFromEnvelopeIdsJson, todoMetaJson
            ) VALUES('$id', 'TEXT', '$text', NULL, NULL,
                'REFERENCE', NULL, 'USER_CHIP', '[]',
                $clock, '2026-06-05', 0, 0, NULL, NULL,
                'WORK_EMAIL', 'STILL', 'UTC', 21, 5,
                'REGULAR', NULL, NULL)
            """.trimIndent()
        )
    }
}
