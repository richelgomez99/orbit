# Orbit MVP to Vision Execution Plan - 2026-06-02

## Product Thesis

Orbit lets you capture something from your phone together with the intention you had for why you captured it. That paired artifact plus intent becomes context for a personal knowledge graph and an agent that helps you close loops.

The important shift is that Orbit is not only another place where the user starts an AI conversation and manually provides context. Orbit already has the context because the user captured attention in the moment. The agent can start from that memory, ask lightweight questions to fill gaps, and help the user complete the implied loop: find time for a task, draft a message, create a grocery list from a recipe, add an event, compare saved options, or decide something is not relevant anymore.

## Current MVP Truth

The current MVP proves the first memory loop:

1. Capture or seed saved moments.
2. Show them in Diary as chronological memory.
3. Search them in Library.
4. Ask Orbit for cited local answers.
5. Surface Follow-ups in Orbit without turning Diary into a task queue.
6. Open every answer/result back to the original capture.

This is not yet the full agentic product. The current APK does not ship the full knowledge graph, autonomous planning, A2UI, BYOM local model manager, curious profiling, or external action execution. Those remain roadmap specs.

## Operating Rule

Every phase must validate the previous layer before adding the next feature layer.

- Do not add agent autonomy before cited retrieval is stable.
- Build product surfaces first, but add a narrow model-enablement follow-up whenever a surface needs semantic/model behavior to be genuinely useful.
- Do not add external writes before approval boundaries are clear; Spec 006 may add user-approved drafts before full resolution semantics.
- Do not add a knowledge graph backend before capture context and provenance are reliable.
- Do not add generative UI before the agent has trustworthy structured outputs.
- Do not let the roadmap claim shipped capabilities that are still only vision.

## Execution Order

### Phase 0 - Lock Spec 005 MVP

Branch: `feature/005-atlas-memory-index-search-20260530`

Goal: close the current MVP branch with evidence.

Tasks:

- Finish `T005-061`: demo Ask Orbit for at least 3 cited answers and 1 refusal.
- Run backend and Android gates.
- Run one scripted S24 demo pass.
- Update `quickstart.md`, `tasks.md`, and `CODEX_HANDOFF.md`.
- Record known limits: Ask is cited retrieval, not full agent reasoning; Atlas is compact index/search, not source of truth.

Exit criteria:

- Diary, Library, Orbit tabs work without crash in the scripted demo.
- Library search finds context-note captures such as `qr code`.
- Ask is documented as a limited local cited retrieval preview. It answers only from citations or refuses, but it is not yet semantic/vector/LLM Ask.
- Follow-ups are Orbit-only, collapsible, scrollable, and not duplicated.

### Phase 1 - Semantic Retrieval And Grounded Ask

Spec/branch: `005A-semantic-retrieval-grounded-ask` / `feature/005a-semantic-retrieval-grounded-ask-...`

Goal: turn the Spec 005 Ask/Library surfaces from deterministic token retrieval into a reliable semantic retrieval layer with grounded answer behavior.

Why next:

Screenshots from the Spec 005 APK showed that lexical matching can find obvious Library items, but it is not enough for trustworthy Ask. It ranked weak keyword matches for startup, flight, and unsupported passport-number questions. Before Orbit starts drafting actions or plans, retrieval must understand meaning, rank evidence correctly, and refuse unsupported questions.

Scope:

- Lock embedding provider, model, dimensions, consent category, and payload caps.
- Generate compact embeddings for indexed memory records without storing raw screenshots/full OCR in Atlas.
- Add Atlas Vector Search index and user-scoped vector search endpoint.
- Add hybrid lexical + vector retrieval with deterministic local fallback.
- Add grounded Ask answer/refusal policy over retrieved citations.
- Add evaluation fixtures from current demo/screenshot failures: startup event, flight receipt, recipe, qr code, reschedule/rescheduling, cancelled/rescheduled, unsupported passport number.

Validation:

- `startup event` returns the actual event-related capture above restaurant noise.
- `flight receipt` ranks the receipt before adjacent travel/family captures.
- `qr code` works from user-provided context when OCR lacks the phrase.
- `passport number` refuses unless a real cited saved capture supports it.
- Disabling cloud/embeddings degrades to local cited retrieval without breaking Diary, Library, or Orbit.

### Phase 2 - Approval Action Runtime

Spec/branch: `006-approval-action-runtime` / `feature/006-approval-action-runtime-...`

Goal: make Orbit help close loops with user-approved actions.

Why next:

The full vision is about closing the loops started by captures. After Spec 005A makes retrieval semantically reliable, the next useful branch is concrete, user-approved action drafts rather than broad autonomous agency.

Scope:

- Action Drafts for calendar events, todos, reminders, message drafts, grocery lists.
- Preview before execution.
- User confirmation required before writing to external apps.
- Receipts, audit rows, undo/retry where possible.

Validation:

- "Create grocery list from this recipe" produces a draft list.
- "Draft a reply" produces a draft message but does not send.
- "Add this event" opens/prepares a calendar action with confirmation.
- Rejections improve future suggestions.

