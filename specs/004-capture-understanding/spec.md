# Feature Specification: Capture Understanding

**Feature Branch**: `004-capture-understanding`
**Created**: 2026-05-13
**Status**: Draft
**Input**: User description: "Capture Understanding. Saved captures should become trustworthy understood objects before Orbit builds Ask, actions, memory, KG, or agent coordination on top."

Capture Understanding defines what Orbit can truthfully know about a saved capture immediately after save and after bounded enrichment. The feature turns each saved item into an evidence-backed object with source identity, canonical URL handling, evidence records, a compact summary, known limitations, user-controlled understanding depth, correction feedback, and deletion/invalidation behavior. It prepares the substrate for future retrieval, Ask, memory, KG, and agent work, but it does not implement those features.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See What Orbit Knows About A Capture (Priority: P1)

As an Orbit user, I want the capture detail view to show the source, the evidence Orbit used, a compact summary, and any limitations, so I can decide whether Orbit understood the save well enough to rely on it later.

**Why this priority**: Trustworthy capture detail is the core value of this feature. Future Ask, actions, memory, KG, and agent coordination should not build on captures whose source, evidence, and limitations are ambiguous.

**Independent Test**: Save a public article URL, open the capture detail, and verify that it shows source identity, canonical URL or domain, evidence used, compact summary, and limitations without requiring Ask Orbit or any retrieval feature.

**Acceptance Scenarios**:

1. **Given** a saved public URL with readable content, **When** the user opens the capture detail, **Then** Orbit shows the resolved source, canonical URL or domain, evidence used, compact summary, confidence, and a clear indication that the summary is based on fetched readable text.
2. **Given** a saved URL where only metadata is available, **When** the user opens the capture detail, **Then** Orbit shows a limited or metadata-only understanding and does not present a definitive content summary.
3. **Given** a screenshot-only capture with OCR text, **When** the user opens the capture detail, **Then** Orbit labels the summary as based on visible screenshot text and shows any OCR confidence or extraction limitations.

---

### User Story 2 - Control How Hard Orbit Tries To Understand (Priority: P1)

As an Orbit user, I want Basic, Smart, and Deep understanding controls, plus per-capture overrides, so Orbit can stay lightweight for sensitive or routine saves and try harder for research-worthy captures.

**Why this priority**: The product is local-first and cloud-augmented. Users need a plain-language control over effort, risk, cost, and cloud use before Orbit can deepen understanding safely.

**Independent Test**: Change the global understanding depth and repeat the same URL capture under Basic, Smart, and Deep. Verify that each result records the selected depth, uses only allowed evidence paths, and displays appropriate limitations.

**Acceptance Scenarios**:

1. **Given** Basic depth is selected, **When** a user saves a URL, **Then** Orbit saves it, identifies the likely source, canonicalizes the URL, performs safe public hydration when allowed, and avoids managed extraction, browser fallback, broad search enrichment, and cloud visual understanding.
2. **Given** Smart depth is selected, **When** static public extraction fails for a non-sensitive public URL, **Then** Orbit may use bounded cloud enrichment if policy, budget, and audit controls allow it, and the detail view records what was attempted.
3. **Given** Deep depth is selected for a capture, **When** Orbit tries harder to gather public context, **Then** it still excludes logged-in browsing, CAPTCHA bypass, connected-account access, external writes, and sensitive visual cloud analysis unless those capabilities are separately consented in future features.
4. **Given** a user chooses `Summarize only what I saved` for a capture, **When** Orbit refreshes understanding, **Then** the refreshed understanding uses only the originally saved artifact and records that broader enrichment was suppressed by user choice.

---

### User Story 3 - Trust Source Identity And Duplicate Handling (Priority: P1)

As an Orbit user, I want Orbit to distinguish the content provider, the app I copied from, and the generic category, so a YouTube link copied from Brave, Messages, or another app still appears as YouTube without pretending every video capture is YouTube.

