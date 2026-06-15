package com.orbit.app.generativeui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OrbitAgentUiRenderer(
    document: OrbitAgentUiDocument,
    onOpenEvidence: (OrbitAgentUiEvidence) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("orbit-agent-ui-document"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            document.components.forEach { component ->
                when (component) {
                    is OrbitAgentUiTitle -> TitleComponent(component)
                    is OrbitAgentUiBody -> BodyComponent(component)
                    is OrbitAgentUiLimitations -> LimitationsComponent(component)
                    is OrbitAgentUiQuestion -> QuestionComponent(component)
                    is OrbitAgentUiStep -> StepComponent(component)
                    is OrbitAgentUiEvidence -> EvidenceComponent(
                        component = component,
                        onOpen = { onOpenEvidence(component) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TitleComponent(component: OrbitAgentUiTitle) {
    Text(
        text = component.text,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun BodyComponent(component: OrbitAgentUiBody) {
    Text(
        text = component.text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun LimitationsComponent(component: OrbitAgentUiLimitations) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        component.items.forEach { item ->
            Text(
                text = item.replace('_', ' '),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun QuestionComponent(component: OrbitAgentUiQuestion) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = component.text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        component.choices.forEach { choice ->
            Text(
                text = choice.label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun StepComponent(component: OrbitAgentUiStep) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = component.label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        val detail = buildList {
            component.detail?.takeIf { it.isNotBlank() }?.let(::add)
            if (component.requiresApproval) add("Requires your approval")
        }.joinToString(" | ")
        if (detail.isNotBlank()) {
            Text(
                text = detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun EvidenceComponent(
    component: OrbitAgentUiEvidence,
    onOpen: () -> Unit,
) {
    TextButton(
        onClick = onOpen,
        modifier = Modifier.testTag("orbit-agent-ui-evidence-${component.id}"),
    ) {
        Text(
            text = component.label,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
