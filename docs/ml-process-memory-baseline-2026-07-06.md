# :ml Process Memory Baseline — 2026-07-06

Gap-4 measurement (see `docs/orbit-roadmap-queue-2026-06-02.md`, Device
Validation Milestone follow-ups). This is the **M2 precondition gate**:
no BYOM model work lands until the numbers below are understood and the
tripwire test guards them.

## Method

Live capture on the S24 Ultra (SM-S928U1, Android 16, 12 GB RAM) with
the app freshly launched, Diary loaded (i.e. `:ml` had opened the
SQLCipher database and served the day page over Binder):

```
adb shell dumpsys meminfo <process>
```

Corpus at capture time: small personal corpus (single-digit envelopes —
early-alpha scale). The instrumented tripwire test
(`MlMemoryFootprintTest`) covers the 1k-envelope scale.

## Baselines (PSS unless noted)

| Process | TOTAL PSS | TOTAL RSS | Java heap | Native heap | Notes |
|---|---:|---:|---:|---:|---|
| `com.orbit.app` (UI) | 148.7 MB | 243.0 MB | 8.7 MB | 9.5 MB | Graphics 78.6 MB — Compose surfaces, normal |
| `:capture` | 56.4 MB | 114.8 MB | 1.1 MB | 0.9 MB | Overlay + observers |
| `:ml` | **39.1 MB** | 78.3 MB | 0.4 MB | 0.8 MB | SQLCipher DB open, Binder served |
| `:net` | 19.0 MB | 79.9 MB | 0.5 MB | 1.0 MB | Idle gateway |

Device at capture: `MemTotal` 11.3 GB, `MemAvailable` ~2.1 GB (machine
was mid test-campaign; expect 4-6 GB available in normal use).

## Implications for BYOM (M2)

- `:ml` is effectively empty today: the encrypted Room stack costs
  ~39 MB PSS. Nearly the entire model budget is headroom.
- The vision's tiers: Speed ~900 MB (INT4), Intelligence ~1.15 GB
  (1-bit). Loaded as anonymous memory that takes `:ml` to ~0.95-1.2 GB
  PSS — survivable on a 12 GB flagship, fatal on the 4 GB devices the
  vision targets **unless weights are mmap'd**.
- **Requirement for the M2 implementation**: model weights MUST be
  memory-mapped from disk (LiteRT-LM and MLC both support this), so
  pages are file-backed and reclaimable under pressure rather than
  anonymous. PSS then reflects the working set, not the file size.
- Re-measure with this method immediately after the first model loads;
  add the numbers to this doc as a second table.

## Regression tripwire

`app/src/androidTest/java/com/orbit/app/data/MlMemoryFootprintTest.kt`
seeds 1,000 envelopes + continuation results through the real encrypted
(SQLCipher) Room stack, exercises day-page reads, tokenized agent
evidence search, and cluster-candidate queries, and asserts the Java +
native heap delta stays under a deliberately generous ceiling (128 MB).
It exists to catch order-of-magnitude regressions (a cache that pins
the corpus in memory, a leak in a hot query path) — not to police
kilobytes.

**First run (2026-07-06, S24 Ultra): retained heap delta after the full
workload was 0.0 MB.** The Room/SQLCipher read paths stream; nothing
retains the corpus. Conclusion: at 1k-envelope scale the database
contributes essentially zero standing heap to `:ml`, and the entire
model budget (§Implications) is genuinely available.

## Deferred

- `ProfilingManager` OOM/anomaly trigger wiring in `:ml` (AGENTS.md
  memory-pressure note) — pair it with the first real model load in M2,
  when there is something worth heap-dumping.
