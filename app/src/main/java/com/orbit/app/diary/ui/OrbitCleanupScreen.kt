package com.orbit.app.diary.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbit.app.ui.primitives.AgentActionRow
import com.orbit.app.ui.primitives.AgentCardBody
import com.orbit.app.ui.primitives.AgentCardMeta
import com.orbit.app.ui.primitives.AgentCardTitle
import com.orbit.app.ui.primitives.AgentPrimaryAction
import com.orbit.app.ui.primitives.AgentSecondaryAction
import com.orbit.app.ui.primitives.AgentSectionHeader
import com.orbit.app.ui.primitives.AgentSurface
import com.orbit.app.ui.primitives.SurfacedCard
import com.orbit.app.ui.primitives.MonoLabel
import com.orbit.app.ui.tokens.OrbitType
import com.orbit.app.data.ipc.ActionDraftParcel
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.orbit.app.curious.CuriousQuestionCandidate
import com.orbit.app.curious.CuriousQuestionChoice
import com.orbit.app.data.ipc.AgentEvidenceParcel
import com.orbit.app.data.ipc.AgentPlanParcel
import com.orbit.app.data.ipc.MemoryCandidateParcel
import com.orbit.app.diary.ActionPreviewSheet
import com.orbit.app.diary.ActiveIntentUiState
import com.orbit.app.diary.DiaryViewModel
import com.orbit.app.diary.EnvelopeDetailActivity
import com.orbit.app.generativeui.OrbitAgentUiRenderer
import com.orbit.app.generativeui.toOrbitAgentUiDocument
import com.orbit.app.memory.SourceAppLabelDisplay
import com.orbit.app.orbit.AskOrbitViewModel
import com.orbit.app.orbit.ui.AskOrbitPanel
import com.orbit.app.understanding.domain.ResolutionReason
import androidx.compose.ui.platform.LocalContext

