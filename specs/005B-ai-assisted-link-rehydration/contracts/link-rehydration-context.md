# Contract: Link Rehydration Context

## Purpose

Provide `UrlHydrateWorker` with compact capture context so AI-assisted summaries can explain why a link matters without uploading raw artifacts.

## Proposed Shape

```json
{
  "envelopeId": "uuid",
  "sourceAppLabel": "Chrome",
  "latestNote": "Saved to decide whether to attend.",
  "contentType": "TEXT",
  "caps": {
    "sourceAppLabel": 80,
    "latestNote": 300
  }
}
```

## Allowed Fields

- `envelopeId`
- `sourceAppLabel`
- `latestNote`
- `contentType`
- `caps`

## Forbidden Fields

- screenshot/image bytes
- raw OCR/full OCR body
- raw HTML/readable HTML beyond the existing summarizer cap
- access tokens
- refresh tokens
- provider API keys
- prompt text
- model responses

## Failure Contract

If context lookup fails, `UrlHydrateWorker` continues with final URL + page title + readable text only.
