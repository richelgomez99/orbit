# Contract: Cloud Enrichment Trace

**Feature**: `004-capture-understanding`  
**Boundary**: content-free trace/audit records for optional cloud/model enrichment and public extraction attempts.

## 1. Applicability

This contract applies when Smart or Deep understanding attempts cloud/model/public-context enrichment through the established `:net` gateway. Basic understanding may still create local content-free traces for skipped or suppressed work.

Cloud enrichment is optional. Local save and local limited understanding must remain available when cloud is disabled, unavailable, over budget, blocked by policy, or failed.

## 2. Required Trace Fields

Each cloud/model enrichment attempt MUST produce a bounded trace with:

- `correlationId` or `requestId`.
- `captureId` and/or `evidenceIds`: IDs only.
- `capability`: bounded string such as `public_url_extract`, `metadata_resolve`, `capture_understanding_summary`, `source_refresh`, `limitation_record`.
- `requestedDepth`: `smart` or `deep`.
- `effectiveDepth`: `basic`, `smart`, `deep`, or `manual_override` after policy checks.
- `policyDecision`: bounded reason such as `allowed_cloud_public`, `blocked_sensitive`, `blocked_budget`, `blocked_domain_suppression`, `cloud_disabled`, `offline_local_fallback`.
- `modelOrExtractorLabel`: nullable provider/extractor label or local fallback label.
- `latencyMs`: nullable duration.
- `budgetImpact`: nullable compact cost/budget delta when known.
- `outcome`: `success`, `partial`, `limited`, `failed`, `suppressed`, or `invalidated`.
- `failureCode`: nullable bounded code.
- `createdAt`.

## 3. Forbidden Trace Fields

Trace/audit records MUST NOT contain:

- Raw prompts.
- Raw page text.
- Raw HTML.
- Screenshots or screenshot references that expose content outside local corpus IDs.
- Embedding vectors.
- Full evidence bundles.
- Model response text beyond compact outcome/status.
- JWT contents, cookies, API keys, auth tokens, or provider secrets.
- Connected-account data.

## 4. Policy Gates

Before cloud/model enrichment, implementation MUST evaluate:

- User depth selection and per-capture override.
- Cloud enabled/disabled state.
- Local-mode kill switch.
- Budget availability when known.
- Sensitivity checks for screenshot, OCR, app category, source domain, and saved content signals.
- Domain/source suppressions from correction feedback.
- Source availability: public/non-authenticated only.
- Auditability and deletion/invalidation receipt path.

A blocked attempt records `suppressed` or `limited` with a user-visible reason; it must not silently disappear.

## 5. Gateway Boundary

All cloud/model calls MUST originate from `com.capsule.app.net.*` / `:net` through existing gateway patterns. App packages outside the network boundary may request enrichment only through compact IDs/policy decisions and must not instantiate HTTP/provider clients.

Validation grep:

```sh
rg -n "OkHttpClient|HttpURLConnection|SupabaseClient|createSupabaseClient|@anthropic|OpenAI" app/src/main/java/com/capsule/app --glob '!net/**'
```

Matches outside `net` must be removed or justified by an existing allowed non-network abstraction. Direct provider clients outside `net` are blockers.

## 6. Deletion And Cloud Receipts

Local deletion/invalidation is authoritative. If cloud enrichment produced remote-derived references or receipts:

- Local deletion immediately marks derived local records ineligible.
- A permitted cloud deletion/invalidation receipt may be queued.
- Pending or failed cloud receipt does not re-enable local derived data.
- Receipt traces remain content-free and reference local IDs/correlation IDs only.

## 7. Failure Mapping

At minimum, cloud/model failures map to bounded failure codes:

- `network_unavailable`
- `timeout`
- `provider_4xx`
- `provider_5xx`
- `auth_unavailable`
- `budget_exhausted`
- `policy_blocked`
- `sensitivity_blocked`
- `domain_suppressed`
- `malformed_response`
- `extractor_failed`
- `unknown`

Failures must surface as `limited` or `failed` understanding job states with retry eligibility, not as capture save failures.
