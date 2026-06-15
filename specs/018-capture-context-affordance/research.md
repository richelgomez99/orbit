# Research: Capture Context Affordance

## Decision 1: Keep Context Post-Save For This Branch

**Decision**: Spec 018 should improve the post-save `Add context` flow rather than collecting context before the envelope is sealed.

**Rationale**: The current seal path already handles scrub, duplicate detection, state snapshotting, audit, undo, continuations, and Spec 012 duplicate receipts. Post-save context has a stable envelope id and can route duplicates to the existing envelope. Pre-seal context would have to decide whether context participates in duplicate matching, undo rollback, sensitivity scrub, and audit before there is a target row.

**Alternatives considered**:

- Pre-seal transparent Clarify dialog before every save: rejected for MVP because it increases capture friction and risks overlay keyboard/focus bugs.
- Store context inside the draft before seal: rejected because duplicate attempts would have unclear ownership if the envelope already exists.
- Keep only full detail note entry: rejected because it works but does not meet the lightweight capture-time vision.

## Decision 2: Reuse `EnvelopeNote` And Existing Binder Path

**Decision**: Context continues to write through `createOrUpdateLatestNote(envelopeId, text)`.

**Rationale**: Spec 011 already verified that latest notes feed detail, Library local search, hydration context, and compact memory snapshots. A new table would fragment retrieval/KG semantics and require another migration without new product value.

**Alternatives considered**:

- Add `capture_context` table: rejected as duplicate storage.
- Add a special `IntentEnvelope.intentContext` column: rejected because it mixes the captured artifact with why the user saved it and would require migration/search rewiring.
- Store context in Active Intent sidecars only: rejected because context matters even when there is no follow-up.

## Decision 3: Focused UI Surface Should Live Above Repository, Not In Room

**Decision**: The focused context UI should live in the overlay/detail layer and use `DiaryRepository`/Binder or an equivalent service seam. It must not access Room directly.

**Rationale**: Orbit's security model depends on process boundaries. The overlay runs in `:capture`; the database lives in `:ml`. Any shortcut would violate Principle VI and custom lint expectations.

**Alternatives considered**:

- Direct DAO write from `:capture`: rejected.
- Start a network/cloud summarization call before saving: rejected; context is user-authored and local-first.

## Decision 4: Failure Copy Must Preserve Trust

**Decision**: Save failures should say that Orbit saved the capture but could not attach the context yet, and offer retry/dismiss.

**Rationale**: Losing a typed reason would damage trust. The capture itself should remain saved, and the context draft should remain in UI state until the user retries, cancels, or navigates away.

## Decision 5: Spec 018 May Close As Absorbed If Existing Detail Note Flow Is Preferred

**Decision**: If implementation research finds the focused overlay dialog is higher risk than value, the branch may close as a no-op/absorbed-by-Spec-011 with explicit validation notes.

**Rationale**: The roadmap row intentionally exists to reconcile the May 22 vision slot, not to force churn. A no-op close is acceptable only if it records why the current flow is good enough and what future signal should reopen it.
