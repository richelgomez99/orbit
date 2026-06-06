# Research: Approval Action Runtime

## Decision 1: Treat Spec 006 As Reconciliation And Productization

**Decision**: Build on the existing Spec 003 action runtime instead of replacing it.

**Rationale**: The code already has Room tables, AIDL, `ActionExecutorService`, handlers, proposal chips, preview sheet, audit aggregation, and WorkManager extraction. The current product gap is discoverability, typed approval UX, fixture reliability, and contract consistency.

**Rejected**: Rebuild a new agent/action stack. This would duplicate working process-boundary code and risk breaking local-first invariants.

## Decision 2: Orbit Tab Owns The Action Draft Workspace

**Decision**: Add a pending Action Drafts section/projection to Orbit while preserving Diary inline chips as local context.

**Rationale**: The current vision separates Diary as memory from Orbit as agent/action. Action drafts are loop-closing work and belong in Orbit. Diary chips are still useful when reviewing a capture.

**Rejected**: Keep all proposals inline in Diary. That makes loop-closing work hard to find and turns the daybook into a hidden task queue.

## Decision 3: No New Room Tables Unless Proven Necessary

**Decision**: Represent `ActionDraft` as a projection of existing `action_proposal`, `appfunction_skill`, and source envelope fields.

**Rationale**: Existing schema already records the durable facts. A projection avoids migration risk and keeps Spec 006 scoped to runtime and UX.

**Rejected**: Add `action_draft` table. It would duplicate proposal state and create synchronization bugs.

## Decision 4: Fix `tasks.createTodo` Contract Before UI Work

**Decision**: Align `BuiltInAppFunctionSchemas.TASKS_CREATE_TODO`, extraction fixtures, `TodoActionHandler`, and preview UI around a list-shaped `items` contract. The model-facing schema may include `parentEnvelopeId`, but `proposalId` must be injected by the approval/runtime layer after the proposal row exists.

**Rationale**: Current schema requires `title`, but the handler requires `items`. Also, `proposalId` cannot be supplied by the LLM because it is generated after extraction persistence. Approval runtime cannot be trusted until model-facing and executor-facing contracts are coherent.

**Rejected**: Patch only the UI. That would leave model output and handler expectations inconsistent.

## Decision 5: Calendar Uses System Intent, Not Provider API

**Decision**: Continue using `Intent.ACTION_INSERT` with `CalendarContract.Events.CONTENT_URI`.

**Rationale**: It keeps the final write in the user's Calendar UI, avoids account OAuth, and does not require `READ_CALENDAR` or `WRITE_CALENDAR`.

**Rejected**: Direct Calendar provider writes. They are higher risk, permission-heavy, and violate the "no silent writes" rule.

## Decision 5A: Calendar Has No Orbit Undo Promise

**Decision**: Do not show an "Undo" affordance for `calendar.createEvent`. The UI should say Orbit opened Calendar and that final save/edit/cancel happens in Calendar.

**Rationale**: Orbit cannot retract a Calendar event after the user saves it in the system Calendar app because Orbit does not use Calendar provider APIs and does not hold calendar permissions. A fake undo would damage trust.

**Rejected**: Keep the existing generic 5-second undo toast for all action outcomes. It is acceptable for local reversible writes, but misleading for external managed side effects.

## Decision 6: Deterministic Debug Proposals For Device Validation

**Decision**: Extend debug seeding so demo captures can receive deterministic local `ActionProposal` rows.

**Rationale**: Production extraction can stay charger/unmetered and model-dependent. MVP validation should not depend on WorkManager timing or LLM availability.

**Rejected**: Relax production `ACTION_EXTRACT` constraints for demos. That would weaken Principle IV and make demo behavior less representative.

## Decision 6A: Debug Seeding Uses A Debug-Only Repository Surface

**Decision**: Add any deterministic proposal seeding behind debug source sets or debug-only AIDL methods guarded from release builds.

**Rationale**: Production `IEnvelopeRepository` should not gain a broad "insert arbitrary proposal" surface unless the product uses it. Debug validation needs deterministic proposals, but production proposal creation should remain extraction-owned.

**Rejected**: Create proposals by writing directly to Room from `:ui` or `:capture`. That would violate process ownership and hide the path from tests.

## Decision 8: Failure Visibility Requires A Draft Outcome Projection

**Decision**: Pending Drafts are not enough. The Orbit workspace should also be able to show recent failed/invalidated action outcomes or otherwise surface failure state after confirm.

**Rationale**: Existing `markProposalConfirmed` hides the proposal before `execute` can fail. If failures are only in audit logs, the user sees the draft disappear without understanding what happened.

**Rejected**: Leave failures only in `ActionsAuditAggregator`. Audit is necessary for provenance, but not sufficient for user trust.

## Decision 7: Network Boundary Remains Unchanged

**Decision**: Any model extraction continues through `LlmProvider`; local mode stays in `:ml`, cloud mode routes via `:net`; execution stays in `:capture`.

**Rationale**: Spec 006 is the first external side-effect branch after semantic retrieval. It must preserve the process split:

- `:ml`: proposal rows, source envelope reads, audit rows, local todo envelope creation.
- `:ui`: approval surfaces only.
- `:capture`: external `Intent` dispatch only.
- `:net`: cloud inference only.

**Rejected**: Direct network or database access from `:ui`/`:capture`.
