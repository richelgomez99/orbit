package com.orbit.app.orbit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbit.app.memory.AskOrbitAnswer
import com.orbit.app.memory.AskOrbitCitation
import com.orbit.app.memory.MemorySearchResult
import com.orbit.app.orbit.AskOrbitViewModel
import com.orbit.app.ui.primitives.MonoLabel
import com.orbit.app.ui.tokens.OrbitPalette
import com.orbit.app.ui.tokens.OrbitType

object AskOrbitPanelTestTags {
    const val QUESTION = "ask-orbit-question"
    const val SUBMIT = "ask-orbit-submit"
    const val ANSWER = "ask-orbit-answer"
    const val REFUSAL = "ask-orbit-refusal"
    fun citation(envelopeId: String) = "ask-orbit-citation-$envelopeId"
}

/**
 * Ask Orbit — the agent surface. Quiet Almanac styling per design.md
 * (§2 tone: not Material): mono section label, serif answers, hairline
 * rules, amber affordances. The question field submits on the keyboard
 * Search key, not only the button.
 */
@Composable
fun AskOrbitPanel(
    viewModel: AskOrbitViewModel,
    onOpenCapture: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val c = OrbitPalette.current(dark = isSystemInDarkTheme())
    LaunchedEffect(state.openEnvelopeId) {
        val envelopeId = state.openEnvelopeId ?: return@LaunchedEffect
        onOpenCapture(envelopeId)
        viewModel.onOpenCaptureHandled()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MonoLabel(text = "// Ask saved memory", color = c.inkFaint, size = 9.5.sp)
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
                placeholder = {
                    Text(
                        "What do you want to recall?",
                        style = TextStyle(fontFamily = OrbitType.QuietAlmanac.bodySans, fontSize = 14.sp),
                    )
                },
                shape = RoundedCornerShape(8.dp),
                textStyle = TextStyle(
                    fontFamily = OrbitType.QuietAlmanac.bodySans,
                    fontSize = 14.sp,
                    color = c.ink,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { if (state.canAsk) viewModel.onAskSubmitted() },
                ),
                colors = quietFieldColors(c),
            )
            // Typographic submit affordance — no Material icon (design.md §2 #3).
            Text(
                text = "Ask",
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (state.canAsk) {
                            Modifier.clickable(onClick = viewModel::onAskSubmitted)
                        } else {
                            Modifier
                        }
                    )
                    .background(if (state.canAsk) c.brandAccentDim else c.rule.copy(alpha = 0.4f))
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .testTag(AskOrbitPanelTestTags.SUBMIT),
                style = TextStyle(
                    fontFamily = OrbitType.QuietAlmanac.bodySans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (state.canAsk) c.brandAccent else c.inkFaint,
                ),
            )
        }
        when {
            state.loading -> CircularProgressIndicator(
                color = c.brandAccent,
                modifier = Modifier.height(24.dp).padding(top = 2.dp),
            )
            state.error != null -> StatusText(state.error, c)
            state.answer != null -> AskAnswerView(
                answer = state.answer!!,
                onOpenCapture = viewModel::onOpenCapture,
                c = c,
            )
        }
    }
}

@Composable
private fun quietFieldColors(c: OrbitPalette.Tokens) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = c.brandAccent,
    unfocusedBorderColor = c.rule,
    focusedContainerColor = c.paper,
    unfocusedContainerColor = c.paper,
    cursorColor = c.brandAccent,
    focusedPlaceholderColor = c.inkFaint,
    unfocusedPlaceholderColor = c.inkFaint,
)

