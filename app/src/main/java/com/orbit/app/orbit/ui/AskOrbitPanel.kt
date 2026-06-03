package com.orbit.app.orbit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orbit.app.memory.AskOrbitAnswer
import com.orbit.app.memory.AskOrbitCitation
import com.orbit.app.memory.MemorySearchResult
import com.orbit.app.orbit.AskOrbitViewModel

object AskOrbitPanelTestTags {
    const val QUESTION = "ask-orbit-question"
    const val SUBMIT = "ask-orbit-submit"
    const val ANSWER = "ask-orbit-answer"
    const val REFUSAL = "ask-orbit-refusal"
    fun citation(envelopeId: String) = "ask-orbit-citation-$envelopeId"
}

@Composable
fun AskOrbitPanel(
    viewModel: AskOrbitViewModel,
    onOpenCapture: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.openEnvelopeId) {
        val envelopeId = state.openEnvelopeId ?: return@LaunchedEffect
        onOpenCapture(envelopeId)
        viewModel.onOpenCaptureHandled()
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Ask Orbit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.question,
                    onValueChange = viewModel::onQuestionChanged,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag(AskOrbitPanelTestTags.QUESTION),
                    singleLine = true,
                    placeholder = { Text("Ask saved memory") },
                    shape = RoundedCornerShape(8.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = quietSearchFieldColors(),
                )
                Button(
                    onClick = viewModel::onAskSubmitted,
                    enabled = state.canAsk,
                    modifier = Modifier
                        .height(52.dp)
                        .testTag(AskOrbitPanelTestTags.SUBMIT),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = ButtonDefaults.ContentPadding,
                    colors = quietSearchButtonColors(),
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "Ask")
                }
            }
            when {
                state.loading -> CircularProgressIndicator()
                state.error != null -> StatusText(state.error)
                state.answer != null -> AskAnswerView(
                    answer = state.answer!!,
                    onOpenCapture = viewModel::onOpenCapture,
                )
            }
        }
    }
}

@Composable
private fun quietSearchFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    cursorColor = MaterialTheme.colorScheme.onSurface,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
private fun quietSearchButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
)

@Composable
private fun AskAnswerView(
    answer: AskOrbitAnswer,
    onOpenCapture: (String) -> Unit,
) {
    val hasCitations = answer.citations.isNotEmpty()
    Column(
        modifier = Modifier.testTag(
            if (hasCitations) AskOrbitPanelTestTags.ANSWER else AskOrbitPanelTestTags.REFUSAL
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (hasCitations) {
            Text(
                text = "Found ${answer.citations.size.coerceAtMost(3)} related capture${if (answer.citations.size == 1) "" else "s"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            answer.citations.take(3).forEach { citation ->
                CitationRow(citation = citation, onOpenCapture = onOpenCapture)
            }
        } else {
            Text(
                text = answer.answer,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (answer.candidates.isNotEmpty()) {
                Text(
                    text = "Closest saved matches",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                answer.candidates.take(3).forEach { candidate ->
                    CandidateRow(candidate = candidate, onOpenCapture = onOpenCapture)
                }
            }
        }
    }
}

@Composable
private fun CitationRow(
    citation: AskOrbitCitation,
    onOpenCapture: (String) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenCapture(citation.envelopeId) }
            .testTag(AskOrbitPanelTestTags.citation(citation.envelopeId)),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = citation.excerpt?.takeIf { it.isNotBlank() }
                        ?: citation.title
                        ?: "Saved memory",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(citation.dayLocal, citation.sourceAppLabel)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = { onOpenCapture(citation.envelopeId) }) {
                Text("Open")
            }
        }
    }
}

@Composable
private fun CandidateRow(
    candidate: MemorySearchResult,
    onOpenCapture: (String) -> Unit,
) {
    Text(
        text = candidate.title ?: candidate.summary ?: "Saved memory",
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenCapture(candidate.envelopeId) }
            .padding(vertical = 4.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun StatusText(text: String?) {
    if (text.isNullOrBlank()) return
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
