# Quickstart: Capture Understanding

**Feature**: `004-capture-understanding`  
**Audience**: next implementer running `/speckit.tasks` and app-code work after planning.

## 1. Read The Source Of Truth

Start with these planning artifacts:

1. [spec.md](spec.md) for requirements, stop signs, success criteria, and assumptions.
2. [plan.md](plan.md) for architecture, constitution checks, and implementation locations.
3. [research.md](research.md) for locked decisions.
4. [data-model.md](data-model.md) for Room/entity/job/versioning expectations.
5. Contracts under [contracts/](contracts/) for evidence, Binder/AIDL, and cloud trace boundaries.

Do not implement Ask Orbit, retrieval, KG, memory inspector, action runtime, generic browser automation, agent coordination, or cloud controls screens in this feature.

## 2. Verify Existing Seams Before Editing

From repo root:

```sh
rg -n "primaryCanonicalUrlHash|activePrimaryCanonicalUrlHash|activeTextContentSha256" app/src/main/java/com/capsule/app
rg -n "fetchPublicUrl|callLlmGateway|INetworkGateway|NetworkGatewayImpl" app/src/main app/src/test
rg -n "ProviderMetadataResolver|CanonicalUrlHasher|UrlHydrateWorker|ContinuationEngine" app/src/main app/src/test
rg -n "EnvelopeDetail|EnvelopeViewParcel|SealResultParcel" app/src/main app/src/test
```

Expected result: duplicate keys, network gateway calls, provider metadata, hydration, detail parcels, and detail UI already exist and should be extended instead of replaced.

## 3. Implementation Order For Tasks

Recommended task phases for `/speckit.tasks`:

1. Add pure model enums/value objects and resolver tests for source identity, canonical URL role, evidence kind, understanding depth, job status, limitation codes, and trace outcomes.
2. Add forward-only Room entities, DAOs, migrations, and migration tests for source identity, canonical URLs, evidence bundles, jobs, understandings, correction feedback, and deletion/invalidation records.
3. Wire source identity and canonical URL creation into the existing capture seal/hydration path while preserving spec-017 `Already saved` behavior.
4. Add bounded understanding job scheduling and local/basic evidence generation; save must succeed even when understanding is pending, limited, failed, offline, or blocked.
5. Extend `:net` gateway interactions only if Smart/Deep cloud enrichment is needed; keep all clients under `com.capsule.app.net.*` / `:net`.
6. Extend repository and Binder parcels with compact references/statuses only, then update capture detail UI state and screen.
7. Add correction feedback flows and domain/source suppression.
8. Add deletion/invalidation cascade and content-free audit rows.
9. Build the 100-case evaluation fixture set and dogfood checklist.

## 4. Required Verification Gates

Run targeted checks as implementation lands:

```sh
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew lintDebug
rg -n "OkHttpClient|HttpURLConnection|SupabaseClient|createSupabaseClient|@anthropic|OpenAI" app/src/main/java/com/capsule/app --glob '!net/**'
rg -n "rawHtml|readableHtml|screenshot|embedding|vector|fullEvidence|pageText" app/src/main/aidl app/src/main/java/com/capsule/app/data/ipc
```

The architecture grep must find no direct network/provider clients outside `com.capsule.app.net.*` and no raw-content bulk payloads in AIDL/data IPC.

## 5. Manual QA Matrix

Cover these cases before marking implementation ready:

- Public static article with readable text.
- Metadata-only public URL.
- JavaScript-heavy public page with limited result.
- YouTube variants: `youtu.be`, `youtube.com`, subdomains, `youtube-nocookie.com`, Shorts/live/embed paths, and scheme-less links.
- YouTube copied from browser and messaging app; verify provider plus origin app context.
- Category-only video/browser/social/reading capture; verify generic identity only.
- Multiple URLs in one capture; verify primary/supporting roles.
- Duplicate canonical URL; verify `Already saved` path.
- Screenshot-only capture with OCR text.
- Sensitive screenshot/private message/financial/health/work auth case; verify cloud visual suppression.
- Blocked/private/paywalled/robots-disallowed/app-only source; verify limitation without fabricated summary.
- Cloud disabled/offline/over budget; verify save succeeds and limitations are visible.
- Wrong source/summary feedback and refresh; verify new version preserves history.
- Domain suppression; verify future fetch suppression and visible limitation.
- Delete capture; verify derived records are not displayed or eligible within 30 seconds and content-free lifecycle audit remains.

## 6. Done Means

- Source identity accuracy and branded identity guardrails pass the evaluation set.
- Every visible summary names or implies only evidence that exists.
- Basic/Smart/Deep policy tests pass, including blocked cloud/browser/sensitive paths.
- Local fallback works with cloud disabled or network unavailable.
- Deletion/invalidation prevents downstream eligibility.
- Binder/AIDL and audit traces remain compact and content-free.
- The feature still does not implement any explicitly deferred system.
