#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
APK="${ROOT}/dist/orbit-mvp-debug-20260603-006.apk"
EXPECTED_SHA="05cb7026b5738b7c55f3cba7a0d168a4dabcb182270b2f18ad05863d7fad71c5"
PACKAGE="com.orbit.app"
ACTIVITY="com.orbit.app/.diary.DiaryActivity"
SEED_ACTION="com.orbit.app.debug.SEED_DEMO_MEMORY"
SEED_RECEIVER="com.orbit.app/com.orbit.app.debug.DebugDemoSeedReceiver"
RESET_DATA="false"

for ARG in "$@"; do
  case "${ARG}" in
    --reset-data)
      RESET_DATA="true"
      ;;
    -h|--help)
      cat <<USAGE
Usage: $0 [--reset-data]

Installs the Spec 006 debug APK, launches Orbit, runs the debug demo seed,
and prints the action-runtime validation checklist.

Options:
  --reset-data   Clear existing Orbit app data before install. Use only for a
                 clean demo device/emulator; this deletes local Orbit data.
USAGE
      exit 0
      ;;
    *)
      echo "Unknown argument: ${ARG}" >&2
      exit 1
      ;;
  esac
done

if ! command -v adb >/dev/null 2>&1; then
  echo "adb is not on PATH. Open Android Studio once or add Android SDK platform-tools to PATH." >&2
  exit 1
fi

if [[ ! -f "${APK}" ]]; then
  echo "Missing APK: ${APK}" >&2
  echo "Build it with: ./gradlew :app:assembleDebug" >&2
  exit 1
fi

ACTUAL_SHA="$(shasum -a 256 "${APK}" | awk '{print $1}')"
if [[ "${ACTUAL_SHA}" != "${EXPECTED_SHA}" ]]; then
  echo "APK hash mismatch." >&2
  echo "Expected: ${EXPECTED_SHA}" >&2
  echo "Actual:   ${ACTUAL_SHA}" >&2
  echo "If this APK was intentionally rebuilt, update this script and the Spec 006 quickstart together." >&2
  exit 1
fi

ADB=(adb)
if [[ -n "${ANDROID_SERIAL:-}" ]]; then
  ADB=(adb -s "${ANDROID_SERIAL}")
else
  DEVICES=()
  while IFS= read -r DEVICE; do
    DEVICES+=("${DEVICE}")
  done < <(adb devices | awk 'NR > 1 && $2 == "device" {print $1}')
  if [[ "${#DEVICES[@]}" -eq 0 ]]; then
    echo "No attached Android device. Connect the S24 with USB/wireless debugging and rerun." >&2
    adb devices >&2
    exit 1
  fi
  if [[ "${#DEVICES[@]}" -gt 1 ]]; then
    echo "Multiple devices attached. Rerun with ANDROID_SERIAL=<serial>." >&2
    adb devices >&2
    exit 1
  fi
  ADB=(adb -s "${DEVICES[0]}")
fi

echo "Installing ${APK}"
"${ADB[@]}" shell am force-stop "${PACKAGE}" >/dev/null 2>&1 || true
if [[ "${RESET_DATA}" == "true" ]]; then
  echo "Resetting Orbit app data"
  "${ADB[@]}" shell pm clear "${PACKAGE}" >/dev/null 2>&1 || true
fi
"${ADB[@]}" install -r "${APK}"

echo "Launching Orbit"
"${ADB[@]}" shell am force-stop "${PACKAGE}" >/dev/null 2>&1 || true
sleep 2
"${ADB[@]}" shell am start -n "${ACTIVITY}" >/dev/null
sleep 5

echo "Seeding debug demo memories and action drafts"
"${ADB[@]}" shell am broadcast -a "${SEED_ACTION}" -n "${SEED_RECEIVER}" >/dev/null
sleep 5

echo
echo "Device:"
"${ADB[@]}" shell getprop ro.product.model | tr -d '\r'
echo
echo "Installed package:"
"${ADB[@]}" shell dumpsys package "${PACKAGE}" | grep -E "versionName|versionCode" | head -5 || true
echo
echo "Recent seed log:"
"${ADB[@]}" logcat -d -t 300 | grep "DebugDemoSeedReceiver" | tail -8 || true
if "${ADB[@]}" logcat -d -t 300 | grep -q "DebugDemoSeedReceiver: demo seed failed"; then
  echo "Seed failed. Check logcat for DebugDemoSeedReceiver details." >&2
  exit 1
fi

cat <<'CHECKLIST'

Next manual checks:
1. Orbit: verify Action drafts appears above Follow-ups.
2. Open the calendar draft. It should show typed fields, not raw JSON.
3. Confirm calendar draft. Calendar insert screen should open. Orbit should not show a fake Calendar undo.
4. Open the recipe shopping list draft. It should show editable items, not raw JSON.
5. Confirm shopping list draft. One derived local list envelope should appear in Diary/Library with multiple checklist items.
6. Dismiss any remaining draft and confirm it disappears.

Record evidence in specs/006-approval-action-runtime/quickstart.md.
CHECKLIST
