# Orbit Execution Playbook - 2026-06-02

This playbook exists so future sessions survive context compaction and still execute the Orbit roadmap in the right order.

## Required Pre-Read

Before implementing any branch, read these in order:

1. `AGENTS.md`
2. `VISION-2026-05-22.md`
3. `docs/mvp-to-vision-execution-plan-2026-06-02.md`
4. `docs/orbit-roadmap-queue-2026-06-02.md`
5. `CODEX_HANDOFF.md`
6. The active spec folder in Spec Kit order: `spec.md`, `plan.md`, `data-model.md`, `contracts/`, `tasks.md`, `quickstart.md`

## Source-Of-Truth Rules

- Current code and tests are truth for shipped behavior.
- The active spec is truth for the current branch.
- The roadmap queue is truth for which branch comes next.
- The vision docs are strategic direction, not proof that functionality is shipped.
- If code, spec, and vision conflict, update or create the relevant spec before coding.
- Do not describe AICore/Gemini Nano as final architecture; it is current/legacy until Spec 022 lands.

## Branch Entry Gate

Every new branch starts by writing down:

- Current branch and spec number.
- What shipped in the prior branch.
- What this branch is allowed to change.
- What is explicitly out of scope.
- One manual demo path.
- Automated gates required before handoff.

Do not start an implementation branch if the prior branch still has an unverified MVP-critical task unless the user explicitly defers it.

## Spec Kit Discipline

- Run the full Spec Kit loop only for the active branch: specify -> plan -> tasks -> implementation.
- Do not pre-generate detailed plans/tasks for future branches.
- Every task must map to a user story or validation gate and name the relevant code/doc surface.
- Do not rely on checkboxes alone. Reconcile tasks against code, tests, screenshots, and current branch reality.

## Surface-First, Model-Enablement Pattern

Every feature spec should be planned in two lanes:

- Product surface lane: deterministic or user-driven UX, data model, provenance, audit, approval, and fallback behavior. This lane must be useful without pretending to be intelligent.
- Model enablement lane: embeddings, LLMs, vector retrieval, local model output, or agent planning. This lane writes into the same data structures and must degrade cleanly.

If a product surface cannot be genuinely useful without semantic/model behavior, add a narrow model-enablement follow-up before moving to the next product branch. Do not mark deterministic scaffolding as real AI.

Current application of this rule: Spec 005 built the Library/Ask surfaces and compact index foundation. Spec 005A must add semantic retrieval and grounded Ask before Spec 006 action runtime.

## Validation Before Features

Each branch needs:

- Focused unit tests for new logic.
- UI/instrumentation tests when navigation or Compose behavior changes.
- Full Android gates before final APK handoff when app behavior changes.
- Backend gates when Supabase/Vercel/Atlas code changes.
- One scripted S24/manual demo path when the feature is user-facing.

Do not ship a new capability if the previous layer is unstable in manual testing.

## Handoff Requirements

Before ending a branch or long session, update:

- `CODEX_HANDOFF.md` with current branch, APK/build status, test results, known limits, and exact next action.
- The active `quickstart.md` with real validation results.
- The active `tasks.md` with reconciled status.
- `docs/orbit-roadmap-queue-2026-06-02.md` only if the actual next branch changes.

## Product Guardrails

- Orbit is local-first: Room/SQLCipher on device remains the source of truth.
- Atlas is a compact cloud index/search accelerator, not the canonical memory graph.
- Android never holds Atlas credentials or connects directly to Atlas.
- Network egress stays behind `:net`; user corpus reads stay behind `:ml`.
- Agent actions must be proposed, cited, previewed, and user-approved.
- Diary remains pure memory; Follow-ups and actions belong in Orbit.
- AppFunctions/Spark/platform interop is deferred until approval exists.

## Current Immediate Step

Finish Spec 005 validation honestly, then run Spec Kit for Spec 005A:

1. Record Spec 005 as compact-index + Library + limited local cited Ask preview.
2. Do not continue polishing token-only Ask beyond false-positive guardrails.
3. Run `/speckit.specify`, `/speckit.plan`, and `/speckit.tasks` for `005A-semantic-retrieval-grounded-ask`.
4. 005A must lock embedding provider/model/dimensions, Atlas Vector Search policy, hybrid retrieval, cited LLM answer/refusal behavior, and local fallback.

Only after 005A is validated should Spec 006 start.
