package com.orbit.app.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbit.app.ai.local.DownloadableModel
import com.orbit.app.net.ModelDownloadStore
import com.orbit.app.ui.primitives.MonoLabel
import com.orbit.app.ui.primitives.QuietBackButton
import com.orbit.app.ui.tokens.OrbitType

/** One catalog model plus its live install/download state. */
data class LocalModelUiRow(
    val model: DownloadableModel,
    val progress: ModelDownloadStore.Progress,
)

/**
 * Spec 022 — the model-manager surface. Lets the user turn on on-device AI,
 * download a Gemma bundle (through :net, with an optional Hugging Face token
 * for the license gate), watch progress, and delete installed weights.
 */
@Composable
fun LocalModelsScreen(
    localAiEnabled: Boolean,
    onLocalAiChange: (Boolean) -> Unit,
    rows: List<LocalModelUiRow>,
    token: String,
    onTokenChange: (String) -> Unit,
    onDownload: (DownloadableModel) -> Unit,
    onDelete: (DownloadableModel) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val anyInstalled = rows.any { it.progress.state == ModelDownloadStore.State.COMPLETE }
    val needsToken = rows.any { it.progress.state != ModelDownloadStore.State.COMPLETE }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(QuietSettingsColors.BgDeep)
            .statusBarsPadding(),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 18.dp, top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            QuietBackButton(onClick = onNavigateBack, color = QuietSettingsColors.Cream)
            Text(
                text = "On-device AI",
                color = QuietSettingsColors.Cream,
                style = TextStyle(
                    fontFamily = OrbitType.QuietAlmanac.bodySans,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
        QuietRule()

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
        ) {
            // Hero
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                MonoLabel(
                    text = "// Principle IX · LLM Sovereignty",
                    color = QuietSettingsColors.CreamDim,
                    size = 9.5.sp,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = buildAnnotatedString {
                        append("Run Orbit's intelligence ")
                        withStyle(
                            SpanStyle(
                                color = QuietSettingsColors.Accent,
                                fontStyle = FontStyle.Italic,
                            ),
                        ) { append("on your phone") }
                        append(". Download a model once; summaries and headers then work offline, with nothing leaving the device.")
                    },
                    color = QuietSettingsColors.Cream,
                    style = TextStyle(
                        fontFamily = OrbitType.QuietAlmanac.displaySerif,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                )
            }

            QuietSettingSection(label = "Routing") {
                QuietToggleRow(
                    title = "Prefer on-device AI",
                    description = if (anyInstalled) {
                        "Summaries and day headers run on your downloaded model instead of the cloud."
                    } else {
                        "Download a model below first — then this routes inference on-device."
                    },
                    tag = if (localAiEnabled) "ON" else "OFF",
                    checked = localAiEnabled,
                    testTag = LocalModelsTestTags.LOCAL_AI_TOGGLE,
                    onCheckedChange = onLocalAiChange,
                )
            }

            QuietSettingSection(label = "Models") {
                rows.forEach { row ->
                    ModelRow(
                        row = row,
                        onDownload = { onDownload(row.model) },
                        onDelete = { onDelete(row.model) },
                    )
                }
            }

            if (needsToken) {
                QuietSettingSection(label = "Access") {
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)) {
                        QuietRowDescription(
                            "Gemma weights are license-gated. Accept the license on Hugging Face, " +
                                "then paste a Read token here to download. The token stays in memory " +
                                "for this download only — it is never saved.",
                        )
                        Spacer(Modifier.height(12.dp))
                        TokenField(value = token, onValueChange = onTokenChange)
                    }
                    QuietRule()
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 24.dp, end = 24.dp),
            ) {
                MonoLabel(
                    text = "weights stored in app-private storage · mmap-loaded in :ml · no network at inference",
                    color = QuietSettingsColors.CreamFaint,
                    size = 8.5.sp,
                )
            }
        }
    }
}

@Composable
private fun ModelRow(
    row: LocalModelUiRow,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    val state = row.progress.state
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    QuietRowTitle(row.model.displayName)
                    when (state) {
                        ModelDownloadStore.State.COMPLETE -> QuietTag("INSTALLED")
                        ModelDownloadStore.State.DOWNLOADING -> QuietTag("DOWNLOADING")
                        ModelDownloadStore.State.FAILED -> QuietTag("FAILED")
                        ModelDownloadStore.State.IDLE -> QuietTag(row.model.approxDownloadLabel)
                    }
                }
                Spacer(Modifier.height(4.dp))
                QuietRowDescription(row.model.blurb)
            }
            // Trailing affordance
            when (state) {
                ModelDownloadStore.State.COMPLETE ->
                    ActionText("Delete", QuietSettingsColors.Red, onDelete)
                ModelDownloadStore.State.DOWNLOADING -> Unit
                ModelDownloadStore.State.IDLE, ModelDownloadStore.State.FAILED ->
                    ActionText("Download", QuietSettingsColors.Accent, onDownload)
            }
        }

        if (state == ModelDownloadStore.State.DOWNLOADING) {
            Spacer(Modifier.height(12.dp))
            ProgressBar(fraction = row.progress.fraction)
            Spacer(Modifier.height(6.dp))
            val mb = 1024L * 1024L
            MonoLabel(
                text = "${row.progress.bytesWritten / mb} / ${row.progress.totalBytes / mb} MB" +
                    "  ·  ${(row.progress.fraction * 100).toInt()}%",
                color = QuietSettingsColors.CreamDim,
                size = 9.sp,
            )
        }
        if (state == ModelDownloadStore.State.FAILED && row.progress.error != null) {
            Spacer(Modifier.height(6.dp))
            MonoLabel(
                text = "error: ${row.progress.error}",
                color = QuietSettingsColors.Red,
                size = 9.sp,
            )
        }
    }
    QuietRule()
}

@Composable
private fun ActionText(label: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        color = color,
        style = TextStyle(
            fontFamily = OrbitType.QuietAlmanac.displaySerif,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            fontStyle = FontStyle.Italic,
        ),
    )
}

@Composable
private fun ProgressBar(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(QuietSettingsColors.ToggleOff),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(QuietSettingsColors.Accent),
        )
    }
}

@Composable
private fun TokenField(value: String, onValueChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(QuietSettingsColors.ToggleOff)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = OrbitType.QuietAlmanac.captionMono,
                fontSize = 12.sp,
                color = QuietSettingsColors.Cream,
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(QuietSettingsColors.Accent),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = ImeAction.Done,
            ),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = "hf_…",
                        color = QuietSettingsColors.CreamFaint,
                        style = TextStyle(
                            fontFamily = OrbitType.QuietAlmanac.captionMono,
                            fontSize = 12.sp,
                        ),
                    )
                }
                inner()
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(LocalModelsTestTags.TOKEN_FIELD),
        )
    }
}

object LocalModelsTestTags {
    const val LOCAL_AI_TOGGLE = "local-models-ai-toggle"
    const val TOKEN_FIELD = "local-models-token-field"
}
