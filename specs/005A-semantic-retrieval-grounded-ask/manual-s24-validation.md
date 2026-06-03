# Manual S24 Validation: Spec 005A/005B MVP

Run this after installing:

```text
dist/orbit-mvp-debug-20260603-005b.apk
sha256=7a80f2dc94b00766177eec6f80393bf4289c39c8252cd6f3a44d925c1369c885
```

Emulator preflight already passed on a Pixel 10 Pro Android 17 AVD for tab navigation, Library result open, Ask citation open, and sensitive refusal rendering. This checklist remains required because the MVP demo target is the real S24 build with real capture/device conditions.

## Preflight

1. Connect the S24 with USB or wireless debugging.
2. From the repo root, run:

```bash
specs/005A-semantic-retrieval-grounded-ask/scripts/install-and-seed-device.sh
```

If more than one Android device is attached, run:

```bash
ANDROID_SERIAL=<device-serial> specs/005A-semantic-retrieval-grounded-ask/scripts/install-and-seed-device.sh
```

For a clean demo-only device or emulator, you can reset local Orbit data before install:

```bash
ANDROID_SERIAL=<device-serial> specs/005A-semantic-retrieval-grounded-ask/scripts/install-and-seed-device.sh --reset-data
```

Do not use `--reset-data` on the S24 unless you are intentionally deleting the local Orbit test data.

3. Open Settings.
4. Confirm cloud memory/indexing is enabled if the setting is visible.

Expected: Diary has demo captures or recent test captures, Library can search, and Orbit tab opens without crashing.

## Diary

1. Open Diary.
2. Open a screenshot capture with no known source app.
3. Confirm the detail title/subtitle does not say `unknown`.

Expected copy:

- `Screenshot saved from phone`, or
- `Screenshot from <app name>` when Android captured an app label.

## Library Semantic Search

Run each search from Library and open the top relevant result.

| Query | Expected top/local capture | Pass condition |
| --- | --- | --- |
| `qr code` | `demo-qr-customers-01` / first-1000-customers QR code | Result appears and tapping opens capture detail. |
| `reschedule` | `demo-calendar-01` / dentist or appointment reschedule | Result appears and tapping opens capture detail. |
| `rescheduling` | same reschedule capture | Stemming/fuzzy behavior surfaces the same capture. |
| `flight receipt` | `demo-flight-receipt-01` | Result appears and tapping opens capture detail. |
| `recipe` | recipe demo capture | Result appears and tapping opens capture detail. |

Expected: duplicate-looking rows are collapsed; cloud-only/dead envelopes must not open as results.

## Orbit Grounded Ask

Ask these in the Orbit tab:

| Question | Expected |
| --- | --- |
| `Which flight receipt did I save?` | Answered with citations; citation opens local capture. |
| `What startup event did I save?` | Answered with citations; citation opens local capture. |
| `What recipe did I want to try?` | Answered with citations; citation opens local capture. |
| `What is my passport number?` | Refuses with friendly sensitive-evidence copy; no hallucinated value. |

Expected refusal copy should communicate: Orbit does not have an exact saved capture containing that sensitive detail, so it will not guess.

## 005B Link Rehydration

1. Capture or seed a link with a note/context if possible.
2. Wait for URL hydration.
3. Open the capture detail.

Expected: link summary uses the final URL/page title and can incorporate compact source/note context when available. It must not display raw OCR, prompt text, or model response internals.

## Evidence To Capture

Upload screenshots for:

1. Diary screenshot detail showing non-unknown source copy.
2. Library `qr code` result and opened capture detail.
3. Library `reschedule` or `rescheduling` result and opened capture detail.
4. Orbit answered grounded Ask with visible citation.
5. Orbit passport-number refusal.

## Pass/Fail Recording

Record results in `specs/005A-semantic-retrieval-grounded-ask/tasks.md`:

- Mark `T005A-050` complete only after Library and Orbit checks pass on S24.
- Mark `T005B-015` complete only after the installed APK also passes Diary/link/source wording checks.
