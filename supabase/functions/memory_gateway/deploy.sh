#!/usr/bin/env bash
# Spec 005 — Vercel deploy runbook script for the compact memory gateway.
# Verifies required env vars are set in Vercel production before invoking
# `vercel deploy --prod`. Exits non-zero on any missing var.
#
# Usage:
#   bash deploy.sh           # verify + deploy
#   bash deploy.sh --dry-run # verify only

set -euo pipefail

REQUIRED_VARS=(
  MONGODB_ATLAS_URI
  MONGODB_DB
  MONGODB_MEMORY_COLLECTION
  SUPABASE_URL
  SUPABASE_JWT_SECRET
)

DRY_RUN=0
if [[ "${1-}" == "--dry-run" ]]; then
  DRY_RUN=1
fi

echo "==> Verifying required env vars in Vercel production..."

ENV_FILE=$(mktemp "${TMPDIR:-/tmp}/orbit-memory-gateway-env.XXXXXX")
trap 'rm -f "$ENV_FILE"' EXIT

if ! ENV_LS=$(vercel env pull "$ENV_FILE" --environment=production --yes 2>&1); then
  echo "$ENV_LS"
  echo
  echo "ERROR: Vercel env lookup failed. Link the project first:"
  echo "  vercel link --yes --project orbit-memory-gateway"
  exit 1
fi

missing=()
for v in "${REQUIRED_VARS[@]}"; do
  if ! grep -qE "^${v}=" "$ENV_FILE"; then
    missing+=("$v")
  fi
done

if (( ${#missing[@]} > 0 )); then
  echo "ERROR: Missing required Vercel production env vars:"
  for v in "${missing[@]}"; do
    echo "  - $v"
  done
  echo
  echo "Set with:  vercel env add <NAME> production"
  exit 1
fi

echo "OK: all required env vars present in production."

if (( DRY_RUN )); then
  echo "==> --dry-run: skipping deploy."
  exit 0
fi

echo "==> Deploying to Vercel production..."
vercel deploy --prod
