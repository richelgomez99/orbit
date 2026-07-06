package com.orbit.app.data

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

/**
 * Verifies MIGRATION_12_13 drops the legacy partial-unique index
 * `index_digest_unique_per_day` (created by MIGRATION_1_2, undeclarable
 * via @Entity — same disease as the v12 active_fact_key fix). Seeds a
 * v12 DB carrying the index (as any v1-upgraded device would), runs the
 * migration, and proves the drop plus a clean full Room reopen.
 */
@RunWith(AndroidJUnit4::class)
class OrbitDatabaseMigrationV12toV13Test {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        OrbitDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
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
    fun migrate_v12_to_v13_dropsLegacyDigestIndex() {
        helper.createDatabase(DB_NAME, 12).use { db ->
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_digest_unique_per_day
                ON intent_envelope(day_local)
                WHERE kind = 'DIGEST'
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(
            DB_NAME,
            /* version = */ 13,
            /* validateDroppedTables = */ true,
            MIGRATION_12_13,
        ).close()

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val room = Room.databaseBuilder(context, OrbitDatabase::class.java, DB_NAME)
            .addMigrations(*ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            val cursor = room.openHelper.readableDatabase.query(
                """
                SELECT COUNT(*) FROM sqlite_master
                WHERE type = 'index'
                  AND name = 'index_digest_unique_per_day'
                """.trimIndent(),
            )
            cursor.use {
                assertEquals(true, it.moveToFirst())
                assertEquals(0, it.getInt(0))
            }
        } finally {
            room.close()
        }
    }

    private companion object {
        const val DB_NAME = "orbit-migration-v12-v13-test.db"
    }
}
