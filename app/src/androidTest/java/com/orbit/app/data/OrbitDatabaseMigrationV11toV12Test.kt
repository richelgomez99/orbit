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
 * Verifies MIGRATION_11_12 drops the orphan `index_memory_candidate_active_fact_key`
 * partial-unique index that MIGRATION_8_9 created but the
 * [com.orbit.app.data.entity.MemoryCandidateEntity] annotation never declared.
 * Room 2.7+ validates schema post-migration in production; the mismatch used to
 * crash the `:ml` process on launch on any device that had a pre-v11 database.
 *
 * Also opens Room via `.databaseBuilder(...)` after the migration to prove
 * the schema validator is satisfied end-to-end.
 */
@RunWith(AndroidJUnit4::class)
class OrbitDatabaseMigrationV11toV12Test {

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
    fun migrate_v11_to_v12_dropsOrphanActiveFactKeyIndex() {
        // Seed a v11 DB that reproduces the mismatch: MIGRATION_8_9 would
        // create the offending partial-unique index, and MIGRATION_9_10 /
        // 10_11 do not drop it, so a real v11 DB in the wild carries it.
        helper.createDatabase(DB_NAME, 11).use { db ->
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_memory_candidate_active_fact_key
                ON memory_candidate(candidateKind, subject, predicate, objectValue)
                WHERE state IN ('PENDING', 'ASKED')
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(
            DB_NAME,
            /* version = */ 12,
            /* validateDroppedTables = */ true,
            MIGRATION_11_12,
        ).close()

        // Verify: post-migration, the orphan index no longer exists AND
        // Room's schema validator opens the DB cleanly (this is the
        // regression test for the :ml launch crash).
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val room = Room.databaseBuilder(context, OrbitDatabase::class.java, DB_NAME)
            .addMigrations(*ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            val readable = room.openHelper.readableDatabase
            val cursor = readable.query(
                """
                SELECT COUNT(*) FROM sqlite_master
                WHERE type = 'index'
                  AND name = 'index_memory_candidate_active_fact_key'
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
        const val DB_NAME = "orbit-migration-v11-v12-test.db"
    }
}
