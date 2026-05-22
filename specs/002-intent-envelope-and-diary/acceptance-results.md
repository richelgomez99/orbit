# Spec 002 — Physical-device acceptance results

**Status:** TEMPLATE — not yet executed against real hardware. T110–T113 checkboxes in `tasks.md` are currently closed via downstream-doc proof (spec 003's quickstart and plan treat them as completed prerequisites), but the empirical run that produces this file has not happened. Fill this document during the next physical-device acceptance pass.

**Source procedure:** `specs/002-intent-envelope-and-diary/quickstart.md` §3..§4.7 + §6.
**Source tasks:** T110, T111, T112, T113 (`specs/002-intent-envelope-and-diary/tasks.md` lines 745–749).

---

## Run metadata

| Field | Value |
|---|---|
| Run date | _(YYYY-MM-DD)_ |
| Runner | _(name)_ |
| Build SHA | _(git rev-parse HEAD)_ |
| Build variant | `debug` |
| Branch | _(branch under test)_ |
| Cloud-pivot status at run | _( pre-pivot / post-pivot — affects MITM assertion, see §6 below)_ |
| Total run time (wall clock) | _(HH:MM)_ |

---

## Device matrix

Three devices in the current owned set. Not all checks apply to all devices — see per-device sections below.

| Device | Role | Nano? | Android | Build install verified | Notes |
|---|---|---|---|---|---|
| **Galaxy S24 Ultra** | Primary, Nano local-mode coverage | ✅ Samsung AI / AICore (text subset) | _(version)_ | ☐ | Demo + alpha device. |
| **Pixel 9a** | Clean Pixel design-review device | ❌ (not on Nano list as of 2026-05) | _(version)_ | ☐ | Cloud-mode only — runs §4.4 (Nano unavailable) by default. |
| **Galaxy Tab S9** | Tablet-layout regression + weaker-silicon perf | ❌ (Snapdragon 8 Gen 2, not on Nano list) | _(version)_ | ☐ | Useful "minimum-bar Samsung flagship" data point for T113. |

Spec 002 quickstart §1 originally targeted Pixel 8/9 series; this run substitutes the actual owned device matrix. Cross-reference checks where the original quickstart assumes Nano (e.g., §3 step 8c summary line) — on Pixel 9a and Tab S9 the §4.4 fallback applies.

---

## §3 — Golden path: clipboard URL capture

Source URL used: `https://www.nytimes.com/2026/03/11/technology/android-agents.html` _(or runner's choice — record below)_

**Per-device results:**

### S24 Ultra (Nano)

| Step | Expected | Observed | Pass? |
|---|---|---|---|
| 3.1 | Plug into Wi-Fi (unmetered) + charger | _(verify)_ | ☐ |
| 3.4 | Bubble animates: `CLIPBOARD_RECEIVED` → `AWAITING_INTENT` | _(logcat tag `ClipboardFocus`)_ | ☐ |
| 3.5 | 4-chip row appears: `[Want it] [Reference] [For someone] [Interesting]` | _(observed)_ | ☐ |
| 3.6 | Tap **Reference** → row collapses with 10s undo toast | _(observed)_ | ☐ |
| 3.8a | Within ~30s, no notification fires | _(verify pull-down empty for Orbit)_ | ☐ |
| 3.8b | Diary opens on **today** | _(observed)_ | ☐ |
| 3.8c | Card shows `from {app} · {activity} · {time} · Reference` + Nano summary | _(record summary text — quality check)_ | ☐ |
| Audit | Settings → "What Orbit did today" shows: 1 capture, 1 enrichment, 1 net fetch to `nytimes.com`, 1 Nano summary | _(screenshot)_ | ☐ |

Notes / failures: _(free text)_

### Pixel 9a (no Nano — §4.4 fallback applies)

| Step | Expected | Observed | Pass? |
|---|---|---|---|
| 3.1–3.7 | Same as S24 Ultra | _(observed)_ | ☐ |
| 3.8c | Diary card shows title + domain only. **No summary** (Nano unavailable). | _(observed)_ | ☐ |
| Audit | Settings → "What Orbit did today" shows: 1 capture, 1 enrichment, 1 net fetch, **0 Nano summaries** | _(screenshot)_ | ☐ |

Notes: this is the §4.4 expected behaviour, not a regression. Document as PASS if matches.

### Tab S9 (no Nano — §4.4 fallback applies)

Same expectation as Pixel 9a. Tablet-specific layout check below.

| Step | Expected | Observed | Pass? |
|---|---|---|---|
| 3.1–3.8 (functional) | Matches §4.4 fallback | _(observed)_ | ☐ |
| Tablet layout | Diary card not clipped, chip row not stretched, bubble repositions to tablet-appropriate edge | _(screenshot)_ | ☐ |

---

## §4 — Alternate path verification

Run on at least one device per applicability column. Mark per device.

### §4.1 — Silent-wrap

Source: plain text "1 cup flour, 2 eggs, pinch of salt"

| Device | Classified Reference (high conf) | Silent (no chip row) | "Saved as Reference" toast (2s) | Diary entry today |
|---|---|---|---|---|
| S24 Ultra | ☐ | ☐ | ☐ | ☐ |
| Pixel 9a | ☐ | ☐ | ☐ | ☐ |
| Tab S9 | ☐ | ☐ | ☐ | ☐ |

### §4.2 — Ambiguous chip-row auto-dismiss

Source: short phone number or codename

| Device | Chip row appears | Auto-dismisses after 2s | Sealed with `intent=AMBIGUOUS` | "Needs intent" hint | Tap re-opens picker |
|---|---|---|---|---|---|
| S24 Ultra | ☐ | ☐ | ☐ | ☐ | ☐ |
| Pixel 9a | ☐ | ☐ | ☐ | ☐ | ☐ |
| Tab S9 | ☐ | ☐ | ☐ | ☐ | ☐ |

### §4.3 — Screenshot capture (OCR)

| Device | No chip row | `contentType=IMAGE`, `intent=AMBIGUOUS` | OCR runs ~30s on charger/Wi-Fi | URLs hydrated as in §3 | OCR'd title preview shown |
|---|---|---|---|---|---|
| S24 Ultra | ☐ | ☐ | ☐ | ☐ | ☐ |
| Pixel 9a | ☐ | ☐ | ☐ | ☐ | ☐ |
| Tab S9 | ☐ | ☐ | ☐ | ☐ | ☐ |

### §4.4 — Nano unavailable

Already covered for Pixel 9a + Tab S9 in §3 above. For S24 Ultra: temporarily force Nano UNAVAILABLE via Settings → Diagnostics (debug build).

| Device | Capture as §3 | Diary card: title + domain only | "What Orbit did today": 0 Nano summaries |
|---|---|---|---|
| S24 Ultra (forced unavailable) | ☐ | ☐ | ☐ |
| Pixel 9a (natural) | _(see §3)_ | _(see §3)_ | _(see §3)_ |
| Tab S9 (natural) | _(see §3)_ | _(see §3)_ | _(see §3)_ |

### §4.5 — Privacy kill-switch

Run on one device (S24 Ultra suggested for full Nano-mode coverage of the local silent-summary fallback).

| Step | Expected | Observed | Pass? |
|---|---|---|---|
| 4.5.1 | Settings → toggle "Pause continuations" ON | _(observed)_ | ☐ |
| 4.5.2 | Capture per §3 → envelope created, Diary card stays bare (title only or local-Nano summary if applicable) | _(observed)_ | ☐ |
| 4.5.3 | Audit log shows `PRIVACY_PAUSED`, no `NETWORK_FETCH` after toggle | _(verify)_ | ☐ |
| 4.5.4 | Toggle OFF → previously-paused continuation re-enqueued on next WorkManager tick | _(observed)_ | ☐ |

### §4.6 — Undo

| Device | Capture per §3 → tap Reference | Within 10s tap "Undo" on toast | Diary today excludes envelope | Audit: `ENVELOPE_CREATED` then `envelope_undone` |
|---|---|---|---|---|
| S24 Ultra | ☐ | ☐ | ☐ | ☐ |

(Single-device check sufficient; the undo path is hardware-agnostic.)

### §4.7 — Intent reassignment

| Device | Capture as Interesting | Diary → tap card → "Change intent" → "Want it" | `intentHistoryJson` has 2 entries | Audit: `INTENT_SUPERSEDED` |
|---|---|---|---|---|
| S24 Ultra | ☐ | ☐ | ☐ | ☐ |

### §4.8 — Envelope detail screen

Run on S24 Ultra (primary) + Tab S9 (tablet layout coverage).

| Check | S24 Ultra | Tab S9 |
|---|---|---|
| Tap card opens detail screen | ☐ | ☐ |
| TopAppBar back arrow + overflow menu (Archive / Delete / Open original / Copy / Share) | ☐ | ☐ |
| Full-width intent picker (4 chips, current highlighted) | ☐ | ☐ |
| `from {app} · {activity} · {absolute time}` subtitle | ☐ | ☐ |
| IMAGE thumbnail (if image capture) | ☐ | ☐ |
| Full hydrated title + summary (no clamp) | ☐ | ☐ |
| Domain chip → opens canonical URL in browser | ☐ | ☐ |
| Captured text in monospace selectable block | ☐ | ☐ |
| Intent history list (oldest-first, `intent · source · timestamp`) | ☐ | ☐ |
| Audit trail list (hidden if empty) | ☐ | ☐ |
| Tap chip → reassigns silently, screen reloads | ☐ | ☐ |
| Overflow → Archive → screen closes, card gone from Diary today | ☐ | ☐ |
| Overflow → Delete → confirm dialog → moves to Trash | ☐ | ☐ |

---

## §6 — Constitution-aligned acceptance gates

These are the seven hard gates from quickstart.md §6. All seven must pass for v1 acceptance.

| # | Gate | Budget | S24 Ultra | Pixel 9a | Tab S9 |
|---|---|---|---|---|---|
| 6.1 | P50 seal time (tap → confirmation) | < 200 ms | _(measured ms)_ | _(measured ms)_ | _(measured ms)_ |
| 6.2 | P95 URL hydration (charger + unmetered) | < 30 s | _(measured s)_ | _(measured s)_ | _(measured s)_ |
| 6.3 | Diary "today" first render after launch | < 1 s | _(measured ms)_ | _(measured ms)_ | _(measured ms)_ |
| 6.4 | "What Orbit did today" reflects every action from §3 | exact match | ☐ | ☐ | ☐ |
| 6.5 | **Zero outgoing HTTP from any process except `:net`** _(see MITM section below — pre-pivot framing; cloud-pivot redefines)_ | 0 | ☐ | ☐ | ☐ |
| 6.6 | No crash in 24h background use with 50+ captures | 0 crashes | _(start: ___ ; end: ___ ; captures: ___ ; crashes: ___)_ | _(...)_ | _(...)_ |
| 6.7 | Uninstall + reinstall loses all data (no cloud tail) | total wipe | ☐ | ☐ | ☐ |

Measurement notes:

- **6.1 P50 seal**: instrument with `latency-bench` test suite or manual stopwatch over ≥30 captures, take the median. Record per device.
- **6.2 P95 URL hydration**: time from envelope seal to summary appearing in Diary, ≥20 captures, take the 95th percentile. Charger + unmetered Wi-Fi only.
- **6.3 Diary first render**: cold-launch DiaryActivity ≥10 times, measure from `Activity.onCreate` to first frame committed (Choreographer trace).
- **6.6 24h background**: leave phone plugged in overnight + a normal day with backgrounded app, capture organically + via paste loop. Use `dumpsys dropbox --print | grep capsule` for crash count.
- **6.7 Uninstall hygiene**: `adb uninstall com.capsule.app && adb install ...`, verify Diary is empty, no surfaced legacy state.

---

## T111 — MITM proxy run (`tasks.md:747`)

**Goal:** assert zero outgoing HTTP from any process besides `:net`, no `Referer` header, no cookies. Attach `mitm.log`.

**Setup:**

```bash
# Device-side: route Wi-Fi through mitmproxy (host machine).
# Install mitmproxy CA cert as user CA on each device.
# Confirm SSL pinning is off in debug build.
mitmproxy --listen-port 8080 --ssl-insecure
```

**Run:**

1. Connect each device through proxy.
2. Reset proxy log: `mitmproxy -w mitm-{device}.log`.
3. Execute §3 golden path on the device.
4. Execute §4.3 screenshot capture (triggers OCR + URL hydration).
5. Execute §4.5 privacy kill-switch ON → capture → kill-switch OFF.

**Per-device assertions:**

| Device | All HTTP requests originate from process `:net` only | No `Referer` header on any request | No `Cookie` header on any request | `User-Agent` is fixed Orbit string | mitm.log path |
|---|---|---|---|---|---|
| S24 Ultra | ☐ | ☐ | ☐ | ☐ | _(./mitm-s24u.log)_ |
| Pixel 9a | ☐ | ☐ | ☐ | ☐ | _(./mitm-p9a.log)_ |
| Tab S9 | ☐ | ☐ | ☐ | ☐ | _(./mitm-tabs9.log)_ |

**Cloud-pivot impact (post-pivot run only):** the assertion changes. Post-pivot, `:net` will egress to (a) hydration targets, AND (b) the Vercel AI Gateway endpoint. Update the assertion script to:

> "All HTTP from `:net` only; `:net` egress restricted to (hydration target hostnames) ∪ (Gateway endpoint hostname). Zero egress from any other process."

If running this section post-pivot, document the Gateway hostname here: _(record)_ — and confirm no other endpoints appear.

---

## T112 — Memory leak check (`tasks.md:748`)

**Goal:** Android Studio Profiler over a 10-minute capture burst (50 envelopes). Document overlay + `:ml` unbind path.

**Procedure:**

1. Attach Profiler to `com.capsule.app` (default), `:capture`, `:ml`.
2. Force-GC each process at start (Profiler → trash icon).
3. Record heap baseline per process.
4. Run a paste-loop script that copies 50 distinct URLs over 10 minutes (1 every 12s).
5. After the burst, force-GC each process again.
6. Capture heap dump.
7. Compare baseline vs post-burst retained size per process.
8. Run LeakCanary report (debug build).

**Per-device results:**

| Device | `:capture` Δheap (bytes) | `:ml` Δheap (bytes) | default Δheap (bytes) | LeakCanary leaks reported | Profiler dump path |
|---|---|---|---|---|---|
| S24 Ultra | _(measured)_ | _(measured)_ | _(measured)_ | _(count)_ | _(path)_ |
| Pixel 9a | _(measured)_ | _(measured)_ | _(measured)_ | _(count)_ | _(path)_ |
| Tab S9 | _(measured)_ | _(measured)_ | _(measured)_ | _(count)_ | _(path)_ |

**Pass criteria:** Δheap < 5 MB per process after 10-minute burst + GC. LeakCanary reports 0 application leaks (framework-attributed leaks acceptable, document them).

Notes / leaks identified: _(free text)_

---

## T113 — Performance validation (`tasks.md:749`)

Already partially measured under §6.1, 6.2, 6.3. Consolidated here with explicit pass/fail per gate.

| Gate | Budget | S24 Ultra | Pixel 9a | Tab S9 | Pass on all? |
|---|---|---|---|---|---|
| P50 seal | < 200 ms | _(ms)_ | _(ms)_ | _(ms)_ | ☐ |
| P95 URL hydration | < 30 s | _(s)_ | _(s)_ | _(s)_ | ☐ |
| Diary p50 first render | < 1 s | _(ms)_ | _(ms)_ | _(ms)_ | ☐ |

**If violated:** quickstart instruction is "adjust debouncers if violated." Document violations + the planned debouncer adjustment below.

Violations + remediation plan: _(free text)_

Tab S9 is the expected canary on weaker silicon — record budgets even if they fail; the data informs the cloud-pivot recalibration.

---

## Summary

| Surface | Pass / Fail / Blocked | Notes |
|---|---|---|
| §3 golden path (3 devices) | _(P/F/B)_ | _(...)_ |
| §4.1 silent-wrap | _(P/F/B)_ | _(...)_ |
| §4.2 ambiguous auto-dismiss | _(P/F/B)_ | _(...)_ |
| §4.3 screenshot OCR | _(P/F/B)_ | _(...)_ |
| §4.4 Nano unavailable | _(P/F/B)_ | _(...)_ |
| §4.5 privacy kill-switch | _(P/F/B)_ | _(...)_ |
| §4.6 undo | _(P/F/B)_ | _(...)_ |
| §4.7 intent reassignment | _(P/F/B)_ | _(...)_ |
| §4.8 envelope detail screen | _(P/F/B)_ | _(...)_ |
| §6 constitution gates (7) | _(P/F/B)_ | _(...)_ |
| T111 MITM | _(P/F/B)_ | _(attach mitm logs per device)_ |
| T112 memory leak | _(P/F/B)_ | _(attach Profiler dumps)_ |
| T113 perf validation | _(P/F/B)_ | _(record violations)_ |

**Overall acceptance status:** _(ACCEPTED / BLOCKED / PARTIAL)_

**Blockers found** (if any):

1. _(blocker description, severity, owner)_

**Non-blocking issues:**

1. _(issue, recommendation)_

---

## Cloud-pivot impact on this acceptance run

If this run is executed **before** the cloud pivot lands:

- §6.5 (zero HTTP outside `:net`) holds in its strict pre-pivot form.
- §3 + §4.* "Nano summary" expectations apply only to S24 Ultra; Pixel 9a + Tab S9 run §4.4 fallback by default.
- T165 (Phase 11 cluster-summarization acceptance) is not testable on Pixel 9a / Tab S9 — defer.

If executed **after** the cloud pivot:

- §6.5 assertion narrows: zero egress from `:capture` / `:ml` / default; `:net` egress restricted to (hydration targets) ∪ (Vercel AI Gateway). Update the MITM script accordingly.
- §3 + §4.* "Nano summary" expectations now hold across all three devices (cloud is default; local-mode toggle off by default).
- §4.4 "Nano unavailable" path gets reframed: "AI unavailable" — verifiable by toggling `useLocalAi=true` on Pixel 9a / Tab S9 (where Nano is missing) and confirming the bare-card fallback.
- T165 becomes runnable on all three devices.
- A new acceptance gate enters: cloud-mode round-trip latency budget (TBD; suggested p95 < 3 s for embed, < 8 s for summarize). Add a §6.8 row when the pivot ships.

This document is a **template until the run happens**. After the first physical run, fill in the blanks and replace this notice with a "first run completed YYYY-MM-DD" preamble. Subsequent runs append below as new dated sections rather than overwriting.

---

## Appendix — useful adb commands during the run

```bash
# Process inventory
adb shell ps -A | grep com.capsule.app

# Live state-machine logs (clipboard focus + capture)
adb logcat -s CapsuleCapture:D ClipboardFocus:D ServiceHealth:D BubbleUI:D

# Audit log dump (debug build)
adb shell am broadcast -a com.capsule.app.DEBUG_DUMP

# Per-process net stats (Constitution Principle I check, §7 of spec 003 quickstart)
adb shell dumpsys netstats detail | grep com.capsule.app

# Crash drop count (§6.6)
adb shell dumpsys dropbox --print | grep com.capsule.app | wc -l

# Force-stop to test restart (T110a kill-survival behavioural)
adb shell am force-stop com.capsule.app

# Heap dump capture (T112)
adb shell am dumpheap com.capsule.app /sdcard/orbit-default.hprof
adb pull /sdcard/orbit-default.hprof
```

---

## Appendix — sample paste-loop for §6.6 / T112

```bash
#!/bin/bash
# 50 captures over 10 minutes, 1 every 12s.
URLS=(
  "https://www.nytimes.com/2026/03/11/technology/android-agents.html"
  "https://en.wikipedia.org/wiki/Foreground_service"
  # ... 48 more URLs
)
for url in "${URLS[@]}"; do
  adb shell "echo -n '$url' | am broadcast -a clipper.set --es text '$url'"
  sleep 12
done
```

(Use `clipper` or any clipboard-injection adb shim to drive the bubble. Manual paste from Pixel/Galaxy clipboard works too if the runner is patient.)
