# Orbit

**A mobile attention memory system for Android.**

Your phone is full of things you saved because they mattered for a second: screenshots, copied text, links, receipts, recipes, travel details, group-chat plans, product ideas, articles, places, tasks, and half-formed thoughts.

Then they disappear into folders, clipboards, browser tabs, and apps that do not talk to each other.

Orbit turns your screenshot graveyard into a quiet list of things that are still worth doing. It captures from anywhere, preserves why each thing mattered, brings it back through a quiet Diary, and helps close loops with user-confirmed actions.

It is local-first, not local-only. The encrypted on-device corpus is the source of truth. Orbit stays in Basic local mode until the user asks for more context; Smart and Deep understanding can bring in richer local work, public enrichment, or cloud models, but only behind policy, budget, audit, deletion, and local fallback controls.

Repository: `https://github.com/richelgomez99/orbit`

---

## The Product

Orbit is built around a simple promise:

> Save it fast. Orbit helps you finish it later.

The first version is not a chatbot. It is a closure tool for unfinished saves: the product surface should answer which screenshots and captures still need something from you, which can be marked resolved, and which should simply stay searchable.

Orbit should be able to answer questions like:

- What was that restaurant I saved from Instagram last week?
- Which flight confirmation had the arrival time I need?
- What have I been collecting about coding agents?
- Which screenshots look like open loops I still need to handle?
- What else did I save that relates to this article, person, project, or trip?

And when something is actionable, Orbit should propose the next step without silently doing it: add this calendar event, draft this reminder, make this grocery list, summarize this research cluster, or turn these notes into follow-up questions.

---

## How Orbit Works

Orbit starts with capture and builds upward.

1. **Capture from anywhere**
   Save screenshots, copied text, URLs, shared content, and manual notes from Android.

2. **Preserve context**
   Each capture carries time, source, intent, foreground context, activity context where available, and user feedback.

3. **Understand with evidence**
   Orbit starts with Basic local understanding and records what it actually saw: content hashes, OCR, URL metadata, source identity, skipped work, failures, confidence limits, and the completion key needed to resolve the saved intent. Smart or Deep work only happens when the user escalates.

4. **Return through the Diary and Active Intent**
   The Diary is the memory surface. Active Intent is the cleanup surface: not a screenshot list, but a focused queue of saves that may still need action.

5. **Retrieve with citations**
   Ask and search should answer from saved captures with links back to the source material and clear language when the evidence is thin.

6. **Close loops with approval**
   Actions and agent behavior are approval-first. Orbit can draft, suggest, and prepare. The user confirms before anything consequential happens.

---

## Current Focus

The active foundation is **Capture Understanding**, now focused on **Active Intent cleanup**.

A saved item is only useful if Orbit knows what it can truthfully say about it and what would let the user close the loop. The current work turns raw captures into inspectable, evidence-backed objects with a category, source identity, completion key, and active/resolved state.

Basic mode is the default. The phone can capture, hash, OCR, identify local source context, and store evidence without deciding to chase more context on its own. Smart and Deep modes are user-triggered escalations for captures that deserve more work.

That means:

- an Active Intent list for unresolved saved intent, grouped by useful categories like buy later, recipes, receipts, coupons, QR/tickets, places, gifts, read/watch later, chat actions, and maybe old or inactive
- completion keys for each actionable category: product identity, ingredients, decoded QR payload, order ID, date/time/location, coupon code, canonical URL, place identity, or requested action
- source identity that prefers provider URL evidence, then foreground app label, then generic category, then unknown
- content-hash matching for exact or near-identical captures before model interpretation
- canonical URLs for deduplication, provenance, refresh, retrieval, and deletion
- evidence bundles for OCR, metadata, public fetches, parser output, model attempts, skipped work, and failures
- Basic by default, with Smart and Deep escalation only when the user requests more context
- summaries that expose limitations when content is blocked, private, visual-only, metadata-only, or unavailable
- correction feedback for wrong source, wrong summary, bad relevance, or too much context
- deletion and invalidation rules so stale derived understanding cannot outlive the original capture or leak into future answers/actions
- no auto-delete and no default notification pressure; resolved items hide from Active Intent but remain searchable unless the user explicitly deletes them

This work comes before the larger agent. Orbit should not plan, answer, or act from a memory it cannot explain.

---

## Product Stance

Orbit is not a local-only proof of concept, and it is not a cloud app pretending to be private.

The stance is stricter and more useful:

