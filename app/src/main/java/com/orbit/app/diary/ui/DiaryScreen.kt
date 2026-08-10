package com.orbit.app.diary.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.provider.Settings
import android.widget.Toast
import com.orbit.app.data.ClusterCardModel
import com.orbit.app.data.ipc.ActionProposalParcel
import com.orbit.app.data.ipc.EnvelopeViewParcel
import com.orbit.app.data.model.ClusterState
import com.orbit.app.data.model.Intent
import com.orbit.app.diary.ActiveIntentGroup
import com.orbit.app.diary.ActiveIntentItem
import com.orbit.app.diary.ActiveIntentUiState
import com.orbit.app.diary.ActionPreviewSheet
import com.orbit.app.diary.ActionProposalChipRow
import com.orbit.app.diary.DayUiState
import com.orbit.app.diary.DiaryPagingSource
import com.orbit.app.diary.DiaryViewModel
import com.orbit.app.diary.EnvelopeDetailActivity
import com.orbit.app.ui.IntentChipPicker
import com.orbit.app.ui.primitives.MonoLabel
import com.orbit.app.ui.primitives.OrbitWordmark
import com.orbit.app.ui.primitives.SourceGlyph
import com.orbit.app.ui.primitives.SourceGlyphKind
import com.orbit.app.ui.primitives.SourceIdentityResolver
import com.orbit.app.ui.theme.LocalRuntimeFlags
import com.orbit.app.ui.tokens.OrbitType
import com.orbit.app.understanding.domain.ResolutionReason
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * T050 + T056 — diary screen with unbounded backscroll over non-empty days.
 *
 * If a [pagingSource] is provided, the screen hosts a [HorizontalPager]
 * whose pages map to ISO dates (today at index 0, older non-empty days
 * following). Swiping near the end triggers [DiaryPagingSource.loadMore]
 * to fetch the next 30-day batch.
 *
 * When [pagingSource] is null (legacy callers / tests), the screen
 * renders only the single day the VM currently observes.
 *
 * Empty / Loading / Error / Ready are rendered as exhaustive branches
 * over [DayUiState] — the sealed type enforces that the UI stays in
 * sync with VM reductions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    viewModel: DiaryViewModel,
    modifier: Modifier = Modifier,
    onOpenSetup: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    pagingSource: DiaryPagingSource? = null
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val useNewVisualLanguage = LocalRuntimeFlags.current.useNewVisualLanguage
    val settingsAction = onOpenSettings ?: onOpenSetup

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (useNewVisualLanguage) {
                QuietDiaryTopBar(
                    dateLabel = state.diaryHeaderDateLabel(),
                    onOpenSettings = settingsAction,
                )
            } else {
                TopAppBar(
                    title = { Text("Orbit") },
                    actions = {
                        if (settingsAction != null) {
                            IconButton(onClick = settingsAction) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Settings"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        },
        containerColor = if (useNewVisualLanguage) QuietDiaryColors.BgDeep else MaterialTheme.colorScheme.background
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = if (useNewVisualLanguage) QuietDiaryColors.BgDeep else MaterialTheme.colorScheme.background
        ) {
            if (pagingSource != null) {
                PagedDayHost(
                    pagingSource = pagingSource,
                    viewModel = viewModel,
                    state = state,
                    onOpenSetup = onOpenSetup
                )
            } else {
                RenderDayState(
                    state = state,
                    viewModel = viewModel,
                    onOpenSetup = onOpenSetup
                )
            }
        }
    }
}

@Composable
private fun QuietDiaryTopBar(
    dateLabel: String,
    onOpenSettings: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(QuietDiaryColors.BgDeep)
            .statusBarsPadding()
            .padding(start = 24.dp, end = 18.dp, top = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                MonoLabel(
                    text = dateLabel,
                    color = QuietDiaryColors.CreamDim,
                    size = 9.sp,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                OrbitWordmark(
                    height = 26.dp,
                    ink = QuietDiaryColors.Cream,
                    accent = QuietDiaryColors.Accent,
                )
            }
            if (onOpenSettings != null) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = QuietDiaryColors.Cream,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(QuietDiaryColors.Rule),
        )
    }
}

