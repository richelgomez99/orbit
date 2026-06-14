# Research: Resolution Semantics

## Decision 1: Add `resolution_receipt` Instead Of Overloading Audit

**Decision**: Add a compact local Room table for resolution receipts.

**Rationale**: Audit rows answer "what happened" for accountability. Resolution receipts answer "how should Orbit treat this target next time?" Follow-up surfacing, duplicate suppression, agent planning, and future KG conflict handling need queryable semantic state without scraping audit descriptions.

**Alternatives considered**:

- Audit-only: rejected because audit is append-only narrative, not a stable product-state API.
- Extend every existing table with bespoke columns: rejected because duplicate/action/active-intent/todo/memory/graph targets would drift again.
- Cloud memory index metadata: rejected because resolution is local-first source-of-truth state.

## Decision 2: Receipts Are Compact Provenance, Not Content

**Decision**: Receipt metadata stores ids, enums, counts, timestamps, matched-by labels, and short reason codes only.

**Rationale**: Spec 004/005 privacy constraints already ban raw screenshots, full OCR, prompts, model responses, embeddings, JWTs, cookies, and API keys in compact cross-boundary payloads. Resolution receipts are long-lived and will eventually inform KG/agent planning, so they must be safe by construction.

## Decision 3: Differentiate `DISMISSED`, `NOT_NOW`, And `SNOOZED`

**Decision**: These are distinct `ResolutionKind` values with different surfacing rules.

**Rationale**:

- `DISMISSED`: user says this suggestion is not useful; hide indefinitely unless materially new evidence appears.
- `NOT_NOW`: user declines current cleanup pressure; keep as memory and suppress current active queue.
- `SNOOZED`: user wants it later; hide until `effectiveUntilMillis`.

## Decision 4: Aggregate Todo Completion Is Semantic, Per-Item Toggling Is Lightweight

**Decision**: Do not audit every checkbox toggle, but create a receipt when the aggregate list transitions to all-done or reopens after all-done.

**Rationale**: Existing code intentionally avoids audit rows for high-frequency checkbox toggles. Spec 012 preserves that quiet behavior while giving Orbit one durable loop-closure signal.

## Decision 5: Implement Read-Time Surfacing Verdict

**Decision**: Add a small resolver that consumes receipts and returns `ACTIVE`, `HIDDEN_UNTIL`, `DISMISSED`, `RESOLVED`, `INVALIDATED`, or `STALE`.

**Rationale**: This creates one place for Follow-ups, agent coordinator, and future KG/profile surfaces to ask whether a target should be shown.

## Decision 6: Start With Repository Semantics Before UI Polish

**Decision**: P1 implementation should land schema, DAO, repository helpers, duplicate/action/todo hooks, and focused UI affordances only where needed to prove behavior.

**Rationale**: The user has repeatedly flagged repeated rows and bad follow-up logic as the core pain. Durable rules matter more than adding another surface.
