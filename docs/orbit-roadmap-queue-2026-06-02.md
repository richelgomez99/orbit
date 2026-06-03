# Orbit Roadmap Queue - 2026-06-02

This is the canonical branch queue for getting from the current Spec 005 MVP to the full Orbit vision. Future sessions should read this before deciding what to build next.

## Queue Rules

- The next branch is the first incomplete row in this file unless the user explicitly changes the queue.
- Do not skip validation work to add a flashier feature.
- Do not pre-generate full Spec Kit artifacts for future rows. Generate full artifacts only for the active branch.
- Every branch must update this queue only if the actual next branch changes.
- AppFunctions/Spark/platform-agent interop is intentionally deferred until Orbit has access/approval and the core agent is real.

## Current North Star

Orbit captures what the user saved plus the intention behind why they saved it. That paired artifact and intent becomes private, local-first context for a personal knowledge graph and an agent that helps close loops with user-approved actions.

## Canonical Queue

| Order | Spec / branch | Objective | Why now | Exit criteria |
| ---: | --- | --- | --- | --- |
| 0 | `005-retrieval-and-ask-citations` / `feature/005-atlas-memory-index-search-20260530` | Close the MVP retrieval branch. | Current MVP must be validated before more features. | Library/search/index foundation complete; Ask documented as limited local cited retrieval preview, not semantic AI. |
| 1 | `005A-semantic-retrieval-grounded-ask` / `feature/005a-semantic-retrieval-grounded-ask-20260603` | Add embeddings, Atlas Vector Search, hybrid retrieval, and grounded Ask refusal thresholds. | Spec 005 exposed that token-only Ask is not reliable enough; semantic retrieval must land before broader agent/action work. | Compact embeddings indexed; vector + lexical search user-scoped; Ask answers only from cited evidence and refuses unsupported questions; local fallback remains. |
| 2 | `006-approval-action-runtime` / `feature/006-approval-action-runtime-...` | Add user-approved action drafts. | "Close loops" needs concrete drafts after retrieval can find the right memory. | Calendar/todo/message/list drafts preview correctly, require confirmation, and write audit rows. |
| 3 | `007-memory-candidates-inspector` / `feature/007-memory-candidates-inspector-...` | Let users inspect/correct candidate facts, entities, and patterns. | The private picture of the user must be editable and provenance-backed. | Candidate memories show evidence, can be accepted/rejected/corrected, and never become profile facts silently. |
| 4 | `008-cloud-controls-storage-budgeting` / `feature/008-cloud-controls-storage-budgeting-...` | Make cloud/index/LLM use visible, reversible, and budgeted. | Deeper agent/cloud use needs user-sovereign controls. | User can inspect cloud state, disable indexing/Ask, and see bounded audit records. |
| 5 | `009-kg-backend-poc` / `feature/009-kg-backend-poc-...` | Build the local-first KG foundation behind adapters. | Agent memory needs entities/relationships with provenance. | Entity/fact/edge model exists locally, citations link to captures/user confirmations, Atlas only mirrors compact index data. |
| 6 | `010-agent-coordinator` / `feature/010-agent-coordinator-...` | Move from cited Ask to approval-first agent planning. | The agent can plan only after actions, memory candidates, cloud controls, and KG foundations exist. | Agent proposes cited plans, asks gap-filling questions, and never silently writes externally. |
| 7 | `011-manual-compose-capture-context` / `feature/011-manual-compose-capture-context-...` | Add deliberate/manual capture and near-term Clarify/context capture. | This preserves numeric progression while pulling high-signal context into the mainline. | User can add/edit intent/context at capture/manual compose time; Diary, Library, Ask, sync, and titles use it. |
| 8 | `012-resolution-semantics` / `feature/012-resolution-semantics-...` | Model duplicate, conflict, stale, done, dismissed, snoozed, and not-now states. | Follow-ups and agent loops need durable resolution meaning. | Recaptures update duplicate metadata; dismissal/not-now changes future surfacing; every resolution has provenance. |
| 9 | `018-capture-context-affordance` / `feature/018-capture-context-affordance-...` | Vision-slot follow-up for any capture-context UX not completed in Spec 011. | Keep the May 22 roadmap slot without forcing a premature 005 to 018 jump. | Either no-op/closed as absorbed by Spec 011, or implements remaining overlay affordance polish. |
| 10 | `019-agent-workspace-ia` / `feature/019-agent-workspace-ia-...` | Formalize the durable three-pillar IA. | MVP shell should become a stable product architecture after core loop semantics exist. | Diary is pure memory; Library is retrieval; Orbit is Ask/actions/workbench; navigation state is intentional. |
| 11 | `020-curious-agent-profiling` / `feature/020-curious-agent-profiling-...` | Ask sparse, high-leverage profile questions. | Only after KG and agent coordinator can make questions useful and grounded. | Questions require evidence, answers become editable profile facts, dismissals reduce future noise. |
| 12 | `021-generative-ui-runtime` / `feature/021-generative-ui-runtime-...` | Render agent outputs as safe native Orbit UI. | Structured UI needs trustworthy agent outputs first. | Action cards, pickers, lists, and memory views render natively and degrade safely to text. |
| 13 | `022-local-model-manager` / `feature/022-local-model-manager-...` | Add BYOM/local model manager as the strategic local AI path. | Hardware-adaptive local-first AI comes after feature scope and memory pressure are understood. | Speed/intelligence/cloud tiers are explicit; local mode supports core capabilities; memory pressure is profiled. |

## Current Status Update - 2026-06-03

- Queue row 1 (`005A-semantic-retrieval-grounded-ask`) is implemented and locally validated on branch `feature/005a-semantic-retrieval-grounded-ask-20260603`.
- Verified gates: backend typecheck, backend unit tests (39), retrieval eval (6/6), production memory-gateway deploy, live semantic smoke, Android focused tests, compile, lint, assemble, Android secret scan, and Pixel 10 Pro Android 17 emulator connected smoke.
- Production memory gateway alias: `https://orbit-memory-gateway.vercel.app`; latest deployment `https://orbit-memory-gateway-f7nk21ubl-richels-projects-834ef114.vercel.app`.
- Current APK for phone validation: `dist/orbit-mvp-debug-20260603-005b.apk` (`7a80f2dc94b00766177eec6f80393bf4289c39c8252cd6f3a44d925c1369c885`).
- S24 validation confirms the row 1 Library/Orbit actions by user report and uploaded grounded-Ask screenshots. The post-fix S24 source-label recheck also passed: no more `from IntentResolver`.
- Row 2 (`006-approval-action-runtime`) is now the next branch.

## Deferred Until Approval Or Explicit Re-Scope

- Platform-agent interop through AppFunctions/Spark/Gemini system agents.
- Any external-agent bridge that exposes Orbit context outside the app.
- Any claim that platform agents can consume Orbit memory.

When this becomes available, create a fresh spec after approval, likely `023-platform-agent-interop`, and keep it downstream of KG, agent coordinator, and action approvals.
