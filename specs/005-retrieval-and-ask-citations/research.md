# Research: Atlas Memory Index + Cited Library

**Date**: 2026-05-30
**Spec**: [spec.md](spec.md)

## 1. Atlas Role

Decision: Use MongoDB Atlas as an MVP cloud memory index, not as Orbit's source of truth.

Rationale:

- Startup credits make Atlas practical now.
- Atlas is good for flexible JSON-like memory records, metadata filtering, and later vector search.
- Orbit's constitution requires the device corpus to remain authoritative.
- A compact index gives the user visible value quickly without building the full Orbit Cloud/KG stack.

Rejected:

- Direct Android-to-Atlas connection strings: rejected because credentials would live on device.
- Atlas Device Sync/Realm Sync/Data API/App Services: rejected because they are not the desired backend path and are deprecated/EOL for the relevant app-service use cases.
- Replacing Room: rejected because it violates local-first supremacy.

## 2. Backend Boundary

Decision: Add `supabase/functions/memory_gateway` as a sibling to `llm_gateway`.

Rationale:

- Reuses the deployed gateway pattern: JWT auth, Zod validation, typed responses, Vitest coverage.
- Keeps Atlas credentials in backend env.
- Keeps Android networking inside `:net`.
- Separates memory operations from LLM capability calls, which keeps audit semantics clearer.

Open implementation choice:

- Deploy as Vercel function or Supabase Edge Function style matching existing operational setup. The codebase currently names the directory `supabase/functions`, but `llm_gateway` uses Vercel edge conventions. Match the existing deployment path first for speed.

## 3. Auth and User Isolation

Decision: Use Supabase JWT subject as `userId` for Atlas documents and queries.

Required safeguards:

- Every write stamps `userId` from verified JWT, never from client payload.
- Every search/ask/tombstone filter includes authenticated `userId`.
- Tests must prove user B cannot read, update, or tombstone user A records.

## 4. Compact Payload Policy

Decision: Store capped, derived fields only.

Allowed MVP fields:

- Local envelope id and provenance.
- Day/time metadata.
- Intent/category.
- Source app label/category.
- Canonical URL/domain when already known.
- Title/summary/excerpts under caps.
- Tags and compact evidence descriptors.
- Local content hash/digest.
- Tombstone markers.

Banned:

- Raw screenshots or image bytes.
- Full OCR bodies.
- Full clipboard bodies.
- Raw HTML/readability bodies.
- Prompts.
- Model responses.
- Local audit log rows.
- Credentials/tokens.

## 5. Search Strategy

Decision: Start metadata/lexical first, then add vector search once embedding field policy is locked.

Rationale:

- Monday MVP needs a reliable Library more than perfect semantic ranking.
- Atlas collection and indexes are already verified.
- The embedding provider/dimensions must be decided before creating a vector index.

MVP ranking:

1. User-scoped filter.
2. Text match over title, summary, tags, evidence excerpts, domain, source labels.
3. Recency and intent/source filters.
4. Return citation metadata.

Vector follow-up:

- Decide embedding provider and dimensions.
- Store embeddings only for the compact text representation, never raw artifact bodies.
- Add Atlas Vector Search index over `embedding`.

## 6. Ask Strategy

Decision: Retrieval-grounded Ask only, and only after Library retrieval works.

Flow:

1. Search memory records for the user query.
2. If enough evidence exists, assemble an answer from returned compact snippets.
3. Return citations for every claim.
4. If not enough evidence exists, return a refusal/fallback with cited nearest matches.

MVP answer generation options:

- Deterministic extractive answer first if enough snippets directly match.
- Cloud LLM answer through existing `llm_gateway` only after compact retrieval records are selected and audited.

Stop signs:

- The backend must not assemble prompts from raw server-held user bodies. It can only use compact index records that passed the device-side payload policy.
- Ask must not consume the implementation budget before Library search and source-opening work.

## 7. Audit Strategy

Decision: Local audit rows are required for memory upsert, tombstone, search, and Ask. Backend operator logs must be bounded and must not include raw queries or memory text.

Local audit details:

- Provider: `mongodb_atlas`.
- Endpoint/capability.
- Envelope id when relevant.
- Payload digest or query digest.
- Result count.
- Outcome/error kind.

The audit log itself remains local-only.

## 8. Atlas Setup Already Verified

Verified local development state:

- `MONGODB_ATLAS_URI` is present in `supabase/functions/memory_gateway/.env.local`.
- `MONGODB_DB=orbit_dev`.
- `MONGODB_MEMORY_COLLECTION=memory_items`.
- `orbit_dev.memory_items` exists.
- Indexes exist: `_id_`, `uniq_user_envelope`, `user_day_created`, `user_intent_created`, `user_tombstone`.

Vector index is not created yet.

## 9. MVP Scope Guard

To reach a real MVP by Monday, do not include:

- Full KG collections.
- A2UI rendering.
- Full chat-first Orbit workspace before Library is useful.
- BYOM local model manager.
- Multi-device sync conflict resolution.
- Cloud object storage for screenshots.
- Autonomous action execution.

These remain later specs that build on cited retrieval.
