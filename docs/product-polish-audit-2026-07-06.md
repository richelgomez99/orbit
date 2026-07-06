# Product Polish Baseline Audit — 2026-07-06

Gap 5 from the stabilization campaign. Inventory of every user-facing
surface, scored for the three things every consumer screen needs and no
spec explicitly owns: **empty states, error states, accessibility**.
Fixes land in prioritized batches; this doc is the checklist.

## Screen inventory

Activities (manifest): DiaryActivity, MainActivity (capture setup),
OnboardingActivity, ReducedModeActivity, SettingsActivity, TrashActivity,
AuditLogActivity, EnvelopeDetailActivity.

Compose surfaces: DiaryScreen, OrbitCleanupScreen, EnvelopeDetailScreen,
ManualComposeScreen, ClusterDetailScreen, SettingsScreen, TrashScreen,
AuditLogScreen, LibraryScreen.

## Findings, prioritized

### P1 — Accessibility: back-button glyph is invisible to TalkBack *(fixing this pass)*
Four Quiet screens (Settings, Trash, AuditLog, EnvelopeDetail) implement
the back affordance as a bare `Text("‹")` with `.clickable(onClick =
onBack)` and no semantics. TalkBack announces nothing actionable — a
blind user cannot navigate back. Fix: attach a "Back" content
description + button role to each. Consistent, low-risk, high-impact.

### P1 — Accessibility: decorative/icon coverage is thin
DiaryScreen has 3 `contentDescription = null` icons (verify each is
truly decorative); OrbitCleanupScreen, ManualComposeScreen,
ClusterDetailScreen, SettingsScreen declare zero content descriptions
across interactive rows/toggles. Toggles carry test tags but no
spoken label. Audit each interactive element for a label.

### P2 — Library has no error state — CORRECTED: false positive
On inspection Library IS fully handled: `LibraryViewModel` wraps search
in try/catch and maps failure to `unavailableTitle`/`unavailableDetail`
(the grep missed these differently-named fields), and `LibraryScreen`
renders an unavailable branch. Re-submitting the query is the retry.
No work needed.

### P2 — ClusterDetailScreen has no state handling — CORRECTED: not wired
`ClusterDetailScreen`'s only call site is its own `@Preview`. It is a
design-complete surface with no production navigation entry point, so
loading/error/empty branches would be premature. Revisit when the
cluster-detail route is actually wired into the app; tracked as a
build-but-unwired note, not a polish gap.

### P3 — Empty-state copy consistency
Diary/Trash/AuditLog have empty handling; verify each reads as
intentional "Quiet Almanac" voice, not a bare "No items". Library empty
state exists — confirm it distinguishes "no query yet" from "no matches".

### P3 — Onboarding / ReducedMode not yet audited for first-run polish
Deferred to a dedicated first-run pass — these are lower-traffic but
matter for the very first impression.

## Fix batches

1. **Batch A (this pass)**: P1 back-button a11y across the 4 Quiet
   screens. Add a shared `QuietBackButton` primitive so the fix is one
   definition, not four copies, and future Quiet screens inherit it.
2. Batch B: interactive-element content descriptions (P1 coverage).
3. Batch C: Library error state + ClusterDetail state handling (P2).
4. Batch D: empty-state copy pass + onboarding first-run polish (P3).
