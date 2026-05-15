# capsule-app Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-05-13

## Active Technologies
- Kotlin 2.x (Android, target SDK aligned with existing app module); Postgres 15 + pgvector 0.9 (Supabase, server-side); SQL for migrations and tests. (013-cloud-llm-routing)
- TypeScript 5.x (Vercel Edge Function runtime — Web standard (cloud-pivot)
- Supabase Postgres 15 — existing `audit_log_entries` table (no new columns); one (cloud-pivot)
- Kotlin 2.x Android app; SQL for Room migrations/tests. Supabase Postgres 15 + pgvector and Vercel Edge Function gateway are available helpers but are not authoritative for this feature. (004-capture-understanding)
- Local SQLCipher Room database remains source of truth for saved captures and derived understanding state. Cloud traces/receipts may mirror content-free metadata only when policy allows; cloud data never supersedes local deletion or invalidation. (004-capture-understanding)

- Kotlin 2.x (latest stable, matching 001) (spec/002-intent-envelope-and-diary)

## Project Structure

```text
src/
tests/
```

## Commands

# Add commands for Kotlin 2.x (latest stable, matching 001)

## Code Style

Kotlin 2.x (latest stable, matching 001): Follow standard conventions

## Recent Changes
- 004-capture-understanding: Added Kotlin 2.x Android app; SQL for Room migrations/tests. Supabase Postgres 15 + pgvector and Vercel Edge Function gateway are available helpers but are not authoritative for this feature.
- cloud-pivot: Added TypeScript 5.x (Vercel Edge Function runtime — Web standard
- 013-cloud-llm-routing: Added Kotlin 2.x (Android, target SDK aligned with existing app module); Postgres 15 + pgvector 0.9 (Supabase, server-side); SQL for migrations and tests.


<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
