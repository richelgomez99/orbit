# Feature Specification: Crash Telemetry

**Feature Branch**: `feature/022-local-model-manager-20260614` (phase A rides the active branch; phase B gets its own)
**Created**: 2026-07-06
**Status**: Phase A active, Phase B deferred
**Input**: Production-grade gap 3 ("no safety net outside this desk") from the 2026-07-06 stabilization campaign.

## Why

Orbit currently has zero crash visibility. The 2026-07-06 device campaign
found the app had been crash-looping on launch (`:ml` schema validation)
with nothing recording that it ever happened. If a crash occurs on any
device other than the developer's, nobody learns anything. A drop-in SDK
(Crashlytics et al.) is constitutionally unacceptable: it would give
every process a third-party network path and ship stack context off
device without consent.

## User Stories

### US1 - Crashes Become Visible Locally (Phase A)

As the user, when Orbit crashes, the next launch records what happened
so "What Orbit did today" shows the crash instead of silence.

**Acceptance Criteria**

1. An uncaught exception in ANY Orbit process writes a sanitized crash
   record to local app storage before the process dies, then delegates
   to the system handler (normal crash UX preserved).
2. Sanitized means: process name, app version, SDK level, thread name,
   exception class chain, and code frames only. Exception MESSAGES are
   dropped entirely — they can embed user content (SQL text, file
   paths, clipboard fragments).
3. On the next default-process launch, pending crash records become
   `CRASH_DETECTED` audit rows (bounded metadata only, per Principle
   "Bounded Observation") and the records are marked reported.
4. At most a bounded number of crash records are retained on disk.

### US2 - Consent-Gated Upload (Phase B, deferred)

As the developer, with the user's explicit opt-in, sanitized crash
records upload through `:net` so field crashes are learnable.

**Phase B constraints (design pinned now, implementation deferred):**
- Upload happens ONLY from `:net` via the existing gateway pattern; no
  other process gains a network path.
- Gated behind a new Spec-008-style consent toggle, default OFF.
- Server side: Supabase table + endpoint on the existing gateway infra.
- The uploaded record is the same sanitized format as Phase A — the
  consent toggle changes where records go, never what they contain.

## Requirements (Phase A)

- **FR-023-001**: A default uncaught-exception handler wrapper MUST be
  installed in every Orbit process at Application.onCreate, MUST write
  the sanitized record best-effort, and MUST always delegate to the
  previously installed handler.
- **FR-023-002**: The sanitizer MUST drop all exception messages, cap
  stack frames and cause-chain depth, and produce a deterministic,
  line-oriented plain-text format.
- **FR-023-003**: Crash records live under the app's private files dir;
  writing MUST NOT touch the database (the DB may be the crash cause).
- **FR-023-004**: The default process MUST drain unreported records on
  launch into `CRASH_DETECTED` audit rows carrying only: process name,
  exception class, frame count, record digest.
- **FR-023-005**: Disk retention is capped (oldest records pruned).

## Non-Goals (Phase A)

- No network upload (Phase B).
- No ANR watchdog, no non-fatal logging API.
- No crash UI beyond the existing audit surface.

## Stop Signs

- Stop if any implementation path adds a network call outside `:net`.
- Stop if exception messages or any user content reach the record.
- Stop if the handler can swallow a crash (delegation must be
  unconditional).
