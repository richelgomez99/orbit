package com.orbit.app.diary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbit.app.diary.ManualComposeResult
import com.orbit.app.diary.ManualComposeViewModel
import com.orbit.app.ui.primitives.MonoLabel
import com.orbit.app.ui.tokens.OrbitType

object ManualComposeTestTags {
    const val DIALOG = "manual-compose-dialog"
    const val BODY = "manual-compose-body"
    const val CONTEXT = "manual-compose-context"
    const val SAVE = "manual-compose-save"
}

/** Quiet Almanac palette — mirrors the overlay capture panel (QuietOverlayColors). */
private object McColors {
    val BgDeep = Color(0xFF080B14)
    val Cream = Color(0xFFF3EAD8)
    val CreamDim = Color(0xBFF3EAD8)
    val CreamFaint = Color(0x66F3EAD8)
    val Accent = Color(0xFFE8B06A)
    val AccentInk = Color(0xFF211607)
    val Rule = Color(0x29F3EAD8)
    val Red = Color(0xFFD97A6C)
}

/**
 * Spec 015 — "Save to Orbit" manual compose, refit to the Quiet Almanac
 * language so it matches the overlay capture flow (CaptureContextPanel)
 * instead of a stock Material AlertDialog. A bottom sheet on the deep-navy
 * surface: mono kicker, serif prompts, hairline fields, an amber Save.
 * The ViewModel contract is unchanged.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = McColors.BgDeep,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(width = 34.dp, height = 4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(McColors.CreamFaint),
            )
        },
        modifier = Modifier.testTag(ManualComposeTestTags.DIALOG),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MonoLabel(text = "// SAVE TO ORBIT", color = McColors.Accent, size = 9.5.sp)
                MonoLabel(
                    text = "DIARY DAY · ${state.dayLocal}",
                    color = McColors.CreamFaint,
                    size = 9.sp,
                )
            }

            QuietComposeField(
                prompt = "What should Orbit remember?",
                value = state.bodyText,
                onValueChange = viewModel::onBodyChanged,
                minLines = 4,
                enabled = !state.saving,
                testTag = ManualComposeTestTags.BODY,
            )

            QuietComposeField(
                prompt = "Why save this?",
                value = state.contextText,
                onValueChange = viewModel::onContextChanged,
                minLines = 2,
                enabled = !state.saving,
                placeholder = "For the dentist reschedule, Mom's birthday…",
                testTag = ManualComposeTestTags.CONTEXT,
            )

            state.error?.let {
                Text(
                    text = it,
                    color = McColors.Red,
                    style = TextStyle(fontFamily = OrbitType.QuietAlmanac.bodySans, fontSize = 12.sp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Cancel",
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(enabled = !state.saving, onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    color = McColors.CreamDim,
                    style = TextStyle(
                        fontFamily = OrbitType.QuietAlmanac.bodySans,
                        fontSize = 14.sp,
                    ),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(McColors.Accent)
                        .clickable(enabled = !state.saving, onClick = viewModel::save)
                        .testTag(ManualComposeTestTags.SAVE)
                        .padding(horizontal = 22.dp, vertical = 11.dp),
                ) {
                    Text(
                        text = if (state.saving) "Saving…" else "Save",
                        color = McColors.AccentInk,
                        style = TextStyle(
                            fontFamily = OrbitType.QuietAlmanac.displaySerif,
                            fontSize = 16.sp,
                            fontStyle = FontStyle.Italic,
                        ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuietComposeField(
    prompt: String,
    value: String,
    onValueChange: (String) -> Unit,
    minLines: Int,
    enabled: Boolean,
    testTag: String,
    placeholder: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = prompt,
            color = McColors.Cream,
            style = TextStyle(
                fontFamily = OrbitType.QuietAlmanac.displaySerif,
                fontSize = 18.sp,
                lineHeight = 22.sp,
            ),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            minLines = minLines,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            textStyle = TextStyle(
                fontFamily = OrbitType.QuietAlmanac.bodySans,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = McColors.Cream,
            ),
            placeholder = placeholder?.let {
                {
                    Text(
                        text = it,
                        color = McColors.CreamFaint,
                        style = TextStyle(
                            fontFamily = OrbitType.QuietAlmanac.bodySans,
                            fontSize = 14.sp,
                        ),
                    )
                }
            },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = McColors.Accent,
                unfocusedBorderColor = McColors.Rule,
                cursorColor = McColors.Accent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = McColors.Cream,
                unfocusedTextColor = McColors.Cream,
            ),
        )
    }
}
