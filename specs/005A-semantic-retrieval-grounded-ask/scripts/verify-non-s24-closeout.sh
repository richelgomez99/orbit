#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
APK="${ROOT}/dist/orbit-mvp-debug-20260603-005b.apk"
EXPECTED_SHA="7a80f2dc94b00766177eec6f80393bf4289c39c8252cd6f3a44d925c1369c885"

cd "${ROOT}"

echo "== APK hash =="
if [[ ! -f "${APK}" ]]; then
  echo "Missing APK: ${APK}" >&2
  exit 1
fi
ACTUAL_SHA="$(shasum -a 256 "${APK}" | awk '{print $1}')"
echo "${ACTUAL_SHA}  ${APK}"
if [[ "${ACTUAL_SHA}" != "${EXPECTED_SHA}" ]]; then
  echo "APK hash mismatch. Expected ${EXPECTED_SHA}" >&2
  exit 1
fi

echo "== Android local CI =="
./gradlew \
  :app:compileDebugKotlin \
  :app:testDebugUnitTest \
  :build-logic:lint:test \
  :app:lintDebug \
  :app:compileDebugAndroidTestKotlin \
  :app:assembleDebug

echo "== Memory gateway backend gate =="
(
  cd supabase/functions/memory_gateway
  npm run typecheck
  npm run test:unit
  npm run eval:retrieval
)

echo "== Android secret scan =="
if rg -n "OPENAI_API_KEY|MONGODB_ATLAS_URI|mongodb\\+srv|MongoClient|ZEROENTROPY|ANTHROPIC_API_KEY" app/src; then
  echo "Unexpected Android secret/client match." >&2
  exit 1
fi

echo "== Android network-boundary scan =="
if rg -n "OkHttpClient\\(|HttpURLConnection|Socket\\(|HttpClient\\(" app/src/main/java app/src/debug/java app/src/release/java; then
  echo "Unexpected direct network constructor match." >&2
  exit 1
fi

echo "== Helper syntax =="
bash -n specs/005A-semantic-retrieval-grounded-ask/scripts/install-and-seed-device.sh
bash -n specs/005A-semantic-retrieval-grounded-ask/scripts/verify-non-s24-closeout.sh

echo "== Git hygiene =="
git diff --check
if [[ -n "$(git status --short dist)" ]]; then
  echo "dist/ appears in normal git status; do not commit generated APK artifacts." >&2
  git status --short dist >&2
  exit 1
fi

echo "== Device state =="
adb devices || true

cat <<'SUMMARY'

Non-S24 MVP closeout gates passed.

S24 MVP validation is tracked separately in:
- specs/005A-semantic-retrieval-grounded-ask/manual-s24-validation.md
- specs/005A-semantic-retrieval-grounded-ask/mvp-closeout-audit.md

As of 2026-06-03, T005A-050 and T005B-015 are complete from the S24 flow and source-label recheck.
SUMMARY
