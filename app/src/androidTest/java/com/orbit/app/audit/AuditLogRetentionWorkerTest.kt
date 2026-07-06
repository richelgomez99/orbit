package com.orbit.app.audit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.orbit.app.data.OrbitDatabase
import com.orbit.app.data.entity.AuditLogEntryEntity
import com.orbit.app.data.model.AuditAction
import com.orbit.app.data.security.KeystoreKeyProvider
import kotlinx.coroutines.test.runTest
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * T086 — `AuditLogRetentionWorker` end-to-end test.
 *
 * Per audit-log-contract.md §2 / data-model.md §Retention:
 *   • Rows older than 90 days are purged.
 *   • Retention runs **silently** — no audit-of-audit row.
 *
 * This test seeds 100 rows dated 91 days ago + 10 fresh rows, runs the
 * worker via `TestListenableWorkerBuilder`, and asserts only 10 rows
 * survive and no new audit row of any kind was written by the worker.
 *
 * Driving the worker (rather than just `purge`) catches WorkManager wiring
 * regressions — `OrbitDatabase.getInstance` opens a real on-device encrypted
 * DB inside the worker, so this also exercises the SQLCipher passphrase
 * roundtrip on the test device.
 */
@RunWith(AndroidJUnit4::class)
class AuditLogRetentionWorkerTest {

    private lateinit var db: OrbitDatabase
    private lateinit var context: Context

    @Before
    fun setUp() {
        System.loadLibrary("sqlcipher")
        context = ApplicationProvider.getApplicationContext()
        // Open the real singleton DB so the worker (which calls
        // `OrbitDatabase.getInstance`) sees the seeded rows.
        //
        // 2026-07-06: this test runs against the REAL user database on the
        // test device. It must never wipe rows it did not seed — the previous
        // version called deleteOlderThan(Long.MAX_VALUE) here and erased the
        // user's entire audit history on every run. All seeding uses the
        // TEST_ID_PREFIX and cleanup targets only that prefix; assertions are
        // relative (before/after deltas), never absolute table counts.
        db = OrbitDatabase.getInstance(context)
        deleteSeededRows()
    }

    @After
    fun tearDown() {
        deleteSeededRows()
        AuditLogRetentionWorker.clockOverride = null
    }

    /** Remove only rows this suite seeded — never touch real user rows. */
    private fun deleteSeededRows() {
        db.openHelper.writableDatabase.execSQL(
            "DELETE FROM audit_log WHERE id LIKE '$TEST_ID_PREFIX%'"
        )
    }

    @Test
    fun worker_purgesOnlyOldRows_andWritesNoAuditOfAudit() = runTest {
        val dao = db.auditLogDao()
        val now = System.currentTimeMillis()
        val ninetyOneDays = TimeUnit.DAYS.toMillis(91)

        // 100 rows aged 91 days.
        repeat(100) { i ->
            dao.insert(
                AuditLogEntryEntity(
                    id = "${TEST_ID_PREFIX}old-$i-${UUID.randomUUID()}",
                    at = now - ninetyOneDays - i.toLong(),
                    action = AuditAction.ENVELOPE_CREATED,
                    description = "old #$i",
                    envelopeId = UUID.randomUUID().toString(),
                    extraJson = null
                )
            )
        }
        // 10 fresh rows aged < 1 day.
        repeat(10) { i ->
            dao.insert(
                AuditLogEntryEntity(
                    id = "${TEST_ID_PREFIX}fresh-$i-${UUID.randomUUID()}",
                    at = now - i.toLong() * 1_000L,
                    action = AuditAction.ENVELOPE_CREATED,
                    description = "fresh #$i",
                    envelopeId = UUID.randomUUID().toString(),
                    extraJson = null
                )
            )
        }
        val seeded = dao.listAll().filter { it.id.startsWith(TEST_ID_PREFIX) }
        assertEquals(110, seeded.size)
        val totalBeforeWorker = dao.listAll().size

        // Pin the worker's clock so the 91-day cutoff is stable.
        AuditLogRetentionWorker.clockOverride = { now }

        val worker = TestListenableWorkerBuilder<AuditLogRetentionWorker>(context).build()
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)

        val after = dao.listAll()
        val seededSurvivors = after.filter { it.id.startsWith(TEST_ID_PREFIX) }
        assertEquals(10, seededSurvivors.size)
        assertTrue(seededSurvivors.all { it.id.startsWith("${TEST_ID_PREFIX}fresh-") })

        // Silent retention: the worker MUST NOT write any new row.
        // (No "audit of audit" — purge itself is invisible per the contract.)
        // Relative check against the real table: the worker may legitimately
        // purge pre-existing user rows older than 90 days, so the total may
        // shrink by MORE than our 100 seeds, but it must never grow beyond
        // (before - 100).
        assertTrue(
            "Retention worker must not write any new audit rows",
            after.size <= totalBeforeWorker - 100
        )
    }

    @Test
    fun worker_succeedsCleanlyWhenNothingToPurge() = runTest {
        val dao = db.auditLogDao()
        val now = System.currentTimeMillis()
        repeat(3) { i ->
            dao.insert(
                AuditLogEntryEntity(
                    id = "${TEST_ID_PREFIX}fresh-$i-${UUID.randomUUID()}",
                    at = now - i.toLong() * 1_000L,
                    action = AuditAction.ENVELOPE_CREATED,
                    description = "fresh #$i",
                    envelopeId = null,
                    extraJson = null
                )
            )
        }
        AuditLogRetentionWorker.clockOverride = { now }

        val worker = TestListenableWorkerBuilder<AuditLogRetentionWorker>(context).build()
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(3, dao.listAll().count { it.id.startsWith(TEST_ID_PREFIX) })
    }

    private companion object {
        /** Marks every row this suite seeds so cleanup can target them exactly. */
        const val TEST_ID_PREFIX = "test-ret-"
    }
}
