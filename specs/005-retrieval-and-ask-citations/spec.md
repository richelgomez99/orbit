# Feature Specification: Atlas Memory Index + Cited Library

**Feature Branch**: `feature/005-atlas-memory-index-search-20260530`
**Created**: 2026-05-30
**Status**: Draft - Speckit specify phase complete
**Input**: "Proceed with Speckit workflow for the true vision" after Atlas startup credits were approved and `orbit_dev.memory_items` was verified.

## Summary

Orbit needs the first real Library surface without pretending the full knowledge graph, A2UI runtime, BYOM local model manager, or chat-first agent workspace already exists. This feature adds an opt-in Atlas-backed cloud memory index that stores only compact, derived memory records and returns cited Library search results that open the original local envelope. A thin cited Ask layer is allowed only after retrieval works; it is not the first product move.

Room + SQLCipher remains the source of truth. Atlas is an index and retrieval accelerator. Android never connects to Atlas directly and never carries Atlas credentials. All cloud reads/writes go through a backend memory gateway and the existing `:net` egress boundary.

## Vision Fit

This is the bridge from the current Diary-heavy implementation to the May 22 three-pillar Orbit vision:

- **Diary** remains the local chronological daybook.
- **Library** becomes real through search, filters, and cited retrieval over saved memories.
- **Orbit** can later answer questions using retrieved local episodes as citations before later specs add full agent workspace, A2UI, KG, and BYOM local models.

This spec deliberately avoids making Atlas authoritative. The "true vision" requires a trustworthy memory layer, not a cloud archive.

## Current Context

- Current branch before this work: `feature/004-active-intent-cleanup-20260518`.
- Spec 004 implemented Room v8 sidecars for Basic Understanding and Active Intent.
- `supabase/functions/llm_gateway/` already contains the authenticated LLM gateway with Supabase JWT verification and audited LLM request routing.
- `supabase/functions/memory_gateway/.env.local` now exists locally and is ignored by git.
- Atlas setup was verified against `orbitcluster.zzkkmfu.mongodb.net`.
- Atlas database/collection verified: `orbit_dev.memory_items`.
- Baseline indexes verified: `_id_`, `uniq_user_envelope`, `user_day_created`, `user_intent_created`, `user_tombstone`.
- Vector Search index is intentionally deferred until this spec locks embedding model, dimensions, and field policy.

## User Scenarios & Testing

### User Story 1 - Search My Saved Memories (Priority: P1)

As a user, I want to search saved captures from Library so I can find the product, article, event, place, or chat clue I saved without scrolling the Diary.

**Why this priority**: Library is the missing second pillar. A memory product without retrieval feels like a prettier screenshot list.

**Independent Test**: Seed or capture at least 20 envelopes, index compact memory records, run a Library query, and verify results show title/summary/source/date/citation and open the original local envelope.

**Acceptance Scenarios**:

1. **Given** compact indexed memories exist for the signed-in user, **When** the user searches "flight receipt", **Then** Library shows matching records with source/date/citation and a tap opens the local capture detail.
2. **Given** Atlas is unavailable, **When** the user opens Library, **Then** Diary and local capture detail still work and Library shows a cloud-index unavailable state rather than failing the app.
3. **Given** a capture was deleted locally, **When** sync runs, **Then** the matching Atlas record is tombstoned or removed from visible results.

---

### User Story 2 - Compact Cloud Index Sync (Priority: P1)

As a privacy-conscious user, I want Orbit to sync only compact memory metadata to Atlas so search works without uploading raw screenshots, full OCR, prompts, or model responses.

**Why this priority**: This is the structural trust requirement that makes Atlas acceptable for Orbit.

**Independent Test**: Inspect outbound memory-gateway payloads and Atlas documents for a mixed capture set and prove banned fields never appear.

**Acceptance Scenarios**:

1. **Given** a text or screenshot capture is sealed and Basic Understanding exists, **When** index sync runs, **Then** the payload contains capped title/summary/excerpts/tags/source metadata/provenance and excludes raw screenshots, full OCR, raw HTML, prompts, and model responses.
2. **Given** an envelope lacks enough compact evidence, **When** sync runs, **Then** Orbit indexes a minimal metadata record or skips with an audit row rather than uploading raw content to compensate.
3. **Given** a user disables memory indexing, **When** sync is scheduled, **Then** no new Atlas writes occur and the local audit log records the skipped cloud write.

