# Data Model: Capture Understanding

**Feature**: `004-capture-understanding`  
**Date**: 2026-05-13

This model is implementation-facing and storage-oriented. Field names are planned logical names; implementation may adapt casing to Room conventions while preserving contracts and validation rules.

## Entity Relationship Overview

```text
SavedCapture (existing IntentEnvelopeEntity)
├── CanonicalUrl*                    # primary + supporting URL identities
├── SourceIdentity*                  # current + historical source identity versions
├── EvidenceBundle*                  # what Orbit actually saw/acquired
├── UnderstandingDepthPolicyOverride*# per-capture/domain policy overrides
├── UnderstandingJob*                # bounded attempts to understand/refresh
├── CaptureUnderstanding*            # current + historical interpreted results
├── CorrectionFeedback*              # user corrections/suppressions
└── DeletionInvalidationRecord*      # lifecycle proof and downstream eligibility gate
```

`SavedCapture` remains the authoritative parent. Derived records must carry `captureId` and must be ignored when the parent capture is deleted or invalidated.

## Saved Capture

Existing entity: `IntentEnvelopeEntity` in the local Room/SQLCipher corpus.

**Relevant existing fields**:
- `id`: stable capture/envelope ID.
- `textContent`, `imageUri`, `contentType`: source artifact references.
- `state`: local context including foreground app/category inputs.
- `createdAt`, `dayLocal`: chronology.
- `isDeleted`, `deletedAt`: lifecycle authority.
- `primaryCanonicalUrlHash`, `activePrimaryCanonicalUrlHash`, `activeTextContentSha256`: spec-017 duplicate keys.

**Relationships**:
- One saved capture may have many canonical URLs, evidence bundles, jobs, understanding versions, feedback records, and lifecycle records.
- One saved capture may have one current source identity and one current capture understanding selected by version/status.

**Validation rules**:
- Deleted captures are ineligible for duplicate matching via active keys.
- Derived records for deleted captures must not be displayed or used as future downstream inputs.
- Existing URL hydration results remain readable; capture detail prefers `CaptureUnderstanding` when present.

## Source Identity

Best answer to `what is this from?` with explicit provenance.

**Fields**:
- `id`: local UUID.
- `captureId`: parent saved capture ID.
- `version`: monotonically increasing per capture.
- `providerKey`: nullable stable provider key, such as `youtube`, only when provider/app evidence supports it.
- `providerLabel`: nullable display label backed by provider URL or foreground app label evidence.
- `originAppLabel`: nullable foreground app label, such as `Brave` or `Messages`, when available.
- `genericCategory`: nullable durable category, such as `video`, `browser`, `social`, `reading`, `other`.
- `displayLabel`: primary user-visible label.
- `secondaryLabel`: optional context label, such as `via Brave` or `copied from Messages`.
- `glyphKind`: `PROVIDER`, `APP`, `GENERIC_CATEGORY`, or `UNKNOWN`.
- `confidence`: float or confidence band.
- `evidenceIdsJson`: compact ordered list of evidence bundle IDs used.
- `limitationsJson`: limitation codes/copy.
- `resolverVersion`: source identity resolver version.
- `createdAt`, `supersededAt`, `invalidatedAt`: lifecycle.

**Validation rules**:
- Provider URL evidence outranks foreground app label evidence.
- Foreground app label evidence outranks durable category evidence.
- Durable category alone must set `glyphKind = GENERIC_CATEGORY` and must not set a branded `providerKey` or `providerLabel`.
- When provider and origin app differ, preserve both; display may become `YouTube` + `via Brave`.
- Current identity cannot reference invalidated evidence.

## Canonical URL

Normalized URL identity used for duplicate matching, display, provenance, refresh, and invalidation.

**Fields**:
- `id`: local UUID.
- `captureId`: parent saved capture ID.
- `originalUrl`: original detected URL or compact display reference.
- `normalizedUrl`: canonical URL string after normalization.
- `canonicalUrlHash`: duplicate key, compatible with existing `CanonicalUrlHasher`.
- `role`: `PRIMARY` or `SUPPORTING`.
- `providerFamily`: nullable recognized family such as `youtube`.
- `host`: normalized host.
- `normalizationVersion`: version of canonicalization rules.
- `detectedAt`, `hydratedAt`: timestamps.
- `isEligibleForDuplicateMatching`: false when capture deleted/archived/ineligible.
- `invalidatedAt`: lifecycle.

**Validation rules**:
- Exactly one active primary canonical URL per capture when any URL exists.
- Duplicate lookup uses the active primary hash and ignores deleted captures.
- Multiple URLs preserve supporting evidence without replacing the primary unless a refresh/correction explicitly does so.
- YouTube-family recognition covers `youtu.be`, `youtube.com` subdomains, `youtube-nocookie.com`, Shorts/live/embed-style paths, and scheme-less links after normalization.

## Evidence Bundle

Provenance-rich record of what Orbit actually saw, fetched, parsed, OCRed, inferred, or skipped.

