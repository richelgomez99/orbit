# Research: Capture Understanding

**Feature**: `004-capture-understanding`  
**Date**: 2026-05-13  
**Input**: [spec.md](spec.md)

No unresolved `NEEDS CLARIFICATION` items were found in the feature spec. The decisions below preserve the user's architecture constraints and the Orbit Constitution.

## 1. Source Identity Hierarchy

**Decision**: Resolve primary source identity by strict evidence priority: provider URL evidence first, foreground app label evidence second, generic durable category third, unknown fallback last.

**Rationale**: This is the only hierarchy that can support branded provider display without overclaiming. A YouTube URL copied from Brave or Messages should show YouTube as the content provider while preserving the origin app as context; a category-only video capture must stay generic.

**Alternatives considered**:
- Use foreground app as the primary label always: rejected because browser/message copies would hide content provider truth.
- Use durable category to infer provider glyphs: rejected because category evidence cannot prove a brand.
- Let summarization/model inference choose the source: rejected because source identity must be auditable before downstream AI features.

## 2. Canonical URL And Duplicate Integration

**Decision**: Canonical URL records should extend current spec-017 duplicate behavior by storing original URL, normalized URL, primary/supporting role, canonical URL hash, provider family when supported, normalization version, and deletion eligibility.

**Rationale**: Existing `primaryCanonicalUrlHash` and active unique keys are already the user-visible dedupe seam. Capture Understanding must build beside that seam so repeated saves return the existing `Already saved` path instead of creating indistinguishable rows, including saves that happen before hydration finishes.

**Alternatives considered**:
- Treat URL hydration cache reuse as duplicate detection: rejected because cache reuse is not user-visible capture identity.
- Recanonicalize only after cloud enrichment: rejected because duplicates must be caught on the local save path.
- Store only raw URL text: rejected because tracking parameters, fragments, casing, mobile subdomains, and YouTube variants need stable matching.

## 3. Evidence Bundles Before Claims

**Decision**: Every displayed source label, summary, title, confidence, limitation, and future downstream eligibility must cite evidence bundle IDs. Evidence bundles store kind, source reference, acquisition method/depth, confidence, content hash/reference, retention class, limitations, and acquisition status; raw content is not duplicated into IPC or audit traces.

**Rationale**: The feature's central promise is that Orbit can say what it saw and where it stopped. Evidence records make unsupported full-content claims mechanically invalid, especially for metadata-only, blocked, private, paywalled, visual-only, and low-confidence captures.

**Alternatives considered**:
- Store a single summary blob on the capture: rejected because it cannot prove what the summary was based on.
- Store raw fetched content in the audit log: rejected by local-first/audit constraints and unnecessary for user-visible trust.
- Allow future retrieval/memory/KG to consume captures directly: rejected because downstream features need provenance and lifecycle gates.

## 4. Understanding Depth Policy

**Decision**: Model Basic, Smart, Deep, and per-capture overrides as explicit policy decisions recorded on each understanding job and result. Basic uses local/basic evidence and safe public hydration only; Smart may use bounded cloud enrichment for allowed public captures; Deep may try harder for public context but still excludes logged-in browsing, CAPTCHA bypass, connected-account access, external writes, and sensitive visual cloud analysis.

**Rationale**: Depth is the user-facing control that converts cost, cloud use, sensitivity, retry effort, and extraction breadth into understandable product choices. Recording the effective policy prevents later ambiguity about why something was skipped.

**Alternatives considered**:
- Expose vendor/extractor menus: rejected because users need plain-language controls.
- Make Deep a browser automation mode: rejected by the feature stop signs and architecture constraints.
- Hide policy failures as absent summaries: rejected because limited understanding is a valid visible state.

## 5. Gateway-Only Cloud Enrichment

**Decision**: Any network, provider SDK, model, or cloud enrichment client must live under `com.capsule.app.net.*` / `:net`; app code outside the boundary communicates through compact requests and receives compact results/traces only.

**Rationale**: The existing architecture already uses `INetworkGateway`, `NetworkGatewayImpl`, public URL fetches, and `callLlmGateway`. Reusing this boundary preserves privilege separation and makes audit/egress validation tractable.

