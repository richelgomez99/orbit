package com.orbit.app.ui.understanding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ActiveIntentScreen(
    viewModel: ActiveIntentViewModel,
    onOpenCapture: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    ActiveIntentContent(
        state = state,
        onOpenCapture = onOpenCapture,
        onResolve = viewModel::resolve,
        onArchive = viewModel::archive,
        onNotInterested = viewModel::notInterested,
        modifier = modifier
    )
}

@Composable
fun ActiveIntentContent(
    state: ActiveIntentUiState,
    onOpenCapture: (String) -> Unit,
    onResolve: (String) -> Unit,
    onArchive: (String) -> Unit,
    onNotInterested: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (state) {
            ActiveIntentUiState.Loading -> CircularProgressIndicator()
            is ActiveIntentUiState.Error -> Text(state.message, style = MaterialTheme.typography.bodyMedium)
            is ActiveIntentUiState.Ready -> {
                if (state.groups.isEmpty()) {
                    EmptyActiveIntentState()
                } else {
                    state.groups.forEach { group ->
                        ActiveIntentGroupSection(
                            group = group,
                            onOpenCapture = onOpenCapture,
                            onResolve = onResolve,
                            onArchive = onArchive,
                            onNotInterested = onNotInterested
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveIntentGroupSection(
    group: ActiveIntentGroupUi,
    onOpenCapture: (String) -> Unit,
    onResolve: (String) -> Unit,
    onArchive: (String) -> Unit,
    onNotInterested: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(group.intentType.toDisplayLabel(), style = MaterialTheme.typography.titleMedium)
        group.items.forEach { item ->
            ActiveIntentCard(
                item = item,
                onOpenCapture = onOpenCapture,
                onResolve = onResolve,
                onArchive = onArchive,
                onNotInterested = onNotInterested
            )
        }
    }
}

@Composable
private fun ActiveIntentCard(
    item: ActiveIntentCardUi,
    onOpenCapture: (String) -> Unit,
    onResolve: (String) -> Unit,
    onArchive: (String) -> Unit,
    onNotInterested: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.HourglassEmpty, contentDescription = null)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onResolve(item.intentId) }) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = "Resolve")
                }
                IconButton(onClick = { onArchive(item.intentId) }) {
                    Icon(Icons.Outlined.Archive, contentDescription = "Archive")
                }
                IconButton(onClick = { onNotInterested(item.intentId) }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Not interested")
                }
            }
            Text(item.primaryAction ?: item.intentType.toDisplayLabel(), style = MaterialTheme.typography.titleSmall)
            item.evidenceSummary?.let { summary ->
                Text(summary, style = MaterialTheme.typography.bodyMedium)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(item.completionStatus) }, leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) })
                item.sourceBasis?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                item.sourceTrust?.let { AssistChip(onClick = {}, label = { Text(it) }) }
            }
            Button(onClick = { onOpenCapture(item.captureId) }) {
                Text("Open capture")
            }
        }
    }
}

@Composable
private fun EmptyActiveIntentState() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Nothing needs you right now", style = MaterialTheme.typography.titleMedium)
        Text("Resolved saves stay searchable.", style = MaterialTheme.typography.bodyMedium)
    }
}

private fun String.toDisplayLabel(): String = lowercase()
    .split('_')
    .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
