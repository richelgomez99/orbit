# Feature Specification: Capture Understanding - Active Intent Cleanup

**Feature Branch**: `004-capture-understanding`
**Created**: 2026-05-13
**Status**: DRAFT (Reframed)
**Input**: Strategic decision: "Orbit Basic Mode: turn Android screenshots into Active Intent."

Capture Understanding turns saved screenshots and captures into evidence-backed Active Intent: a quiet list of things that may still be worth doing. The public promise is closure, not productivity and not a chatbot. Orbit should help users stop carrying their screenshot pile in their head by identifying unresolved saves, extracting the evidence needed to resolve them, and letting the user mark them done, archived, expired, invalidated, or still active.

Basic mode answers one product question:

> Which screenshots still need something from me?

The feature still provides source identity, content hashes, canonical URLs, evidence bundles, grounding constraints, user-triggered Smart/Deep escalation, correction feedback, and deletion/invalidation behavior. The key change is that these are no longer generic understanding outputs. They exist to support a local-first Active Intent cleanup loop.

## Product Principles

- **Closure before search**: Search and Ask come later. The first mainstream loop is screenshot -> intent -> completion key -> possible action -> resolved.
- **No shame language**: Internally, the user may be an everyday digital hoarder. Publicly, Orbit speaks about screenshot piles, unfinished saves, open loops, and mental clutter.
- **Completion key or humility**: A capture is not truly understood until Orbit extracts the evidence needed to resolve it. If the key is missing, Orbit must say what is missing.
- **Local first by default**: Basic mode uses local capture context, OCR, hashes, source app, timestamp, local category rules, and compact evidence. Smart/Deep are explicit user-triggered escalations.
- **Resolve, do not nag**: Orbit creates an Active Intent list, not a notification cannon. Notifications are opt-in and only for obvious deadline-based captures.
- **Never auto-delete**: Resolved means hidden from Active Intent but still searchable unless the user explicitly deletes the capture.

## User Scenarios & Testing

### User Story 1 - Review Active Intent (Priority: P1)

As an Orbit user, I want to see screenshots and captures that may still need action, grouped by intent, so I can clear unfinished saves without manually digging through Gallery.

**Why this priority**: This is the product wedge. The first useful surface is not a search box or chatbot; it is an Active Intent cleanup list.

**Independent Test**: Import or create a mixed set of screenshots. Open Active Intent and verify Orbit groups active captures into categories such as Buy Later, Recipes, Receipts, Coupons, QR/Tickets, Places, Gift Ideas, Read/Watch Later, Chat Actions, and Maybe Old/Inactive.

---

### User Story 2 - Extract The Completion Key (Priority: P1)

As an Orbit user, I want each actionable capture to show the specific evidence needed to resolve it, so I can decide quickly whether to act, archive, or mark it done.

**Why this priority**: Classification alone is demo-level. Completion keys make the product useful: product identity, ingredients, order ID, decoded QR payload, event date/time/location, coupon code/expiry, canonical URL, place identity, or requested action.

**Independent Test**: For each top screenshot category, verify Orbit either extracts the completion key with evidence and confidence or shows a limited state explaining what it could not find.

---

### User Story 3 - Trust Source Identity And Evidence (Priority: P1)

As an Orbit user, I want Orbit to distinguish provider, source app, visible text/logo evidence, and low-confidence inference, so I can trust why Orbit thinks a saved thing came from a particular app, merchant, or site.

**Why this priority**: Source identity is commercially powerful only when it is honest. `Captured from Instagram; product appears to be Zara` is different from `Captured from Zara app`.

**Independent Test**: Save captures with a foreground app package, share/deeplink URL, visible logo text, and model-only guess. Verify the UI labels the source-confidence tier correctly and does not overclaim.

---

### User Story 4 - Control Understanding Depth (Priority: P1)

As an Orbit user, I want Basic understanding by default, with Smart and Deep as explicit user-triggered actions, so I control risk, cost, network work, and cloud use.

**Why this priority**: Orbit is quiet and local-first. The cleanup loop should not silently browse, enrich, or send private screenshot content away from the device.

**Independent Test**: Save a capture and verify it defaults to Basic. Tap Get More Context and verify Smart/Deep escalation is audit-logged before dispatch and falls back locally when offline or cloud is disabled.

---

### User Story 5 - Resolve Or Invalidate Intent (Priority: P1)

As an Orbit user, I want active captures to become resolved, expired, archived, superseded, invalidated, or still active, so Orbit reduces clutter instead of creating another list to manage.

**Why this priority**: The goal is not just to remember. The goal is to know what no longer needs attention.

**Independent Test**: Mark captures bought, not interested, cooked, read, visited, replied, expired, superseded, or deleted. Verify Active Intent removes them from active view while preserving searchable records unless explicitly deleted.

## Dead Intent Taxonomy

| Type | Completion key | Done state |
|------|----------------|------------|
| Buy later product | product name, merchant/source, visible price, link/deeplink when available | add to list, bought, not interested |
| Recipe | recipe title/source, ingredient list | add ingredients, save recipe, cooked, not cooking |
| QR/barcode | decoded payload, expiry/context when visible | copy/open code, quick access, expired/used |
| Receipt/order | merchant, order ID, total, date, tracking/return clue | track, expense, return reminder, resolved |
| Event/ticket/reservation | date/time, location, confirmation/ticket identity | calendar draft, directions, quick access, passed |
| Coupon/promo | code, merchant, expiration, terms/minimum spend | copy, approved reminder, expired, archived |
| Read/watch later | title, source, canonical URL/app identity | queue, cite/summarize, read/watched |
| Place/travel idea | place name, address/map identity, source | save place/trip, open maps, visited/not interested |
| Gift idea | item, possible recipient/occasion, source/price | gift list, reminder, bought/not interested |
| Chat action | person/source, requested action, deadline if present | task/reminder/reply draft, done, stale |

