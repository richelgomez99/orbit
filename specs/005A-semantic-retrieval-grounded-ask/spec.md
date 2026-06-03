# Feature Specification: Semantic Retrieval And Grounded Ask

**Feature Branch**: `feature/005a-semantic-retrieval-grounded-ask-20260603`
**Created**: 2026-06-03
**Status**: Draft - Speckit specify phase complete
**Input**: Continue after Spec 005 closeout. Spec 005 shipped compact Atlas memory index, Library search, and a limited local cited Ask preview, but screenshot testing showed token-only retrieval is not reliable enough for Ask.

## Summary

Orbit needs semantic retrieval before it can safely move into action drafting. Spec 005 proved the product surface: Diary, Library, Orbit, compact Atlas index, local-backed result opening, citations, Follow-ups, and basic false-positive guardrails. This follow-up upgrades retrieval quality without changing the product architecture.

The branch adds embeddings for compact memory records, a user-scoped Atlas Vector Search path, hybrid lexical + vector ranking, and a grounded Ask contract that answers only when retrieved evidence is strong enough. Room + SQLCipher remains authoritative. Atlas remains a compact cloud index. Android still never holds Atlas credentials or direct MongoDB drivers.

## Why This Is Next

The full Orbit vision depends on the agent closing loops from saved intention. That only works if the retrieval layer can reliably find the right saved memory and refuse unsupported questions. Real screenshots exposed failures that deterministic token matching cannot solve well enough:

- `startup event` can rank nearby restaurant or chat captures above the actual event capture.
- `flight receipt` can rank adjacent travel/family captures above the receipt.
- `passport number` can retrieve unrelated numbers instead of refusing.
- user-provided context such as `qr code` is high signal and must participate in retrieval.

This branch is the model-enablement lane for Spec 005. Spec 006 approval action runtime starts only after this branch validates semantic retrieval and grounded Ask.

## User Scenarios & Testing

### User Story 1 - Semantic Library Retrieval (Priority: P1)

As a user, I want Library search to find the saved thing I mean even when the exact words differ, so I can retrieve memories by intent and meaning rather than exact OCR text.

**Why this priority**: Library is the product's retrieval pillar. If retrieval is brittle, every later agent feature is built on weak evidence.

**Independent Test**: Seed or capture at least 20 local envelopes, sync embeddings, search `qr code`, `reschedule`, `rescheduling`, `startup event`, `flight receipt`, and `recipe`, and verify the expected local-backed capture appears in the top results with a source citation.

**Acceptance Scenarios**:

1. **Given** a screenshot has user context note "QR code for first 1000 customers", **When** the user searches `qr code`, **Then** Library ranks that capture above code-only OCR noise and opens local detail.
2. **Given** a saved cancellation/rescheduling capture, **When** the user searches `reschedule`, `rescheduling`, or `cancelled`, **Then** the same relevant memory appears without duplicate-looking rows.
3. **Given** Atlas vector search is unavailable, **When** the user searches Library, **Then** Orbit falls back to local cited retrieval and clearly avoids dead result links.

---

### User Story 2 - Grounded Ask Ranking And Refusal (Priority: P1)

As a user, I want Ask Orbit to answer only from saved evidence and refuse when evidence is weak, so I can trust it with personal memory questions.

**Why this priority**: Ask becomes dangerous if it confidently presents weak keyword matches. Grounded refusal is as important as better answers.

**Independent Test**: Run the screenshot-derived Ask fixture suite: `What startup event did I save?`, `Which flight receipt did I save recently?`, `What recipe did I want to try?`, `What is my passport number?`. The first three must cite the correct saved captures; the unsupported passport-number question must refuse unless a real saved passport capture exists.

**Acceptance Scenarios**:

1. **Given** the corpus contains startup-event, nearby restaurant, and event-adjacent chat captures, **When** the user asks about the startup event, **Then** the answer cites the actual event capture first.
2. **Given** the corpus contains flight receipt, family travel chat, and calendar conflict captures, **When** the user asks about the flight receipt, **Then** the answer cites the receipt first.
3. **Given** the corpus has unrelated numbers but no passport-number capture, **When** the user asks for a passport number, **Then** Ask refuses and may show no answer rather than inventing or surfacing unrelated numbers.

---

### User Story 3 - Compact Embedding Sync And Audit (Priority: P1)

As a privacy-conscious user, I want embeddings to be generated only from compact memory text and audited locally, so semantic retrieval does not become raw cloud sync.

**Why this priority**: Semantic search must preserve the local-first trust boundary established in Spec 005.

**Independent Test**: Inspect outbound memory-gateway payloads and Atlas documents after sync. Embeddings may exist only beside compact memory fields, with provider/model/dimensions recorded, and banned raw fields remain absent.

**Acceptance Scenarios**:

1. **Given** a local memory record has title, summary, tags, note/context, and capped evidence, **When** embedding sync runs, **Then** the embedding input is the compact representation only and excludes raw screenshots/full OCR/prompts/model responses.
2. **Given** a memory item is tombstoned or local indexing is disabled, **When** sync runs, **Then** vector-search results no longer surface it.
3. **Given** embedding generation fails, **When** Library/Ask runs, **Then** local cited retrieval still works and the failure is represented by bounded audit metadata.

---

### User Story 4 - Retrieval Quality Evaluation Harness (Priority: P2)

As the builder, I want a repeatable retrieval/Ask evaluation harness so quality regressions are caught before we add agent actions.

**Why this priority**: Semantic retrieval adds ranking and model dependencies. The branch needs measurable fixtures, not ad hoc screenshot judgment.

**Independent Test**: Run backend and Android fixture tests that verify expected top envelope IDs, refusal behavior, duplicate collapse, and local fallback behavior.

**Acceptance Scenarios**:

1. **Given** the fixture corpus is loaded, **When** the evaluation runs, **Then** each query records top result, expected result, score bands, retrieval mode, and answer/refusal status.
2. **Given** a ranking change drops a required expected capture below threshold, **When** tests run, **Then** the failing fixture names the query and expected envelope ID.

## Edge Cases

- Atlas Vector Search index exists but is building or misconfigured.
- Embedding provider key is missing in backend env.
- Embedding dimensions in stored records do not match the active index.
- Compact text is empty after caps/scrubbing.
- A query asks for a sensitive identifier such as passport, SSN, credit card, access code, or API key.
- Duplicate captures have different envelope IDs but same normalized content.
- Local Room has a capture that Atlas has not indexed yet.
- Atlas returns an envelope ID that no longer exists locally.

## Requirements

### Functional Requirements