@Composable
fun OrbitCleanupScreen(
    viewModel: DiaryViewModel,
    askOrbitViewModel: AskOrbitViewModel? = null,
    onOpenCapture: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.activeIntentState.collectAsState()
    val actionDrafts by viewModel.observeActionDrafts().collectAsState(initial = emptyList())
    val memoryCandidates by viewModel.observeMemoryCandidates().collectAsState(initial = emptyList())
    val curiousQuestions by viewModel.curiousQuestions.collectAsState()
    val actionNotice by viewModel.actionNotice.collectAsState()
    val agentPlanState by viewModel.agentPlanState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var pendingActionDraft by remember { mutableStateOf<ActionDraftParcel?>(null) }
    var editingMemoryCandidate by remember { mutableStateOf<MemoryCandidateParcel?>(null) }
    LaunchedEffect(actionNotice?.id) {
        val notice = actionNotice ?: return@LaunchedEffect
        Toast.makeText(context, notice.message, Toast.LENGTH_LONG).show()
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (askOrbitViewModel != null && onOpenCapture != null) {
            AskOrbitPanel(
                viewModel = askOrbitViewModel,
                onOpenCapture = onOpenCapture,
            )
        }
        actionNotice?.let { notice ->
            ActionNoticePanel(
                message = notice.message,
                onDismiss = { viewModel.onActionNoticeDismissed() },
            )
        }
        AgentPlanPanel(
            state = agentPlanState,
            onPlan = viewModel::onPlanAgentRequest,
            onOpenEvidence = { evidence ->
                if (evidence.sourceType == "ENVELOPE") {
                    onOpenCapture?.invoke(evidence.sourceId)
                        ?: context.startActivity(
                            EnvelopeDetailActivity.newIntent(
                                context,
                                evidence.sourceId,
                                dayLocal = null,
                            )
                        )
                }
            },
        )
        CuriousQuestionsPanel(
            questions = curiousQuestions,
            onAnswer = { q, choice -> viewModel.onAnswerCuriousQuestion(q.id, choice) },
            onDismiss = { q -> viewModel.onDismissCuriousQuestion(q.id) },
        )
        ActionDraftsPanel(
            drafts = actionDrafts,
            onReview = { pendingActionDraft = it },
            onDismiss = { viewModel.onDismissProposal(it.proposalId) },
            onOpenCapture = { draft ->
                onOpenCapture?.invoke(draft.sourceEnvelopeId)
                    ?: context.startActivity(
                        EnvelopeDetailActivity.newIntent(
                            context,
                            draft.sourceEnvelopeId,
                            dayLocal = null,
                        )
                    )
            },
        )
        MemoryReviewPanel(
            candidates = memoryCandidates,
            onAccept = { viewModel.onAcceptMemoryCandidate(it.candidateId) },
            onReject = { viewModel.onRejectMemoryCandidate(it.candidateId, "user_rejected") },
            onEdit = { editingMemoryCandidate = it },
            onOpenCapture = { candidate ->
                candidate.primarySourceEnvelopeId?.let { sourceId ->
                    onOpenCapture?.invoke(sourceId)
                        ?: context.startActivity(
                            EnvelopeDetailActivity.newIntent(
                                context,
                                sourceId,
                                dayLocal = null,
                            )
                        )
                }
            },
        )
        if (state is ActiveIntentUiState.Ready) {
            ActiveIntentCleanupPanel(
                state = state,
                onResolve = { item ->
                    viewModel.onResolveActiveIntent(item.intentId, item.defaultResolutionReason)
                },
                onArchive = { item ->
                    viewModel.onResolveActiveIntent(item.intentId, ResolutionReason.USER_ARCHIVED)
                },
                onOpenCapture = { item ->
                    context.startActivity(
                        EnvelopeDetailActivity.newIntent(context, item.captureId, dayLocal = null)
                    )
                },
                onAddContext = { item ->
                    context.startActivity(
                        EnvelopeDetailActivity.newIntent(
                            context,
                            item.captureId,
                            dayLocal = null,
                            startNote = true,
                        )
                    )
                },
                onNotNow = { item ->
                    viewModel.onActiveIntentNotNow(item.intentId)
                },
                onSnooze = { item ->
                    viewModel.onSnoozeActiveIntentTomorrow(item.intentId)
                },
                onEscalate = { item ->
                    viewModel.onRequestActiveIntentEscalation(item.intentId)
                    // "Ask Orbit" currently just flags the item for a closer
                    // look (audit-only; the deep-reasoning agent that answers a
                    // brief isn't wired yet). Say what actually happens.
                    Toast.makeText(context, "Flagged for Orbit to review", Toast.LENGTH_SHORT).show()
                },
            )
        } else {
            Text(
                text = when (val s = state) {
                    ActiveIntentUiState.Loading -> "Loading Orbit"
                    ActiveIntentUiState.Empty -> "No active cleanup"
                    is ActiveIntentUiState.Error -> s.message
                    is ActiveIntentUiState.Ready -> ""
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    pendingActionDraft?.let { draft ->
        val proposal = draft.toProposalParcel()
        ActionPreviewSheet(
            proposal = proposal,
            onConfirm = { editedArgsJson ->
                viewModel.onConfirmProposal(proposal, editedArgsJson)
                pendingActionDraft = null
            },
            onDismiss = {
                viewModel.onDismissProposal(draft.proposalId)
                pendingActionDraft = null
            },
        )
    }

    editingMemoryCandidate?.let { candidate ->
        MemoryCandidateEditDialog(
            candidate = candidate,
            onDismiss = { editingMemoryCandidate = null },
            onAccept = { label, fact ->
                viewModel.onAcceptMemoryCandidate(candidate.candidateId, label, fact)
                editingMemoryCandidate = null
            },
        )
    }
}

@Composable
private fun AgentPlanPanel(
    state: DiaryViewModel.AgentPlanUiState,
    onPlan: (String) -> Unit,
    onOpenEvidence: (AgentEvidenceParcel) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val c = com.orbit.app.ui.tokens.OrbitPalette.current(dark = isSystemInDarkTheme())
    val isLoading = state is DiaryViewModel.AgentPlanUiState.Loading
    val canPlan = !isLoading && query.isNotBlank()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("agent-plan-panel"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        com.orbit.app.ui.primitives.MonoLabel(
            text = "// Agent plan",
            color = c.inkFaint,
            size = 9.5.sp,
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("agent-plan-query"),
            placeholder = {
                Text(
                    "What loop should Orbit help close?",
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = com.orbit.app.ui.tokens.OrbitType.QuietAlmanac.bodySans,
                        fontSize = 14.sp,
                    ),
                )
            },
            singleLine = false,
            minLines = 2,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = com.orbit.app.ui.tokens.OrbitType.QuietAlmanac.bodySans,
                fontSize = 14.sp,
                color = c.ink,
            ),
            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (canPlan) onPlan(query) }),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.brandAccent,
                unfocusedBorderColor = c.rule,
                focusedContainerColor = c.paper,
                unfocusedContainerColor = c.paper,
                cursorColor = c.brandAccent,
                focusedPlaceholderColor = c.inkFaint,
                unfocusedPlaceholderColor = c.inkFaint,
            ),
        )
        // Typographic submit affordance (design.md §2 #3 — no Material button).
        Text(
            text = if (isLoading) "Planning…" else "Plan",
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .then(if (canPlan) Modifier.clickable { onPlan(query) } else Modifier)
                .background(if (canPlan) c.brandAccentDim else c.rule.copy(alpha = 0.4f))
                .padding(horizontal = 22.dp, vertical = 12.dp)
                .testTag("agent-plan-submit"),
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = com.orbit.app.ui.tokens.OrbitType.QuietAlmanac.bodySans,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (canPlan) c.brandAccent else c.inkFaint,
            ),
        )
        when (state) {
            DiaryViewModel.AgentPlanUiState.Idle -> Unit
            DiaryViewModel.AgentPlanUiState.Loading -> Text(
                text = "Finding saved evidence",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            is DiaryViewModel.AgentPlanUiState.Error -> Text(
                text = state.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
            is DiaryViewModel.AgentPlanUiState.Ready -> AgentPlanResult(
                plan = state.plan,
                onOpenEvidence = onOpenEvidence,
            )
        }
    }
}

