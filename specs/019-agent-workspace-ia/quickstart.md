# Quickstart: Agent Workspace IA

## Local Validation

```bash
./gradlew :app:testDebugUnitTest --tests "com.orbit.app.library.LibraryViewModelTest"
./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin
git diff --check
```

## Demo Flow

1. Search in Library.
2. Switch to Diary.
3. Return to Library; query/results should be reset.
4. Ask a question in Orbit.
5. Switch to Diary or Library.
6. Return to Orbit; Ask input/answer should be reset while follow-ups/action drafts remain visible from durable flows.

## Validation Log

2026-06-14:

- Created Spec 019 artifacts. Current app already has Diary/Library/Orbit tabs; branch scope is IA state-boundary hardening.
