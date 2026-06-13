package com.orbit.app.data.ipc

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.orbit.app.audit.AuditLogImpl
import com.orbit.app.audit.AuditLogWriter
import com.orbit.app.ai.LlmProviderRouter
import com.orbit.app.ai.extract.ActionExtractor
import com.orbit.app.continuation.ContinuationEngine
import com.orbit.app.data.ActionsRepositoryDelegate
import com.orbit.app.data.ActiveIntentRepository
import com.orbit.app.data.AppFunctionRegistry
import com.orbit.app.data.ClusterRepository
import com.orbit.app.data.ClusterSummarizeDelegate
import com.orbit.app.data.EnvelopeRepositoryImpl
import com.orbit.app.data.LocalRoomBackend
import com.orbit.app.data.MemoryRepositoryDelegate
import com.orbit.app.data.OrbitDatabase
import com.orbit.app.data.WeeklyDigestDelegate
import com.orbit.app.ai.ClusterSummariser
import com.orbit.app.ai.DigestComposer
import com.orbit.app.graph.GraphRepositoryDelegate
import com.orbit.app.graph.RoomGraphBackendAdapter
import com.orbit.app.memory.MemoryIndexSyncScheduler
import com.orbit.app.memory.MemoryIndexSyncDelegate
import com.orbit.app.understanding.BasicUnderstandingWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Bound service running in :ml process. Owns the encrypted Room database
 * and exposes the IEnvelopeRepository AIDL surface.
 *
 * Callers: :capture (seal path), :ui (read + mutate paths).
 *
 * The backend is selected via [com.orbit.app.data.EnvelopeStorageBackend]
 * so v1.1 cloud / v1.3 BYOC can be added without modifying this service.
 */
class EnvelopeRepositoryService : Service() {

    private val serviceJob: Job = SupervisorJob()
    private val serviceScope: CoroutineScope = CoroutineScope(serviceJob)

    private lateinit var repository: EnvelopeRepositoryImpl
    private lateinit var auditLog: AuditLogImpl

