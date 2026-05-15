# Data Model: Capture Understanding - Active Intent Cleanup

## Overview

Spec 004 uses capture understanding as the evidence layer for Active Intent cleanup. The goal is not to classify screenshots for its own sake. The goal is to identify unresolved saved intent, extract the completion key needed to resolve it, and keep the state honest as the user resolves, archives, expires, supersedes, or deletes captures.

Existing v8 entities provide the first foundation:

- `CaptureUnderstandingEntity`
- `EvidenceBundleEntity`
- `InvalidationRecordEntity`
- `CorrectionFeedbackEntity`

The Active Intent pivot adds sidecar concepts that can be implemented as a v9 migration or equivalent local sidecar depending on branch timing.

## Entity: CaptureUnderstandingResult

Purpose: Evidence-backed understanding for a single capture.

Fields:

| Field | Type | Notes |
|-------|------|-------|
| `capture_id` | String | FK to `IntentEnvelopeEntity.id` |
| `mode` | BASIC / SMART / DEEP | Defaults to BASIC |
| `status` | READY / LIMITED / FAILED | LIMITED when evidence is sparse or completion key missing |
| `category` | IntentCategory | Dead Intent taxonomy category |
| `category_confidence` | Float | Confidence for category only |
| `title` | String? | Compact display label |
| `summary_text` | String? | Must not exceed evidence |
| `completion_key_json` | String? | Structured key for resolution |
| `completion_key_status` | FOUND / MISSING / NOT_ACTIONABLE | Required for Active Intent |
| `source_identity_json` | String? | Provider/app/category/confidence/evidence basis |
| `content_hash_hex` | String? | Raw artifact hash or normalized content hash |
| `perceptual_hash_hex` | String? | Optional future screenshot visual dedupe |
| `canonical_url` | String? | URL identity when known |
| `grounding_constraints_json` | String | Evidence boundaries for future features |
| `created_at` | Long | Epoch millis |
| `updated_at` | Long | Epoch millis |
| `invalidated_at` | Long? | Set when parent capture is invalidated |

## Entity: ActiveIntent

Purpose: User-facing actionable state for a capture.

Fields:

| Field | Type | Notes |
|-------|------|-------|
| `intent_id` | String | UUID primary key |
| `capture_id` | String | FK to `IntentEnvelopeEntity.id` |
| `intent_type` | IntentCategory | Buy later, recipe, receipt, etc. |
| `status` | ACTIVE / RESOLVED / EXPIRED / ARCHIVED / INVALIDATED | ACTIVE is the default only when completion key is sufficient or likely recoverable |
| `primary_evidence_json` | String | Compact evidence used for card display, no raw image/text bodies |
| `primary_action` | String? | Suggested next action label/type |
| `due_at` | Long? | For events, chat deadlines, reminders |
| `expires_at` | Long? | For coupons, QR/tickets, return windows |
| `resolution_reason` | String? | BOUGHT, NOT_INTERESTED, COOKED, READ, VISITED, REPLIED, USER_ARCHIVED, AUTO_EXPIRED, SUPERSEDED, SOURCE_DELETED |
| `resolved_at` | Long? | Set for resolved/expired/archived/invalidated |
| `user_confirmed` | Boolean | True when user explicitly resolved or approved action |
| `created_at` | Long | Epoch millis |
| `updated_at` | Long | Epoch millis |

Rules:

- Active Intent rows are sidecars; deleting a capture invalidates them.
- Resolved or archived rows hide from Active Intent but remain searchable.
- No row may trigger a notification unless the user approves a deadline reminder.
- No row may execute an external action without user confirmation.

## Enum: IntentCategory

Values:

- `BUY_LATER_PRODUCT`
- `RECIPE`
- `QR_OR_BARCODE`
- `RECEIPT_OR_ORDER`
- `EVENT_TICKET_RESERVATION`
- `COUPON_OR_PROMO`
- `READ_OR_WATCH_LATER`
- `PLACE_OR_TRAVEL_IDEA`
- `GIFT_IDEA`
- `CHAT_ACTION`
- `MAYBE_OLD_OR_INACTIVE`
- `UNKNOWN`

