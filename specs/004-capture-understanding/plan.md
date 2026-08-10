# Implementation Plan: Screenshot Cleanup + Active Intent

## Objective

Build the first working Orbit loop: Basic local understanding turns captures into an Active Intent cleanup queue that answers "Which screenshots still need something from me?"

## Current Branch State

- Branch: `feature/004-active-intent-cleanup-20260518`
- Base: clean Orbit identity commit `196e260`
- App package: `com.orbit.app`
- Room version: 7
- Next migration: v7 to v8
- Visual language: alpha default on
- Debug APK: one user-facing launcher icon

## Findings Rechecked From Earlier Audit

| Finding | Current Status | Plan Impact |
| --- | --- | --- |
| Old branch `spec/003-orbit-actions`, empty stashes | Stale | Current branch is the clean Spec 004 feature branch; recovery stashes exist and are preserved. |
| Dirty tree likely did not compile due to missing `DismissTargetUI` | Fixed | `DismissTargetUI` exists and `OrbitOverlayService` imports it. Build gates passed on the hygiene commit. |
| Debug Diagnostics launcher icon | Fixed | Debug activities are declared without launcher intent filters. |
| Missing `ActionsSettingsActivity` manifest class | Fixed | Manifest entry was removed and documented. |
| Local-only README/constitution conflict | Mostly fixed | README now says local-first/cloud-augmented. Spec 004 must keep Basic local and escalations explicit. |
| Template backup/data extraction rules with `allowBackup=true` | Still real | Track outside Spec 004 slice unless Active Intent introduces new backup-sensitive storage rules. |
| Direct `OrbitDatabase.getInstance()` outside stated `:ml` owner | Still real | Spec 004 must not add more direct DB access outside `:ml`; new writes should route through existing repository/binder seams or be explicitly approved. |
| AppFunctions `1.0.0-alpha01` age | Still real but unrelated | Leave to Spec 003/dependency audit unless implementation touches AppFunctions. |

## Architecture

### Storage

Add v8 sidecar tables rather than overloading `IntentEnvelopeEntity`:

- `capture_understanding`: compact Basic understanding per capture.
- `evidence_bundle`: compact evidence descriptors only, no raw bodies.
- `invalidation_record`: derived-state invalidation facts.
- `active_intent`: user-facing unresolved intent state.

### Engine

Basic mode is deterministic and local. Inputs may include:

- foreground package and app label
- app category dictionary
- capture timestamp
- URL/deeplink/canonical URL when present
- existing OCR/text hints when already locally available
- local regexes for prices, order IDs, dates, coupon codes, addresses, ingredients, URLs, and QR payloads

Basic mode must not call network, cloud providers, public fetch, oEmbed, Readability, browser automation, or VLM.

### Repository Boundary

Implementation should prefer the existing `:ml` database owner boundary. The first coding slice should inspect `EnvelopeRepositoryService`, `EnvelopeRepositoryImpl`, and binder parcels before choosing whether Active Intent is exposed by:

1. extending the existing envelope repository contract with compact Active Intent models, or
2. adding a narrowly scoped Active Intent repository service in `:ml`.

Do not add new direct Room calls from `:ui` or `:capture` during Spec 004.

### UI

The first UI surface is not chat. It is an Active Intent cleanup view/card surface with:

- category
- source/provider/app basis
- completion key status
- compact evidence snippet
- primary resolution action
- archive/resolve controls
- explicit escalation control when Basic is insufficient

## Implementation Order

1. Lock Speckit artifacts and task list.
2. Add v8 data model, DAOs, migrations, schemas, and migration tests.
3. Add domain models and Basic classifier/extractor tests.
4. Add Active Intent projection and invalidation service.
5. Add repository access through the chosen `:ml` boundary.
6. Add UI state and minimal Compose surface.
7. Run Gradle gates and physical device validation.

## Validation Gates

Run after each meaningful slice:

- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:lintDebug`
- `./gradlew :app:compileDebugAndroidTestKotlin`

Run after schema work:

- `./gradlew :app:kspDebugKotlin`
- migration instrumentation test for v7 to v8

Run before device dogfood:

- `./gradlew :app:assembleDebug`
- APK badging check for one launcher
- install on S24; Tab S9 verification is intentionally skipped for this slice

## Out Of Scope For This Branch

- Ask Orbit
- KG backend
- action execution beyond local resolution state
- auto-delete
- notifications by default
- cloud VLM by default
- AppFunctions dependency upgrade
- backup/data extraction policy cleanup unless required by new tables