@Composable
private fun AskAnswerView(
    answer: AskOrbitAnswer,
    onOpenCapture: (String) -> Unit,
    c: OrbitPalette.Tokens,
) {
    val hasCitations = answer.citations.isNotEmpty()
    Column(
        modifier = Modifier.testTag(
            if (hasCitations) AskOrbitPanelTestTags.ANSWER else AskOrbitPanelTestTags.REFUSAL
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (hasCitations) {
            // Preserve exact copy + casing (contract in AskOrbitPanelTest);
            // mono font, not MonoLabel (which uppercases).
            Text(
                text = "Found ${answer.citations.size.coerceAtMost(3)} related capture${if (answer.citations.size == 1) "" else "s"}",
                style = TextStyle(
                    fontFamily = OrbitType.QuietAlmanac.captionMono,
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp,
                    color = c.inkFaint,
                ),
            )
            answer.citations.take(3).forEach { citation ->
                CitationRow(citation = citation, onOpenCapture = onOpenCapture, c = c)
            }
        } else {
            RefusalBlock(answer, c)
            if (answer.candidates.isNotEmpty()) {
                MonoLabel(text = "Closest saved matches", color = c.inkFaint, size = 9.sp)
                answer.candidates.take(3).forEach { candidate ->
                    CandidateRow(candidate = candidate, onOpenCapture = onOpenCapture, c = c)
                }
            }
        }
    }
}

@Composable
private fun RefusalBlock(answer: AskOrbitAnswer, c: OrbitPalette.Tokens) {
    val title = when (answer.status) {
        "sensitive_refusal" -> "Not enough saved evidence"
        "provider_unavailable" -> "Could not check the memory index"
        else -> "No grounded answer yet"
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = TextStyle(
                fontFamily = OrbitType.QuietAlmanac.bodySans,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = c.ink,
            ),
        )
        Text(
            text = answer.answer,
            style = TextStyle(
                fontFamily = OrbitType.QuietAlmanac.displaySerif,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontStyle = FontStyle.Italic,
                color = c.inkFaint,
            ),
        )
        answer.limitations.firstOrNull()?.takeIf { it.isNotBlank() }?.let { limitation ->
            // Original casing preserved (asserted as substring in tests).
            Text(
                text = limitation,
                style = TextStyle(
                    fontFamily = OrbitType.QuietAlmanac.captionMono,
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    letterSpacing = 0.4.sp,
                    color = c.inkFaint,
                ),
            )
        }
    }
}

@Composable
private fun CitationRow(
    citation: AskOrbitCitation,
    onOpenCapture: (String) -> Unit,
    c: OrbitPalette.Tokens,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenCapture(citation.envelopeId) }
            .testTag(AskOrbitPanelTestTags.citation(citation.envelopeId))
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = citation.excerpt?.takeIf { it.isNotBlank() }
                ?: citation.title
                ?: "Saved memory",
            style = TextStyle(
                fontFamily = OrbitType.QuietAlmanac.displaySerif,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                color = c.ink,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = listOfNotNull(citation.dayLocal, citation.sourceAppLabel)
                    .filter { it.isNotBlank() }
                    .joinToString("  ·  "),
                style = TextStyle(
                    fontFamily = OrbitType.QuietAlmanac.captionMono,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                    color = c.inkFaint,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Open ›",
                style = TextStyle(
                    fontFamily = OrbitType.QuietAlmanac.captionMono,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                    color = c.brandAccent,
                ),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(1.dp)
                .background(c.rule),
        )
    }
}

@Composable
private fun CandidateRow(
    candidate: MemorySearchResult,
    onOpenCapture: (String) -> Unit,
    c: OrbitPalette.Tokens,
) {
    Text(
        text = candidate.title ?: candidate.summary ?: "Saved memory",
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenCapture(candidate.envelopeId) }
            .padding(vertical = 4.dp),
        style = TextStyle(
            fontFamily = OrbitType.QuietAlmanac.displaySerif,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = c.inkFaint,
        ),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun StatusText(text: String?, c: OrbitPalette.Tokens) {
    if (text.isNullOrBlank()) return
    Text(
        text = text,
        style = TextStyle(
            fontFamily = OrbitType.QuietAlmanac.bodySans,
            fontSize = 13.sp,
            color = c.inkFaint,
        ),
    )
}