**Why this priority**: Source identity is the first trust signal on every capture surface. Wrong provider glyphs, duplicate URL rows, and vague app/category labels make later summaries and actions less believable.

**Independent Test**: Save YouTube URLs from multiple app contexts and save a generic video-category capture without a provider URL or app label. Verify that provider-backed captures show YouTube with origin context, while category-only captures use a generic video identity.

**Acceptance Scenarios**:

1. **Given** a capture contains a recognized YouTube-family URL, **When** Orbit resolves source identity, **Then** it shows YouTube as the primary source even if the foreground app was a browser or messaging app.
2. **Given** a capture has no recognized provider URL but has foreground app label evidence, **When** Orbit resolves source identity, **Then** it uses that app label as the primary source and keeps category as supporting context.
3. **Given** a capture only has a durable category such as video, browser, social, or reading, **When** Orbit resolves source identity, **Then** it uses a generic category identity and does not show a branded provider glyph or name.
4. **Given** the user captures the same canonical URL again, **When** Orbit recognizes the duplicate, **Then** it keeps the existing capture as the primary record and offers the existing `Already saved` actions instead of creating an indistinguishable duplicate row.

---

### User Story 4 - Correct Bad Understanding (Priority: P2)

As an Orbit user, I want to mark a source or summary as wrong, too broad, or not relevant, so Orbit can record the correction and avoid silently preserving a bad interpretation.

**Why this priority**: Trust is not only about initial accuracy. Users need a way to repair wrong understanding before future retrieval, Ask, memory, or action features rely on it.

**Independent Test**: Save a capture, mark the source as wrong, and refresh understanding. Verify that Orbit records feedback, creates or selects a corrected understanding version, and shows the updated source/summary without losing the original audit trail.

**Acceptance Scenarios**:

1. **Given** a user marks the source as wrong, **When** Orbit records feedback, **Then** the capture detail shows that feedback was recorded and any future source identity version references the correction.
2. **Given** a user marks the summary as wrong or too broad, **When** Orbit refreshes understanding, **Then** the new understanding is linked to the prior one and retains evidence/limitation history.
3. **Given** a user says a capture should never fetch a domain, **When** another capture from that domain is saved, **Then** Orbit suppresses disallowed fetching and shows the resulting limitation.

---

### User Story 5 - Delete Or Invalidate Derived Understanding (Priority: P1)

As an Orbit user, I want deleting a capture to remove or invalidate all derived understanding tied to it, so summaries, evidence, future search inputs, and downstream memory/action candidates cannot outlive their source.

**Why this priority**: Deletion and invalidation are prerequisites for every later cloud, retrieval, KG, memory, and agent feature. Orbit cannot safely build higher-order knowledge until derived data has a mechanical parent link and lifecycle.

**Independent Test**: Save a capture, let source identity/evidence/understanding be created, delete the capture, and verify that derived understanding is no longer displayed or eligible for future downstream use.

**Acceptance Scenarios**:

1. **Given** a capture has source identity, evidence, and understanding records, **When** the user deletes the capture, **Then** user-visible derived records for that capture are removed or invalidated and a deletion/invalidation audit event is recorded without raw content.
2. **Given** future downstream features have derived inputs from this capture, **When** the source capture is deleted, **Then** Orbit marks this capture's derived inputs ineligible so downstream features must drop or re-evaluate their own multi-capture results.
3. **Given** cloud enrichment produced derived references for a capture, **When** the capture is deleted or invalidated locally, **Then** Orbit records the local deletion as the source of truth and queues any permitted cloud deletion/invalidation receipt for audit.

---

### User Story 6 - Stay Useful When Cloud Or Deep Extraction Is Unavailable (Priority: P2)

As an Orbit user, I want captures to save and remain understandable at a limited level when cloud enrichment is disabled, unavailable, over budget, or blocked by policy.

**Why this priority**: Orbit is local-first and cloud-augmented, not cloud-dependent. Save reliability and honest limitations matter more than completing expensive understanding immediately.

