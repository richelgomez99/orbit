# Contract: Memory Candidates Inspector

## Process Boundaries

- `:ml`: owns `memory_candidate`, `promoted_memory`, provenance joins, decisions, and audit writes.
- `:ui`: renders Memory Review and sends accept/reject/edit decisions.
- `:capture`: no new responsibilities.
- `:net`: no new responsibilities for this spec.

No direct Android Atlas/MongoDB/OpenAI clients are introduced.

## AIDL Extensions

Extend `IEnvelopeRepository` or a nested repository delegate with compact operations:

```aidl
void listPendingMemoryCandidates(IMemoryCandidateObserver observer, int limit);
void listPromotedMemories(IPromotedMemoryObserver observer, int limit);
MemoryDecisionResultParcel acceptMemoryCandidate(String candidateId, String editedLabel, String editedFactText);
MemoryDecisionResultParcel rejectMemoryCandidate(String candidateId, String reason);
MemoryDecisionResultParcel invalidateMemoryCandidate(String candidateId, String reason);
```

Add parcels:

- `MemoryCandidateParcel`
- `PromotedMemoryParcel`
- `MemoryDecisionResultParcel`
- observer interfaces for candidate/promoted lists

## Decision Semantics

### Accept

Input:

- `candidateId`
- optional edited display label
- optional edited fact text

Behavior:

1. Load candidate in `PENDING` or `ASKED`.
2. Validate edited values if present.
3. Create or update one promoted memory row.
4. Mark candidate `PROMOTED`.
5. Write audit row.
6. Return promoted memory id and user-facing result copy.

Idempotency:

- A second accept for an already promoted candidate returns the existing promoted memory id without creating duplicates.

### Reject

Input:

- `candidateId`
- optional reason code/text

Behavior:

1. Load candidate.
2. Mark `REJECTED` if not terminal.
3. Write audit row.
4. Return user-facing result copy.

Idempotency:

- A second reject returns current rejected state without duplicate audit writes.

### Edit Then Accept

Behavior:

- Edited text becomes the promoted memory display text.
- Candidate original text remains in `memory_candidate`.
- Audit extra records that user edited before accepting without storing raw sensitive source content.

## Cloud Sync Boundary

Spec 007 must not add candidate or promoted memory details to compact Atlas sync by default.

Allowed in future specs:

- Compact promoted memory ids/counts or cloud-safe metadata after cloud controls exist.

Forbidden now:

- Pending candidates.
- Rejected candidates.
- Local-only candidate/promoted memory text.
- Sensitive candidate/promoted memory text.
- Raw supporting evidence/capture text.

