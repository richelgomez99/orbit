# Contract: Capture Context Affordance

## User-Facing Contract

### Open Focused Context Entry

Input:

- `targetEnvelopeId: String`
- `origin: NEW_CAPTURE | DUPLICATE_CAPTURE | DETAIL_FALLBACK`
- optional existing note preview

Behavior:

- Opens a focused context entry surface.
- Does not perform network or model calls.
- Does not create a new envelope.
- Keeps typed text in memory until save/cancel/failure dismissal.

### Save Context

Input:

- `targetEnvelopeId: String`
- `text: String`

Behavior:

- Trim text.
- If blank, return local `Blank` result and do not call Binder.
- Otherwise call existing `createOrUpdateLatestNote(targetEnvelopeId, trimmedText)`.
- On success, return `Saved`.
- On Binder unavailable/failure, return `Unavailable` or `Failed` with user-friendly copy and no raw diagnostic payload.

## Existing Binder Contract Reused

Spec 018 should reuse:

```kotlin
suspend fun DiaryRepository.createOrUpdateLatestNote(
    envelopeId: String,
    text: String
): Boolean
```

and the existing AIDL method:

```aidl
boolean createOrUpdateLatestNote(String envelopeId, String text);
```

No new AIDL method is expected unless the implementation proves the overlay process cannot reasonably access the existing Binder seam.

## Process Boundary Contract

- `:capture` may host overlay UI and call Binder.
- `:ui` may host a fallback Activity/Dialog and call Binder.
- `:ml` owns Room and applies note writes.
- `:net` is not involved.

## Deferred Contract: Pre-Seal Clarify

If a future branch implements pre-seal Clarify, it must define a separate contract for:

- how context participates in duplicate matching;
- whether undo removes a newly attached note;
- how context is audited before an envelope id exists;
- keyboard/focus handling for overlay windows;
- sensitivity scrub rules for user-authored context.
