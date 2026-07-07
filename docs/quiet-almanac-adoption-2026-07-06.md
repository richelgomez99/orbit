# Quiet Almanac Adoption & Design-Divergence Map — 2026-07-06

Grounded in `.specify/memory/design.md` (the ratified visual bible) and
evaluated against a **populated** corpus (see `DebugCorpusSeeder`, below).
The earlier "does it render" QA missed all of this because every screen
was empty. This is the "how good does it look / how does it feel" pass.

## The bar (design.md, non-negotiables)

- **§1 / §2.3 — Zero Material icons.** Every affordance is typography
  (`▸`, `—`) or one of four wax-seal glyphs. Only raster asset = launcher.
- **§2 diff #2 — Intents are wax-seal glyphs (▲ ◆ ● ○), four SHAPES**,
  not pills, not color-only (must survive colorblindness + WCAG AA).
- **§2 diff #1 — Time in the left margin**, monospace, hanging. ✅ shipped.
- **§3.1 — Type roles:** Fraunces (display), Geist (UI sans), Newsreader
  (content serif), Berkeley Mono (mono). No Roboto/Material defaults.
- **§2.tone — Not Google Material.** Applies to every surface.

## Adoption map

`useNewVisualLanguage` / Quiet primitives are referenced in 16 files.
Coverage by surface, from the populated walkthrough:

| Surface | Quiet Almanac? | Notes |
|---|---|---|
| Diary shell (wordmark, day header, margin time, nav) | ✅ strong | serif "Orbit.", mono margins — the reference implementation |
| Settings | ✅ strong | serif hero, mono `// PRINCIPLE` labels, ON/OFF pills |
| Trash / Audit ("What Orbit did today") | ✅ | Quiet variants + accessible back button (Batch A) |
| **Diary capture rows** | ⚠️ partial | serif body ✅ BUT intent = colored **dot + label**, not wax-seal glyph; **Material trash icon** + **Material category avatars** (↗ ✉) |
| **Onboarding (5 steps)** | ❌ none | plain Material dark, Material buttons, dead vertical space |
| **Orbit tab** (Ask Orbit, Agent plan, Plan button) | ❌ none | plain Material cards + blue button |
| **Empty-state copy** (Diary/Orbit) | ❌ | Roboto, not Fraunces/Geist |

## Real divergences (ranked)

### V1 — Intent indicators are dots, not wax-seal glyphs *(design.md §2 diff #2)*
Shipped: a small colored dot + uppercase mono label (`● REFERENCE`).
Designed: four distinct SHAPES (▲ ◆ ● ○). Color-only fails the
colorblind requirement design.md explicitly calls out. This is the
single most identity-defining divergence — the wax seals are what make
a screenshot recognizably Orbit.

### V2 — Material icons throughout the Diary rows *(design.md §2 diff #3)*
Trash-can delete icon and circular category avatars (↗ for link, ✉ for
messaging) are Material. The rule is zero Material icons — affordances
should be typographic or wax-seal. Highest-count violation.

### V3 — Onboarding is entirely off-brand *(design.md §1, §2.tone)*
Five Material permission screens before first value. First impression
reads as a generic app, not the daybook. design.md §1 explicitly rules
out "onboarding carousel" Material shapes.

### V4 — Orbit (agent) tab is off-brand *(design.md §2.tone)*
The flagship "chat IS the app" surface (Ask Orbit + Agent plan) is plain
Material with a blue CTA — the least Orbit-looking screen in the app,
and the one the vision cares most about.

### V5 — Empty-state copy in Roboto *(design.md §3.1)*
"Nothing saved yet", "No active cleanup" render in the system default,
not the type system.

## Test-data enablement

`DebugCorpusSeeder` (DEBUG only) seeds a believable multi-day corpus —
10 envelopes across all four intents, hydrated URL link-cards, spanning
6 days — so these patterns are actually evaluable. Trigger over adb:

```
adb shell am broadcast -a com.orbit.app.DEBUG_SEED_CORPUS   # populate
adb shell am broadcast -a com.orbit.app.DEBUG_CLEAR_CORPUS  # reset
```
(App must be running; DEBUG builds only.)

## Recommended sequence

1. **V1 + V2 together** — a shared `IntentSeal` glyph primitive + purge
   Material icons from Diary rows. Biggest identity win, one focused pass.
2. **V4** — rebrand the Orbit tab to Quiet Almanac (spec 021 territory).
3. **V3** — onboarding rebrand + tighten (spec 024 Batch D).
4. **V5** — empty-state copy through the type system (cheap, fold into any).
5. **D1** (separate QA doc) — bubble hides when Orbit foreground.
