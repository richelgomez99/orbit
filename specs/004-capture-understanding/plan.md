# Implementation Plan: Capture Understanding

**Branch**: `004-capture-understanding` | **Date**: 2026-05-13 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/004-capture-understanding/spec.md`

## Summary

Capture Understanding makes every saved capture a trustworthy, evidence-backed local object before later Ask, retrieval, memory, KG, action, or agent features rely on it. The implementation extends the existing capture seal, URL hydration, duplicate feedback, Room/SQLCipher corpus, Binder repository boundary, and `:net` gateway rather than replacing them. It introduces first-class source identity, canonical URL provenance, evidence bundles, understanding jobs, compact capture understanding results, correction feedback, and deletion/invalidation records.

The local encrypted corpus remains the source of truth. Cloud and model-assisted enrichment are optional helpers behind Basic/Smart/Deep policy, sensitivity checks, explicit budget/audit decisions, content-free traces, and local fallback. Android network clients stay under `com.capsule.app.net.*` / `:net`, and Binder/AIDL payloads carry IDs, compact summaries, status strings, policy decisions, and pagination tokens only.

## Technical Context

**Language/Version**: Kotlin 2.x Android app; SQL for Room migrations/tests. Supabase Postgres 15 + pgvector and Vercel Edge Function gateway are available helpers but are not authoritative for this feature.

**Primary Dependencies**:
- Room over SQLCipher via existing `OrbitDatabase` and migration path.
- Jetpack Compose for capture detail/settings surfaces.
- WorkManager for bounded understanding jobs and retries.
- Existing AIDL/Binder repository boundary under `com.capsule.app.data.ipc`.
- Existing `INetworkGateway` / `NetworkGatewayImpl` in `com.capsule.app.net.*` for public URL fetches and any cloud/model gateway calls.
- Existing `ProviderMetadataResolver`, `ReadabilityExtractor`, `CanonicalUrlHasher`, `UrlHydrateWorker`, `ContinuationEngine`, `AuditLogWriter`, and spec-017 duplicate keys.
- `kotlinx.serialization` for compact JSON fields/parcels where the app already uses serialized wire shapes.

**Storage**: Local SQLCipher Room database remains source of truth for saved captures and derived understanding state. Cloud traces/receipts may mirror content-free metadata only when policy allows; cloud data never supersedes local deletion or invalidation.

**Testing**: JVM unit tests for URL canonicalization, source identity resolution, depth policy, evidence gating, and deletion/invalidation eligibility; Room migration and DAO tests; Binder/parcel round-trip tests; WorkManager tests for job status/retry behavior; Compose UI tests for detail limitations and controls; physical QA on supported dogfood devices; a 100-case capture understanding evaluation fixture set.

**Target Platform**: Android 13+ existing multi-process app (`:capture`, `:ml`, `:net`, `:ui` semantics preserved).

**Project Type**: Mobile app feature with local data model, background continuations, compact IPC contracts, and optional cloud-augmented enrichment through the existing gateway.

**Performance Goals**:
- Duplicate/source lookup stays indexed; no full-content scans on the seal path.
- Readable public text cases produce compact understanding within 30 seconds when policy/network allow.
- Deletion or invalidation makes derived understanding and future downstream inputs ineligible within 30 seconds.
- Binder payloads stay compact and do not carry raw HTML, screenshots, full extracted text, embeddings, vectors, or evidence bundles.

**Constraints**:
- Local Room/SQLCipher capture corpus is authoritative for captures, understanding state, correction state, deletion, and invalidation.
- Network clients and provider SDKs are confined to `com.capsule.app.net.*` / `:net`; no direct clients from UI, capture, data, ML, diary, settings, or future feature packages.
- Cloud enrichment requires explicit depth/policy/budget/audit/deletion/local-fallback controls and must degrade to local limited understanding.
- Source identity evidence hierarchy is provider URL > foreground app label > generic durable category > unknown. Durable category alone cannot imply branded identity.
- Cross-process payloads carry IDs, compact summaries, short status strings, policy decisions, trace IDs, and pagination tokens only.
- Generic browser automation is not implemented in this feature; 004 may only record limitations that explain when browser-rendered fallback may be useful in a later explicit feature. No logged-in browsing, CAPTCHA bypass, connected-account access, external writes, sensitive visual cloud analysis, Ask Orbit, KG backend, memory inspector, action runtime, agent coordinator, or multi-agent orchestration.

**Scale/Scope**: One Android feature slice across capture detail, background understanding, local schema, source identity, duplicate compatibility, feedback, audit, and deletion/invalidation. Expected implementation touches app-local Kotlin/Room/AIDL/UI tests and planning contracts only; no Supabase schema ownership, no Edge Function feature expansion, and no app-code implementation during this planning command.

**Clarifications**: No `NEEDS CLARIFICATION` items remain in the plan. The feature spec's assumptions are preserved; future cloud-controls screens, Ask, retrieval, KG, memory, actions, and agents remain explicitly out of scope.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Orbit Constitution v3.2.0 governs this plan.

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Local-First Supremacy | PASS | Room/SQLCipher corpus remains source of truth; cloud enrichment is helper-only and must fall back locally. `RuntimeFlags.useLocalAi` and gateway-bound egress constraints remain intact. |
| II. Effortless Capture, Any Path | PASS | Save success is preserved even when understanding is limited, failed, blocked, offline, or over budget. Controls are surfaced after save, not as capture-blocking dialogs. |
| III. Intent Before Artifact | PASS | Existing envelope intent and duplicate feedback remain primary; understanding enriches the saved object without rewriting sealed capture intent silently. |
| IV. Continuations Grow Captures | PASS | Understanding jobs are continuations with bounded retries, visible status, and honest limitations. |
| V. Under-Deliver on Noise | PASS | No proactive notification or nudge expansion. Limited understanding is shown in detail rather than noisy alerts. |
| VI. Privilege Separation By Design | PASS | All public fetch/model/cloud activity stays in `:net` / `com.capsule.app.net.*`; Binder carries compact parcels only. |
| VII. Context Beyond Content | PASS | Foreground app label and durable category are used as local evidence, with provider/app/category distinctions preserved. |
| VIII. Collect Only What You Use | PASS | New records are justified by user-visible source/evidence/summary/limitation/correction/deletion behavior. Raw content is not duplicated into audit traces or IPC parcels. |
| IX. User-Sovereign Cloud Escape Hatch (LLM) | PASS | Cloud/model calls are optional and policy-gated. This feature records bounded trace metadata and respects local/offline fallback; full cloud controls UI is deferred and not implemented here. |
| X. Sovereign Cloud Storage | PASS | Local lifecycle wins. Cloud-derived references must reconcile to local deletion/invalidation; no cloud storage category management screen is added. |
| XI. Consent-Aware Prompt Assembly | PASS | Any outbound model call must be assembled on device and routed through `:net`; traces exclude raw prompts/content. Sensitive visual cloud analysis remains excluded unless a future consent feature allows it. |
| XII. Provenance Or It Didn't Happen | PASS | Every source identity, summary, limitation, and future downstream eligibility is tied to evidence bundle IDs and capture IDs; unsupported claims are invalid. |

**Gate verdict**: PASS. No constitutional violations require Complexity Tracking.

**Post-design re-check**: PASS. The Phase 1 data model and contracts preserve local authority, evidence hierarchy, compact IPC, gateway-only network egress, deletion/invalidation, and feature stop signs.

## Project Structure

### Documentation (this feature)

```text
specs/004-capture-understanding/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── source-identity-and-evidence-contract.md
│   ├── compact-ipc-payload-contract.md
│   └── cloud-enrichment-trace-contract.md
└── tasks.md                         # Phase 2 output of /speckit.tasks; not created by this command
```

### Source Code (repository root)

Planned implementation locations; this planning workflow does not create or modify app code.

```text
app/src/main/java/com/capsule/app/
├── capture/
│   ├── StateSnapshotCollector.kt                    # source app/category evidence inputs
│   └── AppCategoryDictionary.kt                     # durable category evidence inputs
├── continuation/
│   ├── ContinuationEngine.kt                        # schedule bounded understanding jobs
│   ├── UrlHydrateWorker.kt                          # preserve existing public URL hydration compatibility
│   └── CaptureUnderstandingWorker.kt                # NEW planned worker for evidence -> understanding
├── data/
│   ├── OrbitDatabase.kt                             # add entities/DAOs/migrations
│   ├── OrbitMigrations.kt                           # forward-only Room migrations
│   ├── EnvelopeRepositoryImpl.kt                    # local source-of-truth writes and detail reads
│   ├── dao/
│   │   └── CaptureUnderstandingDao.kt               # NEW planned DAO aggregation surface
│   ├── entity/
│   │   ├── SourceIdentityEntity.kt                  # NEW planned entity
│   │   ├── CanonicalUrlEntity.kt                    # NEW planned entity
│   │   ├── EvidenceBundleEntity.kt                  # NEW planned entity
│   │   ├── UnderstandingJobEntity.kt                # NEW planned entity
│   │   ├── CaptureUnderstandingEntity.kt            # NEW planned entity
│   │   ├── CorrectionFeedbackEntity.kt              # NEW planned entity
│   │   └── DeletionInvalidationEntity.kt            # NEW planned entity
│   └── ipc/
│       ├── EnvelopeViewParcel.kt                    # extend with compact understanding references only
│       └── CaptureUnderstandingSummaryParcel.kt     # NEW planned compact parcel if needed
├── diary/
│   ├── EnvelopeDetailViewModel.kt                   # display source/evidence/summary/limitations/actions
│   ├── EnvelopeDetailUiState.kt                     # compact UI state shape
│   └── ui/EnvelopeDetailScreen.kt                   # user-visible trust surface
├── net/
│   ├── CanonicalUrlHasher.kt                        # preserve duplicate hash behavior
│   ├── NetworkGatewayImpl.kt                        # gateway-only network/model calls
│   ├── ProviderMetadataResolver.kt                  # provider metadata, YouTube-family handling
│   └── ReadabilityExtractor.kt                      # static public extraction
├── settings/
│   └── PrivacyPreferences.kt                        # global understanding depth policy storage
└── audit/
    └── AuditLogWriter.kt                            # content-free trace, correction, deletion/invalidation rows

