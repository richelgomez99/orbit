# Research: Screenshot Cleanup + Active Intent

## Decision 1: Basic Mode Is Local And Deterministic

**Decision**: Basic mode uses local metadata, source/app hints, URL identity, existing local OCR/text hints, and deterministic parsers only.

**Rationale**: The product wedge is trust. If Basic requires cloud to be useful, Orbit cannot honestly explain what it knows or preserve a local-mode fallback.

**Rejected**:

- Cloud VLM by default: too expensive and privacy-sensitive for the first loop.
- Public fetch by default: useful later, but not Basic.
- Browser automation: out of scope for this branch.

## Decision 2: Active Intent Is A Sidecar

**Decision**: Add Active Intent as derived sidecar storage rather than modifying `IntentEnvelopeEntity` semantics.

**Rationale**: Captures are source facts; Active Intent is an invalidatable interpretation that changes when the user resolves, archives, expires, corrects, or deletes a capture.

## Decision 3: Completion Key Status Is First-Class

**Decision**: Store whether the completion key is found, missing, not actionable, or needs escalation.

**Rationale**: The useful answer is not just a category. The user needs to know whether Orbit has enough to help close the loop.

## Decision 4: No New Direct DB Access Outside `:ml`

**Decision**: Spec 004 should avoid adding direct `OrbitDatabase.getInstance()` calls from `:ui` or `:capture`.

**Rationale**: A previous audit finding is still true: current code has direct DB calls outside the stated `:ml` ownership boundary. This branch should not deepen that debt.

## Decision 5: Backup Policy Is Deferred But Not Forgotten

**Decision**: Do not block Spec 004 slice 1 on backup/data extraction cleanup, but track it as deferred risk.

**Rationale**: The existing XML is template-like with `allowBackup=true`. That deserves a focused privacy/storage pass. The first Active Intent slice should not silently expand backup-sensitive data without revisiting this.

## Initial Category Set

- Buy later product
- Recipe
- QR/barcode
- Receipt/order
- Event/ticket/reservation
- Coupon/promo
- Read/watch later
- Place/travel idea
- Gift idea
- Chat action
- Maybe old/inactive
- Unknown

## Evidence Ladder

1. Strong URL/deeplink/canonical URL.
2. Foreground app and package label.
3. App category dictionary.
4. Existing OCR/text hint.
5. Local regex completion key.
6. Timestamp/context.
7. User correction or explicit escalation.

## Open Questions For Implementation Slice 1

- Which existing capture path provides local OCR/text hints today, and are they already available at seal time?
- Should Active Intent repository access extend `EnvelopeRepositoryService` or use a separate `ActiveIntentRepositoryService` in `:ml`?
- What is the smallest UI route that lets alpha users actually review Active Intent without building a new top-level navigation system?