@Composable
private fun AgentPlanResult(
    plan: AgentPlanParcel,
    onOpenEvidence: (AgentEvidenceParcel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val document = remember(plan) { plan.toOrbitAgentUiDocument() }
    val evidenceById = remember(plan.evidence) { plan.evidence.associateBy { it.evidenceId } }
    OrbitAgentUiRenderer(
        document = document,
        onOpenEvidence = { evidence ->
            evidenceById[evidence.id]?.let(onOpenEvidence)
        },
        modifier = modifier.testTag("agent-plan-result"),
    )
}

@Composable
private fun AgentEvidenceRow(
    evidence: AgentEvidenceParcel,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = evidence.label,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
            evidence.dayLocal?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        if (evidence.sourceType == "ENVELOPE") {
            TextButton(onClick = onOpen) {
                Text("Open")
            }
        }
    }
}

@Composable
private fun ActionNoticePanel(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Quiet confirmation, not an error-red alarm — a benign "saved" notice.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(AgentSurface.Accent.copy(alpha = 0.14f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(AgentSurface.Accent),
        )
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            color = AgentSurface.Cream,
            style = TextStyle(fontFamily = OrbitType.QuietAlmanac.bodySans, fontSize = 13.sp),
        )
        AgentSecondaryAction("Dismiss", onDismiss)
    }
}

@Composable
private fun ActionDraftsPanel(
    drafts: List<ActionDraftParcel>,
    onReview: (ActionDraftParcel) -> Unit,
    onDismiss: (ActionDraftParcel) -> Unit,
    onOpenCapture: (ActionDraftParcel) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (drafts.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AgentSectionHeader(kicker = "// ACTION DRAFTS", title = "Ready to act")
        drafts.forEach { draft ->
            ActionDraftCard(
                draft = draft,
                onReview = { onReview(draft) },
                onDismiss = { onDismiss(draft) },
                onOpenCapture = { onOpenCapture(draft) },
            )
        }
    }
}

@Composable
private fun ActionDraftCard(
    draft: ActionDraftParcel,
    onReview: () -> Unit,
    onDismiss: () -> Unit,
    onOpenCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sourceApp = SourceAppLabelDisplay.userFacingOrNull(draft.sourceAppLabel)
    val sourceLine = listOfNotNull(draft.displayName, sourceApp?.let { "from $it" }, draft.sourceDayLocal)
        .joinToString("  ·  ")
    SurfacedCard(modifier = modifier) {
        AgentCardTitle(draft.previewTitle)
        draft.previewSubtitle?.takeIf { it.isNotBlank() }?.let { AgentCardBody(it) }
        draft.sourceTitle.takeIf { it.isNotBlank() }?.let { AgentCardBody(it, dim = true) }
        if (sourceLine.isNotBlank()) AgentCardMeta(sourceLine)
        AgentActionRow {
            AgentSecondaryAction("Open", onOpenCapture)
            AgentSecondaryAction("Dismiss", onDismiss)
            AgentPrimaryAction("Review", onReview)
        }
    }
}

