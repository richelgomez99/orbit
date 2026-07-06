package com.orbit.app.data.ipc

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.orbit.app.audit.AuditLogWriter
import com.orbit.app.data.EnvelopeRepositoryImpl
import com.orbit.app.data.LocalRoomBackend
import com.orbit.app.data.OrbitDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AgentPlanBinderContractTest {

    private lateinit var db: OrbitDatabase
    private lateinit var repository: EnvelopeRepositoryImpl
    private val scopeJob = SupervisorJob()
    private val scope = CoroutineScope(scopeJob + Dispatchers.IO)
    private var now = 1_780_000_000_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, OrbitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = EnvelopeRepositoryImpl(
            backend = LocalRoomBackend(db),
            auditWriter = AuditLogWriter(clock = { now }),
            scope = scope,
            clock = { now },
        )
    }

    @After
    fun tearDown() {
        scopeJob.cancel()
        db.close()
    }

    @Test
    fun planAgentRequestReturnsCitedLocalPlanWithoutExecuting() {
        repository.seal(
            IntentEnvelopeDraftParcel(
                contentType = "TEXT",
                textContent = "Dentist appointment canceled. Need to reschedule.",
                imageUri = null,
                intent = "WANT_IT",
                intentConfidence = null,
                intentSource = "USER_CHIP",
            ),
            StateSnapshotParcel(
                appCategory = "OTHER",
                activityState = "STILL",
                tzId = "UTC",
                hourLocal = 12,
                dayOfWeekLocal = 6,
            )
        )

        // FR-010-013: natural-language queries are tokenized ("help"/"me"
        // are stopword/short-dropped; "reschedule" + "dentist" both match
        // the sealed envelope), so the coordinator finds evidence instead
        // of refusing on whole-phrase mismatch.
        val plan = repository.planAgentRequest(
            requestId = "request-1",
            query = "help me reschedule dentist",
            attachedEnvelopeIds = emptyArray(),
            maxEvidence = 5,
            allowModelAssist = true,
        )

        assertEquals("PLAN", plan.outcome)
        assertEquals(null, plan.modelLabel)
        assertTrue(plan.evidence.isNotEmpty())
        assertTrue(plan.steps.any { it.kind == "REVIEW_EVIDENCE" })
        assertFalse(plan.steps.any { it.label.contains("executed", ignoreCase = true) })

        val rendered = plan.toString()
        listOf("rawOcr", "imageBytes", "prompt", "embedding", "modelResponse", "apiKey").forEach {
            assertFalse(rendered.contains(it))
        }
    }

    @Test
    fun planAgentRequestRefusesWithoutEvidence() {
        val plan = repository.planAgentRequest(
            requestId = "request-1",
            query = "what is my passport number",
            attachedEnvelopeIds = emptyArray(),
            maxEvidence = 5,
            allowModelAssist = false,
        )

        assertEquals("REFUSE", plan.outcome)
        assertEquals(listOf("not_enough_saved_evidence"), plan.limitations)
        assertEquals(0, plan.evidence.size)
    }
}