    override fun onCreate() {
        super.onCreate()
        val db = OrbitDatabase.getInstance(applicationContext)
        val backend = LocalRoomBackend(db)
        val auditWriter = AuditLogWriter()
        // T068 — engine wired here so every `seal()` with URLs fires a
        // WorkManager job once the Room transaction commits. Tests that
        // construct `EnvelopeRepositoryImpl` directly pass `null` for the
        // engine and stay oblivious to WorkManager.
        val engine = ContinuationEngine.create(applicationContext)
        val memoryIndexSyncScheduler = MemoryIndexSyncScheduler.create(applicationContext)
        val memoryIndexSyncDelegate = MemoryIndexSyncDelegate(
            context = applicationContext,
            database = db,
        )
        // T025/T027 — actions registry + delegate live in :ml so binder
        // calls from :ui flow through here. Registry is constructed eagerly
        // but boot-time `registerAll(BUILT_IN)` is owned by Application.
        val registry = AppFunctionRegistry(
            database = db,
            skillDao = db.appFunctionSkillDao(),
            usageDao = db.skillUsageDao(),
            auditLogDao = db.auditLogDao(),
            auditWriter = auditWriter
        )
        val actionsDelegate = ActionsRepositoryDelegate(
            database = db,
            registry = registry,
            auditWriter = auditWriter,
            scope = serviceScope
        )
        val graphBackendAdapter = RoomGraphBackendAdapter(db)
        val memoryRepositoryDelegate = MemoryRepositoryDelegate(
            database = db,
            auditWriter = auditWriter,
            graphRepositoryDelegate = GraphRepositoryDelegate(
                adapter = graphBackendAdapter,
                promotedMemorySupportDao = db.promotedMemorySupportDao()
            ),
            scope = serviceScope
        )
        // T043/T044 — extractor lives in :ml and is invoked via
        // [IEnvelopeRepository.extractActionsForEnvelope] from
        // [com.orbit.app.ai.extract.ActionExtractionWorker].
        val actionExtractor = ActionExtractor(
            database = db,
            envelopeDao = db.intentEnvelopeDao(),
            proposalDao = db.actionProposalDao(),
            auditLogDao = db.auditLogDao(),
            registry = registry,
            llmProvider = LlmProviderRouter.createPreferLocal(applicationContext),
            auditWriter = auditWriter
        )
        // T072/T074 — weekly digest delegate. Composer reuses the same
        // NanoLlmProvider instance, taking advantage of the locale-gated
        // structured fallback when AICore isn't available. Backend
        // pointer is shared so the partial unique index check happens
        // inside the same Room writer.
        val weeklyDigestDelegate = WeeklyDigestDelegate(
            database = db,
            backend = backend,
            composer = DigestComposer(LlmProviderRouter.createPreferLocal(applicationContext)),
            auditWriter = auditWriter
        )
        // Spec 002 Phase 11 Block 13 (T163) — cluster.summarize delegate.
        // Constructed eagerly so `summarizeCluster` over the binder
        // resolves without a second lookup. Reuses the local-preferring
        // LlmProvider so the same cloud-fallback semantics apply as
        // weekly digest + per-envelope summaries.
        val clusterRepo = ClusterRepository(
            clusterDao = db.clusterDao(),
            auditLogDao = db.auditLogDao(),
            auditWriter = auditWriter
        )
        val clusterSummarizeDelegate = ClusterSummarizeDelegate(
            database = db,
            backend = backend,
            clusterRepository = clusterRepo,
            summariser = ClusterSummariser(
                llmProvider = LlmProviderRouter.createPreferLocal(applicationContext)
            ),
            auditWriter = auditWriter
        )
        val activeIntentRepository = ActiveIntentRepository(
            activeIntentDao = db.activeIntentDao(),
            auditLogDao = db.auditLogDao(),
            scope = serviceScope
        )
        val basicUnderstandingWriter = BasicUnderstandingWriter(
            captureUnderstandingDao = db.captureUnderstandingDao(),
            evidenceBundleDao = db.evidenceBundleDao(),
            activeIntentDao = db.activeIntentDao(),
            auditLogDao = db.auditLogDao(),
            auditWriter = auditWriter
        )
        serviceScope.launch(Dispatchers.IO) {
            basicUnderstandingWriter.refreshActiveFromSidecars()
        }
        repository = EnvelopeRepositoryImpl(
            backend = backend,
            auditWriter = auditWriter,
            scope = serviceScope,
            continuationEngine = engine,
            actionsDelegate = actionsDelegate,
            actionExtractor = actionExtractor,
            weeklyDigestDelegate = weeklyDigestDelegate,
            // Spec 002 Phase 11 Block 5 / T135 — cluster read surface.
            // Block 10 (T148 review FU#2): repository now owns dismiss
            // writes too, so wire the audit log + clock for CLUSTER_DISMISSED rows.
            clusterRepository = clusterRepo,
            clusterSummarizeDelegate = clusterSummarizeDelegate,
            activeIntentRepository = activeIntentRepository,
            basicUnderstandingWriter = basicUnderstandingWriter,
            memoryIndexSyncScheduler = memoryIndexSyncScheduler,
            memoryIndexSyncDelegate = memoryIndexSyncDelegate,
            memoryRepositoryDelegate = memoryRepositoryDelegate,
            graphBackendAdapter = graphBackendAdapter
        )
        // T088 — same service binder pool exposes the audit-log surface on a
        // distinct intent action so the Settings / audit viewer process can
        // read audit rows without a second service class.
        auditLog = AuditLogImpl(auditLogDao = db.auditLogDao())
    }

    override fun onBind(intent: Intent?): IBinder {
        // T088 — dispatch on intent action. Default (null action or the
        // envelope-repository action) returns the repository binder for
        // backwards compatibility with all existing callers.
        return when (intent?.action) {
            ACTION_BIND_AUDIT_LOG -> auditLog
            else -> repository
        }
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    companion object {
        /** T088 — intent action for binding to the audit-log surface. */
        const val ACTION_BIND_AUDIT_LOG = "com.orbit.app.action.BIND_AUDIT_LOG"
    }
}
