package com.orbit.app.diary.ui

import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orbit.app.diary.ActiveIntentUiState
import com.orbit.app.diary.DiaryViewModel
import com.orbit.app.diary.EnvelopeDetailActivity
import com.orbit.app.orbit.AskOrbitViewModel
import com.orbit.app.orbit.ui.AskOrbitPanel
import com.orbit.app.understanding.domain.ResolutionReason
import androidx.compose.ui.platform.LocalContext

@Composable
fun OrbitCleanupScreen(
    viewModel: DiaryViewModel,
    askOrbitViewModel: AskOrbitViewModel? = null,
    onOpenCapture: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.activeIntentState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (askOrbitViewModel != null && onOpenCapture != null) {
            AskOrbitPanel(
                viewModel = askOrbitViewModel,
                onOpenCapture = onOpenCapture,
            )
        }
        if (state is ActiveIntentUiState.Ready) {
            ActiveIntentCleanupPanel(
                state = state,
                onResolve = { item ->
                    viewModel.onResolveActiveIntent(item.intentId, item.defaultResolutionReason)
                },
                onArchive = { item ->
                    viewModel.onResolveActiveIntent(item.intentId, ResolutionReason.USER_ARCHIVED)
                },
                onOpenCapture = { item ->
                    context.startActivity(
                        EnvelopeDetailActivity.newIntent(context, item.captureId, dayLocal = null)
                    )
                },
                onAddContext = { item ->
                    context.startActivity(
                        EnvelopeDetailActivity.newIntent(
                            context,
                            item.captureId,
                            dayLocal = null,
                            startNote = true,
                        )
                    )
                },
                onEscalate = { item ->
                    viewModel.onRequestActiveIntentEscalation(item.intentId)
                    Toast.makeText(context, "Orbit added a decision brief", Toast.LENGTH_SHORT).show()
                },
            )
        } else {
            Text(
                text = when (val s = state) {
                    ActiveIntentUiState.Loading -> "Loading Orbit"
                    ActiveIntentUiState.Empty -> "No active cleanup"
                    is ActiveIntentUiState.Error -> s.message
                    is ActiveIntentUiState.Ready -> ""
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
