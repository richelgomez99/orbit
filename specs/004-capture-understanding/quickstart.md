# Quickstart: Screenshot Cleanup + Active Intent

## Purpose

Use this checklist to validate that Spec 004 is building the actual app wedge: Basic mode answers "Which screenshots still need something from me?"

## Local Gates

Before coding:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug
```

After Room schema work:

```bash
./gradlew :app:kspDebugKotlin :app:compileDebugAndroidTestKotlin
```

Before device install:

```bash
./gradlew :app:assembleDebug
$HOME/Library/Android/sdk/build-tools/37.0.0/aapt dump badging app/build/outputs/apk/debug/app-debug.apk | rg "package:|application-label:|launchable-activity"
```

Expected badging:

- package: `com.orbit.app`
- label: `Orbit`
- launchable activity: `com.orbit.app.diary.DiaryActivity`

## Basic Mode Rules

Basic mode may use:

- foreground app/package and user-facing app label
- local app category dictionary
- capture timestamp
- URL/deeplink/canonical URL when already available
- local OCR/text hints when already available
- local regex and deterministic parsers

Basic mode must not use:

- cloud LLM providers
- public URL fetch
- oEmbed
- Readability extraction
- browser automation
- VLM
- network clients outside `com.orbit.app.net.*`

## Screenshot Validation Set

Use at least 20 screenshots/captures covering:

- buy later product
- recipe
- QR or barcode
- receipt or order
- event/ticket/reservation
- coupon or promo
- read/watch later
- place or travel idea
- gift idea
- chat action
- stale or already resolved item
- unknown / not actionable

For each capture, record:

| Field | Notes |
| --- | --- |
| Capture ID | Local id only |
| Expected category | One initial category |
| Still active? | active / resolved / expired / stale / unclear |
| Completion key expected | product, code, date, URL, address, order ID, requested action, etc. |
| Completion key extracted? | found / missing / not actionable / needs escalation |
| Evidence basis | app, URL, OCR hint, timestamp, local regex |
| User trust | high / medium / low |
| Action taken | resolve / archive / mark not interested / escalate / none |

## Pass Threshold For First Dogfood

- 20+ captures reviewed.
- No Basic-mode network calls.
- No crash or ANR.
- 60%+ of active captures get the right category.
- 50%+ of active captures get a useful completion key or an honest missing-key state.
- Maybe-old/stale captures do not dominate the primary Active Intent queue.

## Device Validation Log

### 2026-05-19 — S24 retry

- Device: `SM-S928U1` on Android 16, user 0.
- Installed verified debug APK directly with `adb install --user 0 -r app/build/outputs/apk/debug/app-debug.apk`.
- APK badging verified locally: package `com.orbit.app`, label `Orbit`.
- Device package path verified for user 0: `/data/app/.../com.orbit.app.../base.apk`.
- Launcher query verified exactly one Orbit launcher activity for user 0: `com.orbit.app/.diary.DiaryActivity`.
- Focused connected UI test passed on S24: `com.orbit.app.diary.ui.ActiveIntentCleanupPanelTest`.
- Direct launch verified: `com.orbit.app/.diary.DiaryActivity` routes to `com.orbit.app.onboarding.OnboardingActivity` and foreground focus becomes Orbit.
- Legacy package still present on the same device: `com.capsule.capsule`. Do not count this as an Orbit launcher; remove only with explicit user approval because uninstalling may delete old local app data.
- Tab S9 verification intentionally skipped for this slice by product decision on 2026-05-19.
- Not yet complete: capture flow, overlay permission flow, and Active Intent surface with real Gallery screenshots.

### 2026-05-19 — Capture-to-cleanup wiring

- Basic understanding persistence is wired in `:ml` after envelope seal commits.
- Screenshot OCR hydration now refreshes the same Basic understanding and Active Intent sidecars using local OCR text and URL hints.
- Resolved Active Intent rows are not resurrected by later Basic refreshes.
- Verified gates: focused understanding JVM tests, `:app:compileDebugKotlin`, `:app:compileDebugAndroidTestKotlin`, and `:app:lintDebug`.

### 2026-05-19 — Seeded Basic-mode biopsy

- Validation type: deterministic seeded capture biopsy using 21 local text/source/url samples in `BasicUnderstandingBiopsyTest`; no Gallery data or cloud behavior used.
- Command: `./gradlew --console=plain :app:testDebugUnitTest --tests com.orbit.app.understanding.BasicUnderstandingBiopsyTest --tests com.orbit.app.understanding.BasicUnderstandingEngineTest --tests com.orbit.app.diary.ActiveIntentUiStateTest`.
- Result: `BUILD SUCCESSFUL`.
- Screenshots/captures reviewed: 21 seeded captures.
- Category distribution:
  - buy later product: 2
  - recipe: 2
  - QR/barcode: 2
  - receipt/order: 2
  - event/ticket/reservation: 2
  - coupon/promo: 2
  - read/watch later: 2
  - place/travel idea: 2
  - gift idea: 1
  - chat action: 2
  - maybe old/inactive: 1
  - unknown: 1
- Category correctness: 21/21 expected categories in the seeded set.
- Active/unresolved percentage: 20/21 are current unresolved or context-needed captures; 1/21 is maybe-old triage. Maybe-old does not dominate the queue.
- Completion-key extraction: 18/21 found a compact key; 1/21 produced an honest missing-key state; 1/21 produced not-actionable for stale/empty; 1/21 produced needs-escalation for current empty/unknown.
- User action simulation: no live user actions were taken in this seeded pass. Expected UI actions are category-specific resolution labels plus `Add context` for missing/unknown rows and `Not needed` for rows the user wants to clear from follow-up.
- Trust notes: Basic mode stayed local and deterministic. The appointment-from-Messages regression was fixed so weak event words like `appointment` no longer override messaging context; stronger ticket/booking/boarding evidence still classifies as event/reservation.
- Top remaining risks: existing persisted rows created by older classifier logic need reprocessing/versioning before they relabel automatically; real OCR screenshots may be noisier than seeded text; unknown current-empty captures may need product tuning if they create too much low-value context work.
- Product decision: continue with Active Intent as the Basic-mode wedge, then validate against real S24 Gallery screenshots before expanding cloud/agentic behavior.

### 2026-05-19 — S24 empty-Diary setup detour removal

- User confirmation: S24 empty Diary flow works after removing the giant `Set up Orbit` CTA from the empty state.
- Installed corrected debug APK on S24 user 0 through pinned ADB transport 61.
- Device package path verified: `/data/app/.../com.orbit.app.../base.apk`.
- Launcher query verified Orbit resolves to `com.orbit.app/.diary.DiaryActivity`.
- Visual confirmation: empty Diary now shows only `Nothing saved yet` and `Copy something, then tap the bubble.` with setup reachable through the settings gear instead of the empty-state detour.

### 2026-05-19 — S24 real screenshot capture flow

- Device: `SM-S928U1` on Android 16, user 0, ADB transport 61.
- Capture readiness: Orbit overlay service was already running as a foreground service; `SYSTEM_ALERT_WINDOW`, `GET_USAGE_STATS`, `POST_NOTIFICATIONS`, `READ_MEDIA_IMAGES`, and `ACTIVITY_RECOGNITION` were granted/allowed for validation.
- Test input: opened Brave to `https://example.com/?promo_code=ORBIT15&expires=May_20_2026&sale_ends=true`, then triggered a real system screenshot with `adb shell input keyevent 120`.
- Screenshot observer result: logcat recorded two `ScreenshotObserver: ENVELOPE_SEALED` entries for real MediaStore image URIs under `content://media/external/images/media/...`.
- OCR hydration result: WorkManager recorded `SUCCESS` for two `com.orbit.app.continuation.ScreenshotUrlExtractWorker` jobs tagged `type:SCREENSHOT_OCR`.
- Active Intent surface result: explicitly launched `com.orbit.app/.diary.DiaryActivity`; the S24 Diary showed a populated `Needs follow-up` surface with 3 captures, including `Capture to review`, `Event or reservation`, and `Message follow-up` rows with the clarified action labels (`Mark handled`, `Not needed`, `Saved or attended`, `Replied or handled`, `Add context`).
- Product note: real screenshot capture-to-cleanup path works end-to-end. Some rows reflected prior persisted validation captures before the refresh fix below.

