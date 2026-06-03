# Feature Specification: Screenshot Cleanup + Active Intent

**Feature Branch**: `feature/004-active-intent-cleanup-20260518`
**Status**: Planning locked for implementation slice 1
**Input**: Product pivot: Basic mode should answer "Which screenshots still need something from me?"

## Summary

Orbit should turn saved screenshots and captures into a small, trustworthy cleanup queue. After a capture is saved, Orbit performs Basic local understanding, classifies whether the capture still represents unfinished user intent, extracts the minimum completion key, and shows unresolved items in an Active Intent surface.

This feature is not Ask Orbit, not a knowledge graph, and not an autonomous agent. It is the substrate that makes those later features honest: source identity, evidence limits, invalidation, compact summaries, and explicit escalation.

## Current Context

- The app package is now `com.orbit.app`.
- Room is currently version 7; the next migration for this feature is v7 to v8.
- Debug Diagnostics and Cluster Eval activities are no longer launcher icons.
- `RuntimeFlags.useNewVisualLanguage` defaults to `true` for alpha.
- README and constitution now describe Orbit as local-first and cloud-augmented, not local-only.

## User Stories

### User Story 1 - Active Screenshot Cleanup (P1)

As a user with many screenshots, I want Orbit to show which captures still need something from me so I can close loops without manually sorting my gallery.

**Independent Test**: Seed captures representing products, recipes, receipts, coupons, events, read/watch later, places, and chat actions. Basic mode creates Active Intent rows only for unresolved or likely recoverable captures and leaves stale/non-actionable captures out of the primary queue.

### User Story 2 - Trustworthy Basic Understanding (P1)

As a user, I want Orbit to be clear about what it knows locally, what evidence it used, and what is missing, so I can trust the cleanup queue.

**Independent Test**: For a screenshot-only capture, Basic mode never calls network or cloud code, records evidence limits, and marks missing completion keys instead of inventing details.

### User Story 3 - Explicit Escalation (P2)

As a user, I want to ask Orbit to try harder on a specific capture without enabling broad cloud behavior.

**Independent Test**: Smart or Deep escalation is user-triggered, audit logged, routed through existing network boundaries, and never sends raw screenshots, raw HTML, embeddings, or full evidence bundles over Binder.

### User Story 4 - Invalidation And Resolution (P1)

As a user, I want resolved, archived, expired, deleted, or corrected captures to disappear from Active Intent without losing auditability.

**Independent Test**: Deleting or resolving a capture invalidates derived understanding and Active Intent rows; resolved rows hide from the active queue but remain auditable/searchable later.

## Functional Requirements

- **FR-004-001**: System MUST add local sidecar storage for capture understanding, compact evidence, invalidation, and active intent state using a v7 to v8 Room migration.
- **FR-004-002**: Basic mode MUST be deterministic and local. It may use foreground package, app labels/categories, URL/deeplink/canonical URL, timestamp, existing OCR/text signals, and local regexes. It MUST NOT call cloud LLMs, public fetch, oEmbed, Readability, browser automation, or VLM.
- **FR-004-003**: System MUST classify captures into the initial Active Intent categories: buy later product, recipe, QR/barcode, receipt/order, event/ticket/reservation, coupon/promo, read/watch later, place/travel idea, gift idea, chat action, maybe old/inactive, unknown.
- **FR-004-004**: System MUST extract a completion key status: found, missing, not actionable, or needs escalation.
- **FR-004-005**: System MUST store only compact evidence summaries in Room and Binder-facing models. Raw HTML, full screenshots, full OCR bodies, embeddings, prompts, and model responses MUST NOT cross Binder or be stored in evidence sidecars for this slice. Compact fields MUST use explicit length caps and allowlisted JSON keys.
- **FR-004-006**: Active Intent rows MUST support lifecycle statuses: active, resolved, archived, expired, and invalidated. `MAYBE_OLD_OR_INACTIVE` is an intent category, not a lifecycle status.
- **FR-004-007**: User resolution actions MUST include at least: bought, not interested, cooked, read/watched, visited, replied/done, archived, expired, and invalidated.
- **FR-004-008**: Any Smart or Deep escalation MUST create an audit event before dispatch and must remain capability-scoped.
- **FR-004-009**: Spec 004 MUST NOT add new network clients outside `com.orbit.app.net.*`.
- **FR-004-010**: Spec 004 MUST NOT add new direct database access from `:capture` or `:ui`; new storage writes should be implemented behind the existing `:ml` ownership boundary or explicitly documented as technical debt before coding.
- **FR-004-011**: The user-facing Orbit Follow-ups surface MUST be narrower than the underlying Active Intent sidecar table. It should surface only concrete next-action signals: explicit reply/action-language chat captures, chat captures with a found completion key, QR/barcode, receipt/order, event/reservation, and coupon/promo captures with a found completion key, plus explicit Orbit review rows. Product pages, recipes, places, gifts, read/watch-later items, unknown captures, and stale captures remain available in Diary/Library/search unless the user explicitly escalates or reviews them.

## Non-Goals

- Ask Orbit chat.
- Knowledge graph tables or graph backend.
- Autonomous agent planning/execution.
- Default notifications.
- Auto-delete.
- Cloud VLM by default.
- External writes without confirmation.

## Stop Signs

- A task requires raw screenshots, full text, raw HTML, embeddings, prompts, or model responses over Binder.
- A task adds network code outside `com.orbit.app.net.*`.
- A task relies on cloud to make Basic mode useful.
- A task adds new direct Room access outside `:ml` without explicit approval.
