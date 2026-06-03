# Tasks: AI-Assisted Link Rehydration

## Phase 0: Spec Lock

- [x] T005B-001 Create `specs/005B-ai-assisted-link-rehydration/`.
- [x] T005B-002 Document current state, privacy boundary, and implementation sequence.

## Phase 1: Prompt Foundation

- [x] T005B-003 Extend `app/src/main/java/com/orbit/app/ai/prompts/UrlSummaryPrompt.kt` with optional final URL, source app, and user note fields.
- [x] T005B-004 Extend `app/src/main/java/com/orbit/app/ai/NanoSummariser.kt` to pass compact context fields into the prompt.
- [x] T005B-005 Pass final URL from `app/src/main/java/com/orbit/app/continuation/UrlHydrateWorker.kt` into `NanoSummariser`.
- [x] T005B-006 Add `NanoSummariserTest` coverage that URL/source/note hints enter the prompt.

## Phase 2: User-Facing Source Copy

- [x] T005B-007 Update screenshot detail fallback copy so missing source does not render as `unknown`.
- [x] T005B-008 Add/extend detail UI tests for `Screenshot saved from phone` fallback.

## Phase 3: Repository Context Packet

- [x] T005B-009 Add compact hydration context DTO containing `envelopeId`, `sourceAppLabel`, latest note, content type, and caps.
- [x] T005B-010 Expose the context through `IEnvelopeRepository` or an existing safe binder path.
- [x] T005B-011 Update `UrlHydrateWorker` to request context before summarization.
- [x] T005B-012 Prove banned fields never enter context or prompt.

## Phase 4: Validation

- [x] T005B-013 Run focused JVM tests for summarizer and prompt.
- [x] T005B-014 Run Android compile + lint gate.
- [x] T005B-015 Build and install APK for manual link-rehydration testing. APK built at `dist/orbit-mvp-debug-20260603-005b.apk` (`7a80f2dc94b00766177eec6f80393bf4289c39c8252cd6f3a44d925c1369c885`) and installed/launched on the S24. User confirmed actions worked, and post-fix S24 visual recheck confirmed capture/detail copy no longer shows `from IntentResolver`.
