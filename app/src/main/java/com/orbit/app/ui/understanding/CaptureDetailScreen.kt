package com.orbit.app.ui.understanding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CaptureDetailScreen(
    viewModel: CaptureDetailViewModel,
    onGetMoreContext: () -> Unit,
    onLinkDuplicateEvidence: (String) -> Unit,
    onKeepDuplicateSeparate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    CaptureDetailContent(
        state = state,
        onGetMoreContext = onGetMoreContext,
        onLinkDuplicateEvidence = onLinkDuplicateEvidence,
        onKeepDuplicateSeparate = onKeepDuplicateSeparate,
        modifier = modifier
    )
}

@Composable
fun CaptureDetailContent(
    state: CaptureDetailUiState,
    onGetMoreContext: () -> Unit,
    onLinkDuplicateEvidence: (String) -> Unit,
    onKeepDuplicateSeparate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (state) {
            CaptureDetailUiState.Loading -> CircularProgressIndicator()
            is CaptureDetailUiState.Error -> Text(state.message, style = MaterialTheme.typography.bodyMedium)
            is CaptureDetailUiState.Ready -> {
                SourceIdentityChip(state.sourceIdentity)
                if (state.activeIntents.isNotEmpty()) {
                    ActiveIntentSummaryCard(state.activeIntents)
                }
                EvidenceTypeSummaryRow(state.evidenceTypes)
                CompactSummaryCard(
                    title = state.title,
                    summaryText = state.summaryText,
                    mode = state.mode
                )
                state.duplicateWarning?.let { warning ->
                    DuplicateWarningBanner(
                        warning = warning,
                        onLinkDuplicateEvidence = onLinkDuplicateEvidence,
                        onKeepDuplicateSeparate = onKeepDuplicateSeparate
                    )
                }
                if (state.status == "LIMITED" || state.groundingConstraints.isNotEmpty()) {
                    GroundingConstraintBanner(state.groundingConstraints)
                }
                Button(onClick = onGetMoreContext) {
                    Text("Get More Context")
                }
            }
        }
    }
}

@Composable
private fun SourceIdentityChip(sourceIdentity: SourceIdentityUi) {
    val label = sourceIdentity.provider ?: sourceIdentity.appLabel ?: sourceIdentity.category
    AssistChip(
        onClick = {},
        label = {
            Text(
                listOfNotNull(label, sourceIdentity.evidenceBasis, sourceIdentity.trustLevel)
                    .joinToString(" · ")
            )
        },
        leadingIcon = { Icon(Icons.Outlined.Source, contentDescription = null) }
    )
}

@Composable
private fun ActiveIntentSummaryCard(activeIntents: List<CaptureActiveIntentUi>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.TaskAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Active Intent", style = MaterialTheme.typography.titleSmall)
            }
            activeIntents.forEach { activeIntent ->
                Text(
                    activeIntent.primaryAction ?: activeIntent.intentType,
                    style = MaterialTheme.typography.bodyMedium
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(activeIntent.status) })
                    AssistChip(onClick = {}, label = { Text(activeIntent.completionStatus) })
                }
                activeIntent.evidenceSummary?.let { summary ->
                    Text(summary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun EvidenceTypeSummaryRow(evidenceTypes: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val badges = evidenceTypes.ifEmpty { listOf("BASIC") }
        badges.forEach { evidenceType ->
            AssistChip(
                onClick = {},
                label = { Text(evidenceType) },
                leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun CompactSummaryCard(title: String?, summaryText: String?, mode: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title ?: "Untitled capture", style = MaterialTheme.typography.titleMedium)
            summaryText?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            Text(mode, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun GroundingConstraintBanner(constraints: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            constraints.ifEmpty { listOf("limited-evidence") }.forEach { constraint ->
                Text(constraint, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DuplicateWarningBanner(
    warning: DuplicateWarningUi,
    onLinkDuplicateEvidence: (String) -> Unit,
    onKeepDuplicateSeparate: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Link, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Duplicate evidence: ${warning.matchType}", style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onLinkDuplicateEvidence(warning.existingCaptureId) }) {
                    Text("Link evidence")
                }
                OutlinedButton(onClick = onKeepDuplicateSeparate) {
                    Text("Keep separate")
                }
            }
        }
    }
    Spacer(Modifier.height(2.dp))
}
