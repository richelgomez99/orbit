# Plan: AI-Assisted Link Rehydration

## Current State

`UrlHydrateWorker` already calls `NanoSummariser`, and `NanoSummariser` already uses `LlmProviderRouter`. Because `LlmProviderRouter.hasNanoCapableHardware()` currently returns false, the production path routes through `CloudLlmProvider` and the existing `:net` `INetworkGateway.callLlmGateway` surface when auth/config are available.

The missing product layer is context. Before this spec, `UrlSummaryPrompt` accepted page title + readable text only. Spec 005A follow-up added prompt slots for final URL, source app label, and user note, but `UrlHydrateWorker` can currently provide only final URL without a repository context extension.

## Implementation Sequence

1. **Prompt foundation**: Extend `UrlSummaryPrompt` and `NanoSummariser` to accept compact URL/source/note hints.
2. **Source wording**: Ensure Diary/detail fallbacks do not render `unknown`.
3. **Repository context packet**: Add a compact binder method or extend existing hydration write path so `UrlHydrateWorker` can request `sourceAppLabel` and latest note for an envelope before summarization.
4. **Prompt safety tests**: Add tests for allowed context fields and banned field exclusion.
5. **Hydration integration tests**: Prove copied links and screenshot-derived links produce summaries with context when available and deterministic fallback otherwise.

## Privacy Boundary

Allowed prompt fields:
- URL/final URL/canonical host
- page title
- source app label
- latest user note/context, capped
- readable extracted text, capped by `UrlSummaryPrompt.MAX_CONTENT_CHARS`

Forbidden prompt fields:
- raw screenshot bytes
- full OCR bodies
- raw HTML
- access/refresh tokens
- provider API keys
- prior prompts or model responses

## Validation

```bash
./gradlew :app:testDebugUnitTest --tests "com.orbit.app.ai.NanoSummariserTest"
./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin
./gradlew :app:lintDebug :app:assembleDebug
```
