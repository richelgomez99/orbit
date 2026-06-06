package com.orbit.app.data

import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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
    fun migrate_v8_to_v9_addsMemoryCandidateAndPromotedMemorySidecars() {
        helper.createDatabase(DB_NAME, 8).use { db ->
            insertEnvelopeV8(db, id = "env-1")
            insertEnvelopeV8(db, id = "env-2")
        }

        val migrated = helper.runMigrationsAndValidate(
            DB_NAME,
            /* version = */ 9,
            /* validateDroppedTables = */ true,
            MIGRATION_8_9
        )
        migrated.close()

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val room = Room.databaseBuilder(context, OrbitDatabase::class.java, DB_NAME)
            .addMigrations(MIGRATION_8_9)
            .allowMainThreadQueries()
            .build()
        try {
            val writable = room.openHelper.writableDatabase

            assertIndex(writable, "memory_candidate", "index_memory_candidate_state")
            assertIndex(writable, "memory_candidate_support", "index_memory_candidate_support_envelopeId")
            assertIndex(writable, "promoted_memory", "index_promoted_memory_candidateId")
            assertIndex(writable, "promoted_memory_support", "index_promoted_memory_support_memoryId")

            writable.execSQL(
                """
                INSERT INTO memory_candidate(
                    id, candidateKind, state, displayLabel, subject, predicate, objectValue,
                    confidence, sensitivity, supportingEnvelopeIdsJson, supportingEvidenceIdsJson,
                    supportingFeedbackIdsJson, askUserCopy, createdAt, updatedAt, expiresAt,
                    decidedAt, decisionReason, modelLabel, promptVersion, source
                ) VALUES(
                    'candidate-1', 'INTEREST', 'PENDING', 'Interested in pre-seed fundraising',
                    'user', 'interested_in', 'pre-seed fundraising',
                    0.82, 'NORMAL', '["env-1"]', NULL,
                    NULL, 'Remember this interest?', 1000, 1000, NULL,
                    NULL, NULL, 'debug_seed', NULL, 'DEBUG_SEED'
                )
                """.trimIndent()
            )
            writable.execSQL(
                """
                INSERT INTO memory_candidate_support(candidateId, envelopeId, supportType, evidenceId, createdAt)
                VALUES('candidate-1', 'env-1', 'CAPTURE', NULL, 1000)
                """.trimIndent()
            )
            assertInt(writable, 1, "SELECT COUNT(*) FROM memory_candidate_support WHERE candidateId = 'candidate-1'")

            try {
                writable.execSQL(
                    """
                    INSERT INTO memory_candidate(
                        id, candidateKind, state, displayLabel, subject, predicate, objectValue,
                        confidence, sensitivity, supportingEnvelopeIdsJson, supportingEvidenceIdsJson,
                        supportingFeedbackIdsJson, askUserCopy, createdAt, updatedAt, expiresAt,
                        decidedAt, decisionReason, modelLabel, promptVersion, source
                    ) VALUES(
                        'candidate-dup', 'INTEREST', 'PENDING', 'Duplicate interest',
                        'user', 'interested_in', 'pre-seed fundraising',
                        0.80, 'NORMAL', '["env-2"]', NULL,
                        NULL, NULL, 1001, 1001, NULL,
                        NULL, NULL, 'debug_seed', NULL, 'DEBUG_SEED'
                    )
                    """.trimIndent()
                )
                fail("Expected duplicate active candidate fact key to be rejected")
            } catch (_: SQLiteConstraintException) {
                // expected
            }

            writable.execSQL(
                """
                INSERT INTO promoted_memory(
                    id, candidateId, memoryKind, state, displayLabel, subject, predicate, objectValue,
                    confidenceLabel, sensitivity, source, supportingEnvelopeIdsJson,
                    supportingEvidenceIdsJson, supportingFeedbackIdsJson, createdAt, updatedAt,
                    validFrom, validTo, invalidatedAt, lastUsedAt, useCount
                ) VALUES(
                    'memory-1', 'candidate-1', 'RECURRING_INTEREST', 'ACTIVE',
                    'Interested in pre-seed fundraising', 'user', 'interested_in', 'pre-seed fundraising',
                    'CONFIRMED', 'NORMAL', 'USER_CONFIRMED', '["env-1"]',
                    NULL, NULL, 1100, 1100,
                    1100, NULL, NULL, NULL, 0
                )
                """.trimIndent()
            )
            writable.execSQL(
                """
                INSERT INTO promoted_memory_support(memoryId, envelopeId, supportType, evidenceId, createdAt)
                VALUES('memory-1', 'env-1', 'CAPTURE', NULL, 1100)
                """.trimIndent()
            )
            assertInt(writable, 1, "SELECT COUNT(*) FROM promoted_memory_support WHERE memoryId = 'memory-1'")

            writable.execSQL("DELETE FROM intent_envelope WHERE id = 'env-1'")
            assertInt(writable, 0, "SELECT COUNT(*) FROM memory_candidate_support WHERE candidateId = 'candidate-1'")
            assertInt(writable, 0, "SELECT COUNT(*) FROM promoted_memory_support WHERE memoryId = 'memory-1'")

            writable.execSQL("DELETE FROM memory_candidate WHERE id = 'candidate-1'")
            assertString(writable, null, "SELECT candidateId FROM promoted_memory WHERE id = 'memory-1'")
        } finally {
            room.close()
        }
    }

    private fun insertEnvelopeV8(db: SupportSQLiteDatabase, id: String) {
        db.insert(
            "intent_envelope",
            android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL,
            ContentValues().apply {
                put("id", id)
                put("contentType", "TEXT")
                put("textContent", "fundraising notes")
                put("imageUri", null as String?)
                put("textContentSha256", "$id-hash")
                put("intent", "REFERENCE")
                put("intentConfidence", null as Float?)
                put("intentSource", "FALLBACK")
                put("intentHistoryJson", "[]")
                put("createdAt", 1000L)
                put("day_local", "2026-06-05")
                put("isArchived", 0)
                put("isDeleted", 0)
                put("deletedAt", null as String?)
                put("sharedContinuationResultId", null as String?)
                put("kind", "REGULAR")
                put("derivedFromEnvelopeIdsJson", null as String?)
                put("todoMetaJson", null as String?)
                put("derivedVia", null as String?)
                put("primaryCanonicalUrlHash", null as String?)
                put("activePrimaryCanonicalUrlHash", null as String?)
                put("activeTextContentSha256", "$id-hash")
                put("appCategory", "OTHER")
                put("activityState", "STILL")
                put("tzId", "UTC")
                put("hourLocal", 21)
                put("dayOfWeekLocal", 5)
                put("sourceAppLabel", "Notes")
            }
        )
    }

    private fun assertInt(db: SupportSQLiteDatabase, expected: Int, sql: String) {
        db.query(sql).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(expected, cursor.getInt(0))
        }
    }

    private fun assertString(db: SupportSQLiteDatabase, expected: String?, sql: String) {
        db.query(sql).use { cursor ->
            assertTrue(cursor.moveToFirst())
            if (expected == null) {
                assertNull(cursor.getString(0))
            } else {
                assertEquals(expected, cursor.getString(0))
            }
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

    private companion object {
        const val DB_NAME = "orbit-migration-v8-v9-test.db"
    }
}
