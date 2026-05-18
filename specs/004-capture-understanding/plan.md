# Implementation Plan: Capture Understanding - Active Intent Cleanup

## Summary

Spec 004 is reframed around Active Intent cleanup. The existing implementation already provides a trust foundation: Basic local understanding, source identity, canonical URL handling, content hashing, evidence bundles, user-triggered escalation, invalidation, and compact detail UI. The next pass should build the product loop on top of that foundation.

Product loop:

1. User takes or saves a screenshot/capture with no required tagging.
2. Orbit performs Basic local understanding.
3. Orbit classifies likely saved intent.
4. Orbit extracts the completion key or clearly marks what is missing.
5. Orbit creates or updates an Active Intent sidecar row.
6. Active Intent shows only unresolved, still-relevant saves.
7. User resolves, archives, marks not interested, approves a deadline reminder, or escalates for more context.

## Technical Context

- Android/Kotlin app package: `com.orbit.app`.
- Room database currently at v8 with capture understanding entities.
- Network clients must remain in `com.orbit.app.net.*`.
- Basic mode must not call network, cloud, oEmbed, public fetch, ReadabilityExtractor, or model providers.
- Binder/IPC must pass compact IDs/statuses/summaries only, never raw HTML, full screenshots, full text, embeddings, prompts, model responses, or full evidence bundles.

## Scope

### In Scope

- Active Intent data model and DAO.
- Completion key extraction schemas for the first 10 screenshot categories.
- Basic local classifier using OCR/source/app/category hints.
- Active Intent list UI grouped by intent category and status.
- Resolution actions: bought, not interested, cooked, read/watched, visited, replied/done, archived, expired, invalidated.
- Source-confidence hierarchy and UI language.
- Device validation with real screenshot piles.

### Out of Scope

- Ask Orbit.
- Autonomous agent behavior.
- Default notification flows.
- Auto-delete.
- Cloud VLM by default.
- External writes without confirmation.

## Architecture

### Data Layer

Add a v9 sidecar if this lands after the v8 implementation:

- `ActiveIntentEntity`
- `ActiveIntentDao`
- `CompletionKey` domain classes serialized into compact JSON.
- Extended source identity JSON with trust level and evidence basis.

Use sidecar rows rather than overloading `IntentEnvelopeEntity`. The envelope remains the raw saved item; Active Intent is a derived, invalidatable view of unresolved saved intent.

### Engine Layer

Add:

- `IntentCategoryClassifier`
- `CompletionKeyExtractor`
- `ActiveIntentResolver`
- `ActiveIntentInvalidationPolicy`
- `SourceEvidenceRanker`

Basic mode should be deterministic and local-first:

- foreground app/package
- app category dictionary
- capture timestamp
- OCR spans when available
- URL/deeplink/canonical URL when available
- known regexes for dates, prices, order IDs, coupon codes, QR payloads, addresses, ingredients, and URLs

Smart/Deep may improve completion keys only after audit-logged user escalation.

### UI Layer

Add:

- Active Intent screen grouped by category.
- Active Intent card with thumbnail, category, source app/provider, extracted evidence, confidence, primary action, resolve/archive controls.
- Empty/cleanup state for `Probably old or inactive`.
- Source confidence copy that distinguishes app/source/evidence basis.

Do not make the first screen a chatbot. The primary CTA is Review Active Intent.

## Validation

### Automated

- Unit tests for classifier fixtures.
- Unit tests for completion key extraction per category.
- Unit tests for source-confidence hierarchy.
- Unit tests for status transitions and invalidation.
- Room migration test for Active Intent sidecar.
- IPC/lint checks for raw-content boundary.

### Product Biopsy

Before overbuilding, run a screenshot biopsy with 15-20 Android users who screenshot heavily.

For each user's last 100 screenshots, label:

- Was there intent?
- Is it still active?
- What category?
- What evidence would make it useful?
- What action would close it?
- Should it be hidden from Active Intent?
- Would the user trust Orbit to infer this automatically?

Pass thresholds:

- 40%+ of screenshots have recoverable intent.
- 25%+ are still active or unresolved.
- 60%+ of active screenshots have extractable completion keys.
- 50%+ of users say cleanup makes Gallery feel less overwhelming.
- 30%+ take a real action during test.

## Implementation Order

1. Amend v8 foundation with domain enums and storage for intent category/completion key if possible without risky migration churn; otherwise add v9 sidecar.
2. Build pure classifier/extractor fixtures first.
3. Add Active Intent DAO/repository and status transitions.
4. Integrate BasicUnderstandingEngine -> ActiveIntentResolver.
5. Build Active Intent list UI.
6. Add device validation script and quickstart record.
7. Only then revisit Smart/Deep enrichment for categories where Basic misses completion keys.

## Risks

- Screenshot categories may be noisier than expected; biopsy gates prevent overbuilding.
- Completion keys can invite hallucination; LIMITED state must be normal and honest.
- Notification creep would undermine the product stance; keep reminders opt-in.
- Medical/anxiety claims are unnecessary; use mental clutter/open loop language.
