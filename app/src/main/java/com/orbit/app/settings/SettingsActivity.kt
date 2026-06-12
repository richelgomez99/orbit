package com.orbit.app.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.orbit.app.BuildConfig
import com.orbit.app.audit.AuditLogActivity
import com.orbit.app.continuation.ContinuationEngine
import com.orbit.app.ui.MainActivity
import com.orbit.app.ui.theme.OrbitTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * T070 — entry point for the Settings screen. Exposed via a deep-link
 * intent from the Diary overflow menu (wired in a future slice); not
 * launcher-visible.
 *
 * Owns the [PrivacyPreferences] state read + write and bridges to
 * [ContinuationEngine] on every flip: pause → `cancelAll`, resume →
 * no-op (enqueues will naturally re-gate on the pref).
 */
class SettingsActivity : ComponentActivity() {

    private lateinit var prefs: PrivacyPreferences
    private lateinit var engine: ContinuationEngine
    private lateinit var trashRepo: BinderTrashRepository

    private var trashCountState: androidx.compose.runtime.MutableState<Int>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = PrivacyPreferences(applicationContext)
        engine = ContinuationEngine.create(applicationContext)
        trashRepo = BinderTrashRepository(applicationContext)

        setContent {
            OrbitTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    var paused by remember { mutableStateOf(prefs.continuationsPaused) }
                    var memoryIndexingEnabled by remember { mutableStateOf(prefs.memoryIndexingEnabled) }
                    var cloudAskSynthesisEnabled by remember { mutableStateOf(prefs.cloudAskSynthesisEnabled) }
                    var cloudAiRoutingEnabled by remember { mutableStateOf(prefs.cloudAiRoutingEnabled) }
                    val count = remember { mutableIntStateOf(0) }
                    trashCountState = count
                    var exportInProgress by remember { mutableStateOf(false) }
                    var exportStatus by remember { mutableStateOf<String?>(null) }
                    var demoSeedStatus by remember { mutableStateOf<String?>(null) }
                    SettingsScreen(
                        paused = paused,
                        onPauseChange = { next ->
                            paused = next
                            prefs.continuationsPaused = next
                            if (next) engine.cancelAll("user_paused")
                        },
                        memoryIndexingEnabled = memoryIndexingEnabled,
                        onMemoryIndexingChange = { next ->
                            memoryIndexingEnabled = next
                            prefs.memoryIndexingEnabled = next
                        },
                        cloudAskSynthesisEnabled = cloudAskSynthesisEnabled,
                        onCloudAskSynthesisChange = { next ->
                            cloudAskSynthesisEnabled = next
                            prefs.cloudAskSynthesisEnabled = next
                        },
                        cloudAiRoutingEnabled = cloudAiRoutingEnabled,
                        onCloudAiRoutingChange = { next ->
                            cloudAiRoutingEnabled = next
                            prefs.cloudAiRoutingEnabled = next
                        },
                        onNavigateBack = { finish() },
                        onOpenCaptureSetup = {
                            startActivity(Intent(this, MainActivity::class.java))
                        },
                        trashCount = count.value,
                        onOpenTrash = {
                            startActivity(Intent(this, TrashActivity::class.java))
                        },
                        onOpenAuditLog = {
                            startActivity(Intent(this, AuditLogActivity::class.java))
                        },
                        onExportData = {
                            exportInProgress = true
                            exportStatus = null
                            lifecycleScope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    ExportService.export(applicationContext)
                                }
                                exportInProgress = false
                                exportStatus = when (result) {
                                    is ExportService.ExportResult.Success ->
                                        "Saved to Downloads/${result.folderName}"
                                    is ExportService.ExportResult.Failure ->
                                        "Export failed: ${result.message}"
                                }
                            }
                        },
                        exportInProgress = exportInProgress,
                        exportStatus = exportStatus,
                        onSeedDemoMemories = if (BuildConfig.DEBUG) {
                            {
                                memoryIndexingEnabled = true
                                prefs.memoryIndexingEnabled = true
                                demoSeedStatus = "Seed requested. Wait 20-30 seconds, then search Library."
                                sendBroadcast(
                                    Intent(ACTION_SEED_DEMO_MEMORY).setPackage(packageName)
                                )
                            }
                        } else {
                            null
                        },
                        demoSeedStatus = demoSeedStatus,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshTrashCount()
    }

    override fun onDestroy() {
        if (::trashRepo.isInitialized) trashRepo.disconnect()
        super.onDestroy()
    }

    private fun refreshTrashCount() {
        lifecycleScope.launch {
            val n = runCatching {
                withContext(Dispatchers.IO) {
                    trashRepo.connect().countSoftDeletedWithinDays(30)
                }
            }.getOrDefault(0)
            trashCountState?.value = n
        }
    }

    private companion object {
        const val ACTION_SEED_DEMO_MEMORY = "com.orbit.app.debug.SEED_DEMO_MEMORY"
    }
}
