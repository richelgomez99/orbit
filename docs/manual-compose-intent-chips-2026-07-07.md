# Follow-up spec: intent chips in manual "Save to Orbit"

**Created**: 2026-07-07 · **Status**: planned (endorsed follow-up to the
Save-to-Orbit Quiet refit, commit `91a0548`) · **Size**: small (building
blocks exist)

## Why

Every captured envelope carries an **intent** (the *why* — the wax-seal glyphs
▲ WANT_IT · ◆ REFERENCE · ● READ_LATER · ◇ FOR_SOMEONE · ○ INTERESTING). The
**overlay capture** flow lets the user pick it via `overlay/ChipRow.kt`
(`QuietIntentButton` + `IntentChipKind`), stamped `IntentSource.USER_CHIP`.

The **manual "Save to Orbit"** sheet (`diary/ui/ManualComposeScreen.kt`) does
NOT offer intent — it saves body + freeform context, and `ManualComposeSaver`
falls back to `Intent.AMBIGUOUS` / `IntentSource.FALLBACK`. So a manually-saved
envelope is created *differently* from a captured one (the user's observation:
"the way to add context / tags the way other envelopes are created don't
match"). The Quiet refit fixed the visuals; this fixes the **model parity**.

## What exists already (why this is small)

- `ManualComposeSaver` **already accepts** `intentName` and stamps
  `IntentSource.USER_CHIP` when present (else FALLBACK/AMBIGUOUS). No data-model
  or saver change needed — just supply the value.
- The chip UI already exists: `QuietIntentButton` + `IntentChipKind` render the
  five Quiet wax-seal intent chips. Reuse (extract a shared composable if the
  overlay's `ChipRow` is too overlay-coupled — e.g. `ui/primitives/IntentChipRow`).

## Plan

1. Extract a reusable `IntentChipRow(selected: Intent?, onSelect: (Intent) -> Unit)`
   from the overlay `ChipRow` internals (`QuietIntentButton`/`IntentChipKind`),
   so both the overlay and the manual sheet render the same chips. Keep the
   overlay's timeout/auto-seal behavior in the overlay; the shared row is just
   the visual + selection.
2. Add `selectedIntent: Intent?` to the manual compose UI state +
   `onIntentSelected(Intent)` to `ManualComposeViewModel`.
3. Render the chip row in the Save-to-Orbit sheet (between the body field and
   the "Why save this?" context), matching the Quiet palette already there.
   Optional (nice): "Orbit will decide" as the unselected/default state so the
   user can defer to auto-classification.
4. Thread `selectedIntent?.name` into `ManualComposeSaver.intentName` →
   `USER_CHIP` when set, else the existing auto/fallback path (unchanged).

## Non-goals

- **Tags**: still a separate, genuinely new feature (data model + UI); neither
  capture surface has them. Not in this slice.
- No change to auto-classification for envelopes where the user doesn't pick.

## Acceptance

- The manual sheet shows the five Quiet intent chips; picking one saves the
  envelope with that `Intent` + `IntentSource.USER_CHIP`.
- Not picking preserves today's behavior (auto/fallback).
- Overlay capture chips unchanged (shared component, same look).
- Full non-phone gate green.
