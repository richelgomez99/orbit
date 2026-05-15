# Contract: Source Identity And Evidence

**Feature**: `004-capture-understanding`  
**Boundary**: local source/evidence semantics consumed by capture detail, understanding jobs, audit, deletion/invalidation, and future downstream features.

## 1. Source Identity Resolution

Resolvers MUST evaluate evidence in this order:

1. Provider URL evidence.
2. Foreground app label evidence.
3. Generic durable category.
4. Unknown fallback.

Provider URL evidence includes normalized/canonical URLs recognized as a content provider family. Foreground app label evidence is a local context fact from the capture state. Generic durable category includes local categories such as video, browser, social, reading, messaging, work email, and other.

## 2. Branded Identity Guardrail

A branded provider glyph, provider key, or provider display label MUST NOT be displayed unless one of these is true:

- A provider URL evidence bundle supports the provider.
- A foreground app label evidence bundle supports that branded app/provider.

A durable category alone MUST produce `GENERIC_CATEGORY` identity. For example, category `video` alone cannot display YouTube.

## 3. Provider Plus Origin App

When provider and origin app differ, both facts MUST be retained.

Examples:

| Evidence | Primary label | Secondary label |
|----------|---------------|-----------------|
| YouTube URL + Brave foreground app | YouTube | via Brave |
| YouTube URL + Messages foreground app | YouTube | copied from Messages |
| No provider URL + YouTube foreground app | YouTube | app evidence |
| No provider URL + video category only | Video | generic category |

## 4. YouTube-Family URL Recognition

Resolvers MUST recognize YouTube-family URLs across:

- `youtu.be`
- `youtube.com`
- subdomains of `youtube.com`
- `youtube-nocookie.com`
- subdomains of `youtube-nocookie.com`
- Shorts, live, watch, embed, and common path variants
- scheme-less links once normalized

Recognition alone does not imply full video understanding. Summary claims require transcript, readable text, documented metadata, OCR text, or equivalent explicit evidence.

## 5. Evidence Bundle Claim Rules

Every displayed source identity, title, summary, limitation, confidence, and future downstream eligibility MUST cite evidence bundle IDs.

Allowed claim levels:

| Evidence state | Allowed claim | Required limitation |
|----------------|---------------|---------------------|
| Readable public text | Compact content summary | mention fetched readable text |
| Transcript evidence | Media/content summary | mention transcript basis |
| OCR text | Visible-text summary | mention OCR/visible screenshot basis and confidence if available |
| Metadata only | Title/provider/metadata summary only | state metadata-only limitation |
| Blocked/private/paywalled/app-only | Limited source/metadata | state unavailable/private/blocked limitation |
| Failed/suppressed | No content claim | state failure/suppression reason |

Unsupported full-content claims are invalid even if a model produces plausible text.

## 6. Retention And References

Evidence bundles MAY reference retained local artifacts by ID/hash, but audit traces and Binder payloads MUST NOT contain raw content. Evidence retention classes:

- `LOCAL_RAW_ALLOWED`: raw artifact exists in the local encrypted corpus.
- `LOCAL_REFERENCE_ONLY`: only a local reference/hash is retained.
- `CONTENT_FREE_TRACE`: trace metadata only.
- `EPHEMERAL`: temporary intermediate; not eligible for claims after expiry.
- `DELETED_INVALIDATED`: lifecycle-ineligible.

## 7. Limitation Codes

Implementation may add codes, but these must be represented:

- `metadata_only`
- `blocked_source`
- `private_or_authenticated`
- `paywalled`
- `robots_or_policy_blocked`
- `cloud_disabled`
- `cloud_budget_exhausted`
- `domain_suppressed`
- `network_unavailable`
- `extractor_failed`
- `low_confidence`
- `visual_cloud_suppressed_sensitive`
- `summarize_only_saved`
- `evidence_invalidated`

## 8. Deletion And Invalidation

When a parent capture is deleted or invalidated:

- Source identities become ineligible for display.
- Evidence bundles become ineligible for future claims.
- Capture understandings become ineligible for display and downstream use.
- Content-free deletion/invalidation audit metadata may remain.
- Any cloud deletion/invalidation receipt is a follow-up receipt, not the source of truth.
