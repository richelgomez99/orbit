package com.orbit.app.diary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.orbit.app.diary.ManualComposeResult
import com.orbit.app.diary.ManualComposeViewModel

object ManualComposeTestTags {
    const val DIALOG = "manual-compose-dialog"
    const val BODY = "manual-compose-body"
    const val CONTEXT = "manual-compose-context"
    const val SAVE = "manual-compose-save"
}

@Composable
fun ManualComposeDialog(
    viewModel: ManualComposeViewModel,
    onDismiss: () -> Unit,
    onOpenCapture: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val result = state.result

    LaunchedEffect(result) {
        when (result) {
            is ManualComposeResult.Saved -> {
                viewModel.consumeResult()
                onDismiss()
                onOpenCapture(result.envelopeId)
            }
            is ManualComposeResult.AlreadySaved -> {
                viewModel.consumeResult()
                onDismiss()
                onOpenCapture(result.existingEnvelopeId)
            }
            is ManualComposeResult.Blocked,
            null -> Unit
        }
    }

    AlertDialog(
        modifier = Modifier.testTag(ManualComposeTestTags.DIALOG),
        onDismissRequest = onDismiss,
        title = { Text("Save to Orbit") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Diary day: ${state.dayLocal}")
                OutlinedTextField(
                    value = state.bodyText,
                    onValueChange = viewModel::onBodyChanged,
                    label = { Text("What should Orbit remember?") },
                    minLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ManualComposeTestTags.BODY),
                )
                OutlinedTextField(
                    value = state.contextText,
                    onValueChange = viewModel::onContextChanged,
                    label = { Text("Why are you saving it?") },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ManualComposeTestTags.CONTEXT),
                )
                state.error?.let { Text(it) }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Button(
                    onClick = viewModel::save,
                    enabled = !state.saving,
                    modifier = Modifier.testTag(ManualComposeTestTags.SAVE),
                ) {
                    if (state.saving) {
                        CircularProgressIndicator()
                    } else {
                        Text("Save")
                    }
                }
            }
        },
    )
}
