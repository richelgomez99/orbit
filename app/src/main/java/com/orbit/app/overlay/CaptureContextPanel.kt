package com.orbit.app.overlay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbit.app.ui.theme.LocalRuntimeFlags
import com.orbit.app.ui.tokens.OrbitType
import kotlinx.coroutines.delay

@Composable
fun CaptureContextPanel(
    state: PostCaptureUi.ContextEntry,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val useNewVisualLanguage = LocalRuntimeFlags.current.useNewVisualLanguage
    val quietColors = QuietOverlayColors
    val surfaceColor = if (useNewVisualLanguage) quietColors.DeepNavy else MaterialTheme.colorScheme.surface
    val contentColor = if (useNewVisualLanguage) quietColors.Cream else MaterialTheme.colorScheme.onSurface
    val dimColor = if (useNewVisualLanguage) quietColors.CreamDim else MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = if (useNewVisualLanguage) quietColors.Accent else MaterialTheme.colorScheme.primary
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        color = surfaceColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (useNewVisualLanguage) quietColors.Rule else MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.StickyNote2,
                    contentDescription = null,
                    tint = accentColor,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (state.origin == ContextOrigin.DUPLICATE_CAPTURE) {
                            "Add today's context"
                        } else {
                            "Why save this?"
                        },
                        color = contentColor,
                        style = if (useNewVisualLanguage) TextStyle(
                            fontFamily = OrbitType.QuietAlmanac.bodySans,
                            fontSize = 15.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.sp,
                        ) else MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "A short hint helps Orbit find and act on it later.",
                        color = dimColor,
                        style = if (useNewVisualLanguage) TextStyle(
                            fontFamily = OrbitType.QuietAlmanac.bodySans,
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            letterSpacing = 0.sp,
                        ) else MaterialTheme.typography.labelSmall,
                    )
                }
            }

            OutlinedTextField(
                value = state.text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth(),
                enabled = !state.isSaving,
                minLines = 2,
                maxLines = 4,
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = { Text("For the dentist reschedule, Mom's birthday...") },
                supportingText = {
                    state.errorMessage?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error)
                    }
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = {
                        focusManager.clearFocus()
                        onCancel()
                    },
                    enabled = !state.isSaving,
                ) {
                    Text("Cancel")
                }
                TextButton(
                    onClick = onOpenDetail,
                    enabled = !state.isSaving,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                    )
                    Text("Open detail")
                }
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onSave()
                    },
                    enabled = !state.isSaving,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = if (useNewVisualLanguage) quietColors.DeepNavy else MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(if (state.isSaving) "Saving" else "Save")
                }
            }
        }
    }
}

@Composable
fun ContextSavedConfirmationPill(
    onExpire: () -> Unit,
    modifier: Modifier = Modifier,
    visibleMillis: Long = OverlayMotion.REMOVED_CONFIRMATION_MS
) {
    val useNewVisualLanguage = LocalRuntimeFlags.current.useNewVisualLanguage
    val quietColors = QuietOverlayColors
    val surfaceColor = if (useNewVisualLanguage) quietColors.DeepNavy else MaterialTheme.colorScheme.surface
    val contentColor = if (useNewVisualLanguage) quietColors.Cream else MaterialTheme.colorScheme.onSurface
    val accentColor = if (useNewVisualLanguage) quietColors.Accent else MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) {
        delay(visibleMillis)
        onExpire()
    }

    Surface(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        color = surfaceColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (useNewVisualLanguage) quietColors.Rule else MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.StickyNote2,
                contentDescription = null,
                tint = accentColor,
            )
            Text(
                text = "Context saved",
                color = contentColor,
                style = if (useNewVisualLanguage) TextStyle(
                    fontFamily = OrbitType.QuietAlmanac.bodySans,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp,
                ) else MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