- **FR-005A-001**: System MUST keep Room + SQLCipher as the source of truth. Atlas remains a compact index only.
- **FR-005A-002**: Android MUST NOT contain Atlas credentials, MongoDB drivers, OpenAI API keys, or direct vector-search credentials.
- **FR-005A-003**: Embeddings MUST be generated server-side or through the existing `:net` gateway boundary; no new Android network path may be added outside `com.orbit.app.net.*`.
- **FR-005A-004**: Embedding input MUST be derived only from compact indexed fields: title, summary, tags, user note/context, source labels, capped evidence, and compact text.
- **FR-005A-005**: Embedding input MUST NOT include raw screenshots, image bytes, full OCR bodies, raw HTML, full clipboard bodies, prompts, model responses, audit logs, access tokens, refresh tokens, or secrets.
- **FR-005A-006**: Every embedded record MUST store `embeddingModel`, `embeddingDimensions`, `embeddingInputHash`, and `embeddedAtMillis`.
- **FR-005A-007**: The initial embedding model for this branch MUST be `openai:text-embedding-3-small` with 1536 dimensions unless research or validation disproves it before implementation starts.
- **FR-005A-008**: Atlas Vector Search MUST be user-scoped and MUST filter out tombstoned records.
- **FR-005A-009**: Hybrid retrieval MUST combine semantic score, lexical score, local existence, recency, context-note match, and duplicate suppression into a deterministic final ranking.
- **FR-005A-010**: Android MUST NOT show a result as tappable unless the envelope exists locally.
- **FR-005A-011**: Ask Orbit MUST answer only from cited evidence and MUST refuse when retrieval confidence, citation coverage, or sensitive-data policy is insufficient.
- **FR-005A-012**: Ask Orbit MUST NOT expose unrelated numeric captures as answers to sensitive identifier questions.
- **FR-005A-013**: Grounded Ask MAY use an LLM only after retrieval selects compact cited evidence; the LLM MUST NOT receive raw artifacts and MUST produce citations/limitations.
- **FR-005A-014**: If LLM answer synthesis is not implemented in this branch, Ask MUST still improve by using hybrid retrieval plus extractive cited answers/refusals.
- **FR-005A-015**: Local audit rows MUST record embedding sync, semantic search, grounded Ask, fallback, and refusal outcomes using digests/counts/model labels only.
- **FR-005A-016**: Disabling cloud indexing or missing provider credentials MUST degrade Library/Ask to local cited retrieval without breaking Diary, detail, or Follow-ups.
- **FR-005A-017**: Tests MUST cover screenshot-derived failure fixtures before this branch can be considered complete.

### Key Entities

- **EmbeddingPolicy**: Active provider/model/dimensions, input field allow-list, caps, and consent category.
- **EmbeddedMemoryIndexItem**: Existing compact memory item plus embedding metadata and vector.
- **HybridRetrievalRequest**: Query/question, filters, limit, retrieval mode, and optional refusal policy.
- **HybridRetrievalResult**: Ranked result with semantic score, lexical score, final score, matched evidence, duplicate representative ID, and local existence status.
- **GroundedAskAnswer**: Answer/refusal with citations, candidates, model label, retrieval mode, confidence, and limitations.
- **RetrievalEvalFixture**: Deterministic corpus/query/expected-result/refusal fixture for regression testing.

## Success Criteria

- **SC-005A-001**: Screenshot-derived fixtures pass: startup event and flight receipt rank the intended capture first; recipe returns relevant captures; passport-number refuses.
- **SC-005A-002**: Library `qr code` succeeds from user context/note even when OCR lacks the phrase.
- **SC-005A-003**: Atlas documents contain embedding metadata and vectors only for compact memory records; banned raw fields remain absent.
- **SC-005A-004**: Turning off cloud/embeddings preserves Diary, detail, Follow-ups, and local cited search.
- **SC-005A-005**: Backend tests prove vector search is user-scoped and tombstoned records do not surface.
- **SC-005A-006**: Android tests prove dead Atlas envelope IDs are filtered before display.

## Assumptions

- Spec 005 code and gateway are the base layer for this branch.
- Supabase Auth remains the backend identity source.
- MongoDB Atlas credits are available for vector-search development.
- OpenAI embeddings may be used by the backend for this branch; local model embeddings remain future Spec 022 work.
- The user already has an OpenAI key available for local development, but Android must never receive it.
- AppFunctions/Spark interop remains deferred until approval exists.

## Non-Goals

- Replacing Room/SQLCipher.
- Full knowledge graph.
- BYOM/local model manager or on-device 1-bit model runtime.
- A2UI/generative native UI.
- Agent planning or external action execution.
- Multi-device sync/conflict resolution.
- Uploading raw screenshots/full OCR/raw HTML/prompts/model responses.
- Making Atlas authoritative.

## Stop Signs

- A task requires Android to hold cloud provider keys or Atlas connection strings.
- A task sends raw artifacts or full OCR to the embedding or Ask provider.
- A task bypasses `:net` for network egress.
- A task makes Ask answer without citations or refusal thresholds.
- A task starts Spec 006 action drafting before semantic retrieval fixtures pass.