## Entity: CompletionKey

Purpose: The minimum evidence needed to resolve an Active Intent.

Common shapes:

| Category | Required fields |
|----------|-----------------|
| Product | `productName`, `merchantOrSource`, `visiblePrice?`, `urlOrDeepLink?` |
| Recipe | `recipeTitle`, `source?`, `ingredients[]` |
| QR/barcode | `decodedPayload`, `expiry?`, `context?` |
| Receipt/order | `merchant`, `orderId?`, `total?`, `date?`, `trackingOrReturnClue?` |
| Event/ticket | `dateTime`, `location?`, `confirmationId?`, `ticketIdentity?` |
| Coupon | `code`, `merchant?`, `expiresAt?`, `terms?` |
| Read/watch later | `title`, `source`, `canonicalUrlOrAppIdentity?` |
| Place | `placeName`, `addressOrMapIdentity?`, `source?` |
| Gift | `item`, `recipientOrOccasion?`, `source?`, `price?` |
| Chat action | `personOrSource`, `requestedAction`, `deadline?` |

Rules:

- Missing required fields set `completion_key_status = MISSING` and produce LIMITED understanding.
- Completion keys must cite evidence bundle IDs or compact evidence references.
- Pure model guesses are never enough for a high-confidence completion key.

## Entity: SourceIdentity

Fields:

| Field | Type | Notes |
|-------|------|-------|
| `provider` | String? | Site/provider/merchant when evidence supports it |
| `foreground_app_package` | String? | Package at capture time |
| `foreground_app_label` | String? | Human label at capture time |
| `category` | AppCategory / IntentCategory? | Fallback grouping |
| `trust_level` | HIGH / MEDIUM / LOW_MEDIUM / LOW / UNKNOWN | Separate from provider string |
| `evidence_basis` | URL / DEEPLINK / FOREGROUND_APP / OCR_LOGO / OCR_TEXT / MODEL_INFERENCE / CATEGORY_ONLY / UNKNOWN | What supports the claim |
| `confidence` | Float | Numeric confidence |

Source hierarchy:

1. URL/deeplink/share intent.
2. Foreground app/package.
3. OCR from app chrome or visible logo.
4. Product/source inferred from visible text.
5. Pure model guess.
6. Generic category or unknown.

## Entity: EvidenceBundle

Existing v8 entity remains valid, but `payloadJson` needs structured metadata-only shapes for Active Intent.

Allowed evidence types:

- `OCR_SPANS`
- `SOURCE_CONTEXT`
- `URL_METADATA`
- `COMPLETION_KEY_EXTRACTION`
- `PUBLIC_FETCH`
- `PARSER_OUTPUT`
- `MODEL_ATTEMPT`
- `SKIPPED`
- `FAILURE`

Payload rules:

- No raw HTML body.
- No full screenshots.
- No full text body.
- No embeddings.
- Include compact snippets, spans, bounding boxes, hashes, counts, evidence method, confidence, and local raw reference IDs only.

## Invalidation Semantics

Invalidation reasons:

- `CAPTURE_DELETED`
- `USER_RESOLVED`
- `USER_ARCHIVED`
- `TIME_EXPIRED`
- `SUPERSEDED`
- `SOURCE_DELETED`
- `RECEIPT_MATCHED`
- `OPENED_OR_COMPLETED_FROM_ORBIT`
- `NO_LONGER_ACTIONABLE`
- `CORRECTION_APPLIED`

Rules:

- Source capture invalidation blocks CaptureUnderstandingResult and ActiveIntent from downstream retrieval/actions.
- Resolved/archived/expired Active Intent rows remain searchable unless source capture is deleted.
- Superseded captures should link to the newer capture ID when known.
