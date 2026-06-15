# Research: Curious Agent Profiling

## Decision 1: Question Candidates Before Facts

**Decision**: The first Curious Agent layer should produce question candidates, not profile facts.

**Rationale**: The product thesis says Orbit should ask before assuming identity. Candidate questions create a safe feedback loop while preserving user control.

## Decision 2: Deterministic Local Generator First

**Decision**: Use deterministic local signals for the first branch.

**Rationale**: Model-generated questions are useful later, but this branch can prove the trust boundary with local evidence thresholds, caps, and refusal behavior.

## Decision 3: Orbit Tab Is The First Surface

**Decision**: Surface curious questions in Orbit, below higher-priority action/agent work.

**Rationale**: Diary remains memory and Library remains retrieval. Profile questions are agentic feedback, so they belong in Orbit.

## Decision 4: Reuse Existing Memory/KG Provenance

**Decision**: Source evidence should come from promoted memories, graph facts, or envelopes. Do not create a parallel provenance model.

**Rationale**: Spec 007 and Spec 009 exist specifically to keep user memory inspectable and source-backed.
