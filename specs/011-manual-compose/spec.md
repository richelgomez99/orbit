# Feature Specification: Manual Compose And Capture Context

**Feature Branch**: `feature/011-manual-compose-capture-context-20260613`
**Created**: 2026-06-13
**Status**: Draft
**Input**: User vision: Orbit captures what was saved plus the intention behind why it was saved. Spec 011 preserves numeric progression while pulling high-signal capture context into the mainline.

## User Scenarios & Testing

### User Story 1 - Clarify A Fresh Capture (Priority: P1)

After saving a clipboard or screenshot capture, the user can immediately add a short context note such as "for Mom's birthday" or "reschedule this dentist appointment" without hunting through the Diary.

**Why this priority**: Intent at capture time is the highest-signal data Orbit can get. This directly improves titles, Library search, Ask/agent evidence, memory indexing, and future KG facts without requiring a model to guess.

**Independent Test**: Save a capture through the overlay, tap the context affordance in the post-capture surface, enter a note, and verify the saved envelope detail/Library search surfaces the note while the capture still went through duplicate detection, scrub, audit, and Room storage.

**Acceptance Scenarios**:

1. **Given** the overlay just saved a new capture, **When** the user taps `Add context`, **Then** Orbit opens a focused note-entry surface for that exact envelope.
2. **Given** the user saves a non-empty context note, **When** they open the capture detail, **Then** the note appears in the existing Context/Orbit notes area.
3. **Given** the user searches Library for a word that exists only in the note, **When** local search runs, **Then** the capture appears with note evidence/citation.
4. **Given** cloud memory indexing is enabled later, **When** the compact memory snapshot is built, **Then** the note is included through the existing compact note field and not as raw screenshot/OCR.

---

### User Story 2 - Deliberate Manual Compose (Priority: P2)

The user can create an Orbit memory from inside the app by typing a note/thought/link and optional context, without relying on clipboard, screenshots, or another app.

**Why this priority**: Some intentions start in the user's head, not in another app. Manual compose gives Orbit a deliberate capture path while still using the same IntentEnvelope storage model.

**Independent Test**: From the Diary, create a manual text capture, save it, and verify it appears on the selected/current day with normal detail, search, duplicate, audit, and undo/delete behavior.

**Acceptance Scenarios**:

1. **Given** the user is in Diary, **When** they open manual compose and save text, **Then** Orbit creates a regular text envelope through the repository seal path.
2. **Given** the user enters optional context with a manual capture, **When** save succeeds, **Then** Orbit attaches that context as the latest envelope note.
3. **Given** the manual text exactly duplicates an existing active text capture, **When** the user saves, **Then** Orbit does not create a second envelope and shows/open routes to the existing capture.
4. **Given** the user backfills onto a non-today Diary page, **When** they save, **Then** the envelope is shown on that `dayLocal` but the audit trail preserves wall-clock creation time.

---

### User Story 3 - Context Updates Improve Downstream Surfaces (Priority: P3)

Context added during capture or manual compose should improve existing product surfaces without adding a parallel "notes app" model.

**Why this priority**: Context must become product substrate, not dead metadata. The existing note path already participates in local search, compact memory index snapshots, and link rehydration context.

**Independent Test**: Add context to a capture with a generic screenshot/title, then verify detail, Library local search, hydration context packet, and compact memory builder use the same note value.

**Acceptance Scenarios**:

1. **Given** a capture has a user note, **When** Library local search matches only that note, **Then** the result cites `Context` evidence and opens the source capture.
2. **Given** a URL hydration worker requests context, **When** a note exists, **Then** the compact hydration context includes the capped latest note.
3. **Given** a compact memory snapshot is built, **When** a note exists, **Then** the note contributes to compact title/summary/evidence without raw artifacts crossing Binder.

## Edge Cases

- Blank or whitespace-only context must not create a note row.
- Context save failure must not duplicate or roll back the already-saved capture.
- Duplicate capture context should attach to the existing envelope, not create a duplicate.
- Manual compose of blank content must be blocked before repository calls.
- Manual compose must not bypass sensitivity scrub, duplicate checks, state snapshot creation, or audit logging.
- Backfilled manual entries must be visually placed on the selected diary day but remain audit-honest about `createdAt`.
- No raw screenshots, OCR bodies, prompts, embeddings, model responses, or secrets may be included in any context Binder payload.

## Requirements

### Functional Requirements

- **FR-011-001**: Orbit MUST expose a capture-time context affordance for newly saved overlay captures.
- **FR-011-002**: Capture-time context MUST attach to the saved envelope through the existing envelope note path.
- **FR-011-003**: Context note creation MUST reject blank text and cap display/Binder payloads using existing compact limits.
- **FR-011-004**: Context affordances MUST use existing `EnvelopeDetailActivity`/note-entry flow or a functionally equivalent focused note surface that writes through `createOrUpdateLatestNote`.
- **FR-011-005**: Manual compose MUST create text envelopes through the same repository seal path as other captures.
- **FR-011-006**: Manual compose MUST support optional context that attaches as an envelope note only after a successful envelope save.
- **FR-011-007**: Manual compose MUST preserve duplicate suppression semantics for exact text/URL matches.
- **FR-011-008**: Backfill MUST be wall-clock honest: `createdAt` remains the actual save time while `dayLocal` may target the selected Diary day.
- **FR-011-009**: Diary detail, Library local search, hydration context, and compact memory snapshots MUST continue to use the same latest-note source.
- **FR-011-010**: The feature MUST work offline and without cloud/model availability.
- **FR-011-011**: The implementation MUST not add a new context database table unless the existing `envelope_note` path cannot satisfy the contract.
- **FR-011-012**: All new Binder/UI contracts MUST preserve the `:ui`/`:capture`/`:ml` process boundaries and avoid direct Room access outside `:ml`.

### Key Entities

- **IntentEnvelope**: Existing saved capture record. Manual compose creates a regular text envelope; overlay context attaches to an existing/new envelope.
- **EnvelopeNote**: Existing latest-note/context record for one envelope. Spec 011 reuses it as capture intent/context substrate.
- **ManualComposeDraft**: UI-only draft containing body text, optional context text, intended day, and optional intent choice.
- **CaptureContextRequest**: UI action that targets a saved envelope id and opens/saves a context note.

## Success Criteria

### Measurable Outcomes

- **SC-011-001**: A user can save a capture and begin adding context in no more than two taps after the overlay post-capture surface appears.
- **SC-011-002**: A saved context note appears in capture detail and is searchable locally by note-only terms.
- **SC-011-003**: Manual compose creates a visible Diary envelope on the chosen day without any network dependency.
- **SC-011-004**: Exact duplicate manual compose attempts do not create duplicate active envelopes.
- **SC-011-005**: Full non-phone gate passes: compile, JVM tests, custom lint tests, Android lint, APK assembly, and Android test compilation.

## Assumptions

- Existing `envelope_note` is the correct storage surface for user-provided context in this branch.
- Manual compose v1 is text/link only. Voice, attachments, share sheet, and rich media compose are later branches.
- A lightweight focused note-entry path may reuse the existing capture detail note dialog rather than creating a new transparent activity if that is the safest implementation.
- Connected phone/manual validation is useful but not required for repo-side completion while the device is unavailable.
