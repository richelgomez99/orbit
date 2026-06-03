# Specification: AI-Assisted Link Rehydration

**Branch target**: `feature/005b-ai-assisted-link-rehydration-20260603`
**Status**: Drafted from 005A follow-up; implementation may be stacked after 005A closeout.

## User Need

When Orbit hydrates links found in copied text or screenshots, the resulting title and summary should reflect why the user saved the link, not just the website's generic title. User context such as the URL, source app, domain, capture note, and compact OCR/link text should guide the summary.

## Requirements

- FR-005B-001: URL hydration MAY use the existing `LlmProvider` route to produce an assisted summary when cloud/local AI is available.
- FR-005B-002: The prompt MUST include only compact fields: final URL, domain, page title, source app label if known, user note/context if present, and capped readable text.
- FR-005B-003: The prompt MUST NOT include raw screenshots, full OCR bodies, raw HTML, prompt history, model responses, secrets, access tokens, or refresh tokens.
- FR-005B-004: If AI summary fails, hydration MUST persist deterministic metadata and `summaryModel="fallback"` or the existing provider label behavior.
- FR-005B-005: Existing process boundaries remain: Android network access stays in `:net`; Room/source-of-truth stays in `:ml`; cloud LLM calls go through `INetworkGateway.callLlmGateway`.
- FR-005B-006: The UI must never show `unknown` as a user-facing source. Missing source should render as `phone capture`, `saved from phone`, or a category fallback.
- FR-005B-007: Tests must prove context hints enter the prompt and banned fields do not.

## Non-Goals

- Raw screenshot VLM analysis.
- Uploading full OCR or raw HTML to a cloud model.
- Knowledge graph entity extraction.
- Action proposal generation from hydrated links.

## Acceptance

1. A copied link with a user note gets a summary that uses the note as context.
2. A screenshot-derived link without source app label displays as a phone capture, not unknown.
3. Disabling cloud/model access still hydrates deterministic metadata and leaves existing Diary/Library behavior working.