---

### User Story 3 - Three-Pillar Entry Point (Priority: P1)

As a user, I want a clear place for Diary and Library so Orbit feels like the memory system from the vision, not one overloaded chronological screen.

**Why this priority**: The May 22 vision depends on Diary/Library/Orbit separation. Library search is much more legible if the product has a Library entry point.

**Independent Test**: Launch the app and verify the user can move between Diary and Library, run a search, and return to the original capture detail.

**Acceptance Scenarios**:

1. **Given** the app opens to Diary, **When** the user taps Library, **Then** they see search and filters rather than Active Intent cleanup.
2. **Given** the user opens a Library result, **When** they tap back, **Then** they return to Library state, not a reset Diary scroll.
3. **Given** Orbit workspace is not fully built yet, **When** the shell displays the third pillar, **Then** it may route to the existing Active Intent cleanup surface without adding full chat.

---

### User Story 4 - Ask Orbit With Citations (Priority: P2)

As a user, I want to ask Orbit a question about my saved captures and see which memories support the answer so I can trust the result.

**Why this priority**: Ask is valuable only after retrieval and citations work. It is a P2 layer over Library, not the first MVP dependency.

**Independent Test**: Ask "what was that startup event I saved?" and verify the answer uses only returned memory records, includes citations, and exposes "View capture" for each cited envelope.

**Acceptance Scenarios**:

1. **Given** search returns relevant memory records, **When** the user asks a question, **Then** Ask Orbit returns a concise answer with 1-5 citations and no uncited factual claims about the user's memory.
2. **Given** retrieval confidence is low, **When** Ask Orbit cannot ground an answer, **Then** it says it could not find enough saved evidence and offers the top cited matches instead of hallucinating.
3. **Given** `RuntimeFlags.useLocalAi` or cloud storage controls disable cloud Ask, **When** the user asks, **Then** no network request is made for Ask and the UI explains the local-only limitation.

---

### User Story 5 - Audit Cloud Memory Operations (Priority: P2)

As a user, I want Orbit to record cloud index writes and searches locally so I can see what left the device and why.

**Why this priority**: Principle X requires local auditability for cloud reads/writes, but the MVP can ship search first if every write/search has at least a bounded local audit record.

**Independent Test**: Trigger an index upsert, search, Ask, and tombstone propagation; verify bounded audit rows exist locally and do not include secrets or raw content.

**Acceptance Scenarios**:

1. **Given** sync upserts a memory item, **When** the operation completes, **Then** a local audit row records provider, endpoint, envelope id, payload digest, and outcome.
2. **Given** Library search or Ask calls the memory gateway, **When** the response returns, **Then** a local audit row records query digest, result count, latency, and outcome.

## Functional Requirements

- **FR-005-001**: System MUST preserve Room + SQLCipher as the source of truth for captures, audit log, consent ledger, and local envelope detail.
- **FR-005-002**: System MUST store Atlas credentials only in the backend memory gateway environment, never in Android, Gradle, Room, assets, or committed files.
- **FR-005-003**: Android MUST route all memory index network calls through the `:net` process. No HTTP client or Atlas driver may be added outside `com.orbit.app.net.*`.
- **FR-005-004**: The backend memory gateway MUST authenticate requests with the existing Supabase JWT model before reading or writing Atlas.
- **FR-005-005**: The backend MUST scope every Atlas document and query by authenticated `userId`.
- **FR-005-006**: The compact memory item MUST include provenance to at least one local source envelope id.
- **FR-005-007**: The compact memory item MUST cap user-visible text fields: title <= 140 chars, summary <= 500 chars, each evidence excerpt <= 240 chars, max 5 evidence snippets.
- **FR-005-008**: Atlas payloads MUST NOT include raw screenshots, raw image bytes, full OCR bodies, full clipboard bodies, raw HTML, prompts, model responses, Android secrets, Supabase refresh tokens, Atlas credentials, or local audit log rows.
- **FR-005-009**: The first MVP MUST support memory upsert, tombstone/delete, and Library search with citations.
- **FR-005-010**: Search results MUST include enough citation metadata for Android to open the original local envelope by id.
- **FR-005-010a**: Android MUST NOT show a cloud search result as tappable unless the referenced envelope exists in local Room. If Atlas sync lags, Library MAY merge local Room search results so the demo remains local-first and dead detail links are impossible.
- **FR-005-011**: If Ask is implemented in this branch, Ask responses MUST include citations to returned memory records and MUST refuse or fall back to cited matches when retrieval is insufficient.
- **FR-005-012**: Local audit rows MUST be emitted for memory index upsert, tombstone/delete, search, and Ask request outcomes.
- **FR-005-013**: Atlas unavailability MUST degrade Library/Ask only; Diary capture, local envelope detail, Active Intent cleanup, and local mode must continue.
- **FR-005-014**: Vector Search MAY be enabled only after embedding provider, dimensions, consent category, and banned-field policy are documented in a follow-up Spec Kit plan/data model. For current roadmap ordering, this belongs to Spec 005A.
- **FR-005-015**: This feature MUST NOT use Atlas Device Sync, Realm Sync, App Services Data API, GraphQL API, or direct mobile-to-Atlas connection strings.

