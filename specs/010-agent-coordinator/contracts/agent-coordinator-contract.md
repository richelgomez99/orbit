# Contract: Agent Coordinator

## Boundary

The coordinator runs in `:ml` or a repository layer reached through `:ml`. UI/default process callers must use Binder/repository abstractions and cannot read Room directly.

## Proposed Binder Shape

Either extend `IEnvelopeRepository` or add a narrow graph/agent Binder surface:

```aidl
AgentPlanParcel planAgentRequest(
    String requestId,
    String query,
    in String[] attachedEnvelopeIds,
    int maxEvidence,
    boolean allowModelAssist
);
```

## Parcel Rules

- `AgentPlanParcel` max steps: 5.
- `AgentPlanParcel` max evidence refs: 10.
- `AgentPlanParcel` max questions: 3.
- Strings are capped before crossing Binder.
- Evidence refs carry ids and labels only.
- No raw screenshots, full OCR, prompts, embeddings, model responses, cookies, JWTs, API keys, or raw provider payloads.

## Coordinator Algorithm V1

1. Normalize/cap request text.
2. Gather explicit attached envelopes if provided.
3. Retrieve local/cited evidence from existing Library/Ask search path.
4. Ask KG `whyThis` for graph-backed evidence when relevant.
5. Inspect existing action drafts/proposals and AppFunction schemas.
6. If evidence is weak, return `REFUSE` or `ASK_USER`.
7. If evidence is ambiguous, return `ASK_USER` with cited choices.
8. If an action is possible, return `PLAN` with `DRAFT_ACTION` step requiring approval.
9. If model assistance is enabled, let the model improve step wording/selection only after compact evidence/function schemas are assembled.
10. Validate model output against local evidence ids and registered functions before returning.

## Trust Rules

- The model cannot create new source facts.
- The model cannot create executable actions.
- Unknown function ids are rejected.
- Uncited claims are dropped or force refusal.
- Sensitive identifier questions require exact saved evidence.
- External writes require existing Spec 006 confirmation paths.

## Audit/Trace Rules

Trace records, if implemented, store only:

- request id
- query digest
- outcome
- counts
- selected source ids
- model label
- latency
- refusal/gap reason

They do not store request raw text, prompts, model responses, raw source text, or provider payloads.
