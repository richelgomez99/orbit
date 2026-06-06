# Contract: Action Approval Runtime

## Process Ownership

- `:ml` owns action proposal, action execution, app function registry, skill usage, source envelope reads, and audit writes.
- `:ui` renders action draft lists and approval sheets.
- `:capture` owns external Android `Intent` dispatch through `IActionExecutor`.
- `:net` is used only when `LlmProvider` routes cloud extraction. No execution path uses network.

## Binder Surfaces

Existing:

- `IEnvelopeRepository.observeProposalsForEnvelope(envelopeId, observer)`
- `IEnvelopeRepository.markProposalConfirmed(proposalId)`
- `IEnvelopeRepository.markProposalDismissed(proposalId)`
- `IEnvelopeRepository.recordActionInvocation(...)`
- `IEnvelopeRepository.createDerivedTodoEnvelope(parentEnvelopeId, itemsJson, proposalId)`
- `IActionExecutor.execute(ActionExecuteRequestParcel)`
- `IActionExecutor.cancelWithinUndoWindow(executionId)`

Likely new or extended for Spec 006:

- `IEnvelopeRepository.observePendingActionDrafts(observer, limit)` or equivalent paged/list API.
- `ActionDraftParcel` containing proposal + source envelope + skill summary fields required by the Orbit workspace.

## Approval Sheet Rules

- Approval sheets must show human-readable typed fields.
- Approval sheets must show source/evidence and side-effect disclosure.
- Approval sheets must allow Dismiss without executing.
- Confirm must be disabled or produce a visible validation error when required fields are missing.
- Raw `argsJson` must not be displayed in normal UI.

## Execution Rules

1. User confirms in `:ui`.
2. `:ui` calls `markProposalConfirmed` in `:ml`.
3. `:ui` calls `IActionExecutor.execute` in `:capture` with edited args.
4. `:capture` revalidates function id and schema version through `:ml`.
5. `:capture` validates args shape.
6. Handler dispatches external intent or calls `:ml` binder for local write.
7. `:capture` calls `recordActionInvocation` in `:ml`.
8. `:ml` writes execution, audit, and skill usage rows in one transaction.

## Calendar Contract

- Function id: `calendar.createEvent`
- Side effect: external system Calendar insert UI.
- Required before dispatch: `title`, `startEpochMillis`.
- Optional: `endEpochMillis`, `location`, `notes`, `tzId`.
- No calendar read/write permissions.

## Todo/List Contract

- Function id: `tasks.createTodo`
- Default side effect: local DB write through `:ml`.
- Required before local dispatch: `parentEnvelopeId`, `proposalId`, non-empty `items`.
- Model-generated args are not expected to know `proposalId`; the approval/runtime layer injects it before execution.
- The preview sheet may let the user edit item text and due dates.
- Local approval creates one derived list envelope, not one envelope per item.
- The derived list envelope must store all approved checklist items in `todoMetaJson.items[]`.
- The derived list envelope must link back to source proposal/source envelope through existing provenance fields and audit extras.

## Undo/Reversibility Contract

- External managed side effects, including `calendar.createEvent`, must not show an Orbit "Undo" promise.
- Calendar success copy should say the Calendar insert screen was opened and final save/cancel happens in Calendar.
- Local todo/list writes may show undo only if the implementation can actually reverse or cancel the local write within the promised window.
- If true reversal is not implemented for a local action, show outcome copy without an undo button.

## Failure Contract

- Missing `:ml` binder: no external intent; return `FAILED/ml_binder_unavailable`.
- Unknown handler: no external intent; return `FAILED/unknown_skill`.
- Schema version mismatch: no external intent; return `FAILED/schema_invalidated` and invalidate proposal.
- Bad args JSON: no external intent; return `FAILED/schema_mismatch` and invalidate proposal.
- No target app: return `FAILED/no_handler`.
- Security exception: return `FAILED/security_exception`.
- Failure must be visible in the action workspace or equivalent UI, not only in the audit log.
