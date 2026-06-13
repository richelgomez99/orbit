package com.orbit.app.data.ipc

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.orbit.app.audit.AuditLogWriter
import com.orbit.app.data.EnvelopeRepositoryImpl
import com.orbit.app.data.LocalRoomBackend
import com.orbit.app.data.OrbitDatabase
import com.orbit.app.graph.GraphEntityDraft
import com.orbit.app.graph.GraphEntityType
import com.orbit.app.graph.GraphFactDraft
import com.orbit.app.graph.GraphProvenanceDraft
import com.orbit.app.graph.GraphSourceType
import com.orbit.app.graph.GraphSupportKind
import com.orbit.app.graph.GraphTargetType
import com.orbit.app.graph.RoomGraphBackendAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GraphWhyThisBinderContractTest {

    private lateinit var db: OrbitDatabase
    private lateinit var graphAdapter: RoomGraphBackendAdapter
    private lateinit var repository: EnvelopeRepositoryImpl
    private val scopeJob = SupervisorJob()
    private val scope = CoroutineScope(scopeJob + Dispatchers.IO)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, OrbitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        graphAdapter = RoomGraphBackendAdapter(db) { 1_780_000_000_000L }
        repository = EnvelopeRepositoryImpl(
            backend = LocalRoomBackend(db),
            auditWriter = AuditLogWriter(clock = { 1_780_000_000_000L }),
            scope = scope,
            graphBackendAdapter = graphAdapter
        )
    }

    @After
    fun tearDown() {
        scopeJob.cancel()
        db.close()
    }

    @Test
    fun getGraphWhyThisReturnsCompactSourceProjection() = runTest {
        graphAdapter.upsertEntity(
            GraphEntityDraft(
                id = "user",
                userId = "local-user",
                type = GraphEntityType.USER,
                canonicalName = "You"
            )
        )
        graphAdapter.writeFact(
            fact = GraphFactDraft(
                id = "fact-1",
                userId = "local-user",
                subjectEntityId = "user",
                predicate = "saved_for",
                objectText = "dentist reschedule"
            ),
            provenance = listOf(
                GraphProvenanceDraft(
                    id = "prov-1",
                    userId = "local-user",
                    targetType = GraphTargetType.FACT,
                    targetId = "fact-1",
                    sourceType = GraphSourceType.ENVELOPE,
                    sourceId = "env-1",
                    supportKind = GraphSupportKind.ASSERTS
                )
            )
        )

        val result = repository.getGraphWhyThis("FACT", "fact-1")!!

        assertEquals("FACT", result.targetType)
        assertEquals("fact-1", result.targetId)
        assertEquals(listOf("env-1"), result.sources.map { it.sourceId })
        val rendered = result.toString()
        listOf("rawOcr", "imageBytes", "prompt", "embedding", "modelResponse").forEach {
            assertFalse(rendered.contains(it))
        }
    }

    @Test
    fun getGraphWhyThisRejectsUnknownTargetType() {
        assertNull(repository.getGraphWhyThis("BAD", "fact-1"))
    }
}