@Composable
private fun PagedDayHost(
    pagingSource: DiaryPagingSource,
    viewModel: DiaryViewModel,
    state: DayUiState,
    onOpenSetup: (() -> Unit)?
) {
    val days by pagingSource.days.collectAsState()
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { days.size }
    )
    val scope = rememberCoroutineScope()

    // Tell the VM to observe whichever day is currently settled. Only fires
    // when the `currentPage` stops changing (i.e. after a swipe settles).
    LaunchedEffect(pagerState, days) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { idx ->
                days.getOrNull(idx)?.let { iso -> viewModel.observe(iso) }
            }
    }

    // Prefetch older days when the user approaches the end.
    LaunchedEffect(pagerState, days.size) {
        snapshotFlow { pagerState.currentPage to days.size }
            .distinctUntilChanged()
            .collect { (page, total) ->
                if (total > 0 && page >= total - PREFETCH_DISTANCE) {
                    pagingSource.loadMore()
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DayNavBar(
            currentPage = pagerState.currentPage,
            totalPages = days.size,
            onPrev = {
                // Older day = higher page index because `reverseLayout = true`.
                scope.launch {
                    val target = pagerState.currentPage + 1
                    if (target >= days.size) {
                        // Try to fetch the next batch first; it's safe to call
                        // repeatedly — the paging source dedupes + exhausts.
                        pagingSource.loadMore()
                    }
                    if (target < pagingSource.days.value.size) {
                        pagerState.animateScrollToPage(target)
                    }
                }
            },
            onNext = {
                scope.launch {
                    val target = pagerState.currentPage - 1
                    if (target >= 0) pagerState.animateScrollToPage(target)
                }
            }
        )

        HorizontalPager(
            state = pagerState,
            // T056 — today sits on the right edge; swiping right walks into older
            // non-empty days (matches a physical calendar flipping backwards).
            reverseLayout = true,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val iso = days.getOrNull(page)
            // Only the settled page's state is accurate; adjacent pages show a
            // lightweight placeholder until the VM swaps its observation.
            if (iso == null || page != pagerState.currentPage) {
                LoadingView()
            } else {
                RenderDayState(state = state, viewModel = viewModel, onOpenSetup = onOpenSetup)
            }
        }
    }
}

@Composable
private fun DayNavBar(
    currentPage: Int,
    totalPages: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    // T056 — button-based day navigation. Swipe still works, but buttons
    // make the unbounded backscroll UX discoverable.
    // `currentPage == 0` means today (reverseLayout inverts the mapping).
    val canGoNext = currentPage > 0
    // Prev is always enabled when we have at least today rendered: the
    // paging source transparently fetches the next batch on tap and
    // short-circuits once the history is exhausted.
    val canGoPrev = totalPages > 0
    val useNewVisualLanguage = LocalRuntimeFlags.current.useNewVisualLanguage
    if (useNewVisualLanguage) {
        QuietDayNavBar(
            canGoPrev = canGoPrev,
            canGoNext = canGoNext,
            onPrev = onPrev,
            onNext = onNext,
        )
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onPrev, enabled = canGoPrev) {
            Icon(imageVector = Icons.Filled.ChevronLeft, contentDescription = null)
            Spacer(Modifier.height(0.dp))
            Text("Older day")
        }
        TextButton(onClick = onNext, enabled = canGoNext) {
            Text("Newer day")
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun QuietDayNavBar(
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(QuietDiaryColors.BgDeep)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QuietDayNavAction(
            label = "‹ Older captures",
            enabled = canGoPrev,
            onClick = onPrev,
        )
        QuietDayNavAction(
            label = "Newer captures ›",
            enabled = canGoNext,
            onClick = onNext,
        )
    }
}

@Composable
private fun QuietDayNavAction(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 3.dp),
        color = if (enabled) QuietDiaryColors.CreamDim else QuietDiaryColors.CreamFaint,
        style = TextStyle(
            fontFamily = OrbitType.QuietAlmanac.captionMono,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            letterSpacing = 1.4.sp,
        ),
    )
}

private const val PREFETCH_DISTANCE: Int = 3

