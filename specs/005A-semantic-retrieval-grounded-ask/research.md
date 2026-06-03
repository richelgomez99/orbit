# Research: Semantic Retrieval And Grounded Ask

**Spec**: `005A-semantic-retrieval-grounded-ask`
**Date**: 2026-06-03

## Decision 1: Use Compact-Text Embeddings, Not Raw Artifact Embeddings

Decision: Generate embeddings from the same compact memory representation used by Spec 005: title, summary, tags, user note/context, source labels, capped evidence, and compact text.

Rationale:

- Orbit's local-first promise depends on not uploading raw screenshots, full OCR bodies, raw HTML, prompts, or model responses.
- The compact representation already has caps and banned-field tests.
- User-provided context is often higher signal than OCR, as shown by the `qr code` screenshot issue.

Alternatives rejected:

- Raw OCR embeddings: better recall, but violates the payload boundary and may leak private text.
- Screenshot/image embeddings: useful later, but requires a separate consent/model policy and image-vector field. Not needed for 005A.
- Android-generated cloud embeddings: would put provider credentials or an additional cloud path too close to the client.

## Decision 2: Initial Embedding Model

Decision: Use `openai:text-embedding-3-small` with 1536 dimensions for the first implementation.

Rationale:

- OpenAI's embeddings guide lists `text-embedding-3-small` as a current embedding model, and the model is 1536 dimensions.
- 1536-dimensional vectors fit well within MongoDB Atlas Vector Search dimension limits and are lower cost/storage than 3072-dimensional large embeddings.
- The branch's goal is retrieval quality sufficient for MVP Ask, not final local/offline inference. Spec 022 owns local model embeddings.

Alternatives:

- `text-embedding-3-large`: higher-quality and 3072 dimensions, but more expensive and larger. Keep as a policy upgrade option after fixture evidence shows small is insufficient.
- ZeroEntropy/gbrain embeddings: useful for development memory, but gbrain is not Orbit's product backend.
- Local model embeddings now: strategically aligned, but would pull Spec 022 into 005A and delay the retrieval fix.

Source notes:

- OpenAI embeddings docs: `https://platform.openai.com/docs/guides/embeddings`
- OpenAI embeddings FAQ/models page: `https://help.openai.com/en/articles/6824809-embeddings-frequently-asked-questions`

## Decision 3: Atlas Vector Search Shape

Decision: Add a vector search index on `embedding` with:

```json
{
  "fields": [
    {
      "type": "vector",
      "path": "embedding",
      "numDimensions": 1536,
      "similarity": "cosine"
    },
    {
      "type": "filter",
      "path": "userId"
    },
    {
      "type": "filter",
      "path": "tombstonedAt"
    },
    {
      "type": "filter",
      "path": "intent"
    },
    {
      "type": "filter",
      "path": "appCategory"
    },
    {
      "type": "filter",
      "path": "dayLocal"
    }
  ]
}
```

Rationale:

- Atlas Vector Search supports vector fields with dimensions and similarity, plus filter fields used by `$vectorSearch` filters.
- User scoping and tombstone exclusion must happen at query time and in tests.
- Cosine similarity is standard for normalized text embeddings and matches existing cluster/similarity concepts.

Alternatives:

- Separate collection for embeddings: safer for migrations, but more moving parts. Use embedded vector fields first because Spec 005 already reserved them.
- Supabase pgvector: still viable later, but current Atlas credits and collection are already set up.

Source notes:

- Atlas Vector Search docs: `https://www.mongodb.com/docs/atlas/atlas-vector-search/`
- `$vectorSearch` aggregation stage docs: `https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/`

## Decision 4: Hybrid Retrieval, Not Vector-Only Retrieval

Decision: Return a deterministic hybrid score that combines:

- semantic score from Atlas Vector Search;
- lexical score from compact text/title/summary/tags/evidence;
- explicit context-note match bonus;
- local-existence gate on Android;
- duplicate representative collapse;
- recency tie-breaker only after relevance.

Rationale:

- Vector-only retrieval can be semantically broad and may over-rank nearby-but-wrong memories.
- Lexical-only retrieval caused the Spec 005 screenshot failures.
- Hybrid scoring lets context notes and exact user language stay strong while semantic retrieval handles paraphrase.

## Decision 5: Ask Is Retrieval-Grounded First, LLM-Synthesized Second

Decision: Implement Ask in two stages inside 005A:

1. Hybrid retrieval and extractive cited answer/refusal.
2. Optional LLM synthesis over only the top compact cited evidence after thresholds pass.

Rationale:

- The product value is finding and citing the right saved evidence.
- An LLM cannot fix bad retrieval and can make wrong results sound confident.
- If time is tight, hybrid retrieval + extractive answer is still a real improvement and independently testable.

## Decision 6: Sensitive Identifier Refusal Policy

Decision: Add a policy gate for sensitive identifier questions. Passport, SSN, credit card, access code, API key, password, and token-style questions require a high-confidence exact evidence match and otherwise refuse.

Rationale:

- Screenshot testing showed unrelated numeric captures can appear for `passport number`.
- A memory agent must avoid leaking or inventing sensitive identifiers.
- This policy is useful even after LLM synthesis lands.

## Decision 7: Evaluation Fixtures Are Required Before Implementation Is Complete

Decision: The branch cannot close without backend and Android tests for:

- startup event ranking;
- flight receipt ranking;
- recipe relevant captures;
- qr-code context-note search;
- reschedule/rescheduling/cancelled recall;
- unsupported passport-number refusal;
- duplicate-looking capture collapse;
- local-existence filtering.

Rationale:

- These fixtures come from actual S24 screenshots and debugging, not hypothetical QA.
- They convert "feels better" into a regression gate.
