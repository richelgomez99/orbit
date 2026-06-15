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

## Current Status Update - 2026-06-12

- Queue row 1 (`005A-semantic-retrieval-grounded-ask`) is implemented and locally/S24 validated on branch `feature/005a-semantic-retrieval-grounded-ask-20260603`.
- Verified row 1 gates: backend typecheck, backend unit tests, retrieval eval, production memory-gateway deploy, live semantic smoke, Android focused tests, compile, lint, assemble, Android secret scan, and connected-device smoke.
- Production memory gateway alias: `https://orbit-memory-gateway.vercel.app`; latest known deployment `https://orbit-memory-gateway-f7nk21ubl-richels-projects-834ef114.vercel.app`.
- Queue row 2 (`006-approval-action-runtime`) is committed at `ba2dcf7` on branch `feature/006-approval-action-runtime-20260603`.
- Spec 006 implementation is locally complete: Orbit Action Drafts, typed Calendar approval, typed local list approval, grouped list envelope projection, visible outcome/failure state, audit/skill usage paths, no Calendar undo promise, permission regression, debug seeding, APK build, and Android local gates have passed. User confirmed on S24 that the shopping-list approval now creates one envelope with multiple checklist items.
- Current Spec 006 APK for final phone validation: `dist/orbit-mvp-debug-20260603-006.apk` (`05cb7026b5738b7c55f3cba7a0d168a4dabcb182270b2f18ad05863d7fad71c5`).
- Remaining Spec 006 phone-only closeout: one concise demo pass on the latest APK for Action Drafts visible, Calendar approval opens Calendar insert, dismiss works, failure copy is user-friendly, and no misleading Calendar undo appears.
- Older phone data may still contain pre-fix individual ingredient rows from earlier APKs; that is historical local data, not the current list approval behavior.
- Queue row 3 (`007-memory-candidates-inspector`) is repo-side complete on branch `feature/007-memory-candidates-inspector-20260605`.
- Spec 007 has Room v9 candidate/promoted memory sidecars, support/provenance junction tables, compact Binder projections, local accept/reject/edit decision methods with audit rows, debug seeding, ViewModel decision-flow coverage, source-ready Compose UI tests, expanded source-ready DAO/repository tests, explicit Ask/action/cloud exclusion tests, and an Orbit Memory Review UI. Local non-phone gate passed again on 2026-06-12; connected migration/repository/UI execution and manual phone UI validation are deferred until a device/emulator is available.
- Queue row 4 (`008-cloud-controls-storage-budgeting`) is repo-side complete on branch `feature/008-cloud-controls-storage-budgeting-20260612`.
- Spec 008 artifacts were regenerated on 2026-06-12. The implementation adds durable preferences, policy/receipt primitives, compact index receipt metadata, a cloud Ask synthesis gate, a cloud AI routing gate, three distinct Settings controls, and a Cloud activity audit entry point. Full non-phone gate passed on 2026-06-12. Connected/manual validation is deferred until a phone/emulator is available.
- Queue row 5 (`009-kg-backend-poc`) is repo-side complete on branch `feature/009-kg-backend-poc-20260612`.
- Spec 009 artifacts were regenerated on 2026-06-12. Implementation adds a local Room v10 KG schema, `GraphBackendAdapter`, `RoomGraphBackendAdapter`, provenance-required fact/relationship writes, source invalidation with surviving-support preservation, source-ready migration/adapter tests, deterministic promoted-memory-to-graph-fact projection, a compact Binder `getGraphWhyThis` projection, and adapter evaluation verdicts. External graph products remain adapter candidates only. Connected migration/adapter/Binder test execution is deferred until phone/emulator availability.
- Queue row 6 (`010-agent-coordinator`) is repo-side complete on branch `feature/010-agent-coordinator-20260612`.
- Spec 010 artifacts were regenerated on 2026-06-12. The branch adds deterministic cited agent planning, a minimal Orbit tab planning panel, and advisory model-assisted display copy behind existing `LlmProviderRouter`/Spec 008 controls. The deterministic plan remains authoritative; model output cannot add uncited steps, unknown evidence, or changed function ids. Full non-phone gate passed on 2026-06-13; connected/manual phone validation is deferred until device availability. AppFunctions/Spark interop, A2UI, durable chat sessions, and BYOM local model manager remain deferred.
- Queue row 7 (`011-manual-compose-capture-context`) is repo-side complete on branch `feature/011-manual-compose-capture-context-20260613`.
- Spec 011 artifacts were regenerated on 2026-06-13. The branch should first ship post-capture `Add context` into the existing note path, then manual text compose through the existing seal/Binder path. It must not create a parallel notes/context model unless the current `envelope_note` contract proves insufficient.
- Spec 011 repo-side progress: post-capture `Add context`, manual compose repository seam, Diary manual compose dialog, Library `Context` citation mapping, and downstream note-context verification are implemented. Full non-phone gate passed on 2026-06-13. Physical phone validation is still pending.
- Queue row 8 (`012-resolution-semantics`) is active on branch `feature/012-resolution-semantics-20260613`.
- Spec 012 artifacts were regenerated on 2026-06-13. Repo-side progress now includes Room v11 local `resolution_receipt` storage, compact receipt validation, duplicate recapture receipts, Basic-understanding duplicate receipts, action dismiss/invalidation receipts, derived-list done/reopened receipts, Active Intent resolution receipts, verdict-based Follow-up filtering, and receipt-only Not now/Tomorrow snooze controls. Focused and full non-phone gates passed on 2026-06-14; connected/manual validation remains deferred until device availability.
- Queue row 9 (`018-capture-context-affordance`) is active on branch `feature/018-capture-context-affordance-20260614`.
- Spec 018 artifacts were regenerated on 2026-06-14 after Spec 012 repo-side completion. The branch now adds a focused post-save context surface for new and duplicate captures, while preserving Spec 011 `EnvelopeNote` storage, existing Binder note writes, duplicate targeting, full-detail fallback, and local-first/no-model behavior. Focused and full non-phone gates passed on 2026-06-14. True pre-seal transparent Clarify remains deferred unless implementation research proves it can be added without destabilizing overlay capture.
- Queue row 10 (`019-agent-workspace-ia`) is active on branch `feature/019-agent-workspace-ia-20260614`.
- Spec 019 artifacts were regenerated on 2026-06-14. Current app already has Diary/Library/Orbit tabs; this branch hardens route boundaries rather than replacing navigation. Implementation adds Library transient state reset on tab exit, preserves existing Ask reset on Orbit exit, and adds tests proving the tab-exit reset contract. Focused and full non-phone gates passed on 2026-06-14.
- Queue row 11 (`020-curious-agent-profiling`) is active on branch `feature/020-curious-agent-profiling-20260614`.
- Spec 020 artifacts were regenerated on 2026-06-14. Scope is conservative: evidence-backed curious question candidates before profile facts, deterministic local generator first, no cloud/model persona inference, no silent profile promotion, and no proactive notifications.

## Deferred Until Approval Or Explicit Re-Scope

- Platform-agent interop through AppFunctions/Spark/Gemini system agents.
- Any external-agent bridge that exposes Orbit context outside the app.
- Any claim that platform agents can consume Orbit memory.

When this becomes available, create a fresh spec after approval, likely `023-platform-agent-interop`, and keep it downstream of KG, agent coordinator, and action approvals.
