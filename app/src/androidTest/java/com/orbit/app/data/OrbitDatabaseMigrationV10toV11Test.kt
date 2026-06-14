package com.orbit.app.data

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
class OrbitDatabaseMigrationV10toV11Test {

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
    fun migrate_v10_to_v11_addsResolutionReceipts() {
        helper.createDatabase(DB_NAME, 10).close()

        val migrated = helper.runMigrationsAndValidate(
            DB_NAME,
            /* version = */ 11,
            /* validateDroppedTables = */ true,
            MIGRATION_10_11
        )
        migrated.close()

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val room = Room.databaseBuilder(context, OrbitDatabase::class.java, DB_NAME)
            .addMigrations(MIGRATION_10_11)
            .allowMainThreadQueries()
            .build()
        try {
            val writable = room.openHelper.writableDatabase

            assertIndex(
                writable,
                "resolution_receipt",
                "index_resolution_receipt_targetType_targetId_occurredAtMillis",
            )
            assertIndex(
                writable,
                "resolution_receipt",
                "index_resolution_receipt_envelopeId_occurredAtMillis",
            )
            assertIndex(
                writable,
                "resolution_receipt",
                "index_resolution_receipt_kind_occurredAtMillis",
            )
            assertIndex(
                writable,
                "resolution_receipt",
                "index_resolution_receipt_effectiveUntilMillis",
            )

            writable.execSQL(
                """
                INSERT INTO resolution_receipt(
                    id, targetType, targetId, envelopeId, relatedType, relatedId,
                    kind, actor, reason, occurredAtMillis, effectiveUntilMillis,
                    invalidatesReceiptId, metadataJson
                ) VALUES(
                    'receipt-1', 'ENVELOPE', 'env-1', 'env-1', 'ENVELOPE', 'env-2',
                    'DUPLICATE_RECAPTURE', 'DUPLICATE_DETECTOR', 'EXACT_TEXT',
                    1781000000000, NULL, NULL, '{"matchedBy":"EXACT_TEXT"}'
                )
                """.trimIndent()
            )

            assertInt(writable, 1, "SELECT COUNT(*) FROM resolution_receipt")
            assertString(
                writable,
                "DUPLICATE_RECAPTURE",
                "SELECT kind FROM resolution_receipt WHERE id = 'receipt-1'",
            )
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
        const val DB_NAME = "orbit-migration-v10-v11-test.db"
    }
}
