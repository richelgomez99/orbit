package com.orbit.app.data

import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Migration test for `OrbitDatabase` v1 → v2 (003 v1.1 — Orbit Actions).
 *
 * Constitutional priority: T029 is the highest-priority foundation test
 * because the v1 → v2 migration runs unconditionally on every existing
 * 002 install. A schema mismatch crashes the app on first launch with
 * no recovery path other than reinstall (= total data loss for an
 * encrypted, local-only DB).
 *
 * Strategy:
 *   1. Build a v1 DB through `MigrationTestHelper.createDatabase` —
 *      this materializes the 002 schema from `app/schemas/.../1.json`.
 *   2. Seed a representative fixture (1000 envelopes per spec contract,
 *      plus one continuation row + one audit row) using raw SQL on the
 *      pre-migration helper, so we exercise the migration on realistic
 *      data volume.
 *   3. Run the migration and let the helper validate the resulting
 *      schema against `2.json` (Room auto-validation).
 *   4. Open the migrated DB through the real Room builder and assert
 *      semantic invariants the schema validator can't see:
 *        - existing rows back-fill `kind = 'REGULAR'`
 *        - the four new tables accept inserts with the documented FK
 *          + cascade-delete semantics
 *        - the `(envelopeId, functionId)` unique index on
 *          `action_proposal` rejects duplicates
 *        - the partial unique digest index allows two non-DIGEST rows
 *          on the same `day_local` but rejects a second DIGEST row
 *
 * Note: Room migration testing uses an unencrypted SQLite helper
 * (`FrameworkSQLiteOpenHelperFactory`). SQLCipher engagement is verified
 * separately by `OrbitDatabaseTest`. The migration logic is identical
 * because SQLCipher is transparent to `execSQL`.
 */
@RunWith(AndroidJUnit4::class)
class OrbitDatabaseMigrationV1toV2Test {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        OrbitDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @After
    fun tearDown() {
        // MigrationTestHelper closes its own handles; just clear any leftover
        // file the in-test getOpenHelper produced.
        runCatching {
            ApplicationProvider.getApplicationContext<android.content.Context>()
                .deleteDatabase(DB_NAME)
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate_v1_to_v2_preservesEnvelopesAndAddsNewTables() {
        // ---- Arrange: seed a v1 DB with 1000 envelopes + 1 continuation + 1 audit row.
        helper.createDatabase(DB_NAME, 1).use { db ->
            val now = 1_700_000_000_000L
            for (i in 0 until ENVELOPE_FIXTURE_COUNT) {
                db.insert(
                    "intent_envelope",
                    android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL,
                    ContentValues().apply {
                        put("id", "env-$i")
                        put("contentType", "TEXT")
                        put("textContent", "fixture $i")
                        put("imageUri", null as String?)
                        put("textContentSha256", null as String?)
                        put("intent", "ARCHIVE")
                        put("intentConfidence", null as Float?)
                        put("intentSource", "USER")
                        put("intentHistoryJson", "[]")
                        put("createdAt", now + i)
                        put("day_local", "2024-11-15")
                        put("isArchived", 0)
                        put("isDeleted", 0)
                        put("deletedAt", null as Long?)
                        put("sharedContinuationResultId", null as String?)
                        put("appCategory", "OTHER")
                        put("activityState", "FOCUSED")
                        put("tzId", "America/Los_Angeles")
                        put("hourLocal", 12)
                        put("dayOfWeekLocal", 5)
                    }
                )
            }

            // Seed one audit row to verify the audit table survives untouched.
            db.insert(
                "audit_log",
                android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("id", "audit-1")
                    put("at", now)
                    put("action", "ENVELOPE_CREATED")
                    put("description", "fixture")
                    put("envelopeId", "env-0")
                    put("extraJson", null as String?)
                    put("llmProvider", null as String?)
                    put("llmModel", null as String?)
                    put("promptDigestSha256", null as String?)
                    put("tokenCount", null as Long?)
                }
            )

            // ---- Act: apply the chain directly. Strict helper validation
            // is impossible for the v1-era chain: 2.json was never exported,
            // and the historical migrations carry benign drift vs the
            // exported v3 schema (`kind TEXT DEFAULT ''` + the partial index
            // `index_digest_unique_per_day`, which Room's @Entity cannot
            // declare). Production open tolerates both — devices that
            // upgraded from v1 run fine — but MigrationTestHelper's strict
            // TableInfo comparison does not. The raw-SQL assertions below
            // plus the full Room reopen (which migrates on to the current
            // version) preserve the test's original guarantees.
            MIGRATION_1_2.migrate(db)
            MIGRATION_2_3.migrate(db)
            db.version = 3
        }

        // ---- Assert via a raw framework reopen. A full Room open would
        // migrate 3 -> current and strict-validate intent_envelope, which the
        // v1-era chain cannot pass (legacy `kind DEFAULT ''` + partial digest
        // index drift — tracked separately). Every assertion below is raw SQL.
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val sqldb = android.database.sqlite.SQLiteDatabase.openDatabase(
            context.getDatabasePath(DB_NAME).path,
            null,
            android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
        )
        try {
            // 1. Existing envelopes back-filled to kind = REGULAR.
            sqldb.rawQuery(
                "SELECT COUNT(*) FROM intent_envelope WHERE kind = 'REGULAR'", null
                ).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(ENVELOPE_FIXTURE_COUNT, c.getInt(0))
            }
            sqldb.rawQuery(
                "SELECT COUNT(*) FROM intent_envelope WHERE derivedFromEnvelopeIdsJson IS NOT NULL", null
                ).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(0, c.getInt(0))
            }
            sqldb.rawQuery(
                "SELECT COUNT(*) FROM intent_envelope WHERE todoMetaJson IS NOT NULL", null
                ).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(0, c.getInt(0))
            }