**Fields**:
- `id`: local UUID.
- `captureId`: parent saved capture ID.
- `kind`: `SAVED_TEXT`, `SAVED_URL`, `URL_METADATA`, `READABLE_PUBLIC_TEXT`, `OCR_TEXT`, `DOCUMENT_METADATA`, `MEDIA_METADATA`, `TRANSCRIPT`, `SCREENSHOT_REFERENCE`, `USER_CORRECTION`, `FETCH_LIMITATION`, `MODEL_LIMITATION`, or future-compatible enum.
- `sourceReference`: local artifact ID, canonical URL ID, continuation result ID, or content-free external trace ID.
- `acquisitionDepth`: `BASIC`, `SMART`, `DEEP`, or `MANUAL_OVERRIDE`.
- `acquisitionMethod`: `LOCAL_CAPTURE`, `LOCAL_PARSE`, `NET_PUBLIC_FETCH`, `PROVIDER_METADATA`, `OCR_LOCAL`, `CLOUD_ENRICHMENT`, `USER_FEEDBACK`, `SUPPRESSED_BY_POLICY`, `FAILED_ATTEMPT`.
- `confidence`: float or band.
- `contentHash`: nullable digest of retained content/artifact.
- `retentionClass`: `LOCAL_RAW_ALLOWED`, `LOCAL_REFERENCE_ONLY`, `CONTENT_FREE_TRACE`, `EPHEMERAL`, `DELETED_INVALIDATED`.
- `status`: `READY`, `PARTIAL`, `LIMITED`, `FAILED`, `SUPPRESSED`, `INVALIDATED`.
- `limitationsJson`: typed limitations.
- `createdAt`, `invalidatedAt`.

**Validation rules**:
- Claims cannot cite evidence with `FAILED`, `SUPPRESSED`, or `INVALIDATED` status except as a limitation.
- Metadata-only evidence cannot support full-content summary claims.
- Screenshot-only sensitive evidence cannot be sent for cloud visual understanding in this feature.
- Audit traces and Binder parcels reference evidence IDs, not full evidence bundles or raw content.

## Understanding Depth Policy

Effective policy for how hard Orbit may try to understand captures.

**Storage**:
- Global defaults live in existing `PrivacyPreferences` and are not a Room entity.
- Per-capture and per-domain overrides live in `UnderstandingDepthPolicyOverrideEntity` so they can participate in local deletion, invalidation, and audit behavior.

**Fields**:
- `id`: local UUID for persisted overrides.
- `scope`: `CAPTURE_OVERRIDE` or `DOMAIN_SUPPRESSION` for Room records; `GLOBAL_DEFAULT` is represented by `PrivacyPreferences`.
- `captureId`: nullable for domain suppression, required for capture override.
- `depth`: `BASIC`, `SMART`, or `DEEP`.
- `overrideKind`: nullable `GET_MORE_CONTEXT`, `SUMMARIZE_ONLY_SAVED`, `REFRESH_SOURCE_EVIDENCE`, `SCREENSHOT_ONLY`, `SUPPRESS_DOMAIN_FETCH`, `RESET_TO_GLOBAL`.
- `domainSuppressionKey`: nullable normalized domain/source key.
- `cloudAllowed`: effective boolean after user policy/sensitivity/budget checks.
- `reason`: compact user-visible reason for skips or overrides.
- `createdAt`, `updatedAt`.

**Validation rules**:
- Effective policy resolution reads global defaults from `PrivacyPreferences`, then applies the newest non-invalidated capture override and matching domain/source suppression record.
- Basic never automatically uses managed extraction, browser fallback, broad external search enrichment, cloud visual screenshot analysis, connected-account access, or repeated deep retries.
- Smart may use bounded cloud enrichment only for allowed public captures.
- Deep remains public-context only and excludes browser-rendered automation in this feature, logged-in browsing, CAPTCHA bypass, connected-account access, external writes, and sensitive visual cloud analysis.
- `SUMMARIZE_ONLY_SAVED` must suppress broader enrichment on refresh.

## Understanding Job

A bounded attempt to acquire evidence and/or produce a capture understanding.

**Fields**:
- `id`: local UUID.
- `captureId`: parent saved capture ID.
- `requestedDepth`: requested `BASIC`, `SMART`, or `DEEP`.
- `effectiveDepth`: actual depth after policy/sensitivity/budget checks.
- `status`: `PENDING`, `RUNNING`, `READY`, `LIMITED`, `FAILED`, `CANCELED`, `INVALIDATED`.
- `policyDecision`: compact enum/string, such as `allowed_local`, `allowed_cloud_public`, `blocked_sensitive`, `blocked_budget`, `blocked_domain_suppression`, `offline_local_fallback`.
- `retryEligibility`: `RETRYABLE`, `NOT_RETRYABLE`, `USER_ACTION_REQUIRED`.
- `attemptCount`, `maxAttempts`.
- `traceIdsJson`: content-free trace/correlation IDs.
- `failureCode`: nullable bounded code.
- `userVisibleReason`: nullable short reason.
- `startedAt`, `finishedAt`, `createdAt`, `invalidatedAt`.

