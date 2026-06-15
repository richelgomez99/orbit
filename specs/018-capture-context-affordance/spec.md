# Feature Specification: Capture Context Affordance

**Feature Branch**: `feature/018-capture-context-affordance-20260614`  
**Created**: 2026-06-14  
**Status**: Draft  
**Input**: Vision slot for the "Clarify" capture affordance after Spec 011 shipped safe post-capture `Add context` and manual compose.

## Context

Spec 011 already delivered the durable storage decision: user-provided capture context is an `EnvelopeNote` attached to a saved `IntentEnvelope`. It also added `Add context` from the post-capture pill, but the current UX opens full capture detail note entry. Spec 018 is the narrow vision-slot reconciliation branch: make capture-time context feel lightweight and immediate without creating a parallel context model or risky pre-seal overlay path.

## User Scenarios & Testing

### User Story 1 - Add Context Without Leaving Capture Flow (Priority: P1)

After a new capture is saved, the user can tap the context icon and type a short reason in a focused overlay/dialog instead of being thrown into the full detail screen.

**Why this priority**: Intent at capture time is the highest-signal data Orbit gets. The current post-save affordance works but is heavier than the vision because it navigates into detail.

**Independent Test**: Save a capture, tap `Add context`, enter "for dentist reschedule", save, and verify the note is attached to the same envelope while the post-capture/undo state does not create a duplicate.

**Acceptance Scenarios**:

1. **Given** a new saved capture post-capture pill is visible, **When** the user taps `Add context`, **Then** Orbit opens a focused context entry surface for that envelope without exposing raw Room access to the UI process.
2. **Given** the user saves non-blank context, **When** the save completes, **Then** Orbit writes through the existing `createOrUpdateLatestNote` Binder/repository path and shows a compact confirmation.
3. **Given** the user cancels or submits blank context, **When** the dialog closes, **Then** no note row is created and the saved capture remains unchanged.

---

### User Story 2 - Duplicate Capture Context Still Targets Existing Memory (Priority: P2)

When Orbit says a capture is already saved, the user can add context to the existing capture from the same lightweight surface.

**Why this priority**: Duplicate recapture is often a signal that the saved thing matters again. The user should be able to add today's reason without creating another envelope.

**Independent Test**: Trigger an Already Saved capture, tap context, save a note, and verify the existing envelope receives the note and no duplicate envelope is created.

**Acceptance Scenarios**:

1. **Given** an `AlreadySaved` post-capture state, **When** the user taps context, **Then** the focused context surface targets `existingEnvelopeId`.
2. **Given** context is saved for an existing envelope, **When** Library searches note-only text, **Then** the existing capture appears with `Context` evidence.

---

### User Story 3 - Keep The True Pre-Seal Clarify Path Explicitly Deferred (Priority: P3)

The branch should decide, with evidence, whether a pre-seal transparent Clarify activity belongs now. If not, it must be documented as deferred rather than half-implemented.

**Why this priority**: Pre-seal context matches the long-term vision but adds overlay focus, keyboard, duplicate, and undo complexity. The MVP needs the benefit of user intent without destabilizing capture.

**Independent Test**: Review the implementation and docs: no new pre-seal database field, no bypass of duplicate detection, and no collection of context before an envelope id exists unless the tasks explicitly pull that in.

**Acceptance Scenarios**:

1. **Given** Spec 018 is complete, **When** future agents read it, **Then** they can tell whether pre-seal Clarify was closed as deferred or implemented with tests.
2. **Given** context is captured post-save, **When** the capture is duplicated, undone, or opened from Diary, **Then** context behavior remains consistent with Spec 011 and Spec 012 receipts.

## Edge Cases

- Blank context must not create or overwrite an existing note.
- Context save failure must show user-friendly copy and keep the saved capture intact.
- The context surface must handle `EnvelopeRepositoryService` Binder unavailability without losing typed text until the user dismisses or retries.
- The focused surface must not remain open after the target envelope was undone or deleted; it should close with a clear "already removed" message if detected.
- Context text must be capped before Binder/display and must never include raw screenshots, OCR bodies, model responses, prompts, embeddings, cookies, JWTs, API keys, or raw HTML in logs.
- The context surface must not add network dependencies or cloud/model calls.

## Requirements

### Functional Requirements

- **FR-018-001**: Orbit MUST provide a focused post-save context entry surface for new-capture `SilentWrapPill` and `UndoPill` states.
- **FR-018-002**: Orbit MUST provide the same focused context entry surface for `AlreadySaved` duplicate states, targeting the existing envelope id.
- **FR-018-003**: Context saves MUST write through the existing `createOrUpdateLatestNote` Binder/repository path.
- **FR-018-004**: Context storage MUST remain `EnvelopeNote`; this branch MUST NOT add a new context table or raw sidecar.
- **FR-018-005**: Blank context MUST be rejected locally before Binder calls.
- **FR-018-006**: Context entry MUST be local-first and work without network, embeddings, cloud LLMs, or local model availability.
- **FR-018-007**: The context surface MUST preserve overlay process boundaries: UI/capture surfaces may call Binder but must not open Room directly.
- **FR-018-008**: The implementation MUST keep undo and duplicate suppression semantics intact.
- **FR-018-009**: Library/search/hydration/compact-memory consumers MUST continue using the same latest-note source from Spec 011.
- **FR-018-010**: If true pre-seal Clarify is not implemented, it MUST be listed as deferred with the reason and future trigger.

### Key Entities

- **IntentEnvelope**: Existing saved capture. Context always targets an envelope id.
- **EnvelopeNote**: Existing latest context/note row used by detail, Library, hydration, memory index, and future KG.
- **CaptureContextDraft**: UI/process-boundary draft containing target envelope id, text, origin surface, and optional duplicate/new-capture marker.
- **CaptureContextResult**: Result of saving context: saved, blank, unavailable, target missing, or failed.

## Success Criteria

### Measurable Outcomes

- **SC-018-001**: A user can save context for a fresh capture in one tap from the post-capture pill plus typing/save, without navigating through full detail first.
- **SC-018-002**: Note-only Library search still finds context added through the new surface.
- **SC-018-003**: Duplicate capture context targets the existing envelope and does not create another envelope.
- **SC-018-004**: Focused tests cover blank save, successful save, duplicate target save, Binder failure copy, and no raw payload leakage.
- **SC-018-005**: Full non-phone gate passes: Kotlin compile, JVM tests, custom lint tests, Android lint, debug APK assembly, and Android test compilation.

## Assumptions

- Spec 011's `EnvelopeNote` storage choice remains correct.
- A focused Compose dialog or lightweight transparent Activity is acceptable if it writes through the same Binder method.
- The post-save path is the MVP-safe Clarify implementation. True pre-seal context is deferred unless implementation research proves it can be added without destabilizing overlay capture.
- Phone/manual validation is useful but not required for repo-side completion while the device is unavailable.
