package com.orbit.app.audit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.orbit.app.data.OrbitDatabase
import com.orbit.app.data.model.AuditAction
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Spec 023 FR-023-003/004/005 — write a sanitized record, drain it into
 * a CRASH_DETECTED audit row, verify the record is marked reported and
 * disk retention stays capped.
 */
@RunWith(AndroidJUnit4::class)
class CrashJournalRoundtripTest {

    private lateinit var context: Context
    private lateinit var db: OrbitDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        CrashJournal.journalDir(context).listFiles()?.forEach { it.delete() }
        db = Room.inMemoryDatabaseBuilder(context, OrbitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
        CrashJournal.journalDir(context).listFiles()?.forEach { it.delete() }
    }

    @Test
    fun writeThenDrain_producesBoundedAuditRow_andMarksReported() = runBlocking {
        val t = IllegalStateException("message with user content that must not appear")
        CrashJournal.writeRecord(context, "com.orbit.app:ml", "main", t, nowMillis = 111L)

        val drained = CrashJournal.drainToAudit(context, db.auditLogDao(), AuditLogWriter())
        assertEquals(1, drained)

        val rows = db.auditLogDao().listAll().filter { it.action == AuditAction.CRASH_DETECTED }
        assertEquals(1, rows.size)
        val row = rows.single()
        assertTrue(row.description.contains("com.orbit.app:ml"))
        assertTrue(row.description.contains("IllegalStateException"))
        assertFalse(
            "audit row must not carry message content",
            row.extraJson.orEmpty().contains("user content")
        )
        assertTrue(row.extraJson.orEmpty().contains("recordDigest"))

        // Drained record is marked reported and not re-drained.
        val files = CrashJournal.journalDir(context).listFiles().orEmpty()
        assertTrue(files.all { it.name.endsWith(".reported") })
        assertEquals(0, CrashJournal.drainToAudit(context, db.auditLogDao(), AuditLogWriter()))
    }

    @Test
    fun retention_capsRecordCount() {
        repeat(CrashJournal.MAX_RETAINED_RECORDS + 10) { i ->
            CrashJournal.writeRecord(
                context, "com.orbit.app", "main",
                RuntimeException("r$i"), nowMillis = 1_000L + i,
            )
        }
        val count = CrashJournal.journalDir(context).listFiles().orEmpty().size
        assertTrue(
            "retained $count, cap is ${CrashJournal.MAX_RETAINED_RECORDS}",
            count <= CrashJournal.MAX_RETAINED_RECORDS
        )
    }
}