## Source-Confidence Hierarchy

| Evidence | Trust level | Example |
|----------|-------------|---------|
| Foreground app/package at capture | High | `com.zara.android` |
| Share intent, deeplink, or URL | High | Zara product URL |
| OCR from app chrome or visible logo | Medium | visible Zara logo |
| Product name inferred from image/text | Low-medium | `black midi dress` |
| Pure model guess | Low | `probably Zara` |

Orbit must render source claims with the evidence basis, for example: `Source: Instagram. Product: Zara, inferred from visible text. Price shown: $49.`

## Requirements

### Functional Requirements

- **FR-004-001**: System MUST create a first-class **Active Intent** record for each capture that appears actionable, with status `active`, `resolved`, `expired`, `archived`, or `invalidated`.
- **FR-004-002**: System MUST classify active screenshots into the Dead Intent taxonomy or `maybe_old_or_inactive` without requiring tagging at capture time.
- **FR-004-003**: System MUST extract a **Completion Key** for each actionable category. If the key is missing, the result MUST be Limited and explain which evidence is missing.
- **FR-004-004**: System MUST create a first-class **Source Identity** for every capture, prioritizing URL/deeplink evidence and foreground app package, then visible OCR/logo evidence, then generic category, then unknown.
- **FR-004-005**: System MUST record source-confidence tier and evidence basis separately from provider/app labels.
- **FR-004-006**: System MUST default to **Basic** local understanding for all new captures. **Smart** and **Deep** modes MUST be explicit user-triggered escalations.
- **FR-004-007**: System MUST calculate and store a **Content Hash** for every captured artifact and support normalized content hashes for enriched text when available.
- **FR-004-008**: System MUST produce **Evidence Bundles** for OCR spans, metadata, source context, completion-key extraction, public fetches, model attempts, skipped work, and failures. Bundles MUST include grounding constraints for future retrieval/actions.
- **FR-004-009**: System MUST recognize YouTube-family URLs and handle canonicalization consistently (`youtu.be`, `youtube.com/watch`, `youtube.com/shorts`, embeds, and no-cookie variants).
- **FR-004-010**: System MUST record **Correction Feedback** when the user marks source, category, completion key, summary, or relevance as wrong.
- **FR-004-011**: System MUST implement an **Invalidation Kill-Switch**: derived understanding and Active Intent rows MUST cite source capture IDs and block downstream use after invalidation.
- **FR-004-012**: System MUST produce a **Capture Understanding Result** with status (`Ready`, `Limited`, `Failed`), category, completion key, title, compact summary, source identity, and based-on evidence.
- **FR-004-013**: System MUST NOT allow summaries or completion-key claims to exceed the available evidence, especially for metadata-only, visual-only, or OCR-only captures.
- **FR-004-014**: System MUST provide a local-mode fallback when cloud is disabled or unavailable, ensuring save success with Limited understanding labels.
- **FR-004-015**: System MUST NOT auto-delete captures. Resolution hides items from Active Intent but keeps them searchable unless the user explicitly deletes them.
- **FR-004-016**: System MUST NOT enable default notifications for Active Intent. Deadline notifications require user approval and are limited to clear expiry/date captures.

### Key Entities

- **Capture Understanding Result**: Evidence-backed result for one capture, including category, source identity, completion key, status, summary, limitations, hashes, and evidence IDs.
- **Active Intent**: Actionable sidecar row for a capture with intent type, status, primary evidence, primary action, due/expiry dates, resolution reason, and user-confirmed state.
- **Completion Key**: The minimum evidence needed to resolve an intent, such as product identity, ingredients, QR payload, order ID, event details, coupon code, canonical URL, place identity, or requested action.
- **Source Identity**: Provider, foreground app, category, source-confidence tier, and evidence basis.
- **Evidence Bundle**: Immutable metadata-only record of what Orbit saw or acquired, including OCR spans, bounding boxes, timestamp, source app, extraction method, confidence, and cloud audit pointer when applicable.
- **Grounding Constraints**: Instructions for future LLM/retrieval/action features defining the boundaries of what is known.
- **Invalidation Record**: Tombstone entry used to block citation or action from deleted, superseded, expired, or user-invalidated content.

## Non-Goals

- Do not build Ask Orbit or general question-answering flows here (Spec 005).
- Do not build a full autonomous agent here.
- Do not market or label users as hoarders in product UI or public copy.
- Do not implement autonomous background cloud enrichment without user consent.
- Do not auto-delete screenshots or captures.
- Do not enable default notification pressure for non-deadline captures.
- Do not allow raw HTML, full screenshots, full text, prompts, model responses, embeddings, or full evidence bundles to cross Binder/IPC process boundaries.

## Success Criteria

- **SC-004-001**: At least 40% of a representative screenshot biopsy corpus has recoverable intent.
- **SC-004-002**: At least 25% of screenshots in the biopsy corpus are still active or unresolved.
- **SC-004-003**: Orbit extracts a useful completion key for at least 60% of active screenshots in the biopsy corpus.
- **SC-004-004**: At least 50% of test users say Active Intent cleanup makes their Gallery feel less overwhelming.
- **SC-004-005**: At least 30% of test users take a real action: archive, resolve, add to list, copy code, create reminder, save place, or mark not interested.
- **SC-004-006**: 100% of captures default to Basic mode; escalation to Smart/Deep is logged as user intent before dispatch.
- **SC-004-007**: Deleting or invalidating a capture blocks derived understanding and Active Intent rows within 1 second.
- **SC-004-008**: Zero full-content or source overclaims are observed in metadata-only, visual-only, OCR-only, or low-confidence source cases.