**Alternatives considered**:
- Add direct clients in workers or repositories: rejected because it violates the network boundary.
- Let UI trigger provider SDKs directly for manual refresh: rejected because user invocation does not weaken process/package constraints.
- Route cloud enrichment through a future agent coordinator: rejected because agent coordination is explicitly out of scope for 004.

## 6. Compact Binder/AIDL Payloads

**Decision**: Cross-process payloads carry IDs, compact summaries, status strings, confidence bands, limitation codes, policy decisions, trace IDs, and pagination tokens. Raw HTML, screenshots, full page text, embeddings/vectors, and full evidence bundles never cross Binder/AIDL in bulk.

**Rationale**: Binder is a control and summary channel, not a content shuttle. Keeping payloads compact protects performance, privacy, and process boundaries while still allowing UI surfaces to explain evidence and limitations.

**Alternatives considered**:
- Pass full evidence JSON to detail UI: rejected because it risks raw content leaks and oversized parcels.
- Re-fetch raw evidence from UI: rejected because UI should not own corpus or network access.
- Use untyped string payloads only: rejected because job/status/policy compatibility should be testable.

## 7. Deletion And Invalidation

**Decision**: Deletion/invalidation is local-first and cascade-aware. Deleting a capture marks source identity, canonical URL eligibility, evidence bundles, understanding jobs/results, correction-derived versions, and future downstream inputs ineligible for display/reuse, while retaining content-free lifecycle audit metadata.

**Rationale**: Later Ask, retrieval, memory, KG, action, and agent features can only be safe if source capture lifecycle propagates mechanically. Local deletion must be authoritative even when cloud deletion receipts are pending or unavailable.

**Alternatives considered**:
- Hard-delete everything immediately with no receipt: rejected because users and dogfood debugging need content-free proof of lifecycle actions.
- Leave derived summaries visible after capture deletion: rejected by provenance and deletion requirements.
- Make cloud receipt authoritative: rejected because local corpus remains source of truth.

## 8. Correction Feedback And Versioning

**Decision**: Record wrong source, wrong summary, not relevant, too much context, and domain/source suppression as first-class correction feedback linked to the current understanding version and future refresh jobs.

**Rationale**: Corrections are evidence too. Linking feedback to versions preserves audit history while letting a refreshed result supersede a bad interpretation.

**Alternatives considered**:
- Mutate the current summary/source in place: rejected because it erases why the previous interpretation changed.
- Treat feedback as UI-only state: rejected because future refresh and evaluation need to honor it.
- Store only free-text complaints: rejected because deterministic suppression and evaluation require typed feedback.

## 9. Evaluation Fixture Set

**Decision**: Build a 100-case fixture set covering static URLs, JavaScript-heavy public pages, YouTube URL variants, social/media links, screenshot-only captures, receipts/events, documents, blocked/private/paywalled sources, duplicates, category-only captures, and adversarial low-evidence cases.

**Rationale**: The success criteria are quantitative and include provider identity accuracy, YouTube coverage, limitation correctness, policy compliance, duplicate handling, deletion invalidation, local fallback, and architecture validation.

**Alternatives considered**:
- Rely only on unit tests: rejected because trust surfaces depend on mixed evidence and user-visible copy.
- Use live web only: rejected because tests need deterministic fixtures and deletion/retention behavior.
- Delay evaluation to Ask/retrieval: rejected because those features depend on this substrate.

## 10. Explicit Exclusions

**Decision**: 004 must not implement Ask Orbit, retrieval ranking/citations, KG tables/backends, Graphiti/Zep/Mem0 integration, generic memory inspector, approval/action runtime, generic browser automation as default strategy, connected-account writes, cloud external writes, agent coordinator, or multi-agent orchestration.

**Rationale**: Capture Understanding is substrate work. Its output should be safe for future systems, not a stealth implementation of those systems.

**Alternatives considered**:
- Add Ask preview because summaries exist: rejected because cited question answering belongs to 005.
- Add KG-ready cloud schema now: rejected because KG backend POC belongs to 009 and must wait for deletion/invalidation guarantees.
- Add action/memory suggestions from understanding: rejected because actions and memory controls are future features with separate consent and approval requirements.
