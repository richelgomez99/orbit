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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class OrbitDatabaseMigrationV8toV9Test {

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
    fun migrate_v8_to_v9_activeIntentAcceptsInsertsAndKeepsV8Rows() {
        val now = 1_714_002_000_000L
        seedV8Database(now, "env-v8-001")

        helper.runMigrationsAndValidate(DB_NAME, 9, true, MIGRATION_8_9)

        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OrbitDatabase::class.java,
            DB_NAME
        )
            .addMigrations(MIGRATION_8_9)
            .build()

        try {
            val sdb = db.openHelper.writableDatabase
            sdb.query("SELECT captureId FROM capture_understanding WHERE captureId = ?", arrayOf("env-v8-001")).use { cursor ->
                assertEquals(1, cursor.count)
            }

            sdb.execSQL(
                "INSERT INTO active_intent VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(
                    "intent-001",
                    "env-v8-001",
                    "CHAT_ACTION",
                    "ACTIVE",
                    "{\"completionKey\":{\"status\":\"FOUND\"}}",
                    "Reply or act",
                    null,
                    null,
                    null,
                    null,
                    0,
                    now,
                    now
                )
            )
            sdb.query("SELECT intentId FROM active_intent WHERE captureId = ?", arrayOf("env-v8-001")).use { cursor ->
                assertEquals(1, cursor.count)
            }
        } finally {
            db.close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate_v8_to_v9_cascadeDelete_removesActiveIntentRows() {
        val now = 1_714_003_000_000L
        seedV8Database(now, "env-cascade-v9")
        helper.runMigrationsAndValidate(DB_NAME, 9, true, MIGRATION_8_9)

        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OrbitDatabase::class.java,
            DB_NAME
        )
            .addMigrations(MIGRATION_8_9)
            .build()

        try {
            val sdb = db.openHelper.writableDatabase
            sdb.execSQL(
                "INSERT INTO active_intent VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(
                    "intent-cascade",
                    "env-cascade-v9",
                    "READ_OR_WATCH_LATER",
                    "ACTIVE",
                    "{}",
                    "Read or watch",
                    null,
                    null,
                    null,
                    null,
                    0,
                    now,
                    now
                )
            )

            sdb.execSQL("DELETE FROM intent_envelope WHERE id = ?", arrayOf("env-cascade-v9"))

            sdb.query("SELECT intentId FROM active_intent WHERE captureId = ?", arrayOf("env-cascade-v9")).use { cursor ->
                assertEquals(0, cursor.count)
            }
        } finally {
            db.close()
        }
    }

    private fun seedV8Database(now: Long, captureId: String) {
        helper.createDatabase(DB_NAME, 8).use { db ->
            db.insert(
                "intent_envelope",
                android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("id", captureId)
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
            db.execSQL(
                "INSERT INTO capture_understanding VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(captureId, "BASIC", "READY", "Test Title", "Summary text", "[]", null, "https://example.com/article", null, now, now, null)
            )
        }
    }

    private companion object {
        const val DB_NAME = "orbit_migration_v8_to_v9_test.db"
    }
}
