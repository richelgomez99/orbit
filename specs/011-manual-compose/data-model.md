# Data Model: Manual Compose And Capture Context

## Existing Entity: IntentEnvelope

Manual compose writes the same envelope type as reactive capture.

Relevant fields:

| Field | Use In Spec 011 |
| --- | --- |
| `id` | Target for context notes and detail navigation. |
| `contentType` | Manual compose v1 uses `TEXT`. |
| `textContent` | Manual body/link content only, not the context note. |
| `intent` / `intentSource` | Optional user-selected intent or existing classifier/default behavior. |
| `createdAt` | Actual save time. Must not be backdated. |
| `dayLocal` | Diary placement day. May be selected day for manual backfill if repository supports it. |
| `sourceAppLabel` / state snapshot | Manual compose should identify Orbit/manual source, not `unknown`. |

## Existing Entity: EnvelopeNote

Spec 011 uses `EnvelopeNoteEntity` as capture context storage.

Relevant fields:

| Field | Use In Spec 011 |
| --- | --- |
| `id` | Generated note id. |
| `envelopeId` | Saved capture/manual envelope target. |
| `text` | User context, trimmed, non-empty, capped on display/export. |
| `createdAt` | Actual note creation time. |
| `updatedAt` | Latest edit time. |

Invariant: one latest visible context note per envelope through `getLatestNoteForEnvelope` and `createOrUpdateLatestNote`.

## UI-Only Entity: ManualComposeDraft

Not persisted directly. Converted to an envelope draft plus optional note after save.

| Field | Type | Rules |
| --- | --- | --- |
| `bodyText` | String | Required, trimmed, non-empty. |
| `contextText` | String? | Optional, trimmed, attaches as latest note only after envelope save succeeds. |
| `dayLocal` | String | Selected Diary day or today. |
| `intentName` | String? | Optional; if absent, existing classification/default behavior applies. |
| `createdAtMillis` | Long | UI draft timestamp only; repository/audit owns final created time. |

## UI-Only Entity: CaptureContextRequest

Represents the action of adding context to a saved capture.

| Field | Type | Rules |
| --- | --- | --- |
| `envelopeId` | String | Required existing envelope id. |
| `sourceSurface` | Enum-like String | `OVERLAY_POST_CAPTURE`, `DUPLICATE_CAPTURE`, `DETAIL`, or `MANUAL_COMPOSE`. |
| `initialText` | String? | Optional existing note for editing. |

## No New Room Migration Expected

Spec 011 should not add Room tables. If implementation discovers that backfill day placement cannot be represented safely without a schema change, pause and update this data model before coding.