### 2026-05-19 — Active Basic row refresh

- Added a bounded `:ml` startup refresh for active Basic rows using compact `capture_understanding` sidecars.
- Scope: only rows with stable `basic:<captureId>` ids and `ACTIVE` status are refreshed; resolved, archived, expired, and invalidated rows remain untouched so user decisions are not resurrected.
- Purpose: lets prior active rows relabel after deterministic classifier changes, such as the message-appointment fix that moved weak `appointment` text from event/reservation to message follow-up when source context is messaging.
- Verification: focused `BasicUnderstandingWriterTest`, `BasicUnderstandingEngineTest`, `BasicUnderstandingBiopsyTest`, `ActiveIntentRepositoryContractTest`, `ActiveIntentProjectionTest`, `:app:compileDebugKotlin`, and `:app:compileDebugAndroidTestKotlin` passed.

### 2026-05-20 — Ask Orbit decision brief

- Product change: Active Intent cards now offer `Ask Orbit` on every row instead of framing help as a Basic/Smart/Deep level choice.
- Current implementation: tapping `Ask Orbit` records `ACTIVE_INTENT_ESCALATION_REQUESTED` in `:ml`, then writes a compact local `ORBIT_REVIEW` decision brief back onto the active row. No cloud provider, network fetch, screenshots, raw OCR, prompts, model responses, or embeddings are dispatched in this slice.
- UI behavior: the card changes to `From: Orbit review · Decision brief · <time>`, keeps the compact clue when available, shows a human-readable `Why it appears`, and changes the action label to `Refresh review` once a brief exists.
- UI behavior after T004-051: every cleanup row now starts with `View capture`, offers `Add context` for user notes, keeps `Ask Orbit` as a helper action, and routes context entry to the saved capture detail screen. The current graphite capture detail screen shows the image/text plus the same context note field, so users can inspect the capture before deciding.
- UI behavior after T004-052: the `Needs follow-up` panel can collapse to a compact header and can filter rows by `All`, `Needs context`, `Ready`, and `Maybe old`, so demo and daily review sessions can focus on one kind of decision at a time.
- UI behavior after T004-053: `View capture` shows a meaningful fallback title even when hydration has no title/summary, long captured text remains available in the detail scroll, and screenshot captures render uncropped with tap-to-expand full-screen inspection.
- Updated copy: source lines now read `From: ...`, evidence snippets read `Capture clue: ...`, and reasons read `Why it appears: ...` so the queue is clearer about why a screenshot is present.
- Decision capture: user decisions still use the existing resolve/archive path (`Mark handled`, category-specific handled labels, and `Not needed`) so rows leave the cleanup queue only after the user confirms.
- Decision surface: tapping `Ask Orbit` now opens a focused dialog with the card title, `Source`, optional `Clue`, `Why`, guidance, and the three immediate choices: review with Orbit, mark handled, or not needed.
- Cloud readiness: `ActiveIntentReviewContext` now produces the compact future-dispatch packet and the same packet is included in the audit event. Its contract test rejects forbidden keys for raw screenshots, OCR bodies, HTML, embeddings, prompts, and model responses.
- Gateway readiness: `supabase/functions/llm_gateway` now accepts `active_intent_review`, validates the compact context, rejects forbidden raw evidence keys, routes to a Haiku decision-brief handler, and returns `active_intent_review_response`. Android has matching `LlmGatewayRequest.ActiveIntentReview` and `LlmGatewayResponse.ActiveIntentReviewResponse` DTOs.
- Refresh behavior: active Basic sidecar refresh preserves an existing `ORBIT_REVIEW` brief so startup reprocessing does not erase the user's asked-for review.
- Verification: focused `ActiveIntentUiStateTest`, `ActiveIntentRepositoryContractTest`, `BasicUnderstandingWriterTest`, `LlmGatewayParcelRoundTripTest`, gateway `npm run typecheck`, gateway focused `router.test.ts` and `anthropic_handlers.test.ts`, `:app:compileDebugKotlin`, and `:app:compileDebugAndroidTestKotlin` passed on 2026-05-20. The decision dialog androidTest source also compiles; live connected execution is pending phone availability.

## Known Deferred Risks

- Backup/data extraction XML is still template-like while `allowBackup=true`; this is tracked outside the first Spec 004 slice.
- Existing direct `OrbitDatabase.getInstance()` calls outside `:ml` remain architectural debt; Spec 004 must not add more.
- AppFunctions dependency age is unrelated to Active Intent unless this branch touches action runtime.
