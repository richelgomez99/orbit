# Implementation Plan: Capture Context Affordance

**Branch**: `feature/018-capture-context-affordance-20260614` | **Date**: 2026-06-14 | **Spec**: `specs/018-capture-context-affordance/spec.md`
**Input**: Feature specification from `specs/018-capture-context-affordance/spec.md`

## Summary

Spec 018 reconciles the May 22 "Clarify" affordance with the current implementation. Spec 011 already shipped context storage and post-capture entry through full detail. This branch should add a lighter focused post-save context surface, or explicitly close the slot as absorbed if the implementation risk is not justified.

## Technical Context

**Language/Version**: Kotlin, Android Gradle project, JDK 21  
**Primary Dependencies**: Jetpack Compose, AIDL Binder, existing Diary/Overlay ViewModels  
**Storage**: Existing Room/SQLCipher `envelope_note` via Binder; no new schema expected  
**Testing**: JVM unit tests, Android source-ready Compose tests, compile/lint gates  
**Target Platform**: Android app across `:capture`, `:ui`, `:ml`, `:net` processes  
**Project Type**: Mobile app  
**Performance Goals**: Context entry opens from post-capture pill with one tap and no full-detail navigation; no network dependency  
**Constraints**: Local-first, no direct Room outside `:ml`, no raw artifact payloads, preserve undo/duplicate semantics  
**Scale/Scope**: One focused context entry flow for new and duplicate post-capture states; reuse existing note consumers

## Constitution Check

- **Principle I Local-First Supremacy**: PASS. Context writes locally and works offline.
- **Principle II Effortless Capture, Any Path**: PASS if focused entry is lighter than full detail and does not block saving.
- **Principle III Intent Before Artifact**: PASS. The feature captures why the user saved the artifact.
- **Principle VI Privilege Separation**: PASS if all writes route through Binder into `:ml`.
- **Principle VIII Collect Only What You Use**: PASS. Context already feeds detail, Library, hydration, memory index, and future KG.
- **Principle XII Provenance Or It Didn't Happen**: PASS. Context targets a saved envelope id and remains source-linked.

No constitutional violation is expected.

## Project Structure

### Documentation

```text
specs/018-capture-context-affordance/
├── spec.md
├── research.md
├── data-model.md
├── contracts/
│   └── capture-context-affordance-contract.md
├── plan.md
├── quickstart.md
└── tasks.md
```

### Source Code

Expected implementation surfaces:

```text
app/src/main/java/com/orbit/app/overlay/
├── OverlayViewModel.kt
├── PostCaptureOverlay.kt
├── SilentWrapPill.kt
├── UndoPill.kt
└── CaptureContextDialog.kt        # expected new or equivalent focused UI

app/src/main/java/com/orbit/app/service/
└── OrbitOverlayService.kt

app/src/main/java/com/orbit/app/diary/
├── DiaryRepository.kt
├── BinderDiaryRepository.kt
└── CaptureContextSaver.kt          # optional reusable seam if needed

app/src/test/java/com/orbit/app/overlay/
app/src/test/java/com/orbit/app/diary/
app/src/androidTest/java/com/orbit/app/overlay/
```

**Structure Decision**: Keep UI affordance in overlay/post-capture code and persistence in existing Diary/Binder repository seams. Add a small saver/ViewModel seam only if it reduces duplicated Binder/error handling.

## Implementation Approach

1. Add a small `CaptureContextDraft`/saver or ViewModel seam that validates blank text and calls existing note persistence.
2. Route new and duplicate post-capture context actions to the focused surface instead of immediately launching full detail, while preserving a fallback to full detail if Binder/UI is unavailable.
3. Render a Quiet Almanac focused context dialog/surface with save, cancel, retry, and failure states.
4. Verify context save targets the correct envelope for new and duplicate captures and does not disturb undo/duplicate state.
5. Verify Library/local consumers still see the note through existing Spec 011 coverage; add only focused regression tests if needed.
6. Run focused and full non-phone validation.

## Complexity Tracking

No complexity exceptions expected.
