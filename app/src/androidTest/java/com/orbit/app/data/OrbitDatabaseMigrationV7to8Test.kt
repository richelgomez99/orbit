package com.orbit.app.data

import android.content.ContentValues
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Spec 004 — migration test for the v7 → v8 upgrade.
 *
 * v8 adds four new tables:
 *  - `capture_understanding`     (PK: captureId, FK → intent_envelope CASCADE)
 *  - `evidence_bundle`           (PK: id, FK → intent_envelope CASCADE, index on captureId)
 *  - `invalidation_record`       (PK: captureId, standalone)
 *  - `correction_feedback`       (PK: id, index on captureId)
 *
 * Strategy:
 *  1. Build a v7 DB and seed one `intent_envelope` row.
 *  2. Run MIGRATION_7_8 (helper validates against `8.json`).
 *  3. Re-open via the real Room builder to verify:
 *      - Existing envelope row survives the migration.
 *      - All four new tables accept inserts.
 *      - CASCADE delete from intent_envelope removes capture_understanding rows.
 */
@RunWith(AndroidJUnit4::class)
class OrbitDatabaseMigrationV7to8Test {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        OrbitDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @After
    fun tearDown() {
        runCatching {
            ApplicationProvider.getApplicationContext<android.content.Context>()
                .deleteDatabase(DB_NAME)
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate_v7_to_v8_newTablesExistAndAcceptInserts() {
        val now = 1_714_000_000_000L

        // ---- Arrange: seed v7 envelope row.
        helper.createDatabase(DB_NAME, 7).use { db ->
            db.insert(
                "intent_envelope",
                android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("id", "env-v7-001")
                    put("contentType", "LINK")
                    put("textContent", null as String?)
                    put("imageUri", null as String?)
                    put("textContentSha256", null as String?)
                    put("intent", "ARCHIVE")
                    put("intentConfidence", null as Float?)
                    put("intentSource", "USER")
                    put("intentHistoryJson", "[]")
                    put("createdAt", now)
                    put("day_local", "2026-04-25")
                    put("isArchived", 0)
                    put("isDeleted", 0)
                    put("appCategory", "BROWSER")
                    put("packageName", "com.android.chrome")
                    put("resolvedLinkUrl", "https://example.com/article")
                    put("derivedVia", null as String?)
                    put("clusterMeta", null as String?)
                }
            )
        }

        // ---- Act: run migration (Room validates schema against 8.json).
        helper.runMigrationsAndValidate(DB_NAME, 8, true, MIGRATION_7_8)

        // ---- Assert: re-open with Room to verify.
        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OrbitDatabase::class.java,
            DB_NAME
        )
            .addMigrations(MIGRATION_7_8)
            .build()

        try {
            val sdb = db.openHelper.writableDatabase

            // Existing envelope should still be present.
            sdb.query("SELECT id FROM intent_envelope WHERE id = ?", arrayOf("env-v7-001")).use { cursor ->
                assertEquals(1, cursor.count)
            }

            // Insert into capture_understanding.
            sdb.execSQL(
                "INSERT INTO capture_understanding VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>("env-v7-001", "BASIC", "READY", "Test Title", "Summary text", "[]", null, "https://example.com/article", null, now, now, null)
            )
            sdb.query("SELECT captureId FROM capture_understanding WHERE captureId = ?", arrayOf("env-v7-001")).use { cursor ->
                assertEquals(1, cursor.count)
            }

            // Insert into evidence_bundle.
            sdb.execSQL(
                "INSERT INTO evidence_bundle VALUES (?, ?, ?, ?, ?)",
                arrayOf<Any?>("bundle-001", "env-v7-001", "OG_TAGS", "{\"title\":\"Test\"}", now)
            )
            sdb.query("SELECT id FROM evidence_bundle WHERE captureId = ?", arrayOf("env-v7-001")).use { cursor ->
                assertEquals(1, cursor.count)
            }

            // Insert into invalidation_record.
            sdb.execSQL(
                "INSERT INTO invalidation_record VALUES (?, ?, ?)",
                arrayOf<Any?>("env-v7-001", now + 1000, "USER_REQUESTED")
            )
            sdb.query("SELECT captureId FROM invalidation_record WHERE captureId = ?", arrayOf("env-v7-001")).use { cursor ->
                assertEquals(1, cursor.count)
            }

            // Insert into correction_feedback.
            sdb.execSQL(
                "INSERT INTO correction_feedback VALUES (?, ?, ?, ?, ?)",
                arrayOf<Any?>("fb-001", "env-v7-001", "WRONG_SUMMARY", "Incorrect", now)
            )
            sdb.query("SELECT id FROM correction_feedback WHERE captureId = ?", arrayOf("env-v7-001")).use { cursor ->
                assertEquals(1, cursor.count)
            }
        } finally {
            db.close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate_v7_to_v8_cascadeDelete_removesUnderstandingRow() {
        val now = 1_714_001_000_000L

        helper.createDatabase(DB_NAME, 7).use { db ->
            db.insert(
                "intent_envelope",
                android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("id", "env-cascade-001")
                    put("contentType", "LINK")
                    put("textContent", null as String?)
                    put("imageUri", null as String?)
                    put("textContentSha256", null as String?)
                    put("intent", "ARCHIVE")
                    put("intentConfidence", null as Float?)
                    put("intentSource", "USER")
                    put("intentHistoryJson", "[]")
                    put("createdAt", now)
                    put("day_local", "2026-04-25")
                    put("isArchived", 0)
                    put("isDeleted", 0)
                    put("appCategory", "BROWSER")
                    put("packageName", "com.android.chrome")
                    put("resolvedLinkUrl", "https://example.com")
                    put("derivedVia", null as String?)
                    put("clusterMeta", null as String?)
                }
            )
        }

        helper.runMigrationsAndValidate(DB_NAME, 8, true, MIGRATION_7_8)

        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OrbitDatabase::class.java,
            DB_NAME
        )
            .addMigrations(MIGRATION_7_8)
            .build()

        try {
            val sdb = db.openHelper.writableDatabase

            sdb.execSQL(
                "INSERT INTO capture_understanding VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>("env-cascade-001", "BASIC", "READY", null, null, "[]", null, null, null, now, now, null)
            )

            // Delete parent — should cascade.
            sdb.execSQL("DELETE FROM intent_envelope WHERE id = ?", arrayOf("env-cascade-001"))

            sdb.query("SELECT captureId FROM capture_understanding WHERE captureId = ?", arrayOf("env-cascade-001")).use { cursor ->
                assertEquals(0, cursor.count)
            }
        } finally {
            db.close()
        }
    }

    private companion object {
        const val DB_NAME = "orbit_migration_v7_to_v8_test.db"
    }
}
