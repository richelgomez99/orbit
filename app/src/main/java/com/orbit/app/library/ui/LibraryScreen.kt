package com.orbit.app.library.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import com.orbit.app.library.IntentTypeLabel
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbit.app.library.LibraryViewModel
import com.orbit.app.memory.MemorySearchResult
import com.orbit.app.ui.tokens.OrbitPalette
import com.orbit.app.ui.tokens.OrbitType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onOpenCapture: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.openEnvelopeId) {
        val envelopeId = state.openEnvelopeId ?: return@LaunchedEffect
        onOpenCapture(envelopeId)
        viewModel.onOpenCaptureHandled()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChanged,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("library-query"),
                singleLine = true,
                placeholder = { Text("Search Library") },
                shape = RoundedCornerShape(8.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = quietSearchFieldColors(),
                // The IME action is Search so the keyboard's enter/search key
                // submits — otherwise the only way to run a query is the
                // adjacent button, which reads as "typed but nothing happened".
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { if (state.canSearch) viewModel.onSearchSubmitted() },
                ),
            )
            Button(
                onClick = viewModel::onSearchSubmitted,
                enabled = state.canSearch,
                modifier = Modifier
                    .height(52.dp)
                    .testTag("library-search"),
                shape = RoundedCornerShape(8.dp),
                contentPadding = ButtonDefaults.ContentPadding,
                colors = quietSearchButtonColors(),
            ) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
        }

        when {
            state.loading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            state.unavailableMessage != null -> {
                LibraryStatusText(
                    title = state.unavailableTitle ?: "Library index unavailable",
                    detail = state.unavailableDetail,
                )
            }
            state.searched && state.results.isEmpty() -> {
                LibraryStatusText("No saved memories found")
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("library-results"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.results, key = { it.envelopeId }) { result ->
                        LibraryResultRow(
                            result = result,
                            onOpen = { viewModel.onOpenCapture(result.envelopeId) },
                        )
                    }
                }
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
private fun LibraryStatusText(
    title: String,
    detail: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        detail?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun LibraryResultRow(
    result: MemorySearchResult,
    onOpen: () -> Unit,
) {
    val c = OrbitPalette.current(dark = isSystemInDarkTheme())

    // De-duplicate: for a short capture the title, summary, and matched
    // excerpt are often the same sentence. Show ONE heading and, only if it
    // adds information, ONE distinct snippet.
    val heading = (result.title ?: result.summary ?: "Saved memory").trim()
    // One snippet, and only if it adds information beyond the heading. Prefer
    // the human summary; fall back to the matched excerpt. Picking the first
    // NON-redundant candidate avoids the triplication where title ≈ summary ≈
    // excerpt for short captures.
    val snippet = listOfNotNull(result.summary, result.matchedEvidence.firstOrNull()?.excerpt)
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() && !it.normalizedEquals(heading) }
    val meta = listOfNotNull(result.dayLocal, result.sourceAppLabel, result.domain)
        .filter { it.isNotBlank() }
        .joinToString("  ·  ")
    // Spec 020 (S3) — surface the classifier's type (Recipe, Event, …) so a
    // saved recipe reads as a recipe rather than a generic note. Null for
    // uninformative categories → no badge.
    val typeLabel = IntentTypeLabel.forIntent(result.intent)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("library-result-${result.envelopeId}")
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (typeLabel != null) {
            Text(
                text = typeLabel.uppercase(),
                style = TextStyle(
                    fontFamily = OrbitType.QuietAlmanac.captionMono,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                    color = c.brandAccent,
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(c.brandAccent.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("library-type-badge-${result.envelopeId}"),
            )
        }
        Text(
            text = heading,
            style = TextStyle(
                fontFamily = OrbitType.QuietAlmanac.bodySans,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = c.ink,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (snippet != null) {
            Text(
                text = snippet,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = meta,
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
                text = "View capture ›",
                style = TextStyle(
                    fontFamily = OrbitType.QuietAlmanac.captionMono,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                    color = c.brandAccent,
                ),
            )
        }
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(c.rule),
        )
    }
}

/** Loose equality for dedupe: ignore case, whitespace runs, trailing punctuation. */
private fun String.normalizedEquals(other: String): Boolean {
    fun norm(s: String) = s.lowercase().replace(Regex("\\s+"), " ").trim().trimEnd('.', '…')
    val a = norm(this)
    val b = norm(other)
    return a == b || a.startsWith(b) || b.startsWith(a)
}