**Validation rules**:
- Limited is terminal and displayable, not an absent result.
- Failed/pending jobs must not block save success.
- Cloud failure must produce local fallback or a visible limitation.
- Jobs cannot outlive local deletion eligibility.

## Capture Understanding

Current or historical interpreted result for a capture.

**Fields**:
- `id`: local UUID.
- `captureId`: parent saved capture ID.
- `jobId`: producing job ID.
- `version`: monotonically increasing per capture.
- `status`: `READY`, `PARTIAL`, `LIMITED`, `FAILED`, `INVALIDATED`.
- `title`: nullable title supported by evidence.
- `compactSummary`: nullable compact summary.
- `basedOnEvidenceIdsJson`: ordered evidence IDs.
- `sourceIdentityId`: current or producing source identity version.
- `confidence`: float or band.
- `limitationsJson`: typed limitations and display copy.
- `depthUsed`: effective depth.
- `extractorProvenance`: local extractor/model label, version, or provider label when applicable.
- `producedAt`, `supersededAt`, `invalidatedAt`.

**Validation rules**:
- Summary must not claim full-page/video/document understanding without supporting readable text, transcript, OCR text, document parse, or equivalent evidence.
- Metadata-only understanding must set `LIMITED` or `PARTIAL` with explicit limitation.
- Current version must be the latest non-invalidated version selected by status/confidence/feedback rules.
- Future retrieval, Ask, memory, KG, action, and agent inputs may reference only non-invalidated understandings with valid evidence.

## Correction Feedback

Typed user feedback that source, summary, relevance, context breadth, or fetching policy was wrong or unwanted.

**Fields**:
- `id`: local UUID.
- `captureId`: parent saved capture ID.
- `understandingId`: nullable current understanding version.
- `sourceIdentityId`: nullable current source identity version.
- `feedbackKind`: `WRONG_SOURCE`, `WRONG_SUMMARY`, `NOT_RELEVANT`, `TOO_MUCH_CONTEXT`, `SUPPRESS_DOMAIN`, `SUPPRESS_SOURCE`, `OTHER`.
- `targetReference`: nullable domain/source/evidence/reference ID.
- `note`: optional user-provided note, local-only.
- `createdAt`.
- `resolvedByUnderstandingId`: nullable refreshed understanding version.

**Validation rules**:
- Feedback never deletes audit history.
- New understanding/source versions reference relevant feedback when they supersede corrected versions.
- Domain suppression affects future jobs for matching normalized domains and produces visible limitations.

## Deletion/Invalidation Record

Lifecycle event proving derived records were removed or made ineligible.

**Fields**:
- `id`: local UUID.
- `captureId`: parent saved capture ID.
- `reason`: `CAPTURE_DELETED`, `USER_INVALIDATED`, `CORRECTION_SUPERSEDED`, `SOURCE_STALE`, `DOMAIN_SUPPRESSED`, `CLOUD_RECEIPT_PENDING`, `CLOUD_RECEIPT_CONFIRMED`.
- `affectedRecordRefsJson`: compact IDs/types only.
- `downstreamEligibility`: `INELIGIBLE`, `PARTIALLY_ELIGIBLE`, `RE_EVALUATION_REQUIRED`.
- `auditTraceId`: content-free audit trace ID.
- `createdAt`.
- `cloudReceiptStatus`: `NOT_APPLICABLE`, `PENDING`, `CONFIRMED`, `FAILED`, nullable.

**Validation rules**:
- Deleting a capture makes user-visible derived understanding ineligible within 30 seconds.
- Content-free deletion/invalidation audit metadata may remain.
- Future downstream references must treat this capture's derived records as ineligible and re-evaluate in their owning feature; this feature stores single-capture invalidation records plus content-free affected record references only.

## Audit Trace

Content-free metadata for acquisition, cloud/model calls, policy decisions, corrections, deletion, and invalidation.

**Fields**:
- `id` / `correlationId`.
- `captureId` or `evidenceIds`: IDs only.
- `capability`: `source_identity`, `url_hydration`, `public_extraction`, `cloud_enrichment`, `understanding_refresh`, `correction`, `deletion_invalidation`.
- `policyDecision`.
- `modelOrExtractorLabel`: nullable.
- `latencyMs`: nullable.
- `budgetImpact`: nullable compact value when known.
- `outcome`: `success`, `partial`, `limited`, `failed`, `suppressed`, `invalidated`.
- `failureCode`: nullable.
- `createdAt`.

**Validation rules**:
- Trace rows must not contain raw prompts, raw page text, screenshots, embeddings, vectors, full evidence bundles, auth tokens, or provider secrets.
- Trace rows are sufficient to prove what class of action occurred and why without retaining raw user content.
