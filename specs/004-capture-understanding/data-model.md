# Data Model: Screenshot Cleanup + Active Intent

## Overview

Spec 004 adds local sidecars that turn captures into a trustworthy cleanup queue. The envelope remains the saved artifact. Understanding and Active Intent are derived, invalidatable views.

Current database version is 7. This feature should add migration v7 to v8.

## Table: capture_understanding

Purpose: compact, evidence-bounded understanding for one capture.

| Column | Type | Notes |
| --- | --- | --- |
| `capture_id` | TEXT PRIMARY KEY | References `intent_envelope(id)` with delete invalidation/cascade behavior |
| `mode` | TEXT | `BASIC`, `SMART`, `DEEP`; first slice writes `BASIC` |
| `status` | TEXT | `READY`, `LIMITED`, `FAILED` |
| `category` | TEXT | Active Intent category or `UNKNOWN` |
| `category_confidence` | REAL | 0.0 to 1.0 |
| `title` | TEXT nullable | compact display label, max 120 chars |
| `summary_text` | TEXT nullable | bounded by available evidence, max 280 chars |
| `completion_key_json` | TEXT nullable | compact structured key, max 2048 chars |
| `completion_key_status` | TEXT | `FOUND`, `MISSING`, `NOT_ACTIONABLE`, `NEEDS_ESCALATION` |
| `source_identity_json` | TEXT nullable | provider/app/category/confidence/evidence basis, max 2048 chars |
| `content_hash_hex` | TEXT nullable | raw artifact or normalized content hash |
| `canonical_url` | TEXT nullable | URL identity when known |
| `grounding_constraints_json` | TEXT | limitations and evidence boundaries, max 2048 chars |
| `created_at` | INTEGER | epoch millis |
| `updated_at` | INTEGER | epoch millis |
| `invalidated_at` | INTEGER nullable | set when derived state is invalid |

Indexes:

- `capture_id` unique primary key
- `category`
- `completion_key_status`
- `content_hash_hex`
- `canonical_url`

## Table: evidence_bundle

Purpose: compact evidence descriptors used to justify Basic understanding.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | TEXT PRIMARY KEY | UUID |
| `capture_id` | TEXT | references `intent_envelope(id)`; evidence rows should delete with parent capture |
| `bundle_type` | TEXT | `OCR_HINT`, `URL_METADATA`, `APP_CONTEXT`, `LOCAL_REGEX`, `SKIPPED`, `FAILURE` |
| `payload_json` | TEXT | compact descriptor only, max 2048 chars |
| `created_at` | INTEGER | epoch millis |

Payload must not contain raw HTML, full screenshots, full OCR bodies, embeddings, prompts, or model responses.

Allowed `payload_json` keys for slice 1:

- `kind`
- `label`
- `source`
- `confidence`
- `excerpt`
- `hash`
- `reason`
- `createdAt`

`excerpt` must be capped at 240 chars and must be a short snippet, never a full OCR body or article body.

## Table: invalidation_record

Purpose: durable record that derived state was invalidated.

| Column | Type | Notes |
| --- | --- | --- |
| `capture_id` | TEXT PRIMARY KEY | capture id; intentionally not a cascading FK so it can survive as a tombstone after parent deletion |
| `invalidated_at` | INTEGER | epoch millis |
| `reason` | TEXT | `CAPTURE_DELETED`, `USER_REQUESTED`, `CORRECTION_APPLIED`, `SOURCE_SUPERSEDED` |

## Table: active_intent

Purpose: user-facing unresolved intent row for screenshot cleanup.

| Column | Type | Notes |
| --- | --- | --- |
| `intent_id` | TEXT PRIMARY KEY | UUID |
| `capture_id` | TEXT | references `intent_envelope(id)`; active intent rows should invalidate or delete with parent capture |
| `intent_type` | TEXT | category enum |
| `status` | TEXT | `ACTIVE`, `RESOLVED`, `ARCHIVED`, `EXPIRED`, `INVALIDATED` |
| `completion_key_json` | TEXT nullable | compact key needed to act, max 2048 chars |
| `completion_key_status` | TEXT | mirrors projected understanding status |
| `primary_evidence_json` | TEXT | compact display evidence, max 2048 chars |
| `primary_action` | TEXT nullable | suggested action label/type, max 80 chars |
| `due_at` | INTEGER nullable | events, deadlines, reminders after approval |
| `expires_at` | INTEGER nullable | coupons, tickets, return windows |
| `resolution_reason` | TEXT nullable | reason enum |
| `resolved_at` | INTEGER nullable | epoch millis |
| `user_confirmed` | INTEGER | 0/1 boolean |
| `created_at` | INTEGER | epoch millis |
| `updated_at` | INTEGER | epoch millis |

Indexes:

- `capture_id`
- `status`
- `intent_type`
- `due_at`
- `expires_at`

Display contract:

- `active_intent` is a derived sidecar/audit substrate, not a direct 1:1 UI queue.
- The Orbit Follow-ups surface filters these rows to concrete next-action signals only:
  - `CHAT_ACTION` with explicit reply/action text or a found completion key.
  - `QR_OR_BARCODE`, `RECEIPT_OR_ORDER`, `EVENT_TICKET_RESERVATION`, and `COUPON_OR_PROMO` with `completion_key_status = FOUND`.
  - `ORBIT_REVIEW` evidence rows created by an explicit user review/escalation.
- `BUY_LATER_PRODUCT`, `RECIPE`, `READ_OR_WATCH_LATER`, `PLACE_OR_TRAVEL_IDEA`, `GIFT_IDEA`, `MAYBE_OLD_OR_INACTIVE`, and `UNKNOWN` should remain browsable/searchable memory by default, not pressure-producing follow-up work.

## Enums

`MAYBE_OLD_OR_INACTIVE` is a category used for grouping and triage. It is not an `ActiveIntentStatus` lifecycle value.

### IntentCategory

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

### CompletionKeyStatus

- `FOUND`
- `MISSING`
- `NOT_ACTIONABLE`
- `NEEDS_ESCALATION`

### ResolutionReason

- `BOUGHT`
- `NOT_INTERESTED`
- `COOKED`
- `READ_OR_WATCHED`
- `VISITED`
- `REPLIED_OR_DONE`
- `USER_ARCHIVED`
- `AUTO_EXPIRED`
- `INVALIDATED`
- `SOURCE_DELETED`

## Invalidation Rules

- Deleting a capture invalidates `capture_understanding` and `active_intent` rows.
- Resolving or archiving an Active Intent row hides it from the active queue but preserves audit/search value.
- Applying correction feedback invalidates stale derived understanding before recomputation.
- No row may trigger a notification or external action without explicit user confirmation.