**Independent Test**: Disable cloud enrichment or simulate network failure, save a public URL and a screenshot-only capture, and verify that both save successfully with local/source metadata and limitations rather than failing the capture.

**Acceptance Scenarios**:

1. **Given** cloud use is disabled, **When** a user saves a capture, **Then** Orbit stores the capture locally, performs permitted local/basic understanding, and shows limitations for anything it could not determine.
2. **Given** the network is unavailable during Smart or Deep enrichment, **When** the capture is saved, **Then** save succeeds, enrichment is marked pending/limited/failed as appropriate, and the user can retry later.
3. **Given** a cloud budget or policy blocks deeper extraction, **When** Orbit displays the capture detail, **Then** it shows what was skipped and why without exposing raw prompts, raw page content, screenshots, embeddings, or full evidence bundles in cross-process messages.

### Edge Cases

- YouTube detection must cover `youtu.be`, `youtube.com` including subdomains, `youtube-nocookie.com`, Shorts/live/embed-style paths, and scheme-less links.
- A branded provider glyph or provider name must be backed by provider URL evidence or foreground app label evidence. A durable category alone must fall back to generic category identity.
- When provider and foreground app differ, Orbit must preserve both facts, such as `YouTube via Brave` or `YouTube copied from Messages`.
- URL canonicalization must handle tracking parameters, fragments, host casing, mobile subdomains where applicable, and duplicate captures that occur before or after hydration completes.
- Captures with multiple URLs must record which URL is primary for duplicate/source identity purposes and preserve other detected URLs as supporting evidence.
- Private, authenticated, paywalled, blocked, robots-disallowed, or app-only sources must produce limited understanding rather than fabricated summaries.
- Metadata-only evidence must not be phrased as if Orbit read the full page, watched the full video, or inspected the full document.
- YouTube/media summaries require transcript, readable text, documented metadata, or other explicit evidence; otherwise they must be metadata-only with limitations.
- Screenshot-only captures with likely secrets, financial data, health data, workplace auth, or private messages must suppress cloud visual understanding unless a future explicit-consent feature allows it.
- Cloud/network/model/browser/VLM failures must be visible as bounded understanding states, not hidden absences.
- Existing URL hydration results must remain compatible while new understanding records are introduced beside them.
- Deletion must ignore already-deleted captures for duplicate matching and must not leave future retrieval, memory, KG, action, or agent inputs eligible when their source capture is invalidated.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-004-001**: System MUST create or update a first-class source identity for every saved capture, including display label, optional secondary label, confidence, evidence used, limitations, and resolver version.
- **FR-004-002**: Source identity resolution MUST prioritize recognized content-provider URL evidence first, foreground app label evidence second, generic category identity third, and unknown fallback last.
- **FR-004-003**: System MUST NOT display a branded provider glyph or provider name unless provider URL evidence or foreground app label evidence supports it; durable category alone MUST use a generic category identity.
- **FR-004-004**: System MUST recognize YouTube-family URLs across `youtu.be`, `youtube.com` subdomains, `youtube-nocookie.com`, common YouTube path variants, and scheme-less links.
- **FR-004-005**: System MUST preserve both provider and origin-app facts when they differ, including display copy that can explain cases such as a provider link copied from a browser or messaging app.
- **FR-004-006**: System MUST extract, normalize, and store canonical URL information sufficient for duplicate detection, evidence provenance, display, refresh, and deletion/invalidation.
- **FR-004-007**: System MUST integrate with existing duplicate feedback behavior so repeated canonical URL captures return an existing-capture state instead of creating indistinguishable duplicate rows.
- **FR-004-008**: System MUST preserve current URL hydration compatibility; existing hydration results remain readable, while capture detail prefers the newer understanding record when one exists.
- **FR-004-009**: System MUST record evidence bundles describing what Orbit actually saw, fetched, parsed, or inferred from, including evidence kind, source reference, acquisition depth, acquisition method, confidence, content hash or reference, retention class, and limitations.
- **FR-004-010**: System MUST NOT allow summaries, entity labels, future retrieval inputs, future memory candidates, future KG episodes, or future action drafts to cite or rely on evidence unless an evidence bundle exists for that claim.
- **FR-004-011**: System MUST support a global understanding depth policy with Basic, Smart, and Deep tiers expressed in user-facing language rather than vendor or extractor menus.
- **FR-004-012**: Basic depth MUST save the capture, identify the source, canonicalize URLs, use safe local/basic evidence, and show limitations; it MUST NOT automatically use managed extraction, browser-rendered fallback, broad external search enrichment, cloud visual screenshot analysis, connected-account access, or repeated deep retries.
- **FR-004-013**: Smart depth MAY use bounded cloud enrichment for public URLs and important captures when policy, budget, sensitivity checks, and auditability allow it; it MUST suppress deeper enrichment for sensitive apps, sensitive evidence, blocked/private sources, domain suppressions, and exhausted budgets.
- **FR-004-014**: Deep depth MAY try harder to gather public context and richer public evidence, but it MUST still exclude logged-in browsing, CAPTCHA bypass, connected-account access, external writes, and sensitive visual cloud analysis unless separately consented by future features.
- **FR-004-015**: Users MUST be able to override understanding depth per capture with actions such as getting more context, summarizing only what was saved, refreshing source evidence, using screenshot-only evidence, or suppressing future fetching for a domain when applicable.
- **FR-004-016**: System MUST create a capture understanding result with status, title when supported, compact summary, based-on evidence list, confidence, limitations, produced-at time, and model or extractor provenance when applicable.
- **FR-004-017**: Capture understanding status MUST distinguish ready, partial, limited, and failed outcomes; limited understanding is a valid user-visible result, not an absent result.
- **FR-004-018**: Metadata-only, visual-only, low-confidence, blocked, private, or failed evidence states MUST produce explicit limitations and MUST NOT generate full-content claims.
- **FR-004-019**: Capture detail MUST show the best current source identity, evidence summary, compact understanding, limitations, depth used, and relevant correction/refresh controls without requiring Ask Orbit, retrieval, KG, memory, actions, or agent features.
- **FR-004-020**: System MUST record user feedback for wrong source, wrong summary, not relevant, too much context, and domain/source suppression in a way that can influence future understanding versions and evaluation.
- **FR-004-021**: Understanding jobs MUST be trackable as pending, running, ready, limited, failed, canceled, or invalidated, with retry eligibility and the user-visible reason for limited or failed outcomes.
- **FR-004-022**: System MUST keep the local encrypted capture corpus as the source of truth for saved captures and derived understanding state; cloud enrichment is an allowed helper only behind explicit policy, depth, budget, audit, deletion, and local-fallback controls.
- **FR-004-023**: Any cloud enrichment or model call MUST produce a bounded trace containing request/correlation ID, capability, evidence IDs or capture IDs, policy decision, model or extractor label, latency, cost or budget impact when known, outcome, and failure code when applicable; traces MUST NOT contain raw prompts, raw page text, screenshots, embeddings, or full evidence bundles.
- **FR-004-024**: Network activity for enrichment MUST use the established network gateway boundary; app code outside `com.capsule.app.net.*` or an equivalent `:net` gateway boundary MUST NOT introduce direct network clients.
- **FR-004-025**: Cross-process payloads MUST carry IDs, compact summaries, policy decisions, status, and pagination tokens only; they MUST NOT carry raw HTML, screenshots, embeddings, full page text, or full evidence bundles.
- **FR-004-026**: System MUST provide local-mode fallback when cloud is disabled or unavailable, preserving save success and showing limited understanding rather than blocking capture.
- **FR-004-027**: System MUST define deletion and invalidation rules for source identities, evidence bundles, capture understandings, understanding jobs, and any future retrieval/memory/KG/action inputs derived from the capture.
- **FR-004-028**: Deleting a capture MUST remove or invalidate user-visible derived understanding and prevent invalidated derived data from being used by future Ask, retrieval, memory, KG, action, or agent features.
- **FR-004-029**: Deletion and invalidation audit events MUST retain enough metadata to prove the lifecycle action occurred without retaining raw user content.
- **FR-004-030**: System MUST include an evaluation set for capture understanding covering static URLs, JavaScript-heavy public pages, YouTube URL variants, social/media links, screenshot-only captures, receipts/events, documents, blocked/private/paywalled sources, duplicates, and adversarial low-evidence cases.
- **FR-004-031**: System MUST NOT implement generic browser automation in this feature. Deep depth may record that browser-rendered fallback would be useful for public context, but any actual generic browser automation requires a later explicit policy and feature spec.
- **FR-004-032**: System MUST NOT implement Ask Orbit, cited question answering, retrieval ranking, KG tables, Graphiti/Zep/Mem0 integration, generic memory inspector, approval/action runtime, autonomous agents, multi-agent orchestration, connected-account writes, cloud external writes, or cloud-controls management screens in this feature.