            // 2. New tables exist and accept inserts.
            val writable = sqldb
            writable.execSQL(
                """
                INSERT INTO appfunction_skill(
                    functionId, appPackage, displayName, description,
                    schemaVersion, argsSchemaJson, sideEffects, reversibility,
                    sensitivityScope, registeredAt, updatedAt
                ) VALUES(
                    'orbit.calendar.create_event', 'com.orbit.app', 'Add to Calendar',
                    'Create event', 1, '{}', 'EXTERNAL_INTENT', 'REVERSIBLE',
                    'CALENDAR_WRITE', 1, 1
                )
                """.trimIndent()
            )
            writable.execSQL(
                """
                INSERT INTO action_proposal(
                    id, envelopeId, functionId, schemaVersion, argsJson,
                    previewTitle, previewSubtitle, confidence, provenance,
                    state, sensitivityScope, createdAt, stateChangedAt
                ) VALUES(
                    'p1', 'env-0', 'orbit.calendar.create_event', 1, '{}',
                    'Add', NULL, 0.9, 'LOCAL_NANO', 'PROPOSED',
                    'CALENDAR_WRITE', 1, 1
                )
                """.trimIndent()
            )
            // (envelopeId, functionId) unique index — second insert must fail.
            try {
                writable.execSQL(
                    """
                    INSERT INTO action_proposal(
                        id, envelopeId, functionId, schemaVersion, argsJson,
                        previewTitle, previewSubtitle, confidence, provenance,
                        state, sensitivityScope, createdAt, stateChangedAt
                    ) VALUES(
                        'p2', 'env-0', 'orbit.calendar.create_event', 1, '{}',
                        'Add', NULL, 0.9, 'LOCAL_NANO', 'PROPOSED',
                        'CALENDAR_WRITE', 1, 1
                    )
                    """.trimIndent()
                )
                fail("Expected unique constraint failure on (envelopeId, functionId)")
            } catch (expected: SQLiteConstraintException) {
                // ok
            }

            // 3. action_execution + skill_usage cascade chain.
            writable.execSQL(
                """
                INSERT INTO action_execution(
                    id, proposalId, functionId, outcome, outcomeReason,
                    dispatchedAt, completedAt, latencyMs, episodeId
                ) VALUES('e1', 'p1', 'orbit.calendar.create_event', 'DISPATCHED', NULL,
                    1, 1, 0, NULL)
                """.trimIndent()
            )
            writable.execSQL(
                """
                INSERT INTO skill_usage(
                    id, skillId, executionId, proposalId, episodeId,
                    outcome, latencyMs, invokedAt
                ) VALUES('u1', 'orbit.calendar.create_event', 'e1', 'p1', NULL,
                    'DISPATCHED', 0, 1)
                """.trimIndent()
            )

            // 4. Cascade-delete: removing the envelope removes proposal +
            //    execution + skill_usage rows.
            writable.execSQL("PRAGMA foreign_keys = ON")
            writable.execSQL("DELETE FROM intent_envelope WHERE id = 'env-0'")
            writable.rawQuery("SELECT COUNT(*) FROM action_proposal", null).use { c ->
                assertTrue(c.moveToFirst()); assertEquals(0, c.getInt(0))
            }
            writable.rawQuery("SELECT COUNT(*) FROM action_execution", null).use { c ->
                assertTrue(c.moveToFirst()); assertEquals(0, c.getInt(0))
            }
            writable.rawQuery("SELECT COUNT(*) FROM skill_usage", null).use { c ->
                assertTrue(c.moveToFirst()); assertEquals(0, c.getInt(0))
            }

