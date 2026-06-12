package com.orbit.app.graph

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.orbit.app.data.OrbitDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomGraphBackendAdapterTest {

    private lateinit var db: OrbitDatabase
    private lateinit var adapter: RoomGraphBackendAdapter
    private var now = 1_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, OrbitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        adapter = RoomGraphBackendAdapter(db) { now }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun factWritesRequireProvenance() = runTest {
        adapter.upsertEntity(userEntity())

        val result = adapter.writeFact(profileFact(), provenance = emptyList())

        assertEquals(GraphWriteResult.Rejected("missing_provenance"), result)
    }

    @Test
    fun whyThisReturnsOnlySurvivingSourceEvidence() = runTest {
        adapter.upsertEntity(userEntity())
        adapter.writeFact(
            profileFact(),
            provenance = listOf(
                factSource("support-1", "env-1"),
                factSource("support-2", "env-2"),
            )
        )

        val result = adapter.invalidateBySource(GraphSourceType.ENVELOPE, "env-1", "source_deleted")
        val whyThis = adapter.whyThis(USER_ID, GraphTargetType.FACT, "fact-founder")

        assertTrue(result.invalidatedTargetIds.isEmpty())
        assertEquals(setOf("fact-founder"), result.preservedTargetIds)
        assertEquals(listOf("env-2"), whyThis!!.sources.map { it.sourceId })
    }

    @Test
    fun invalidatingOnlySourceInvalidatesFact() = runTest {
        adapter.upsertEntity(userEntity())
        adapter.writeFact(profileFact(), provenance = listOf(factSource("support-1", "env-1")))

        val result = adapter.invalidateBySource(GraphSourceType.ENVELOPE, "env-1", "source_deleted")

        assertEquals(setOf("fact-founder"), result.invalidatedTargetIds)
        assertTrue(result.preservedTargetIds.isEmpty())
        assertNull(adapter.whyThis(USER_ID, GraphTargetType.FACT, "fact-founder"))
    }

    @Test
    fun feedbackRequiresLocalSourceEpisode() = runTest {
        val result = adapter.writeFeedback(
            GraphFeedbackDraft(
                id = "feedback-1",
                userId = USER_ID,
                targetType = GraphTargetType.FACT,
                targetId = "fact-founder",
                feedbackType = GraphFeedbackType.CORRECTED,
                replacementText = "I am not a founder yet.",
                sourceType = GraphSourceType.USER_CORRECTION,
                sourceId = "",
            )
        )

        assertEquals(GraphWriteResult.Rejected("missing_source_episode"), result)
    }

    private companion object {
        const val USER_ID = "local-user"

        fun userEntity() = GraphEntityDraft(
            id = "user",
            userId = USER_ID,
            type = GraphEntityType.USER,
            canonicalName = "You",
        )

        fun profileFact() = GraphFactDraft(
            id = "fact-founder",
            userId = USER_ID,
            subjectEntityId = "user",
            predicate = "working_on",
            objectText = "Orbit",
        )

        fun factSource(id: String, envelopeId: String) = GraphProvenanceDraft(
            id = id,
            userId = USER_ID,
            targetType = GraphTargetType.FACT,
            targetId = "fact-founder",
            sourceType = GraphSourceType.ENVELOPE,
            sourceId = envelopeId,
            supportKind = GraphSupportKind.ASSERTS,
        )
    }
}
