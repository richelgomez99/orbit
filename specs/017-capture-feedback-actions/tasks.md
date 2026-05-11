# Tasks: Capture Feedback Actions

**Input**: [spec.md](spec.md), [plan.md](plan.md), [data-model.md](data-model.md)
**Branch**: `017-capture-feedback-actions` (off fresh `origin/main`)
**Workflow**: Spec Kit sequence is specify -> plan -> tasks -> implement. This
task list is a planning draft and should still be reviewed by `speckit.tasks`
before implementation.

## Format: `[ID] [P?] [Phase] Description`

## Phase 0 - Branch hygiene and planning

- [ ] **T017-001** Create/switch to branch `017-capture-feedback-actions` with
  the Spec Kit git extension once the current dirty worktree has been split or
  stashed. Do not develop this on `015-phase1-cluster-surface`.
- [ ] **T017-002** Run `speckit.tasks`/review after this draft plan to confirm
  task order, contract artifacts, and migration details before implementation.
- [ ] **T017-003** Confirm dependency state: spec 016 merged or explicitly
  rebased if intent picker/reclassify surfaces are touched.

## Phase 1 - Duplicate keys and repository contract

- [ ] **T017-101** Add envelope-level URL duplicate key support. Preferred shape:
  `primaryCanonicalUrlHash` on `IntentEnvelopeEntity` with Room migration and an
  index for non-deleted/non-archived lookup.
- [ ] **T017-102** Populate `textContentSha256` for exact non-URL text captures.
- [ ] **T017-103** Add DAO queries for duplicate URL and exact-text matches,
  excluding deleted envelopes and treating archived behavior per spec decision.
- [ ] **T017-104** Replace `seal(): String` boundary with a typed seal result
  across repository, AIDL/Binder, orchestrator, and ViewModel layers.
- [ ] **T017-105** Add audit action/event for duplicate capture attempts.

## Phase 2 - Overlay feedback actions

- [ ] **T017-201** Add `SealOutcome.AlreadySaved(existingEnvelopeId, matchedBy)`
  and `PostCaptureUi.AlreadySaved`.
- [ ] **T017-202** Implement compact `Already saved` feedback with actions:
  add note, reclassify intent, open existing capture.
- [ ] **T017-203** Ensure compact feedback windows use visible-content touch
  bounds only; adjacent launcher/app icons must remain tappable.
- [ ] **T017-204** Wire reclassify to update the existing envelope's intent
  history rather than creating a new envelope.
- [ ] **T017-205** Specify and implement note persistence if notes are not
  already modeled. Do not add an ad-hoc note field without a data-model update.

## Phase 3 - Tests and physical QA

- [ ] **T017-301** Repository test: same canonical URL twice returns
  `AlreadySaved` and creates no second visible envelope.
- [ ] **T017-302** Repository test: same exact non-URL text twice returns
  `AlreadySaved`.
- [ ] **T017-303** UI/orchestrator test: `AlreadySaved` state exposes note,
  reclassify, and open actions.
- [ ] **T017-304** Physical QA on S24 and Tab S9: duplicate URL, duplicate text,
  adjacent icon tap outside pill, portrait and landscape.
- [ ] **T017-305** Verification gate: `:app:compileDebugKotlin`,
  `:app:testDebugUnitTest`, `:app:compileDebugAndroidTestKotlin`, `:app:lintDebug`.

## Landing Map

- Spec 015: visual refit, shared source glyph resolver, settings subpage visual
  audit, chip/undo/bubble styling.
- Spec 016: durable intent enum/label migration and contact-ref schema.
- Spec 017: duplicate capture product behavior, already-saved actions, note or
  reclassify behavior, and compact feedback touch contract.