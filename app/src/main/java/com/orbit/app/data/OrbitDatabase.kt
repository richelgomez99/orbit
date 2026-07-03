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
import com.orbit.app.data.dao.GraphDao
import com.orbit.app.data.dao.InvalidationRecordDao
import com.orbit.app.data.dao.IntentEnvelopeDao
import com.orbit.app.data.dao.MemoryCandidateDao
import com.orbit.app.data.dao.MemoryCandidateSupportDao
import com.orbit.app.data.dao.PromotedMemoryDao
import com.orbit.app.data.dao.PromotedMemorySupportDao
import com.orbit.app.data.dao.ResolutionReceiptDao
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
import com.orbit.app.data.entity.GraphEntityEntity
import com.orbit.app.data.entity.GraphFactEntity
import com.orbit.app.data.entity.GraphFeedbackEntity
import com.orbit.app.data.entity.GraphMentionEntity
import com.orbit.app.data.entity.GraphProvenanceEntity
import com.orbit.app.data.entity.GraphRelationshipEntity
import com.orbit.app.data.entity.InvalidationRecordEntity
import com.orbit.app.data.entity.IntentEnvelopeEntity
import com.orbit.app.data.entity.MemoryCandidateEntity
import com.orbit.app.data.entity.MemoryCandidateSupportEntity
import com.orbit.app.data.entity.PromotedMemoryEntity
import com.orbit.app.data.entity.PromotedMemorySupportEntity
import com.orbit.app.data.entity.ResolutionReceiptEntity
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
        ActiveIntentEntity::class,
        // 007 — Memory candidates inspector
        MemoryCandidateEntity::class,
        MemoryCandidateSupportEntity::class,
        PromotedMemoryEntity::class,
        PromotedMemorySupportEntity::class,
        // 009 — Local-first knowledge graph backend POC
        GraphEntityEntity::class,
        GraphMentionEntity::class,
        GraphFactEntity::class,
        GraphRelationshipEntity::class,
        GraphProvenanceEntity::class,
        GraphFeedbackEntity::class,
        // 012 — Durable resolution semantics
        ResolutionReceiptEntity::class
    ],
    version = 12,
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

    // 007 — Memory candidates inspector
    abstract fun memoryCandidateDao(): MemoryCandidateDao
    abstract fun memoryCandidateSupportDao(): MemoryCandidateSupportDao
    abstract fun promotedMemoryDao(): PromotedMemoryDao
    abstract fun promotedMemorySupportDao(): PromotedMemorySupportDao

    // 009 — Local-first knowledge graph backend POC
    abstract fun graphDao(): GraphDao

    // 012 — Resolution semantics
    abstract fun resolutionReceiptDao(): ResolutionReceiptDao

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
                .addMigrations(*ALL_MIGRATIONS)
                .build()
        }
    }
}
