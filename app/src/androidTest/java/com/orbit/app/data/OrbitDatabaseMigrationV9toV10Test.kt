package com.orbit.app.data

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
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class OrbitDatabaseMigrationV9toV10Test {

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
    fun migrate_v9_to_v10_addsLocalKnowledgeGraphSidecars() {
        helper.createDatabase(DB_NAME, 9).close()

        val migrated = helper.runMigrationsAndValidate(
            DB_NAME,
            /* version = */ 10,
            /* validateDroppedTables = */ true,
            MIGRATION_9_10
        )
        migrated.close()

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val room = Room.databaseBuilder(context, OrbitDatabase::class.java, DB_NAME)
            .addMigrations(*ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            val writable = room.openHelper.writableDatabase

            assertIndex(writable, "graph_entity", "index_graph_entity_normalizedName")
            assertIndex(writable, "graph_fact", "index_graph_fact_subjectEntityId")
            assertIndex(writable, "graph_relationship", "index_graph_relationship_fromEntityId")
            assertIndex(writable, "graph_provenance", "index_graph_provenance_targetType_targetId")
            assertIndex(writable, "graph_feedback", "index_graph_feedback_sourceType_sourceId")

            writable.execSQL(
                """
                INSERT INTO graph_entity(
                    id, userId, type, canonicalName, normalizedName, description,
                    status, createdAt, updatedAt, invalidatedAt, invalidatedReason
                ) VALUES(
                    'entity-user', 'debug-user', 'USER', 'You', 'you', NULL,
                    'ACTIVE', 1000, 1000, NULL, NULL
                )
                """.trimIndent()
            )
            writable.execSQL(
                """
                INSERT INTO graph_entity(
                    id, userId, type, canonicalName, normalizedName, description,
                    status, createdAt, updatedAt, invalidatedAt, invalidatedReason
                ) VALUES(
                    'entity-dentist', 'debug-user', 'ORGANIZATION', 'Dentist', 'dentist', NULL,
                    'ACTIVE', 1000, 1000, NULL, NULL
                )
                """.trimIndent()
            )
            writable.execSQL(
                """
                INSERT INTO graph_fact(
                    id, userId, subjectEntityId, predicate, objectText, objectEntityId,
                    confidence, status, createdAt, updatedAt, invalidatedAt, invalidatedReason
                ) VALUES(
                    'fact-1', 'debug-user', 'entity-user', 'needs_to_reschedule',
                    NULL, 'entity-dentist', 0.88, 'ACTIVE', 1100, 1100, NULL, NULL
                )
                """.trimIndent()
            )
            writable.execSQL(
                """
                INSERT INTO graph_provenance(
                    id, userId, targetType, targetId, sourceType, sourceId, supportKind,
                    createdAt, invalidatedAt, invalidatedReason
                ) VALUES(
                    'prov-1', 'debug-user', 'FACT', 'fact-1', 'ENVELOPE', 'env-1',
                    'ASSERTS', 1100, NULL, NULL
                )
                """.trimIndent()
            )
            assertInt(writable, 1, "SELECT COUNT(*) FROM graph_provenance WHERE targetId = 'fact-1'")

            writable.execSQL(
                """
                INSERT INTO graph_relationship(
                    id, userId, fromEntityId, toEntityId, relationshipType,
                    confidence, status, createdAt, updatedAt, invalidatedAt, invalidatedReason
                ) VALUES(
                    'rel-1', 'debug-user', 'entity-user', 'entity-dentist',
                    'HAS_APPOINTMENT_WITH', 0.75, 'ACTIVE', 1200, 1200, NULL, NULL
                )
                """.trimIndent()
            )
            assertInt(writable, 1, "SELECT COUNT(*) FROM graph_relationship WHERE id = 'rel-1'")

            try {
                writable.execSQL(
                    """
                    INSERT INTO graph_fact(
                        id, userId, subjectEntityId, predicate, objectText, objectEntityId,
                        confidence, status, createdAt, updatedAt, invalidatedAt, invalidatedReason
                    ) VALUES(
                        'fact-bad', 'debug-user', 'missing-entity', 'likes', 'bad data',
                        NULL, 0.10, 'ACTIVE', 1300, 1300, NULL, NULL
                    )
                    """.trimIndent()
                )
                fail("Expected graph fact with missing subject entity to fail")
            } catch (_: SQLiteConstraintException) {
                // expected
            }

            writable.execSQL("DELETE FROM graph_entity WHERE id = 'entity-dentist'")
            assertInt(writable, 0, "SELECT COUNT(*) FROM graph_relationship WHERE id = 'rel-1'")
            assertString(writable, null, "SELECT objectEntityId FROM graph_fact WHERE id = 'fact-1'")
        } finally {
            room.close()
        }
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
            assertEquals(expected, cursor.getString(0))
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
        const val DB_NAME = "orbit-migration-v9-v10-test.db"
    }
}
