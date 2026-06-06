package com.orbit.app.diary.ui

import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orbit.app.data.ipc.ActionDraftParcel
import com.orbit.app.diary.ActionPreviewSheet
import com.orbit.app.diary.ActiveIntentUiState
import com.orbit.app.diary.DiaryViewModel
import com.orbit.app.diary.EnvelopeDetailActivity
import com.orbit.app.memory.SourceAppLabelDisplay
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
    val actionDrafts by viewModel.observeActionDrafts().collectAsState(initial = emptyList())
    val actionNotice by viewModel.actionNotice.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var pendingActionDraft by remember { mutableStateOf<ActionDraftParcel?>(null) }
    LaunchedEffect(actionNotice?.id) {
        val notice = actionNotice ?: return@LaunchedEffect
        Toast.makeText(context, notice.message, Toast.LENGTH_LONG).show()
    }
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
        actionNotice?.let { notice ->
            ActionNoticePanel(
                message = notice.message,
                onDismiss = { viewModel.onActionNoticeDismissed() },
            )
        }
        ActionDraftsPanel(
            drafts = actionDrafts,
            onReview = { pendingActionDraft = it },
            onDismiss = { viewModel.onDismissProposal(it.proposalId) },
            onOpenCapture = { draft ->
                onOpenCapture?.invoke(draft.sourceEnvelopeId)
                    ?: context.startActivity(
                        EnvelopeDetailActivity.newIntent(
                            context,
                            draft.sourceEnvelopeId,
                            dayLocal = null,
                        )
                    )
            },
        )
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

    pendingActionDraft?.let { draft ->
        val proposal = draft.toProposalParcel()
        ActionPreviewSheet(
            proposal = proposal,
            onConfirm = { editedArgsJson ->
                viewModel.onConfirmProposal(proposal, editedArgsJson)
                pendingActionDraft = null
            },
            onDismiss = {
                viewModel.onDismissProposal(draft.proposalId)
                pendingActionDraft = null
            },
        )
    }
}

@Composable
private fun ActionNoticePanel(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
private fun ActionDraftsPanel(
    drafts: List<ActionDraftParcel>,
    onReview: (ActionDraftParcel) -> Unit,
    onDismiss: (ActionDraftParcel) -> Unit,
    onOpenCapture: (ActionDraftParcel) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (drafts.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Action drafts",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        drafts.forEach { draft ->
            ActionDraftCard(
                draft = draft,
                onReview = { onReview(draft) },
                onDismiss = { onDismiss(draft) },
                onOpenCapture = { onOpenCapture(draft) },
            )
        }
    }
}

@Composable
private fun ActionDraftCard(
    draft: ActionDraftParcel,
    onReview: () -> Unit,
    onDismiss: () -> Unit,
    onOpenCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sourceApp = SourceAppLabelDisplay.userFacingOrNull(draft.sourceAppLabel)
    val sourceLine = listOfNotNull(sourceApp?.let { "from $it" }, draft.sourceDayLocal)
        .joinToString(" • ")
        .ifBlank { "Saved capture" }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = draft.previewTitle,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            draft.previewSubtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = "${draft.displayName} - $sourceLine",
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = draft.sourceTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onOpenCapture) { Text("Open") }
                TextButton(onClick = onDismiss) { Text("Dismiss") }
                Button(onClick = onReview) { Text("Review") }
            }
        }
    }
}