### Phase 3 - Memory Candidates Inspector

Spec/branch: `007-memory-candidates-inspector` / `feature/007-memory-candidates-inspector-...`

Goal: let users inspect, accept, reject, and correct candidate facts/entities/patterns before they become durable memory.

Why here:

The "private picture of who you are" must be user-visible, editable, and provenance-backed before Orbit starts acting on it.

Validation:

- Candidate memories cite their source captures or user confirmations.
- User acceptance/rejection/correction is recorded.
- Nothing becomes a profile fact silently.

### Phase 4 - Cloud Controls And Storage Budgeting

Spec/branch: `008-cloud-controls-storage-budgeting` / `feature/008-cloud-controls-storage-budgeting-...`

Goal: make cloud/index/LLM use visible, reversible, and budgeted.

Why here:

Deeper agent and retrieval work must preserve the local-first promise. Users need to understand and control what cloud augmentation is doing.

Validation:

- User can inspect cloud memory/index state.
- User can disable indexing or Ask.
- Audit records stay bounded and exclude raw private content.

### Phase 5 - Knowledge Graph Foundation

Spec/branch: `009-kg-backend-poc` / `feature/009-kg-backend-poc-...`

Goal: turn captured context into inspectable, provenance-backed graph memory.

Why here:

The agent should reason over entities, relationships, profile facts, patterns, and open loops only after candidate memories and cloud controls exist.

Canonical direction:

- The phone remains the source of truth.
- Room/SQLCipher stores canonical graph data.
- Atlas can mirror compact graph/index records for retrieval, but not raw memory or authoritative private facts.

Initial graph concepts:

- Entities: people, companies, projects, places, events, products, topics, tasks.
- Mentions: where an entity appears in captures, notes, asks, or actions.
- Relationships: related-to, for-person, part-of-project, mentions, contradicts, resolved-by.
- Profile facts: explicit or inferred user facts, always editable and provenance-backed.
- Patterns: recurring clusters or behaviors with confidence and evidence.

Validation:

- Every entity/fact/relationship cites source captures or user confirmation.
- The user can inspect, correct, dismiss, or delete memory candidates.
- The agent can answer "why this?" for surfaced relationships.

### Phase 6 - Agent Coordinator

Spec/branch: `010-agent-coordinator` / `feature/010-agent-coordinator-...`

Goal: move from cited Ask to an approval-first agent.

Scope:

- Agent plans over captures, KG facts, resolution state, and action tools.
- Agent asks clarification questions when context is missing.
- Agent never silently writes externally.
- Agent produces proposed steps and waits for confirmation.

Validation:

- The agent can start from a captured memory and ask a useful gap-filling question.
- The agent can propose how to close a loop and cite the relevant captures.
- The user can approve, reject, edit, or mark not-now.

### Phase 7 - Manual Compose And Capture Context

Spec/branch: `011-manual-compose-capture-context` / `feature/011-manual-compose-capture-context-...`

Goal: add deliberate/manual memory creation and pull near-term Clarify/context capture into the numbered mainline.

Why here:

The QR-code bug proved that user-supplied context is higher signal than OCR alone. Instead of jumping from Spec 005 to Spec 018, this branch preserves the `006-012` progression while making "capture + intention" first-class.

Scope:

- Add manual compose/backfill for memories that are not screenshots.
- Add or refine a lightweight capture-context affordance for short reasons such as "QR code for check-in" or "make grocery list from this".
- Persist context as note/provenance tied to the envelope.
- Ensure Diary, Library, Ask, compact index sync, and title generation use the context.

Validation:

- Capture or compose an item with context.
- Search by context words.
- Ask a cited question grounded in context.
- Confirm the result opens the original capture.

### Phase 8 - Resolution Semantics

Spec/branch: `012-resolution-semantics` / `feature/012-resolution-semantics-...`

Goal: define what it means for a memory loop to be open, duplicate, stale, dismissed, done, not-now, or merged.

Why here:

After actions, KG foundation, agent coordination, and capture context exist, Orbit needs durable semantics for whether a loop is still alive or resolved.

Scope:

- Canonical duplicate model: `duplicateOf`, `seenCount`, `lastSeenAt`.
- Resolution state: open, done, dismissed, snoozed, not-now, stale, duplicate.
- Conflict/stale fact semantics.
- Provenance for resolution decisions.

Validation:

- Recapturing the same thing updates duplicate metadata instead of creating repeated work.
- Dismissals and not-now choices affect future Follow-ups and Ask responses.
- Every resolution can explain which capture/user action produced it.

### Phase 9 - Capture Context Affordance Vision Slot

Spec/branch: `018-capture-context-affordance` / `feature/018-capture-context-affordance-...`

Goal: preserve the May 22 capture-context roadmap slot without forcing a premature jump from Spec 005 to Spec 018.

Validation:

- If Spec 011 absorbs all near-term capture-context work, close this as absorbed/no-op with notes.
- If overlay affordance polish remains, scope this branch only to that remaining UX.

### Phase 10 - Formal Three-Pillar IA

Spec/branch: `019-agent-workspace-ia` / `feature/019-agent-workspace-ia-...`

Goal: replace the MVP shell with the durable product architecture.

Scope:

- Diary = pure chronological memory.
- Library = retrieval, filters, related captures.
- Orbit = Ask, Follow-ups, action drafts, chat/workbench sessions.
- Navigation state and reset behavior are intentional, not incidental.

Validation:

- User can move between all pillars without stale search/chat state leaking where it should not.
- Each pillar has a clear job and no queue pressure appears in Diary.

### Phase 11 - Curious Agent Profiling

Spec/branch: `020-curious-agent-profiling` / `feature/020-curious-agent-profiling-...`

Goal: let Orbit ask sparse, high-leverage questions to improve relevance.

Examples:

- "You saved several startup-event captures. Is this for attending, fundraising, hiring, or research?"
- "Should recipe captures become grocery-list candidates or long-term references?"
- "You saved this for later. Is later this week, someday, or only if it becomes relevant again?"

Validation:

- Questions appear only after enough evidence.
- Answers become editable profile facts with provenance.
- Dismissals and corrections reduce future noise.

### Phase 12 - Generative Native UI

Spec/branch: `021-generative-ui-runtime` / `feature/021-generative-ui-runtime-...`

Goal: make agent outputs render as native Orbit UI instead of blocks of text.

Scope:

- Declarative UI protocol adapter.
- Compose renderer for safe known components.
- Action cards, disambiguation pickers, draft previews, memory clusters, grocery lists.
- The LLM outputs data/intent, not arbitrary styling or executable code.

Validation:

- Same agent result can render as native card UI and degrade to text.
- UI remains Orbit's design language.
- No arbitrary code execution.

### Phase 13 - Local Model Manager

Spec/branch: `022-local-model-manager` / `feature/022-local-model-manager-...`

Goal: make local-first AI a real structural escape hatch beyond the current legacy Nano path.

Scope:

- BYOM/local model manager using LiteRT-LM or MLC LLM.
- Hardware capability checks.
- Speed model for extraction/basic understanding.
- Intelligence model for deeper offline chat and structured UI generation.
- Cloud gateway remains zero-download default.

Validation:

- Local mode can perform core extraction/retrieval support without network.
- Cloud changes quality/latency, not feature scope.
- Memory pressure is profiled before broad release.

## Pitch-Safe Language

Current shipped MVP:

> Orbit already proves the first loop: capture saved moments, preserve context, search them in Library, ask limited cited local questions, and manage follow-ups in a dedicated Orbit workspace.

Roadmap vision:

> Orbit becomes a proactive personal memory agent. Because it captures attention and intent at the source, it can start from real context, ask clarifying questions, and help close loops with user-approved actions.

Full vision:

> Orbit runs on-device, builds a private picture of who you are from what you save, and has an agent that helps you act on it. It is local-first: your data stays on your phone, and cloud or platform integrations augment the loop instead of owning it.

Google/AppFunctions bridge:

> Downstream, after approval to use the platform surface, AppFunctions could let Orbit feed private context to agents like Spark so they can finally understand what lives outside Google's own apps, without raw memory leaving the phone. But the core product is not Spark integration. The core product is closing the loops the user already started.

Memorable comparison:

> Spark knows everything in your Gmail. Orbit knows everything you screenshotted at 11pm and meant to deal with. One of those is where your real intentions live.

Hardware framing:

> Reach matters. Orbit should adapt to the hardware: use Nano-class system models when they are available, use a local BYOM/1-bit model path on older 4GB phones when possible, and fall back to the cloud gateway by policy. Most of the world is not on the newest flagship, so local-first cannot depend on only one premium-device path.

Founder/operator conversation turn:

> You work across founders building on Google, right? I am curious what you are seeing in the on-device and personal-agent space.

Do not claim yet:

- Full autonomous agent.
- Full knowledge graph.
- A2UI runtime.
- Local BYOM model manager.
- External write execution.
- Curious Agent profiling.

## Next Immediate Action

Finish Spec 005 validation honestly, then create Spec 005A before Spec 006:

1. Record Spec 005 as compact index + Library + limited local cited Ask preview.
2. Preserve the false-positive guardrails already added for token retrieval.
3. Create the `005A-semantic-retrieval-grounded-ask` spec branch.
4. Rerun `/speckit.specify`, `/speckit.plan`, and `/speckit.tasks` for 005A before implementation.
5. Start Spec 006 only after 005A validates semantic retrieval and grounded Ask.

Canonical queue:

- Use `docs/orbit-roadmap-queue-2026-06-02.md` as the source of truth for branch order.
- Use `docs/orbit-execution-playbook-2026-06-02.md` as the workflow guardrail for future sessions.
- Near-term capture-context work belongs in refreshed Spec 011, not a direct `005` -> `018` jump.
