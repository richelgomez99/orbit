package com.orbit.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.orbit.app.data.dao.ActionExecutionDao
import com.orbit.app.data.dao.ActionProposalDao
import com.orbit.app.data.dao.ActiveIntentDao
import com.orbit.app.data.dao.AppFunctionSkillDao
import com.orbit.app.data.dao.AuditLogDao
import com.orbit.app.data.dao.CaptureUnderstandingDao
import com.orbit.app.data.dao.ClusterDao
import com.orbit.app.data.dao.ContinuationDao
import com.orbit.app.data.dao.ContinuationResultDao
import com.orbit.app.data.dao.EnvelopeNoteDao
import com.orbit.app.data.dao.EvidenceBundleDao
import com.orbit.app.data.dao.InvalidationRecordDao
import com.orbit.app.data.dao.IntentEnvelopeDao
import com.orbit.app.data.dao.SkillUsageDao
import com.orbit.app.data.entity.ActionExecutionEntity
import com.orbit.app.data.entity.ActionProposalEntity
import com.orbit.app.data.entity.ActiveIntentEntity
import com.orbit.app.data.entity.AppFunctionSkillEntity
import com.orbit.app.data.entity.AuditLogEntryEntity
import com.orbit.app.data.entity.CaptureUnderstandingEntity
import com.orbit.app.data.entity.ClusterEntity
import com.orbit.app.data.entity.ClusterMemberEntity
import com.orbit.app.data.entity.ContinuationEntity
import com.orbit.app.data.entity.ContinuationResultEntity
import com.orbit.app.data.entity.EnvelopeNoteEntity
import com.orbit.app.data.entity.EvidenceBundleEntity
import com.orbit.app.data.entity.InvalidationRecordEntity
import com.orbit.app.data.entity.IntentEnvelopeEntity
import com.orbit.app.data.entity.SkillUsageEntity
import com.orbit.app.data.security.KeystoreKeyProvider
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        IntentEnvelopeEntity::class,
        ContinuationEntity::class,
        ContinuationResultEntity::class,
        AuditLogEntryEntity::class,
        // 003 v1.1 — Orbit Actions
        ActionProposalEntity::class,
        ActionExecutionEntity::class,
        AppFunctionSkillEntity::class,
        SkillUsageEntity::class,
        // 002 amendment Phase 11 — Cluster Engine
        ClusterEntity::class,
        ClusterMemberEntity::class,
        EnvelopeNoteEntity::class,
        // 004 — Screenshot Cleanup + Active Intent sidecars
        CaptureUnderstandingEntity::class,
        EvidenceBundleEntity::class,
        InvalidationRecordEntity::class,
        ActiveIntentEntity::class
    ],
    version = 8,
    exportSchema = true
)
abstract class OrbitDatabase : RoomDatabase() {

    abstract fun intentEnvelopeDao(): IntentEnvelopeDao
    abstract fun continuationDao(): ContinuationDao
    abstract fun continuationResultDao(): ContinuationResultDao
    abstract fun envelopeNoteDao(): EnvelopeNoteDao
    abstract fun auditLogDao(): AuditLogDao

    // 003 v1.1
    abstract fun actionProposalDao(): ActionProposalDao
    abstract fun actionExecutionDao(): ActionExecutionDao
    abstract fun appFunctionSkillDao(): AppFunctionSkillDao
    abstract fun skillUsageDao(): SkillUsageDao

    // 002 amendment Phase 11
    abstract fun clusterDao(): ClusterDao

    // 004 — Screenshot Cleanup + Active Intent
    abstract fun captureUnderstandingDao(): CaptureUnderstandingDao
    abstract fun evidenceBundleDao(): EvidenceBundleDao
    abstract fun invalidationRecordDao(): InvalidationRecordDao
    abstract fun activeIntentDao(): ActiveIntentDao

    companion object {
        private const val DB_NAME = "orbit.db"

        @Volatile
        private var INSTANCE: OrbitDatabase? = null

        /**
         * Opens the encrypted Room database. Must only be called from the :ml process.
         */
        fun getInstance(context: Context): OrbitDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        /**
         * Test-only seam — instrumented tests may install an in-memory
         * [OrbitDatabase] so they can exercise workers without touching
         * the on-disk SQLCipher file. Pass `null` to clear the override.
         *
         * Intentionally not annotated `@VisibleForTesting` because the
         * `androidx.annotation` dep isn't on the main classpath here;
         * the convention is enforced by review.
         */
        fun overrideInstanceForTest(db: OrbitDatabase?) {
            synchronized(this) { INSTANCE = db }
        }

        private fun buildDatabase(context: Context): OrbitDatabase {
            System.loadLibrary("sqlcipher")
            val passphrase = KeystoreKeyProvider.getOrCreatePassphrase(context)
            val factory = SupportOpenHelperFactory(passphrase)

            return Room.databaseBuilder(
                context.applicationContext,
                OrbitDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(factory)
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8
                )
                .build()
        }
    }
}