- **Local-first source of truth**: the phone owns the primary record.
- **Cloud as capability layer**: remote models and storage are allowed when they are visible, budgeted, auditable, deletable, and reversible.
- **Evidence before claims**: every summary, answer, relationship, and suggestion should trace back to what Orbit saw.
- **Humility over hallucination**: sparse captures produce sparse claims. When Orbit is unsure, it should say so or ask.
- **Quiet by default**: the product should reduce memory burden, not create notification pressure.
- **User-confirmed action**: proposals can be intelligent; execution needs consent.

---

## What Exists Now

| Area | State |
|------|-------|
| Capture overlay | Android foreground overlay, screenshot and clipboard capture foundations |
| Intent envelope | Local model for why a capture was saved, with diary/audit plumbing |
| Diary | Primary return surface for saved captures |
| Actions | Calendar/todo/share-oriented action groundwork and approval boundaries |
| Duplicate feedback | Already-saved feedback, notes, reclassify, and open-existing flows in progress |
| Cloud gateway | Provider-routing and edge gateway baseline for controlled LLM use |
| Capture understanding | Active build: Basic local defaults, Active Intent cleanup, completion keys, content-hash matching, source identity, evidence, canonical URLs, user-triggered escalation, limitations, deletion safety |

---

## Roadmap

| Order | Work | Why it matters |
|------:|------|----------------|
| 004 | `004-capture-understanding` | Turn saved screenshots and captures into evidence-backed Active Intent |
| 005 | `005-retrieval-and-ask-citations` | Answer from the corpus without losing provenance |
| 006 | `006-approval-action-runtime` | Turn captures into confirmed calendar events, todos, drafts, and follow-ups |
| 007 | `007-memory-candidates-inspector` | Promote, edit, suppress, export, and delete memories intentionally |
| 008 | `008-cloud-controls-storage-budgeting` | Make cloud help visible, limited, auditable, and reversible |
| 009 | `009-kg-backend-poc` | Test graph memory behind adapter boundaries after the memory model exists |
| 010 | `010-agent-coordinator` | Add a single approval-first coordinator over typed Orbit capabilities |
| 011 | `011-manual-compose` | Let users intentionally create or edit captures without relying on the overlay |
| 012 | `012-resolution-semantics` | Represent duplicates, conflicts, stale facts, and merged meanings cleanly |

---

## Architecture

Orbit is an Android-first system with explicit boundaries between capture, storage, model work, network work, and UI.

**Android app**

- Kotlin 2.x
- Jetpack Compose
- Room + SQLCipher
- Android Keystore
- WorkManager
- AIDL/Binder process boundaries
- ML Kit OCR and local model integrations where available

**Cloud and gateway**

- Vercel/Supabase edge gateway for LLM routing
- Supabase Postgres 15 + pgvector for planned cloud storage and retrieval work
- provider-agnostic model routing behind app policy
- content-minimized audit traces for cloud/model attempts

**Boundary rules**

- App package: `com.orbit.app`
- Network clients live under `com.orbit.app.net.*`
- Local database: `OrbitDatabase`
- IPC payloads carry IDs, statuses, labels, summaries, page tokens, and policy decisions
- IPC payloads do not carry raw HTML, screenshots, full text, embeddings, prompts, model responses, or full evidence bundles
- Derived understanding invalidates when its capture, content hash, canonical URL, evidence, or user correction changes, so stale summaries cannot feed hallucinated answers, relationships, or actions

---

## Repository Map

| Path | Purpose |
|------|---------|
| `app/` | Android application |
| `build-logic/lint/` | Custom architecture lint checks |
| `specs/` | Feature specs, plans, tasks, contracts, and research |
| `docs/` | Product audits, architecture notes, planning docs, and follow-ups |
| `supabase/` | Migrations, SQL tests, and edge gateway code |
| `.specify/memory/` | Project constitution and long-lived product context |

---

## Development

Android checks:

```sh
./gradlew compileDebugKotlin
./gradlew testDebugUnitTest
./gradlew lintDebug
```

Gateway checks:

```sh
cd supabase/functions/llm_gateway
npm run typecheck
npm run test:unit
```

Some Android flows require a physical device or emulator because Orbit depends on overlay permissions, foreground app context, screenshots, clipboard behavior, and app process boundaries.

---

## Built By

[Richel Gomez](https://www.linkedin.com/in/richelgomez)

Orbit is being built in public through fast product, architecture, and implementation passes. The current work is about trust: making sure every future intelligent feature is grounded in captures the user can inspect, correct, delete, and understand.

---

## License

All rights reserved for now.