@Composable
private fun RenderDayState(
    state: DayUiState,
    viewModel: DiaryViewModel,
    onOpenSetup: (() -> Unit)?
) {
    val context = LocalContext.current
    var pendingProposal by remember { mutableStateOf<ActionProposalParcel?>(null) }
    val activeIntentState by viewModel.activeIntentState.collectAsState()

    // T053 — surface the 5 s undo window via a Toast. Compose Snackbar
    // would be a cleaner host but the rest of Diary already uses Toasts
    // for transient feedback, so we match for visual consistency.
    val undoState by viewModel.undoState.collectAsState()
    LaunchedEffect(undoState?.executionId) {
        val s = undoState ?: return@LaunchedEffect
        val msg = when (s.outcome) {
            "DISPATCHED", "SUCCESS" -> "Added · tap notification to undo"
            "FAILED" -> "Couldn't add: ${s.outcomeReason ?: "unknown"}"
            "USER_CANCELLED" -> "Cancelled"
            else -> s.outcome
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    when (val s = state) {
        is DayUiState.Loading -> LoadingView()
        is DayUiState.Empty -> EmptyDayView(
            isoDate = s.isoDate,
            activeIntentState = activeIntentState,
            onResolveActiveIntent = { item ->
                viewModel.onResolveActiveIntent(item.intentId, item.defaultResolutionReason)
            },
            onArchiveActiveIntent = { item ->
                viewModel.onResolveActiveIntent(item.intentId, ResolutionReason.USER_ARCHIVED)
            },
            onOpenActiveIntentCapture = { item ->
                context.startActivity(
                    EnvelopeDetailActivity.newIntent(context, item.captureId, dayLocal = s.isoDate)
                )
            },
            onAddActiveIntentContext = { item ->
                context.startActivity(
                    EnvelopeDetailActivity.newIntent(
                        context,
                        item.captureId,
                        dayLocal = s.isoDate,
                        startNote = true
                    )
                )
            },
            onEscalateActiveIntent = { item ->
                viewModel.onRequestActiveIntentEscalation(item.intentId)
                Toast.makeText(context, "Orbit added a decision brief", Toast.LENGTH_SHORT).show()
            }
        )
        is DayUiState.Error -> ErrorView(message = s.message)
        is DayUiState.Ready -> DayContentView(
            state = s,
            viewModel = viewModel,
            activeIntentState = activeIntentState,
            onReassign = { id, intent ->
                viewModel.onReassignIntent(id, intent.name, reason = "DIARY_REASSIGN")
            },
            onRetry = { id ->
                viewModel.onRetryHydration(id)
                Toast.makeText(context, "Retrying enrichment\u2026", Toast.LENGTH_SHORT).show()
            },
            onDelete = { id ->
                viewModel.onDelete(id)
                Toast.makeText(context, "Moved to trash", Toast.LENGTH_SHORT).show()
            },
            onOpenDetail = { id ->
                context.startActivity(
                    EnvelopeDetailActivity.newIntent(context, id, dayLocal = s.isoDate)
                )
            },
            onProposalTap = { proposal -> pendingProposal = proposal },
            onResolveActiveIntent = { item ->
                viewModel.onResolveActiveIntent(item.intentId, item.defaultResolutionReason)
            },
            onArchiveActiveIntent = { item ->
                viewModel.onResolveActiveIntent(item.intentId, ResolutionReason.USER_ARCHIVED)
            },
            onOpenActiveIntentCapture = { item ->
                context.startActivity(
                    EnvelopeDetailActivity.newIntent(context, item.captureId, dayLocal = s.isoDate)
                )
            },
            onAddActiveIntentContext = { item ->
                context.startActivity(
                    EnvelopeDetailActivity.newIntent(
                        context,
                        item.captureId,
                        dayLocal = s.isoDate,
                        startNote = true
                    )
                )
            },
            onEscalateActiveIntent = { item ->
                viewModel.onRequestActiveIntentEscalation(item.intentId)
                Toast.makeText(context, "Orbit added a decision brief", Toast.LENGTH_SHORT).show()
            }
        )
    }

    pendingProposal?.let { proposal ->
        ActionPreviewSheet(
            proposal = proposal,
            onConfirm = { editedArgsJson ->
                viewModel.onConfirmProposal(proposal, editedArgsJson)
                pendingProposal = null
            },
            onDismiss = {
                viewModel.onDismissProposal(proposal.id)
                pendingProposal = null
            }
        )
    }
}

@Composable
private fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyDayView(
    isoDate: String,
    activeIntentState: ActiveIntentUiState = ActiveIntentUiState.Empty,
    onResolveActiveIntent: (ActiveIntentItem) -> Unit = {},
    onArchiveActiveIntent: (ActiveIntentItem) -> Unit = {},
    onOpenActiveIntentCapture: (ActiveIntentItem) -> Unit = {},
    onAddActiveIntentContext: (ActiveIntentItem) -> Unit = {},
    onEscalateActiveIntent: (ActiveIntentItem) -> Unit = {}
) {
    // T054 — empty-day copy. Kept short and un-judgmental per product tone.
    val hasActiveIntents = activeIntentState is ActiveIntentUiState.Ready
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (LocalRuntimeFlags.current.useNewVisualLanguage) QuietDiaryColors.BgDeep else Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (hasActiveIntents) {
            ActiveIntentCleanupPanel(
                state = activeIntentState,
                onResolve = onResolveActiveIntent,
                onArchive = onArchiveActiveIntent,
                onOpenCapture = onOpenActiveIntentCapture,
                onAddContext = onAddActiveIntentContext,
                onEscalate = onEscalateActiveIntent,
                modifier = Modifier.padding(top = 18.dp)
            )
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Nothing saved yet",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Copy something, then tap the bubble.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    }
}

@Composable
private fun ErrorView(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Couldn't load today",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Phase 11 Block 11 / T150-revisit — exposed at `internal` so the
 * instrumented placement test
 * (`app/src/androidTest/.../DiaryScreenWithClusterTest`) can drive
 * this composable directly with a fake [DiaryViewModel] + fixture
 * [DayUiState.Ready], replacing the fragile mirror used in Block 10.
 * Test-tags on the cluster card and day header are stable seams for
 * `getBoundsInRoot()` placement assertions.
 */
@Composable
internal fun DayContentView(
    state: DayUiState.Ready,
    viewModel: DiaryViewModel,
    activeIntentState: ActiveIntentUiState = ActiveIntentUiState.Empty,
    onReassign: (String, Intent) -> Unit,
    onRetry: (String) -> Unit,
    onDelete: (String) -> Unit,
    onOpenDetail: (String) -> Unit,
    onProposalTap: (ActionProposalParcel) -> Unit,
    onResolveActiveIntent: (ActiveIntentItem) -> Unit = {},
    onArchiveActiveIntent: (ActiveIntentItem) -> Unit = {},
    onOpenActiveIntentCapture: (ActiveIntentItem) -> Unit = {},
    onAddActiveIntentContext: (ActiveIntentItem) -> Unit = {},
    onEscalateActiveIntent: (ActiveIntentItem) -> Unit = {}
) {
    // Phase 11 Block 9 / T149 — reduce-motion preference. Compose has no
    // first-class API for this; we read `Settings.Global.ANIMATOR_DURATION_SCALE`
    // (== 0f means "Remove animations" in Developer Options + Accessibility).
    // Falls back to `false` if the setting is unreadable.
    val context = LocalContext.current
    val reduceMotion = remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
    val useNewVisualLanguage = LocalRuntimeFlags.current.useNewVisualLanguage

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(if (useNewVisualLanguage) QuietDiaryColors.BgDeep else Color.Transparent),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
    ) {
        if (activeIntentState is ActiveIntentUiState.Ready) {
            item(key = "active-intent-cleanup") {
                ActiveIntentCleanupPanel(
                    state = activeIntentState,
                    onResolve = onResolveActiveIntent,
                    onArchive = onArchiveActiveIntent,
                    onOpenCapture = onOpenActiveIntentCapture,
                    onAddContext = onAddActiveIntentContext,
                    onEscalate = onEscalateActiveIntent,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }

        // T149 — cluster slot renders ABOVE the day-header on cluster days
        // (spec 010 D6 revision: events outrank steady-state). Non-cluster
        // days (clusters.isEmpty()) skip the slot entirely.
        state.clusters.forEach { cluster ->
            item(key = "cluster-${cluster.clusterId}") {
                val cardState = cluster.toCardStateOrNull()
                if (cardState != null) {
                    Column(modifier = Modifier.testTag(DiaryScreenTestTags.CLUSTER_SLOT)) {
                        if (useNewVisualLanguage) {
                            MonoLabel(
                                text = "// Orbit noticed",
                                color = QuietDiaryColors.CreamDim,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                            )
                        }
                        ClusterSuggestionCard(
                            state = cardState,
                            onSummarize = { /* T-future: wired when ClusterSummariser lands (Block 6/10) */ },
                            onOpenAll = { /* T-future: cluster detail navigation */ },
                            // Block 10 review FU#2 — Dismiss is wired end-to-end:
                            // VM -> BinderDiaryRepository -> IEnvelopeRepository
                            // .markClusterDismissed -> ClusterRepository.markDismissed
                            // -> DAO updateState + audit row. The data-layer flow
                            // re-emits without this row so the card vanishes.
                            onDismiss = { viewModel.onDismissCluster(cluster.clusterId) },
                            onRetry = { /* T-future: re-enqueue summary */ },
                            reduceMotion = reduceMotion,
                        )
                    }
                }
            }
        }

        // Sticky-ish day header (LazyColumn's stickyHeader API is overkill
        // for a single item; a plain `item` matches the Figma's "hero
        // paragraph that scrolls up with the rest" behaviour).
        item(key = "header-${state.isoDate}") {
            if (useNewVisualLanguage) {
                QuietDayHeader(
                    isoDate = state.isoDate,
                    modifier = Modifier.testTag(DiaryScreenTestTags.DAY_HEADER),
                )
            } else {
                DayHeader(
                    isoDate = state.isoDate,
                    paragraph = state.header,
                    generationLocale = state.generationLocale,
                    modifier = Modifier.testTag(DiaryScreenTestTags.DAY_HEADER),
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        state.threads.forEach { thread ->
            item(key = "thread-label-${thread.id}") {
                if (!useNewVisualLanguage) {
                    ThreadLabel(label = thread.appCategory)
                }
            }
            items(thread.envelopes, key = { it.id }) { env ->
                if (useNewVisualLanguage) {
                    QuietDiaryEnvelopeRow(
                        envelope = env,
                        onReassign = onReassign,
                        onRetry = onRetry,
                        onDelete = onDelete,
                        onOpenDetail = onOpenDetail,
                        onToggleTodoItem = { id, index, done ->
                            viewModel.onToggleTodoItem(id, index, done)
                        },
                    )
                } else {
                    EnvelopeCard(
                        envelope = env,
                        onReassign = onReassign,
                        onRetry = onRetry,
                        onDelete = onDelete,
                        onOpenDetail = onOpenDetail,
                        onToggleTodoItem = { id, index, done ->
                            viewModel.onToggleTodoItem(id, index, done)
                        }
                    )
                }
                // T051 — chip-row inline beneath the card. The flow is
                // owned by this item so swiping the envelope out of the
                // viewport correctly tears down the proposal observer.
                ActionProposalChipRow(
                    envelopeId = env.id,
                    proposalsFlow = viewModel.observeProposals(env.id),
                    onChipTap = onProposalTap
                )
            }
            item(key = "thread-space-${thread.id}") {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
internal fun ActiveIntentCleanupPanel(
    state: ActiveIntentUiState,
    onResolve: (ActiveIntentItem) -> Unit,
    onArchive: (ActiveIntentItem) -> Unit,
    onOpenCapture: (ActiveIntentItem) -> Unit = {},
    onAddContext: (ActiveIntentItem) -> Unit = {},
    onEscalate: (ActiveIntentItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val ready = state as? ActiveIntentUiState.Ready ?: return
    var decisionItem by remember { mutableStateOf<ActiveIntentItem?>(null) }
    var isExpanded by rememberSaveable { mutableStateOf(true) }
    var selectedFilterName by rememberSaveable { mutableStateOf(ActiveIntentFilter.All.name) }
    val selectedFilter = runCatching { ActiveIntentFilter.valueOf(selectedFilterName) }
        .getOrDefault(ActiveIntentFilter.All)
    val filteredGroups = ready.filteredGroups(selectedFilter)
    val useNewVisualLanguage = LocalRuntimeFlags.current.useNewVisualLanguage
    val horizontalPadding = if (useNewVisualLanguage) 24.dp else 20.dp
    val ink = if (useNewVisualLanguage) QuietDiaryColors.Cream else MaterialTheme.colorScheme.onBackground
    val dim = if (useNewVisualLanguage) QuietDiaryColors.CreamDim else MaterialTheme.colorScheme.onSurfaceVariant
    val rule = if (useNewVisualLanguage) QuietDiaryColors.Rule else MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .testTag(DiaryScreenTestTags.ACTIVE_INTENT_PANEL),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Needs follow-up",
                    color = ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = ready.followUpSummary(),
                    color = dim,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            TextButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.testTag(DiaryScreenTestTags.ACTIVE_INTENT_TOGGLE),
            ) {
                Text(if (isExpanded) "Hide" else "Show")
            }
        }
        AnimatedVisibility(visible = isExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ActiveIntentFilterRow(
                    ready = ready,
                    selectedFilter = selectedFilter,
                    dim = dim,
                    rule = rule,
                    onSelect = { selectedFilterName = it.name },
                )
                if (filteredGroups.isEmpty()) {
                    Text(
                        text = "No captures in this view.",
                        color = dim,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                filteredGroups.forEach { group ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(DiaryScreenTestTags.activeIntentGroup(group.lifecycleStatus, group.category)),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = group.title,
                            color = dim,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        group.items.forEach { item ->
                            ActiveIntentRow(
                                item = item,
                                ink = ink,
                                dim = dim,
                                rule = rule,
                                onResolve = onResolve,
                                onArchive = onArchive,
                                onOpenCapture = onOpenCapture,
                                onAddContext = onAddContext,
                                onEscalate = { decisionItem = it },
                            )
                        }
                    }
                }
            }
        }
    }

    decisionItem?.let { item ->
        ActiveIntentDecisionDialog(
            item = item,
            onDismiss = { decisionItem = null },
            onReview = {
                onEscalate(item)
                decisionItem = null
            },
            onOpenCapture = {
                onOpenCapture(item)
                decisionItem = null
            },
            onAddContext = {
                onAddContext(item)
                decisionItem = null
            },
            onResolve = {
                onResolve(item)
                decisionItem = null
            },
            onArchive = {
                onArchive(item)
                decisionItem = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveIntentFilterRow(
    ready: ActiveIntentUiState.Ready,
    selectedFilter: ActiveIntentFilter,
    dim: Color,
    rule: Color,
    onSelect: (ActiveIntentFilter) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ActiveIntentFilter.entries.forEach { filter ->
            val selected = filter == selectedFilter
            Text(
                text = filter.displayLabel(ready),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selected) QuietDiaryColors.Accent else rule)
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag(DiaryScreenTestTags.activeIntentFilter(filter.name)),
                color = if (selected) QuietDiaryColors.BgDeep else dim,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}

private enum class ActiveIntentFilter(private val title: String) {
    All("All"),
    NeedsContext("Needs context"),
    Ready("Ready"),
    MaybeOld("Maybe old");

    fun matches(item: ActiveIntentItem): Boolean = when (this) {
        All -> true
        NeedsContext -> item.lifecycleStatus == "NEEDS_CONTEXT"
        Ready -> item.lifecycleStatus != "NEEDS_CONTEXT" && item.lifecycleStatus != "MAYBE_OLD"
        MaybeOld -> item.lifecycleStatus == "MAYBE_OLD"
    }

    fun displayLabel(ready: ActiveIntentUiState.Ready): String = "$title ${ready.items().count(::matches)}"
}

private fun ActiveIntentUiState.Ready.items(): List<ActiveIntentItem> = groups.flatMap { it.items }

private fun ActiveIntentUiState.Ready.filteredGroups(filter: ActiveIntentFilter): List<ActiveIntentGroup> =
    groups.mapNotNull { group ->
        val items = group.items.filter(filter::matches)
        if (items.isEmpty()) null else group.copy(items = items)
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveIntentDecisionDialog(
    item: ActiveIntentItem,
    onDismiss: () -> Unit,
    onReview: () -> Unit,
    onOpenCapture: () -> Unit,
    onAddContext: () -> Unit,
    onResolve: () -> Unit,
    onArchive: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(DiaryScreenTestTags.activeIntentDecisionDialog(item.intentId)),
        title = {
            Text(
                text = item.evidenceLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = item.sourceLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                item.clueLabel?.let { clue ->
                    Text(
                        text = clue,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    text = item.reasonLabel,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "Start with the saved capture if the clue is not enough. ${item.guidanceLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Button(
                    onClick = onOpenCapture,
                    modifier = Modifier.testTag(DiaryScreenTestTags.activeIntentDecisionOpenCapture(item.intentId)),
                    shape = RoundedCornerShape(8.dp),
                ) { Text(item.openCaptureActionLabel) }
                TextButton(
                    onClick = onAddContext,
                    modifier = Modifier.testTag(DiaryScreenTestTags.activeIntentDecisionAddContext(item.intentId)),
                ) { Text(item.addContextActionLabel) }
                TextButton(
                    onClick = onReview,
                    modifier = Modifier.testTag(DiaryScreenTestTags.activeIntentDecisionReview(item.intentId)),
                ) { Text(item.askOrbitActionLabel) }
                TextButton(
                    onClick = onResolve,
                    modifier = Modifier.testTag(DiaryScreenTestTags.activeIntentDecisionResolve(item.intentId)),
                ) { Text(item.resolveActionLabel) }
                TextButton(
                    onClick = onArchive,
                    modifier = Modifier.testTag(DiaryScreenTestTags.activeIntentDecisionArchive(item.intentId)),
                ) { Text(item.archiveActionLabel) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveIntentRow(
    item: ActiveIntentItem,
    ink: Color,
    dim: Color,
    rule: Color,
    onResolve: (ActiveIntentItem) -> Unit,
    onArchive: (ActiveIntentItem) -> Unit,
    onOpenCapture: (ActiveIntentItem) -> Unit,
    onAddContext: (ActiveIntentItem) -> Unit,
    onEscalate: (ActiveIntentItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, rule, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(DiaryScreenTestTags.activeIntentItem(item.intentId)),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = item.evidenceLabel,
                    color = ink,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                )
                Text(
                    text = item.sourceLabel,
                    color = dim,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
                item.clueLabel?.let { clue ->
                    Text(
                        text = clue,
                        color = dim,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                    )
                }
                Text(
                    text = item.reasonLabel,
                    color = dim,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 3,
                )
                Text(
                    text = item.guidanceLabel,
                    color = dim,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 3,
                )
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Button(
                onClick = { onOpenCapture(item) },
                modifier = Modifier.testTag(DiaryScreenTestTags.activeIntentOpenCapture(item.intentId)),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = QuietDiaryColors.Accent,
                    contentColor = QuietDiaryColors.BgDeep,
                ),
            ) { Text(item.openCaptureActionLabel) }
            if (item.needsEscalation) {
                TextButton(
                    onClick = { onAddContext(item) },
                    modifier = Modifier.testTag(DiaryScreenTestTags.activeIntentAddContext(item.intentId)),
                ) { Text(item.addContextActionLabel) }
                TextButton(
                    onClick = { onEscalate(item) },
                    modifier = Modifier.testTag(DiaryScreenTestTags.activeIntentEscalate(item.intentId)),
                ) { Text(item.askOrbitActionLabel) }
            }
            TextButton(
                onClick = { onResolve(item) },
                modifier = Modifier.testTag(DiaryScreenTestTags.activeIntentResolve(item.intentId)),
            ) { Text(item.resolveActionLabel) }
            TextButton(
                onClick = { onArchive(item) },
                modifier = Modifier.testTag(DiaryScreenTestTags.activeIntentArchive(item.intentId)),
            ) { Text(item.archiveActionLabel) }
            if (!item.needsEscalation) {
                TextButton(
                    onClick = { onEscalate(item) },
                    modifier = Modifier.testTag(DiaryScreenTestTags.activeIntentEscalate(item.intentId)),
                ) { Text(item.askOrbitActionLabel) }
            }
        }
    }
}

private fun ActiveIntentUiState.Ready.followUpSummary(): String {
    val noun = if (activeCount == 1) "capture" else "captures"
    val base = "$activeCount $noun may need a reply, plan, to-do, or clear decision"
    return if (missingContextCount > 0) {
        val contextVerb = if (missingContextCount == 1) "needs" else "need"
        "$base · $missingContextCount $contextVerb context"
    } else {
        base
    }
}

@Composable
private fun DayHeader(
    isoDate: String,
    paragraph: String,
    generationLocale: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = isoDate,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        if (paragraph.isNotBlank()) {
            Text(
                text = paragraph,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            // If the header was generated in English for a non-English
            // device, surface a small hint so the user knows why.
            if (generationLocale.isNotBlank() && generationLocale != java.util.Locale.getDefault().language) {
                Text(
                    text = "Generated in English",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ThreadLabel(label: String) {
    Text(
        text = label.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() },
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun QuietDayHeader(
    isoDate: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
    ) {
        MonoLabel(
            text = isoDate.toQuietDayLabel(),
            color = QuietDiaryColors.CreamDim,
            size = 10.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
        QuietRule()
    }
}

@Composable
private fun QuietDiaryEnvelopeRow(
    envelope: EnvelopeViewParcel,
    onReassign: (String, Intent) -> Unit,
    onRetry: (String) -> Unit,
    onDelete: (String) -> Unit,
    onOpenDetail: (String) -> Unit,
    onToggleTodoItem: (String, Int, Boolean) -> Unit,
) {
    var pickerOpen by rememberSaveable(envelope.id) { mutableStateOf(false) }
    var confirmDelete by rememberSaveable(envelope.id) { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this capture?") },
            text = {
                Text(
                    "It moves to the trash and is permanently removed after 30 days. " +
                        "You can restore it from Settings -> Trash until then."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(envelope.id)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetail(envelope.id) }
            .padding(horizontal = 24.dp, vertical = 14.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            MonoLabel(
                text = envelope.createdAtMillis.toQuietTimeLabel(),
                color = QuietDiaryColors.CreamFaint,
                size = 10.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
            SourceGlyph(
                kind = envelope.toSourceGlyphKind(),
                size = 20.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val titleOrPreview = envelope.title?.takeIf { it.isNotBlank() }
                    ?: envelope.textContent?.take(160)?.replace('\n', ' ')
                    ?: buildSubtitle(envelope)
                Text(
                    text = titleOrPreview,
                    style = TextStyle(
                        fontFamily = OrbitType.QuietAlmanac.bodySans,
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp,
                        color = QuietDiaryColors.Cream,
                        letterSpacing = 0.sp,
                    ),
                    maxLines = 3,
                )
                envelope.summary?.takeIf { it.isNotBlank() }?.let { summary ->
                    Text(
                        text = summary,
                        style = TextStyle(
                            fontFamily = OrbitType.QuietAlmanac.displaySerif,
                            fontSize = 13.sp,
                            lineHeight = 17.sp,
                            fontStyle = FontStyle.Italic,
                            color = QuietDiaryColors.CreamDim,
                            letterSpacing = 0.sp,
                        ),
                        maxLines = 2,
                    )
                }
                QuietMiniIntent(
                    label = envelope.intent.toQuietIntentLabel(),
                    color = envelope.intent.toQuietIntentColor(),
                    onClick = { pickerOpen = !pickerOpen },
                )

                val todoMeta = envelope.todoMetaJson
                if (!todoMeta.isNullOrBlank()) {
                    val items = remember(todoMeta) { parseTodoItems(todoMeta) }
                    if (items.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            items.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onToggleTodoItem(envelope.id, index, !item.done)
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = item.done,
                                        onCheckedChange = { checked ->
                                            onToggleTodoItem(envelope.id, index, checked)
                                        }
                                    )
                                    Text(
                                        text = item.text,
                                        style = TextStyle(
                                            fontFamily = OrbitType.QuietAlmanac.bodySans,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            color = if (item.done) {
                                                QuietDiaryColors.CreamFaint
                                            } else {
                                                QuietDiaryColors.CreamDim
                                            },
                                            letterSpacing = 0.sp,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }

                if (envelope.shouldShowHydrationRetry()) {
                    MonoLabel(
                        text = "Link not enriched yet / tap to retry",
                        color = QuietDiaryColors.CreamDim,
                        modifier = Modifier.clickable { onRetry(envelope.id) },
                    )
                }

                AnimatedVisibility(visible = pickerOpen) {
                    IntentChipPicker(
                        currentIntent = envelope.intent.toIntentOrAmbiguous(),
                        onPick = { picked ->
                            pickerOpen = false
                            onReassign(envelope.id, picked)
                        }
                    )
                }
            }
            IconButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete capture",
                    tint = QuietDiaryColors.CreamDim,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
    QuietRule()
}

@Composable
private fun QuietMiniIntent(
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
        MonoLabel(text = label, color = QuietDiaryColors.CreamDim, size = 9.sp)
    }
}

@Composable
private fun QuietRule() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(QuietDiaryColors.Rule),
    )
}

// ----------------------------------------------------------------------
// Phase 11 Block 9 / T149 — ClusterCardModel → ClusterSuggestionCardState
// ----------------------------------------------------------------------
//
// Bare-minimum projection. Block 6 (ClusterSummariser) will replace the
// placeholder bodyText / bullets / sourceCategories once Nano summary
// output is wired. Until then the card surfaces honestly: it shows the
// cluster exists and how many captures fed it, without inventing a
// summary it can't yet produce.
//
// Mapping:
//   SURFACED, TAPPED      → Surfaced(...)
//   ACTING                → Acting(...)
//   ACTED                 → null  (Block 6 supplies bullets; render
//                                  nothing rather than fabricate them)
//   FAILED                → Failed(retryCount=0)
//   FORMING/DISMISSED/AGED_OUT → null  (DAO already excludes these but
//                                       the case must be exhaustive)
//
// `headerLabel`, `timeRangeLabel`, `sourceCategories` are pre-formatted
// here so the card stays locale-agnostic.
private fun ClusterCardModel.toCardStateOrNull(): ClusterSuggestionCardState? {
    val header = "Cluster · ${members.size} captures"
    val timeRange = formatBucketRange(timeBucketStart, timeBucketEnd)
    val sources = emptyList<String>() // Block 6 fills from envelope appCategory join.
    val placeholderBody = "${members.size} captures across this period."

    return when (state) {
        ClusterState.SURFACED, ClusterState.TAPPED -> ClusterSuggestionCardState.Surfaced(
            headerLabel = header,
            timeRangeLabel = timeRange,
            sourceCategories = sources,
            bodyText = placeholderBody,
        )
        ClusterState.ACTING -> ClusterSuggestionCardState.Acting(
            headerLabel = header,
            timeRangeLabel = timeRange,
            sourceCategories = sources,
            bodyText = placeholderBody,
        )
        ClusterState.FAILED -> ClusterSuggestionCardState.Failed(
            headerLabel = header,
            timeRangeLabel = timeRange,
            sourceCategories = sources,
            retryCount = 0,
        )
        ClusterState.ACTED,
        ClusterState.FORMING,
        ClusterState.DISMISSED,
        ClusterState.AGED_OUT -> null
    }
}

private val bucketRangeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE h:mma", Locale.US)

private val diaryHeaderFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE · MMM d", Locale.US)

private val diaryDayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE · MMM d", Locale.US)

private val quietTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mma", Locale.US)

private fun DayUiState.diaryHeaderDateLabel(): String = when (this) {
    is DayUiState.Ready -> isoDate.toQuietHeaderLabel()
    is DayUiState.Empty -> isoDate.toQuietHeaderLabel()
    is DayUiState.Error -> isoDate.toQuietHeaderLabel()
    is DayUiState.Loading -> isoDate.toQuietHeaderLabel()
}

private fun String.toQuietHeaderLabel(): String = runCatching {
    java.time.LocalDate.parse(this).format(diaryHeaderFormatter)
}.getOrDefault(this)

private fun String.toQuietDayLabel(): String = runCatching {
    java.time.LocalDate.parse(this).format(diaryDayFormatter)
}.getOrDefault(this)

private fun Long.toQuietTimeLabel(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(quietTimeFormatter)
        .lowercase(Locale.US)

private fun EnvelopeViewParcel.toSourceGlyphKind(): SourceGlyphKind =
    SourceIdentityResolver.glyphKind(
        textContent = textContent,
        canonicalUrl = canonicalUrl,
        sourceAppLabel = sourceAppLabel,
        appCategory = appCategory,
    )

private fun String.toQuietIntentLabel(): String = when (this) {
    "WANT_IT" -> "want it"
    "INTERESTING" -> "interesting"
    "REFERENCE" -> "reference"
    "READ_LATER" -> "read later"
    "FOR_SOMEONE" -> "for someone"
    else -> "unassigned"
}

private fun String.toQuietIntentColor(): Color = when (this) {
    "WANT_IT" -> QuietDiaryColors.Accent
    "INTERESTING" -> Color(0xFFC8A4DC)
    "REFERENCE" -> Color(0xFFA4C8A4)
    "READ_LATER" -> Color(0xFFDCC384)
    "FOR_SOMEONE" -> Color(0xFF84B8D6)
    else -> QuietDiaryColors.Cream
}

private fun String.toIntentOrAmbiguous(): Intent =
    runCatching { Intent.valueOf(this) }.getOrElse { Intent.AMBIGUOUS }

private fun EnvelopeViewParcel.shouldShowHydrationRetry(): Boolean =
    quietUrlPresenceRegex.containsMatchIn(textContent.orEmpty()) &&
        title.isNullOrBlank() &&
        summary.isNullOrBlank() &&
        domain.isNullOrBlank()

private val quietUrlPresenceRegex = Regex(
    """https?://[A-Za-z0-9._~:/?#\[\]@!${'$'}&'()*+,;=%\-]+""",
    RegexOption.IGNORE_CASE,
)

private object QuietDiaryColors {
    val BgDeep = Color(0xFF080B14)
    val Cream = Color(0xFFF3EAD8)
    val CreamDim = Color(0x8CF3EAD8)
    val CreamFaint = Color(0x38F3EAD8)
    val Rule = Color(0x1AF3EAD8)
    val Accent = Color(0xFFE8B06A)
}

private fun formatBucketRange(startMillis: Long, endMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(startMillis).atZone(zone).format(bucketRangeFormatter)
    val end = Instant.ofEpochMilli(endMillis).atZone(zone).format(bucketRangeFormatter)
    return "$start → $end"
}

/**
 * Phase 11 Block 11 / T150-revisit — stable Compose `testTag` seams for
 * the diary placement-contract test. Visible at `internal` so the
 * androidTest source set can reference the same constants the
 * production composable applies; never read from the user UI.
 */
internal object DiaryScreenTestTags {
    const val CLUSTER_SLOT = "diary-cluster-slot"
    const val DAY_HEADER = "diary-day-header"
    const val ACTIVE_INTENT_PANEL = "diary-active-intent-panel"
    const val ACTIVE_INTENT_TOGGLE = "diary-active-intent-toggle"

    fun activeIntentGroup(lifecycleStatus: String, category: String): String =
        "diary-active-intent-group-$lifecycleStatus-$category"

    fun activeIntentItem(intentId: String): String = "diary-active-intent-item-$intentId"
    fun activeIntentFilter(filterName: String): String = "diary-active-intent-filter-$filterName"
    fun activeIntentOpenCapture(intentId: String): String = "diary-active-intent-open-capture-$intentId"
    fun activeIntentAddContext(intentId: String): String = "diary-active-intent-add-context-$intentId"
    fun activeIntentResolve(intentId: String): String = "diary-active-intent-resolve-$intentId"
    fun activeIntentArchive(intentId: String): String = "diary-active-intent-archive-$intentId"
    fun activeIntentEscalate(intentId: String): String = "diary-active-intent-escalate-$intentId"
    fun activeIntentDecisionDialog(intentId: String): String = "diary-active-intent-decision-dialog-$intentId"
    fun activeIntentDecisionOpenCapture(intentId: String): String = "diary-active-intent-decision-open-capture-$intentId"
    fun activeIntentDecisionAddContext(intentId: String): String = "diary-active-intent-decision-add-context-$intentId"
    fun activeIntentDecisionReview(intentId: String): String = "diary-active-intent-decision-review-$intentId"
    fun activeIntentDecisionResolve(intentId: String): String = "diary-active-intent-decision-resolve-$intentId"
    fun activeIntentDecisionArchive(intentId: String): String = "diary-active-intent-decision-archive-$intentId"
}
