# Visual QA — Real-Device Walkthrough, 2026-07-06

First **human-eyes** pass through the running app on the S24 (not the
instrumented suite). Prompted by the operator's observation that during
the automated test runs, screens looked white or "spilled over the top
and interacted with notifications." This pass drove the real app by hand,
screen by screen, judging each as a product.

## Headline

**No real Orbit screen renders white.** The white screens seen during
the campaign were the instrumented-test harness flashing empty
ComposeView activities (they render on white) plus the Android Settings
pages the onboarding "Open settings" buttons jump to — not Orbit UI.

**The status-bar overlap is genuinely fixed.** Diary and Settings both
show their titles cleanly below the clock; the earlier `statusBarsPadding`
fix (commit c5b44c6) holds.

## Confirmed good

- **Diary** — on-brand: serif "Orbit." wordmark, cream accent, mono
  date/nav, graphite bg. Correct inset. Empty state present.
- **Settings** — the design language at its best: serif hero copy,
  `// PRINCIPLE I` mono labels, ON/OFF toggle pills, amber accents.
  Correct inset.

## Real defects found

### D1 — Capture bubble floats over Orbit's OWN UI *(functional, priority 1)*
The capture-bubble overlay window (`:capture` process,
`TYPE_APPLICATION_OVERLAY`, dumpsys Window #11) stays visible while
Orbit itself is foreground and rests at a position that occludes screen
titles: it covers the "L" of **Library** and "Or" of **Orbit** on those
tabs, and sits over the Settings divider. A capture bubble exists to
grab content *from other apps* — showing it on top of Orbit's own
screens is wrong and is almost certainly what read as "spilled over the
top and interacted with" chrome.

**Fix:** hide the bubble whenever an Orbit activity is in the
foreground; restore it when Orbit backgrounds. Crosses the
`:ui`→`:capture` process boundary (activities must signal the overlay
service on resume/pause), so it needs a small deliberate change on the
capture path, not a live hack. Spec + implement as its own slice.

### D2 — Onboarding is five off-brand permission gates *(first-run, priority 2)*
Notifications → capture bubble → usage access → activity recognition →
overlay/capture = five full-screen permission steps before any value,
every one in plain Material dark (no serif, no cream/graphite, no mono
labels) with large dead vertical space. It's the user's first
impression and it looks like a different, generic app than the Diary
and Settings they land on. Rebrand to Quiet Almanac + tighten the
vertical rhythm; consider collapsing optional grants.

### D3 — Orbit tab + empty-state copy are off-brand *(consistency, priority 3)*
The Orbit tab (Ask Orbit box, Agent plan card, blue "Plan" button) is
plain Material, not Quiet Almanac — jarring next to the Diary/Settings.
Empty-state strings ("Nothing saved yet", "No active cleanup") render
in default Roboto rather than the brand type. Matches polish-audit
Batch D.

## What this changes about "production grade"

The functional surface is healthier than the operator feared — the app
is not rendering broken/white in real use, and the navigation works.
The gap is **visual consistency and the bubble's foreground behavior**,
not correctness. D1 is a real bug worth fixing before any demo; D2/D3
are branding-consistency work that the polish audit (spec 024) already
owns.
