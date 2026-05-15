# Contract: Compact IPC Payloads

**Feature**: `004-capture-understanding`  
**Boundary**: Binder/AIDL and repository parcel payloads between app processes and UI/data clients.

## 1. Purpose

IPC payloads are summary/control messages. They allow UI and services to know which local records exist, what their compact status is, and what action may be taken next. They are not raw content transport.

## 2. Allowed Payload Content

Binder/AIDL payloads MAY carry:

- Capture IDs, source identity IDs, evidence bundle IDs, understanding IDs, job IDs, correction IDs, and trace IDs.
- Compact source labels and secondary labels.
- Glyph kind enum: `PROVIDER`, `APP`, `GENERIC_CATEGORY`, `UNKNOWN`.
- Compact title and compact summary strings sized for detail/list UI.
- Status strings: `pending`, `running`, `ready`, `partial`, `limited`, `failed`, `canceled`, `invalidated`.
- Confidence band or small numeric confidence.
- Limitation codes and short user-visible limitation copy.
- Depth/policy decisions: `basic`, `smart`, `deep`, `summarize_only_saved`, `blocked_sensitive`, `blocked_budget`, `domain_suppressed`, and similar bounded enums.
- Retry/action eligibility flags.
- Pagination tokens for detail evidence summaries if needed.

## 3. Forbidden Payload Content

Binder/AIDL payloads MUST NOT carry:

- Raw HTML.
- Screenshots or screenshot byte arrays.
- Full extracted page text.
- Full OCR text beyond compact snippets needed for display.
- Full evidence bundles in bulk.
- Embedding vectors or raw embeddings.
- Raw prompts or model responses.
- Provider API keys, JWT contents, auth tokens, cookies, or connected-account data.
- Unbounded JSON blobs whose schema allows raw content.

## 4. Size And Shape Requirements

- Payloads should be stable, typed, and additive where possible.
- Large lists must paginate or carry counts plus IDs.
- Compact strings must stay bounded for UI display and Binder safety:
	- `sourceLabel`: 80 characters maximum.
	- `secondarySourceLabel`: 80 characters maximum.
	- `title`: 160 characters maximum.
	- `compactSummary`: 600 characters maximum.
	- `limitationSummary`: 240 characters maximum.
	- `limitationCodes`: 12 codes maximum.
	- `evidencePageToken`: 128 characters maximum.
- Evidence summary pages must return at most 20 compact rows per page.
- Summary parcel round-trip tests must assert the marshalled parcel stays under 32 KiB for a max-filled summary payload.
- Raw content must remain in the local encrypted corpus and be read only through authorized local repository methods in the owning process.
- Parcelable round-trip tests must cover every added parcel shape.

## 5. Suggested Detail Summary Parcel

If implementation needs a new parcel, its shape should stay close to:

```text
CaptureUnderstandingSummaryParcel
- captureId: String
- sourceIdentityId: String?
- understandingId: String?
- currentJobId: String?
- sourceLabel: String
- secondarySourceLabel: String?
- glyphKind: String
- title: String?
- compactSummary: String?
- status: String
- confidenceBand: String?
- limitationCodes: List<String>
- limitationSummary: String?
- depthUsed: String
- retryEligible: Boolean
- correctionAvailable: Boolean
- evidenceSummaryCount: Int
- evidencePageToken: String?
```

This parcel intentionally excludes raw evidence details. The UI may request paged evidence summaries by ID if needed.

## 6. Architecture Validation

Implementation is not complete until architecture validation finds:

```sh
rg -n "rawHtml|readableHtml|screenshot|embedding|vector|fullEvidence|pageText" app/src/main/aidl app/src/main/java/com/capsule/app/data/ipc
```

Any match must be reviewed. Matches in forbidden IPC surfaces are blockers unless they are comments/tests asserting the ban.