### Key Entities *(include if feature involves data)*

- **Saved Capture**: The user-owned saved item that remains the local source of truth. It may include text, URL, screenshot/file reference, app state, intent, notes, duplicate state, archive/delete state, and timestamps.
- **Source Identity**: Orbit's best answer to `what is this from?`, including provider, origin app, category fallback, display labels, glyph kind, confidence, evidence, limitations, and resolver version.
- **Canonical URL**: A normalized URL identity used for display, duplicate matching, source provenance, refresh, and invalidation while preserving enough original URL context for user-facing source display.
- **Evidence Bundle**: A provenance-rich record of what Orbit actually saw or acquired, such as saved text, URL metadata, readable public page text, OCR text, document parse metadata, media metadata, or transcript evidence.
- **Understanding Depth Policy**: The user's global and per-capture choice for how hard Orbit may try to understand a capture, expressed as Basic, Smart, Deep, or manual override. Global defaults live in existing preferences; per-capture/domain overrides are persisted as local derived records.
- **Understanding Job**: A bounded enrichment attempt with status, depth, policy decision, retry eligibility, trace IDs, outcome, and failure/limitation reason.
- **Capture Understanding**: The current or historical result of interpreting evidence for a capture, including status, compact summary, title, based-on evidence, confidence, limitations, provenance, and produced-at time.
- **Correction Feedback**: User feedback that a source, summary, relevance judgement, or enrichment depth was wrong or unwanted, used for future versions and evaluation.
- **Deletion/Invalidation Record**: A lifecycle event proving derived source identity, evidence, understanding, and future downstream inputs were removed or made ineligible after source deletion, correction, or staleness.
- **Audit Trace**: A compact, content-free record of acquisition, cloud/model calls, policy decisions, failures, corrections, deletion, and invalidation.

