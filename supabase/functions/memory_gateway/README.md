# Orbit Memory Gateway — Vercel Node Function (Spec 005)

This directory contains the backend gateway for Orbit's compact Atlas memory
index. Android never connects to Atlas directly; it calls this authenticated
gateway through the `:net` process.

Despite living under `supabase/`, this deploys to Vercel to match the existing
`llm_gateway` operational path. It uses Vercel's Node runtime, not Edge,
because the MongoDB driver requires TCP/TLS modules unavailable in Edge.

## Required Vercel Environment Variables

Set these in Production, Preview, and Development:

| Var | Source | Purpose |
| --- | --- | --- |
| `MONGODB_ATLAS_URI` | MongoDB Atlas | Server-side Atlas connection string. Never put this in Android. |
| `MONGODB_DB` | Operator config | Atlas database, e.g. `orbit_dev`. |
| `MONGODB_MEMORY_COLLECTION` | Operator config | Atlas collection, e.g. `memory_items`. |
| `SUPABASE_URL` | Supabase dashboard | JWT issuer verification. |
| `SUPABASE_JWT_SECRET` | Supabase dashboard | HS256 verification for inbound Supabase access tokens. |
| `OPENAI_API_KEY` | OpenAI dashboard | Backend-only embeddings and optional grounded Ask synthesis. Never put this in Android. |
| `MEMORY_EMBEDDING_MODEL` | Operator config | Optional. Defaults to `text-embedding-3-small`. |
| `MEMORY_EMBEDDING_DIMENSIONS` | Operator config | Optional. Defaults to `1536`. Must match Atlas vector index dimensions. |
| `MEMORY_ASK_MODEL` | Operator config | Optional. Defaults to `gpt-4.1-mini`. |

Local development uses `.env.local`, which is gitignored.

## First-Time Setup

```bash
cd supabase/functions/memory_gateway
npm install
vercel link
```

Add env vars:

```bash
vercel env add MONGODB_ATLAS_URI production
vercel env add MONGODB_DB production
vercel env add MONGODB_MEMORY_COLLECTION production
vercel env add SUPABASE_URL production
vercel env add SUPABASE_JWT_SECRET production
vercel env add OPENAI_API_KEY production
vercel env add MEMORY_EMBEDDING_MODEL production
vercel env add MEMORY_EMBEDDING_DIMENSIONS production
vercel env add MEMORY_ASK_MODEL production
```

Repeat for `preview` and `development`.

## Local Development

```bash
cp .env.local.example .env.local
vercel dev
```

Local route:

```bash
http://localhost:3000/memory
```

Smoke without auth should return `401`:

```bash
curl -i -X POST http://localhost:3000/memory \
  -H 'Content-Type: application/json' \
  -d '{}'
```

Smoke with a Supabase access token:

```bash
JWT="<fresh Supabase access_token>"
curl -i -X POST http://localhost:3000/memory \
  -H "Authorization: Bearer $JWT" \
  -H 'Content-Type: application/json' \
  -d '{"type":"memory_search","requestId":"550e8400-e29b-41d4-a716-446655440000","payload":{"query":"demo","limit":5}}'
```

## Production Deploy

```bash
bash deploy.sh --dry-run
bash deploy.sh
```

After deploy, set Android's local build config:

```properties
memory.gateway.url=https://<project>.vercel.app/memory
```

`memory.gateway.url` is not a secret. It belongs in local `local.properties`,
not in committed source.

## MVP Demo Seed

After `memory.gateway.url`, `supabase.url`, `supabase.publishable.key`,
`supabase.debug.email`, and `supabase.debug.password` are populated in the
repo root `local.properties`, seed the deployed memory gateway with compact
demo records:

```bash
npm run demo:seed
```

The script signs in as the normal Supabase debug user, upserts 20 compact
memory records through the deployed `/memory` gateway, verifies searches for
`startup event`, `flight receipt`, and `recipe`, and inspects Atlas documents
when `.env.local` contains `MONGODB_ATLAS_URI`.

Do not use service-role credentials for `supabase.debug.password`. It must be
the password for a normal Supabase Auth test user.

## Semantic Retrieval Setup

Spec 005A adds semantic Library search and grounded Ask. The operational order is:

```bash
cd supabase/functions/memory_gateway
npm run vector:index
npm run memory:embed-stale -- --user-id <supabase-user-id>
npm run eval:retrieval
```

Use `--all-users` instead of `--user-id` only for development/backfill jobs where
that is intentional:

```bash
npm run memory:embed-stale -- --all-users --limit 100
```

The embedding job only reads compact Atlas records and writes vector metadata
back to the same compact index. It does not read raw screenshots, full OCR,
prompts, model responses, or Android's encrypted Room database.

Live semantic search requires the Atlas Search index named
`memory_embedding_v1`. The script creates it with `1536` dimensions by default,
matching `text-embedding-3-small`.

## Safety Invariants

- Atlas credentials stay server-side.
- Every request is scoped by authenticated Supabase `sub`.
- Documents are compact memory index records, not raw captures.
- OpenAI credentials stay server-side.
- Banned fields such as raw screenshots, full OCR, raw HTML, prompts, and
  model responses are rejected before Atlas writes.
