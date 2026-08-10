# Capture Understanding Evaluation Fixtures

These fixtures are synthetic unless a case explicitly says `dogfoodSynthetic: false`.

Retention rules:

- Keep real dogfood content out of this manifest; store only anonymized IDs, source class, expected limitation, and pass/fail notes.
- Do not store screenshots, OCR bulk text, full page text, raw prompts, model responses, embeddings, cookies, tokens, or account identifiers.
- Any dogfood case that contains real user content must be deleted or anonymized before the branch is merged.
- Deletion/invalidation cases must assert that derived records become ineligible while content-free audit metadata may remain.
- Public URLs in this file are examples for resolver behavior, not a guarantee that the remote page is stable.