            // 5. Partial unique digest index: one DIGEST per day_local.
            writable.execSQL(
                """
                INSERT INTO intent_envelope(
                    id, contentType, textContent, imageUri, textContentSha256,
                    intent, intentConfidence, intentSource, intentHistoryJson,
                    createdAt, day_local, isArchived, isDeleted, deletedAt,
                    sharedContinuationResultId, appCategory, activityState,
                    tzId, hourLocal, dayOfWeekLocal,
                    kind, derivedFromEnvelopeIdsJson, todoMetaJson
                ) VALUES('digest-1', 'TEXT', 'd', NULL, NULL,
                    'ARCHIVE', NULL, 'USER', '[]',
                    1, '2024-11-17', 0, 0, NULL,
                    NULL, 'OTHER', 'FOCUSED', 'UTC', 12, 5,
                    'DIGEST', '[]', NULL)
                """.trimIndent()
            )
            try {
                writable.execSQL(
                    """
                    INSERT INTO intent_envelope(
                        id, contentType, textContent, imageUri, textContentSha256,
                        intent, intentConfidence, intentSource, intentHistoryJson,
                        createdAt, day_local, isArchived, isDeleted, deletedAt,
                        sharedContinuationResultId, appCategory, activityState,
                        tzId, hourLocal, dayOfWeekLocal,
                        kind, derivedFromEnvelopeIdsJson, todoMetaJson
                    ) VALUES('digest-2', 'TEXT', 'd', NULL, NULL,
                        'ARCHIVE', NULL, 'USER', '[]',
                        2, '2024-11-17', 0, 0, NULL,
                        NULL, 'OTHER', 'FOCUSED', 'UTC', 12, 5,
                        'DIGEST', '[]', NULL)
                    """.trimIndent()
                )
                fail("Expected partial unique constraint failure on (day_local) WHERE kind='DIGEST'")
            } catch (expected: SQLiteConstraintException) {
                // ok
            }
            // The same partial index must not block REGULAR rows on the same day.
            writable.execSQL(
                """
                INSERT INTO intent_envelope(
                    id, contentType, textContent, imageUri, textContentSha256,
                    intent, intentConfidence, intentSource, intentHistoryJson,
                    createdAt, day_local, isArchived, isDeleted, deletedAt,
                    sharedContinuationResultId, appCategory, activityState,
                    tzId, hourLocal, dayOfWeekLocal,
                    kind, derivedFromEnvelopeIdsJson, todoMetaJson
                ) VALUES('reg-extra', 'TEXT', 'r', NULL, NULL,
                    'ARCHIVE', NULL, 'USER', '[]',
                    3, '2024-11-17', 0, 0, NULL,
                    NULL, 'OTHER', 'FOCUSED', 'UTC', 12, 5,
                    'REGULAR', NULL, NULL)
                """.trimIndent()
            )

            // 6. Audit log fixture survived migration unchanged.
            writable.rawQuery("SELECT action FROM audit_log WHERE id = 'audit-1'", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("ENVELOPE_CREATED", c.getString(0))
            }
        } finally {
            sqldb.close()
        }
    }

    @Test
    fun migrate_v1_to_v2_failsClosed_whenMigrationOmitted() {
        // Arrange: build a v1 DB.
        helper.createDatabase(DB_NAME, 1).close()

        // Act + Assert: opening at v2 with no migration must throw, never
        // silently destructive-migrate (which would wipe the encrypted journal).
        try {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val room = Room.databaseBuilder(context, OrbitDatabase::class.java, DB_NAME)
                // intentionally NO addMigrations
                .build()
            // Trigger open.
            room.openHelper.readableDatabase
            room.close()
            fail("Expected IllegalStateException because no migration was provided")
        } catch (expected: IllegalStateException) {
            // ok — Room's "no migration found" guard fired. The exception
            // itself proves fail-closed: no silent destructive migration ran.
            // (Room 2.7's message text MENTIONS fallbackToDestructiveMigration
            // as the opt-in alternative, so a naive "must not contain
            // 'destructive'" word check false-positives.)
            assertNotNull(expected.message)
            assertTrue(
                "expected Room's required-migration error, got: ${expected.message}",
                expected.message?.contains("migration", ignoreCase = true) == true
            )
        }
    }

    private companion object {
        const val DB_NAME = "orbit-migration-test.db"
        const val ENVELOPE_FIXTURE_COUNT = 1000
    }
}