/**
 * Spec 020 — the Curious Agent surfacing. When Orbit has repeated evidence
 * around a topic, it asks one sparse, cited, dismissible question to fill a
 * knowledge gap. Answers/dismissals retire the question; a choice records a
 * user-authored signal (provenance-backed KG write is S2b).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CuriousQuestionsPanel(
    questions: List<CuriousQuestionCandidate>,
    onAnswer: (CuriousQuestionCandidate, CuriousQuestionChoice) -> Unit,
    onDismiss: (CuriousQuestionCandidate) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (questions.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth().testTag("curious-questions-panel"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AgentSectionHeader(kicker = "// ORBIT IS CURIOUS", title = "A quick question")
        questions.forEach { q ->
            SurfacedCard {
                AgentCardTitle(q.questionText)
                AgentCardMeta("Based on ${q.sourceRefs.size} related saves")
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    q.choices.forEach { choice ->
                        CuriousChoiceChip(label = choice.label, onClick = { onAnswer(q, choice) })
                    }
                }
                AgentActionRow {
                    AgentSecondaryAction("Not sure", { onDismiss(q) })
                }
            }
        }
    }
}

@Composable
private fun CuriousChoiceChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AgentSurface.Accent.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            text = label,
            color = AgentSurface.Accent,
            style = TextStyle(fontFamily = OrbitType.QuietAlmanac.bodySans, fontSize = 13.sp),
        )
    }
}

@Composable
private fun MemoryReviewPanel(
    candidates: List<MemoryCandidateParcel>,
    onAccept: (MemoryCandidateParcel) -> Unit,
    onReject: (MemoryCandidateParcel) -> Unit,
    onEdit: (MemoryCandidateParcel) -> Unit,
    onOpenCapture: (MemoryCandidateParcel) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (candidates.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(DiaryScreenTestTags.MEMORY_REVIEW_PANEL),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AgentSectionHeader(kicker = "// MEMORY REVIEW", title = "What Orbit is learning")
        candidates.forEach { candidate ->
            MemoryCandidateCard(
                candidate = candidate,
                onAccept = { onAccept(candidate) },
                onReject = { onReject(candidate) },
                onEdit = { onEdit(candidate) },
                onOpenCapture = { onOpenCapture(candidate) },
            )
        }
    }
}

@Composable
private fun MemoryCandidateCard(
    candidate: MemoryCandidateParcel,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onEdit: () -> Unit,
    onOpenCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sensitivityCopy = when (candidate.sensitivity) {
        "LOCAL_ONLY" -> "Local only"
        "SENSITIVE" -> "Sensitive"
        else -> candidate.confidenceLabel.replaceFirstChar { it.uppercaseChar() }
    }
    val sourceLine = buildString {
        append(candidate.candidateKind.lowercase().replace('_', ' '))
        append("  ·  ").append(sensitivityCopy)
        append("  ·  ${candidate.sourceCount.coerceAtLeast(0)} source")
        if (candidate.sourceCount != 1) append("s")
        candidate.primarySourceDayLocal?.let { append("  ·  ").append(it) }
    }
    SurfacedCard(
        modifier = modifier.testTag(DiaryScreenTestTags.memoryCandidateCard(candidate.candidateId)),
    ) {
        AgentCardTitle(candidate.displayLabel)
        // Lead with the ask (the question), then the fact it's proposing.
        candidate.askUserCopy?.takeIf { it.isNotBlank() && it != candidate.factText }?.let {
            AgentCardBody(it)
        }
        AgentCardBody(candidate.factText, dim = true)
        candidate.primarySourceTitle?.takeIf { it.isNotBlank() }?.let { AgentCardBody(it, dim = true) }
        AgentCardMeta(sourceLine)
        AgentActionRow {
            if (candidate.primarySourceEnvelopeId != null) {
                AgentSecondaryAction(
                    "Open", onOpenCapture,
                    testTag = DiaryScreenTestTags.memoryCandidateOpenCapture(candidate.candidateId),
                )
            }
            AgentSecondaryAction(
                "Reject", onReject,
                testTag = DiaryScreenTestTags.memoryCandidateReject(candidate.candidateId),
            )
            AgentSecondaryAction(
                "Edit", onEdit,
                testTag = DiaryScreenTestTags.memoryCandidateEdit(candidate.candidateId),
            )
            AgentPrimaryAction(
                "Accept", onAccept,
                testTag = DiaryScreenTestTags.memoryCandidateAccept(candidate.candidateId),
            )
        }
    }
}


@Composable
private fun MemoryCandidateEditDialog(
    candidate: MemoryCandidateParcel,
    onDismiss: () -> Unit,
    onAccept: (String, String) -> Unit,
) {
    var label by remember(candidate.candidateId) { mutableStateOf(candidate.displayLabel) }
    var fact by remember(candidate.candidateId) { mutableStateOf(candidate.factText) }
    val labelValid = label.trim().isNotEmpty() && label.length <= 200
    val factValid = fact.trim().isNotEmpty() && fact.length <= 300
    AlertDialog(
        modifier = Modifier.testTag(DiaryScreenTestTags.MEMORY_CANDIDATE_EDIT_DIALOG),
        onDismissRequest = onDismiss,
        title = { Text("Edit memory") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    singleLine = false,
                    isError = !labelValid,
                    modifier = Modifier.testTag(DiaryScreenTestTags.MEMORY_CANDIDATE_EDIT_LABEL),
                )
                OutlinedTextField(
                    value = fact,
                    onValueChange = { fact = it },
                    label = { Text("Memory") },
                    singleLine = false,
                    isError = !factValid,
                    modifier = Modifier.testTag(DiaryScreenTestTags.MEMORY_CANDIDATE_EDIT_FACT),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAccept(label.trim(), fact.trim()) },
                enabled = labelValid && factValid,
                modifier = Modifier.testTag(DiaryScreenTestTags.MEMORY_CANDIDATE_EDIT_SAVE),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
