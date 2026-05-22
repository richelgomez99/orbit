package com.orbit.app.data

import android.content.ContentValues
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class OrbitDatabaseMigrationV7toV8Test {

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
    fun migrate_v7_to_v8_addsUnderstandingActiveIntentAndInvalidationSidecars() {
        helper.createDatabase(DB_NAME, 7).use { db ->
            insertEnvelopeV7(db, id = "env-1")
        }

        val migrated = helper.runMigrationsAndValidate(
            DB_NAME,
            /* version = */ 8,
            /* validateDroppedTables = */ true,
            MIGRATION_7_8
        )
        migrated.close()

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val room = Room.databaseBuilder(context, OrbitDatabase::class.java, DB_NAME)
            .addMigrations(MIGRATION_7_8)
            .allowMainThreadQueries()
            .build()
        try {
            val writable = room.openHelper.writableDatabase

            assertIndex(writable, "capture_understanding", "index_capture_understanding_category")
            assertIndex(writable, "evidence_bundle", "index_evidence_bundle_captureId")
            assertIndex(writable, "active_intent", "index_active_intent_status")
            assertNoForeignKeys(writable, "invalidation_record")

            writable.execSQL(
                """
                INSERT INTO capture_understanding(
                    captureId, mode, status, category, categoryConfidence,
                    title, summaryText, completionKeyJson, completionKeyStatus,
                    sourceIdentityJson, contentHashHex, canonicalUrl,
                    groundingConstraintsJson, createdAt, updatedAt, invalidatedAt
                ) VALUES(
                    'env-1', 'BASIC', 'READY', 'SHOPPING', 0.87,
                    'Running shoes', 'Price screenshot', '{"kind":"price","label":"price-42"}', 'FOUND',
                    '{"packageName":"com.shop"}', 'hash-env-1', 'https://example.com/shoes',
                    '{"mode":"local"}', 1000, 1000, NULL
                )
                """.trimIndent()
            )
            writable.execSQL(
                """
                INSERT INTO evidence_bundle(id, captureId, bundleType, payloadJson, createdAt)
                VALUES('evidence-1', 'env-1', 'LOCAL_TEXT', '{"kind":"excerpt","label":"Price"}', 1000)
                """.trimIndent()
            )
            writable.execSQL(
                """
                INSERT INTO active_intent(
                    intentId, captureId, intentType, status, completionKeyJson,
                    completionKeyStatus, primaryEvidenceJson, primaryAction,
                    dueAt, expiresAt, resolutionReason, resolvedAt,
                    userConfirmed, createdAt, updatedAt
                ) VALUES(
                    'intent-1', 'env-1', 'SHOPPING', 'ACTIVE', '{"kind":"price","label":"price-42"}',
                    'FOUND', '{"kind":"excerpt","label":"Price"}', 'buy_or_ignore',
                    NULL, NULL, NULL, NULL,
                    0, 1000, 1000
                )
                """.trimIndent()
            )
            writable.execSQL(
                """
                INSERT INTO invalidation_record(captureId, invalidatedAt, reason)
                VALUES('env-1', 2000, 'deleted_capture')
                """.trimIndent()
            )

            assertInt(writable, 1, "SELECT COUNT(*) FROM capture_understanding WHERE captureId = 'env-1'")
            assertInt(writable, 1, "SELECT COUNT(*) FROM evidence_bundle WHERE captureId = 'env-1'")
            assertInt(writable, 1, "SELECT COUNT(*) FROM active_intent WHERE captureId = 'env-1'")
            assertInt(writable, 1, "SELECT COUNT(*) FROM invalidation_record WHERE captureId = 'env-1'")

            writable.execSQL("DELETE FROM intent_envelope WHERE id = 'env-1'")

            assertInt(writable, 0, "SELECT COUNT(*) FROM capture_understanding WHERE captureId = 'env-1'")
            assertInt(writable, 0, "SELECT COUNT(*) FROM evidence_bundle WHERE captureId = 'env-1'")
            assertInt(writable, 0, "SELECT COUNT(*) FROM active_intent WHERE captureId = 'env-1'")
            assertInt(writable, 1, "SELECT COUNT(*) FROM invalidation_record WHERE captureId = 'env-1'")
        } finally {
            room.close()
        }
    }

    private fun insertEnvelopeV7(db: SupportSQLiteDatabase, id: String) {
        db.insert(
            "intent_envelope",
            android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL,
            ContentValues().apply {
                put("id", id)
                put("contentType", "TEXT")
                put("textContent", "screenshot text")
                put("imageUri", null as String?)
                put("textContentSha256", "text-hash")
                put("intent", "REFERENCE")
                put("intentConfidence", null as Float?)
                put("intentSource", "FALLBACK")
                put("intentHistoryJson", "[]")
                put("createdAt", 1000L)
                put("day_local", "2026-05-18")
                put("isArchived", 0)
                put("isDeleted", 0)
                put("deletedAt", null as Long?)
                put("sharedContinuationResultId", null as String?)
                put("kind", "REGULAR")
                put("derivedFromEnvelopeIdsJson", null as String?)
                put("todoMetaJson", null as String?)
                put("derivedVia", null as String?)
                put("primaryCanonicalUrlHash", null as String?)
                put("activePrimaryCanonicalUrlHash", null as String?)
                put("activeTextContentSha256", "text-hash")
                put("appCategory", "SHOPPING")
                put("activityState", "STILL")
                put("tzId", "UTC")
                put("hourLocal", 12)
                put("dayOfWeekLocal", 1)
                put("sourceAppLabel", "Shopping")
            }
        )
    }

    private fun assertInt(db: SupportSQLiteDatabase, expected: Int, sql: String) {
        db.query(sql).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(expected, cursor.getInt(0))
        }
    }

    private fun assertIndex(db: SupportSQLiteDatabase, table: String, indexName: String) {
        db.query("PRAGMA index_list('$table')").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == indexName) {
                    return
                }
            }
        }
        fail("Expected index $indexName on $table")
    }

    private fun assertNoForeignKeys(db: SupportSQLiteDatabase, table: String) {
        db.query("PRAGMA foreign_key_list('$table')").use { cursor ->
            assertEquals(0, cursor.count)
        }
    }

    private companion object {
        const val DB_NAME = "orbit-migration-v7-v8-test.db"
    }
}