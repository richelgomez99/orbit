package com.orbit.app.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.orbit.app.RuntimeFlags
import com.orbit.app.ai.local.DeviceAiHardware
import com.orbit.app.ai.local.DownloadableModel
import com.orbit.app.ai.local.LocalModelCatalog
import com.orbit.app.net.ModelDownloadStore
import com.orbit.app.net.ModelDownloadTrigger
import com.orbit.app.ui.theme.OrbitTheme
import kotlinx.coroutines.delay

/**
 * Spec 022 — the model-manager entry point, launched from Settings. Owns the
 * persistent [PrivacyPreferences.localAiEnabled] toggle, kicks off downloads
 * through :net via [ModelDownloadTrigger], and polls [ModelDownloadStore] for
 * live progress. No secrets persist: the Hugging Face token lives only in
 * Compose state for the duration of a download.
 */
class LocalModelsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = PrivacyPreferences(applicationContext)
        // Models this device has the RAM to run — LLMs plus the memory embedder.
        val hardware = DeviceAiHardware.probe(applicationContext)
        val offerable = LocalModelCatalog.offerableFor(hardware) +
            LocalModelCatalog.EMBEDDERS.filter { hardware.totalRamMb >= it.minTotalRamMb }

        setContent {
            OrbitTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    var localAiEnabled by remember { mutableStateOf(prefs.localAiEnabled) }
                    var token by remember { mutableStateOf("") }
                    // Progress snapshot per model id; refreshed by the poller.
                    var progress by remember {
                        mutableStateOf(readAllProgress(offerable))
                    }

                    // Poll while any model is downloading; back off to a slow
                    // heartbeat otherwise so an installed/idle screen is quiet.
                    LaunchedEffect(Unit) {
                        while (true) {
                            progress = readAllProgress(offerable)
                            val active = progress.values.any {
                                it.state == ModelDownloadStore.State.DOWNLOADING
                            }
                            delay(if (active) 700L else 2_500L)
                        }
                    }

                    val rows = offerable.map { model ->
                        LocalModelUiRow(
                            model = model,
                            progress = progress[model.id]
                                ?: ModelDownloadStore.Progress(
                                    model.id, ModelDownloadStore.State.IDLE, 0, 0,
                                ),
                        )
                    }

                    LocalModelsScreen(
                        localAiEnabled = localAiEnabled,
                        onLocalAiChange = { next ->
                            localAiEnabled = next
                            prefs.localAiEnabled = next
                            // Also flip the in-memory flag so :ui consumers in
                            // this process pick it up immediately; other
                            // processes read the persisted pref.
                            RuntimeFlags.useLocalAi = next
                        },
                        rows = rows,
                        token = token,
                        onTokenChange = { token = it },
                        onDownload = { model ->
                            startDownload(model, token)
                            // Optimistic: reflect DOWNLOADING immediately.
                            progress = progress + (
                                model.id to ModelDownloadStore.Progress(
                                    model.id,
                                    ModelDownloadStore.State.DOWNLOADING,
                                    0,
                                    model.approxDownloadBytes,
                                )
                                )
                        },
                        onDelete = { model ->
                            ModelDownloadStore.delete(applicationContext, model.id)
                            progress = progress + (
                                model.id to ModelDownloadStore.Progress(
                                    model.id, ModelDownloadStore.State.IDLE, 0, 0,
                                )
                                )
                        },
                        onNavigateBack = { finish() },
                    )
                }
            }
        }
    }

    private fun startDownload(model: DownloadableModel, token: String) {
        val auth = token.trim().takeIf { it.isNotBlank() }
        ModelDownloadTrigger.start(
            context = applicationContext,
            modelId = model.id,
            url = model.sourceUrl,
            expectedBytes = model.approxDownloadBytes,
            authToken = auth,
        )
        // Embedders ship weights + tokenizer separately — fetch the companion
        // tokenizer.json under a `::tokenizer` id so both land in models/.
        model.tokenizerUrl?.let { tokenizerUrl ->
            ModelDownloadTrigger.start(
                context = applicationContext,
                modelId = "${model.id}${ModelDownloadStore.TOKENIZER_SUFFIX}",
                url = tokenizerUrl,
                expectedBytes = 0L,
                authToken = auth,
            )
        }
    }

    private fun readAllProgress(
        models: List<DownloadableModel>,
    ): Map<String, ModelDownloadStore.Progress> =
        models.associate { it.id to ModelDownloadStore.readProgress(applicationContext, it.id) }
}