app/src/test/java/com/capsule/app/                  # resolver, policy, deletion, parcel tests
app/src/androidTest/java/com/capsule/app/           # Room migration + Compose detail tests
app/schemas/com.capsule.app.data.OrbitDatabase/     # exported Room schema updates
```

**Structure Decision**: Keep Capture Understanding in the existing Android app module and existing process/package boundaries. Add local Room entities/DAO aggregation for first-class understanding state, extend the existing detail and repository surfaces with compact references, and route any network/cloud work through the established `:net` gateway. No new Gradle module is required unless the future implementation chooses to enforce a physical `:net` module boundary beyond the current package/process boundary.

## Phase 0 - Outline & Research

See [research.md](research.md). Decisions cover source identity hierarchy, canonical URL/duplicate integration, evidence bundle retention, depth policy, gateway-only cloud enrichment, compact Binder payloads, deletion/invalidation, correction feedback, evaluation fixtures, and explicit exclusions.

## Phase 1 - Design & Contracts

### Data Model

See [data-model.md](data-model.md). The model defines Saved Capture relationships, Source Identity, Canonical URL, Evidence Bundle, Understanding Depth Policy, Understanding Job, Capture Understanding, Correction Feedback, Deletion/Invalidation Record, Audit Trace, and downstream eligibility references.

### Contracts

Contracts are needed because this feature spans user-visible detail semantics, Binder/AIDL payload constraints, local provenance records, and optional cloud gateway traces:

1. [source-identity-and-evidence-contract.md](contracts/source-identity-and-evidence-contract.md) defines the evidence hierarchy, branded identity guardrails, evidence claim rules, retention classes, and source/summary limitation semantics.
2. [compact-ipc-payload-contract.md](contracts/compact-ipc-payload-contract.md) defines what may cross Binder/AIDL and explicitly forbids raw HTML, screenshots, full page text, embeddings, vectors, and full evidence bundles.
3. [cloud-enrichment-trace-contract.md](contracts/cloud-enrichment-trace-contract.md) defines bounded trace/audit metadata for any cloud/model enrichment attempt and the local deletion/invalidation reconciliation obligations.

### Quickstart

See [quickstart.md](quickstart.md) for the implementer walkthrough and verification gates.

### Agent Context Update

Run `.specify/scripts/bash/update-agent-context.sh copilot` after generating this plan. The update should record only new planning context for Capture Understanding while preserving manual additions.

## Complexity Tracking

No constitutional violations or unresolved clarifications require justification.

## Output Summary

Generated planning artifacts:
- [plan.md](plan.md)
- [research.md](research.md)
- [data-model.md](data-model.md)
- [quickstart.md](quickstart.md)
- [contracts/source-identity-and-evidence-contract.md](contracts/source-identity-and-evidence-contract.md)
- [contracts/compact-ipc-payload-contract.md](contracts/compact-ipc-payload-contract.md)
- [contracts/cloud-enrichment-trace-contract.md](contracts/cloud-enrichment-trace-contract.md)

Branch: `004-capture-understanding`.

**Next step**: Run `/speckit.tasks` to generate `tasks.md`. Do not implement app code before tasks are generated and reviewed.
