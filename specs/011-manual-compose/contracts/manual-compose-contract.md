# Contract: Manual Compose And Capture Context

## Repository Contract

The feature should reuse existing repository contracts where possible.

### Existing Note Methods

```kotlin
suspend fun getLatestNote(envelopeId: String): String?
suspend fun createOrUpdateLatestNote(envelopeId: String, text: String): Boolean
```

Rules:

- `text` is trimmed before persistence.
- Blank text returns `false` and writes nothing.
- Calls route through `:ui`/`:capture` to `:ml` Binder. UI code must not access Room directly.
- Notes are compact context. They must not contain raw screenshots, OCR bodies, prompts, embeddings, model responses, tokens, cookies, or API keys from system-generated sources.

### Manual Compose Save

Preferred v1 shape:

```kotlin
suspend fun createManualTextCapture(
    bodyText: String,
    contextText: String?,
    dayLocal: String,
    intentName: String? = null
): ManualComposeResult
```

`ManualComposeResult` may be a Kotlin UI-layer result over existing Binder parcels rather than an AIDL parcel if the implementation can safely compose existing `sealWithResult` and note calls.

Result cases:

| Case | Meaning |
| --- | --- |
| `Saved(envelopeId)` | New envelope created; optional context was attached if non-blank. |
| `AlreadySaved(existingEnvelopeId, matchedBy)` | Duplicate path; optional context may attach to existing envelope only after user action or explicit save decision. |
| `Blocked(reason)` | Blank/scrubbed/invalid input or repository failure. |

## UI Contract

### Overlay Post-Capture

For a newly saved envelope, the post-capture UI must offer:

- Undo/remove within the existing undo window.
- Add context/open note path targeting the saved envelope.

The context action must not remove the ability to undo unless the existing undo window has already expired.

### Manual Compose

Manual compose must provide:

- Body text input.
- Optional context input.
- Save/cancel controls.
- Selected day display if opened from a specific Diary day.
- Error copy for blank body and save failure.

Manual compose must not expose implementation wording such as "raw OCR", "embedding", "prompt", or "model response".

## Validation Contract

Tests must prove:

- Capture context writes through `createOrUpdateLatestNote`.
- Manual compose blocks blank body before repository calls.
- Manual compose saves body first and context second.
- Duplicate manual compose does not create a second active envelope.
- Library/search can match note-only terms through the existing local search path.
