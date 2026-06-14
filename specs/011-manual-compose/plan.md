# Implementation Plan: Manual Compose And Capture Context

**Branch**: `feature/011-manual-compose-capture-context-20260613` | **Date**: 2026-06-13 | **Spec**: `specs/011-manual-compose/spec.md`
**Input**: Feature specification from `specs/011-manual-compose/spec.md`

## Summary

Spec 011 adds explicit user context at capture time and a deliberate manual text compose path. The implementation reuses existing IntentEnvelope seal/storage, duplicate suppression, audit, and `envelope_note` APIs rather than adding a parallel notes/context model.

## Technical Context

**Language/Version**: Kotlin, Android Gradle project, JDK 21  
**Primary Dependencies**: Jetpack Compose, Room/SQLCipher, AIDL Binder, WorkManager  
**Storage**: Existing Room v10 `intent_envelope` and `envelope_note`; no new migration planned  
**Testing**: JVM unit tests, Android source-ready tests, custom lint tests, Android lint  
**Target Platform**: Android app across `:ui`, `:capture`, `:ml`, and `:net` processes  
**Project Type**: Mobile app  
**Performance Goals**: Capture context path opens in two taps or fewer after save; no foreground network dependency  
**Constraints**: Local-first, no direct Room outside `:ml`, no network required, no raw artifacts in compact context payloads  
**Scale/Scope**: One overlay post-capture context path, one manual text compose path, reuse existing detail/search/memory-index consumers

## Constitution Check

- **Principle I Local-First Supremacy**: PASS. Context and manual compose write locally first. No cloud/model requirement.
- **Principle II Effortless Capture, Any Path**: PASS. Reactive overlay capture gains context; deliberate compose uses the same seal contract.
- **Principle III Intent Before Artifact**: PASS. The feature captures why the user saved something.
- **Principle VI Privilege Separation**: PASS if all writes route through Binder/repository into `:ml`.
- **Principle VIII Collect Only What You Use**: PASS. User context is immediately used by detail, search, hydration, memory indexing, and future KG.
- **Principle XII Provenance Or It Didn't Happen**: PASS if backfill preserves wall-clock audit and context notes target source envelopes.

No constitutional violations are expected.

## Project Structure

### Documentation

```text
specs/011-manual-compose/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── manual-compose-contract.md
└── tasks.md
```

### Source Code

Expected implementation surfaces:

```text
app/src/main/java/com/orbit/app/overlay/
├── OverlayViewModel.kt
├── UndoPill.kt
└── PostCaptureUi.kt

app/src/main/java/com/orbit/app/service/
└── OrbitOverlayService.kt

app/src/main/java/com/orbit/app/diary/
├── DiaryRepository.kt
├── BinderDiaryRepository.kt
├── DiaryViewModel.kt
├── ManualComposeViewModel.kt        # expected new UI seam
└── EnvelopeDetailActivity.kt

app/src/main/java/com/orbit/app/diary/ui/
├── DiaryScreen.kt / DiaryActivity composition sites
└── ManualComposeScreen.kt           # expected new Compose surface

app/src/test/java/com/orbit/app/overlay/
app/src/test/java/com/orbit/app/diary/
app/src/androidTest/java/com/orbit/app/data/ipc/
```

**Structure Decision**: Keep capture context in overlay/detail surfaces and manual compose in Diary UI. Persistence stays in existing repository/Binder APIs.

## Implementation Approach

1. Add overlay post-capture context action for `SilentWrapPill`/`UndoPill` outcomes.
2. Route context action to `EnvelopeDetailActivity.newIntent(..., startNote = true)` or a focused note dialog that still calls `createOrUpdateLatestNote`.
3. Add manual compose ViewModel/UI with body/context validation.
4. Save manual compose through existing repository seal path, then attach optional context note.
5. Verify existing Library/search/hydration/memory-index consumers still use note context; add focused tests where needed.
6. Run full non-phone validation before closing the branch.

## Complexity Tracking

No complexity exceptions.