## Key Entities

- **MemoryIndexItem**: Compact Atlas document representing one local envelope or derived memory surface. It is searchable but not authoritative.
- **MemoryEvidenceSnippet**: Capped citation/evidence excerpt with source type, label, hash/digest where useful, and local envelope provenance.
- **MemorySearchQuery**: User query plus filters and client request id. The raw query is sent to the backend only when cloud Library is enabled.
- **MemorySearchResult**: Ranked result with citation metadata, never raw artifact bodies.
- **AskOrbitAnswer**: Retrieval-grounded answer with citations and refusal/fallback metadata.
- **MemoryCloudAuditEvent**: Local-only audit row representing a cloud memory operation. The audit log itself never leaves the device.

## Success Criteria

- **SC-005-001**: A seeded 20-capture demo can search Atlas-backed Library and open the original local envelope from each result.
- **SC-005-002**: Library can find at least 3 scripted saved memories and open each original local capture.
- **SC-005-003**: Payload inspection proves banned raw fields are absent from backend requests and Atlas documents.
- **SC-005-004**: Turning off network or breaking Atlas credentials does not break Diary, capture, local detail, or Active Intent cleanup.
- **SC-005-005**: Backend tests prove user A cannot read, search, overwrite, or tombstone user B's memory items.

## Non-Goals

- Replacing Room or implementing full cloud sync.
- Storing raw screenshots or blobs in Atlas.
- Full knowledge graph schema.
- Full Ask Orbit chat beyond retrieval-grounded cited answers.
- Agent planning or external action execution.
- A2UI runtime.
- BYOM/local model manager.
- Multi-device conflict resolution.
- Atlas Device Sync, Realm Sync, Data API, GraphQL, or App Services.

## Assumptions

- Supabase Auth remains the user identity source for backend requests.
- Atlas startup credits are available for MVP development.
- A single `memory_items` collection is enough for MVP; relationship/fact collections can land in later KG specs.
- Initial search can be lexical/metadata-first if vector index setup threatens the Monday MVP; vector search is a planned enhancement behind a locked embedding policy in Spec 005A.
- Ask Orbit is not the next required move. It is a thin cited layer only after Library retrieval is working.
- The current cloud storage consent UI is incomplete. MVP development can use debug/dev opt-in flags, but external alpha requires user-facing controls before claims of full Principle X compliance.

## Follow-Up Spec 005A

Spec 005 closes as compact memory index + Library search + limited local cited Ask preview. It must not be represented as reliable semantic/vector/LLM Ask. The follow-up branch `005A-semantic-retrieval-grounded-ask` owns:

- embedding provider, model, dimensions, and consent policy;
- Atlas Vector Search index creation and user-scoped vector endpoint;
- hybrid lexical + vector retrieval with deterministic local fallback;
- grounded Ask answer/refusal thresholds over cited evidence;
- evaluation fixtures from real UI failures, including startup event ranking, flight receipt ranking, qr-code context search, reschedule/rescheduling recall, cancelled/rescheduled phrasing, and unsupported passport-number refusal.

## Stop Signs

- A task requires Android to hold an Atlas connection string.
- A task stores raw screenshots, full OCR, raw HTML, prompts, model responses, or local audit rows in Atlas.
- A task bypasses `:net` for network egress.
- A task makes Atlas the source of truth.
- A task makes Ask Orbit answer without citations.
- A task uses deprecated Atlas App Services/Data API/Device Sync paths.
