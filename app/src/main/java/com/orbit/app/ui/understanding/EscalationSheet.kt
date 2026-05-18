package com.orbit.app.ui.understanding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orbit.app.understanding.domain.UnderstandingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EscalationSheet(
    isInFlight: Boolean,
    onDismiss: () -> Unit,
    onSelect: (UnderstandingMode) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(Modifier.padding(bottom = 24.dp)) {
            EscalationRow(
                icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null) },
                title = "Quick context",
                supportingText = "Smart",
                enabled = !isInFlight,
                onClick = { onSelect(UnderstandingMode.SMART) }
            )
            EscalationRow(
                icon = { Icon(Icons.Outlined.Cloud, contentDescription = null) },
                title = "Full analysis",
                supportingText = "Deep — uses cloud, may incur cost",
                enabled = !isInFlight,
                onClick = { onSelect(UnderstandingMode.DEEP) }
            )
            if (isInFlight) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun EscalationRow(
    icon: @Composable () -> Unit,
    title: String,
    supportingText: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(supportingText) },
        leadingContent = icon,
        trailingContent = {
            TextButton(enabled = enabled, onClick = onClick) {
                Text("Select")
            }
        }
    )
}