## Dependencies And Constraints

- **Existing capture and URL hydration behavior**: This feature extends current capture sealing, URL hydration, canonical URL handling, and duplicate feedback instead of replacing them in one rewrite.
- **Spec 013 and 014 cloud routing**: Cloud/model work must use the existing provider-agnostic gateway and content-free trace/audit constraints.
- **Spec 017 capture feedback actions**: Duplicate detection, `Already saved`, notes, reclassify, open-existing, and app-recognition lessons are inputs to source identity and capture detail behavior.
- **Source identity plan**: Provider URL or foreground app label evidence is required for branded source identity; category-only captures must use generic identity.
- **Android architecture rules**: Network egress remains gateway-bound, interprocess payloads stay compact, and user-invoked flows are preferred over autonomous background agents.
- **Future retrieval and Ask**: This feature must produce evidence-backed understanding that can become input to future retrieval and cited answers, but those downstream features own their own ranking, answer synthesis, citations UI, and query flows.
- **Future cloud controls/storage budgeting**: This feature records and respects policy, budget, audit, deletion, and fallback decisions, but full cloud controls and storage budgeting management screens belong to a later feature.

## Non-Goals And Stop Signs

- Do not build Ask Orbit, question answering, answer synthesis, or retrieval with citations here; that belongs to `005-retrieval-and-ask-citations`.
- Do not add KG tables, Graphiti, Zep, Mem0, or a canonical memory graph here; KG backend POC belongs to `009-kg-backend-poc` after deletion/invalidation and memory controls exist.
- Do not add a generic memory inspector, memory promotion workflow, approval/action runtime, cloud controls management screen, or agent coordinator in this feature.
- Do not add autonomous agents, multi-agent orchestration, cloud external writes, connected-account writes, or logged-in browsing.
- Do not implement generic browser automation in this feature; only record limitations that explain when browser-rendered fallback may be useful in a later explicit feature.
- Do not pass raw HTML, screenshots, embeddings, full page text, or full evidence bundles over Binder/AIDL or equivalent interprocess boundaries.
- Do not add network clients outside `com.capsule.app.net.*` or the equivalent `:net` gateway boundary.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-004-001**: In a 100-case capture understanding evaluation set, source identity resolves to the correct provider/app/category level in at least 95% of cases, with 100% compliance for the rule that category-only captures cannot show a branded provider.
- **SC-004-002**: All supported YouTube-family URL variants in the evaluation set are recognized, canonicalized consistently, and displayed with provider/origin context when the origin app differs.
- **SC-004-003**: For captures with readable public text available under the selected policy, at least 90% of evaluation fixtures produce a compact summary and evidence/limitation display within 30 seconds of save on a normal network, measured by the evaluation runner.
- **SC-004-004**: For metadata-only, blocked, private, paywalled, screenshot-only, or low-confidence cases, 100% of displayed summaries include an explicit limitation and make no unsupported full-content claim.
- **SC-004-005**: Basic, Smart, Deep, and per-capture override policy tests pass for 100% of covered evidence paths, including suppression of disallowed browser fallback, cloud visual understanding, broad search enrichment, and sensitive-source enrichment.
- **SC-004-006**: Capturing the same canonical URL twice results in one primary visible capture and an `Already saved` path that opens or updates the existing capture rather than creating an indistinguishable duplicate.
- **SC-004-007**: Deleting a capture makes all derived source identity, evidence, understanding, and future downstream inputs ineligible for display or reuse within 30 seconds, while retaining only content-free deletion/invalidation audit metadata.
- **SC-004-008**: With cloud disabled or unavailable, 100% of tested captures still save locally and show either local/basic understanding or a clear limitation instead of failing the save.
- **SC-004-009**: Architecture validation finds zero direct network clients outside the allowed gateway boundary and zero cross-process payloads containing raw HTML, screenshots, embeddings, full page text, or full evidence bundles.
- **SC-004-010**: In dogfood review, at least 8 of 10 testers can correctly answer what evidence Orbit used and where it stopped after viewing a capture detail screen for a limited-understanding capture, using the dogfood scoring form generated for this feature.

## Assumptions

- Internal dogfood builds may default to Smart depth, while Basic remains one tap away and Deep remains opt-in.
- Public URL evidence may use cloud enrichment only when the selected depth, policy, budget, sensitivity checks, auditability, deletion behavior, and local fallback requirements allow it.
- Local saved captures and their current understanding state remain the source of truth; cloud-derived records are helpers and must reconcile back to local lifecycle decisions.
- Existing duplicate handling from spec 017 is available or will be preserved as the basis for `Already saved` behavior.
- Full cloud controls, storage budgeting screens, retrieval ranking, Ask answers, memory controls, KG backend, action approval runtime, and agent coordination are future features and are intentionally excluded here.
- The evaluation set can use synthetic or dogfood fixtures, but any fixture containing real user content must follow the same deletion, retention, and audit rules as normal captures.
