# Research: Memory Candidates Inspector

## Decision 1: Build The Consent Layer Before The KG

**Decision**: Spec 007 adds candidate/promoted memory sidecars and an inspector UI, but does not add graph nodes/edges or a KG backend.

**Rationale**: Architecture Round 3 defines memory as a ladder: capture memory, retrieval memory, feedback memory, candidate memory, promoted memory, then graph memory. Jumping directly to KG would let Orbit infer profile facts before users can inspect or correct them.

**Alternatives rejected**:

- **Implement KG first**: premature; deletion/provenance and user consent are not ready.
- **Use only retrieval feedback**: insufficient; the user needs explicit accept/reject/correct controls for profile-like claims.

## Decision 2: Use Additive Room Sidecars

**Decision**: Add `memory_candidate` and `promoted_memory` Room tables in the `:ml` process.

**Rationale**: Existing architecture stores durable user data in encrypted Room/SQLCipher. Sidecars match the pattern used by capture understanding, evidence bundles, active intents, action proposals, and audit rows.

**Consequences**:

- Requires Room v9 migration and exported schema update.
- Requires invalidation/delete behavior.
- Keeps Android local-first and avoids cloud-as-authority.

## Decision 3: Surface Candidates In Orbit, Not Diary

**Decision**: Add a Memory Review section to the Orbit tab.

**Rationale**: Diary is the chronological daybook and should not become a pressure queue. Orbit owns agent/action/review workflows. Candidate memory review is agent trust work.

**Alternative rejected**:

- **Diary inline prompts**: too noisy and violates "Diary is pure memory".

## Decision 4: Promote Only On Explicit User Decision In This Spec

**Decision**: Spec 007 promotion happens only when the user accepts a candidate or accepts edited candidate text.

**Rationale**: Repeated-behavior auto-promotion is real product direction, but it requires resolution semantics, richer feedback, and sensitivity policy. This branch should prove the trust contract first.

## Decision 5: Keep Binder DTOs Compact

**Decision**: Binder exposes compact `MemoryCandidateParcel` and `PromotedMemoryParcel` projections with ids, labels, source counts, sensitivity, state, and limited source summaries.

**Rationale**: Binder payloads must not carry raw screenshots, raw OCR, full evidence, prompts, model responses, or embeddings. Existing architecture requires ids and compact summaries across process boundaries.

## Decision 6: Exclude Pending/Sensitive Candidates From Cloud Payloads

**Decision**: Compact cloud memory sync must not include pending/rejected candidates or local-only/sensitive candidate details.

**Rationale**: Atlas is a compact index, not the source of truth. Candidate profile facts are not facts, and local-only/sensitive candidates must not cross the network.

## Decision 7: Debug Seed Candidates For MVP Verification

**Decision**: Add debug-only candidate seeding tied to deterministic demo captures.

**Rationale**: Real candidate generation will improve with LLM/model wiring. The MVP branch can still validate the user-facing consent and provenance surface without depending on model quality or WorkManager timing.

