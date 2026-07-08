# Orbit — Agentic Memory Roadmap & Ground Truth (2026-07-07)

**Status:** Active planning brief. Cross-cutting — spans spec-007 (memory candidates),
spec-009 (KG backend), spec-020 (curious agent / profiling), spec-022 (local models).
**Audience:** Any agent (or a future compacted self) picking up the agentic-memory work.

## How to use this document

This is the authoritative brief for the "make Orbit an agent that knows you" body of
work. It captures the vision, the phased plan, the July-2026 research behind every
model/runtime choice, and the decisions made with the product owner (Richel).

**Ground-truth discipline (important):** code drifts; this doc can go stale. Every
concrete claim here was verified against the codebase on 2026-07-07 (file paths/line
anchors given where they matter). Before you *rely* on any specific seam, **re-verify it
against the current code** — if it moved, trust the code and update this doc. Sections
flagged `⚠ VERIFY` are the ones most likely to drift or that were asserted with lower
confidence. If you research something that was vague here, fold the answer back in.

**Two shipped artifacts summarize this for humans:**
- Internal strategy roadmap: https://claude.ai/code/artifact/234560d1-e05b-4a29-9006-9aa20f94d6f7
- Consumer landing page (the *vision*, marketing voice): https://claude.ai/code/artifact/9a436474-7353-4521-86b3-e2ad34beafb6

---

## 1. The vision, in one paragraph

Orbit already captures everything, but it doesn't *understand* any of it. The goal is a
living, on-device knowledge graph of the user — fed by a triage-gated agent at capture
seal, surfaced through semantic recall, and acted on proactively (including drafting
conversations and adding plans to the calendar). Every frustration observed this session
(recall surfaces nothing, empty profile, curiosity that doesn't know you, a recipe filed
as a plain note, patterns that group a concert with work events) is the same missing
organ: **nothing turns what you save into what Orbit knows.** Add that organ and it
becomes a flywheel — the more it learns about you, the sharper it gets at learning more.

## 2. Current state — the vessel is built, it's empty

Verified 2026-07-07. **What's built and wired:**
- **Knowledge graph** (spec-009): real triple store — `graph_entity` / `graph_fact` /
  `graph_relationship` + provenance + feedback. `graph/RoomGraphBackendAdapter.kt`
  (`writeFact` **rejects facts with no provenance**, `invalidateBySource`, `whyThis`),
  `graph/GraphRepositoryDelegate.kt` (`projectPromotedMemory`), `data/dao/GraphDao.kt`.
  Entity IDs are deterministic: `"graph-entity-" + normalize(name)`.
- **Profile facts + review** (spec-007): `promoted_memory` (subject/predicate/object),
  `memory_candidate` + support. `data/MemoryRepositoryDelegate.kt` — `acceptCandidate`
  inserts a `PromotedMemoryEntity` then projects into the graph. The **only** current
  path that creates candidates is `debugSeedDemoMemoryCandidates()` (a debug seed).
- **User plans / "loops to close":** `active_intent` (`data/entity/ActiveIntentEntity.kt`)
  is the durable representation; drives the Orbit workspace. Agent plans (spec-010) are
  **ephemeral** (held in `DiaryViewModel._agentPlanState`, no table).
- **On-device LLM seam:** `LlmProvider` interface; `embed()` exists but returns `null` on
  all local providers today. BYOM/local Gemma proven (spec-022) — see §6.

**What's missing (the gap):** nothing populates the graph in production. Candidates only
come from the debug seed; `recordCuriousAnswer` is a no-op stub; curious reads only
envelopes + promoted memories, never the graph; recall is keyword-only.

## 3. The keystone bet — LiteRT-LM + Gemma 4 E2B

Three of four research threads converged here. Google shipped **Gemma 4** (April 2026),
natively multimodal (text+image+audio, E2B/E4B), and put MediaPipe `tasks-genai` into
**maintenance-only**, steering to **LiteRT-LM**. Finishing the migration (already started
in spec-022: `LiteRtLmProvider.kt`, catalog `GEMMA_4_E2B_LITERTLM`) unlocks three goals
at once: **multimodal screenshots**, **reliable tool-routing via constrained decoding**,
and **classification that actually works** (the 1B failed the M2 tests). This is the
backbone the roadmap hangs on.

## 4. The flywheel & the triage-gated tool agent (the design shape)

`capture → graph-aware triage → one tool → living graph → sharper triage/recall/curiosity`

**Triage is THREE tiers** (cheapest first; keeps the model off most captures):
1. **Deterministic gate** (no model) — heuristics like the existing
   `ai/extract/ActionExtractionPrefilter.kt`: text substance, has-URL, category != UNKNOWN,
   has a completion key, recurring topic, source. Drops the throwaway majority for free.
2. **Graph-relevance** (NEW) — does the capture touch an entity/topic the graph already
   knows the user cares about? Cheap exact path reuses deterministic entity IDs
   (`GraphDao.getEntity`); no new queries needed for exact match. Catches *personal gems*.
3. **Model judgment primed with graph context** (NEW, DEFERRED) — for the still-uncertain,
   ask the local model with a compact "what this user cares about" summary. Gated on
   constrained decoding (not available today — see §6). No-op until then.

**Tools (closed set, agent picks exactly one):**
- `save_to_memory(fact)` → the existing candidate → promoted → graph path.
- `add_to_pattern(topic)` → contribute to a pattern (needs a lightweight topic counter;
  no synchronous "add to pattern" API exists today — cluster subsystem is async/worker).
- `draft_conversation(prompt)` → **NEW durable store**; a card the agent leaves for when
  the user next opens Orbit (proactive agency, vision item).
- `ignore` → the common outcome.

**Cold start:** the graph is empty on day one, so tiers 2–3 match nothing. Bootstrap from
explicit signals — `IntentSource.USER_CHIP` (confidence 1.0), curious answers, completion
keys — so the system is useful *before* it's smart, then graph-aware tiers take over.

**Quality gate (deterministic, precision-first):** auto-promote only high-confidence
(≥0.85), non-sensitive, allowlisted-predicate facts; everything else → review candidate.
Sensitivity is deterministic (redaction markers from `SensitivityScrubber`, financial
completion keys, category) — do **not** call the LLM `scanSensitivity` (M2: over-triggers).
Provenance is mandatory (the graph write path enforces it).

## 5. Phases & parallel tracks

- **Phase A — Triage-gated memory agent** (task #24). Ships **deterministic** (no model
  dependency): triage gate at seal + rule-based router + `save_to_memory` (two seed fact
  types) + `draft_conversation` store. First real ingestion → the graph starts filling.
  Wiring point: `EnvelopeRepositoryImpl.persistBasicUnderstanding` currently **discards**
  the `BasicUnderstandingResult` (~lines 388, 960, 1387) — that's the seam. Runs in `:ml`,
  no network. Full detailed design lives in the completed Plan-agent output referenced in
  the session; reproduce from this doc + the code anchors above.
- **Phase B — Semantic recall** (task #25). EmbeddingGemma-300m @256d + brute-force cosine
  in the existing SQLCipher Room (new `vector BLOB` col, migration v9) + RRF fusion with
  existing keyword/graph. Makes "Ask saved memory" answer semantic/profile questions.
- **Phase C — Curious closes the loop** (task #26). Curious reads graph *gaps*; answers
  write provenance-backed facts (`recordCuriousAnswer` → candidate → graph); persist
  dismissals/answers across process death (in-memory today).
- **Phase D — Persistent planning + proactivity** (task #27). Persist agent plans;
  pattern-triggered conversation drafts; **calendar-from-plan** (see §8).
- **Track: Infrastructure** — complete LiteRT-LM + Gemma 4 E2B migration (the keystone).
- **Track: Multimodal** — screenshots (ML Kit OCR tier 0 → Gemma 4 E2B tier 1), then voice
  (sherpa-onnx + Whisper base.en, a *separate* ONNX engine, later).

## 6. July-2026 on-device research — selections & rationale

All scoped to a 12GB Snapdragon flagship (S24/S25 Ultra), privacy-preserving/local.

| Job | Selection | Why (mid-2026) |
|---|---|---|
| Generation / screenshots / tool-routing | **Gemma 4 E2B · LiteRT-LM** | Native multimodal, <1.5GB, NPU; tasks-genai is maintenance-only. |
| Reliable tool selection | **Constrained decoding** (LiteRT-LM native FC / AI Edge FC SDK, Gemma-only) | Constrain to one of 4 tool tokens. **⚠ VERIFY: not available on the current stack** — `tasks-genai 0.10.35` session sampling *hung*; ship deterministic routing first. Note: constrained decoding guarantees *format*, not correct *choice* (FunctionGemma 58%→85% only after fine-tune). |
| Semantic recall embeddings | **EmbeddingGemma-300m @256d** (Matryoshka), LiteRT build | SOTA sub-500M on-device embedder; ~1KB/note. `embed()` seam exists (returns null). |
| Vector storage | **Room + brute-force cosine** | ANN unnecessary at this corpus size; BLOB col in the existing encrypted DB, sub-ms scan, zero new native libs. (sqlite-vec = SQLCipher-compat risk; ObjectBox = second DB.) |
| Screenshot text (cheap tier) | **ML Kit Text Recognition v2** | On-device, ~4MB, instant. Most screenshots are text; VLM only when OCR isn't enough. |
| Screenshots (rich tier) | **Gemma 4 E2B** (image + OCR hint, 280 img-token budget) | Pass image to `:ml` as PFD/downscaled bytes (Binder ~1MB limit). |
| Voice notes (future) | **sherpa-onnx + Whisper base.en** (or Moonshine Base) | ONNX Runtime, ~51× faster than whisper.cpp on Android; ~1–4s/note; **independent of the Gemma engine** (doesn't touch one-engine-per-process); reuses the whole text pipeline. |

Sources captured in the session research (Gemma 4 / LiteRT-LM Google blogs, FunctionGemma,
EmbeddingGemma HF, VoicePing/Moonshine ASR benchmarks, XGrammar-2). Re-search if a claim
is load-bearing and >a few months old — this field moves fast.

## 7. The intelligence layer — semantic grouping (task #28)

**Problem found on-device (2026-07-07):** the Curious Agent groups saves by **broad
IntentCategory** (`DiaryViewModel.buildCuriousSignals` sets `topicKey = item.categoryLabel`).
So a concert ticket clusters with startup/founder events (all `EVENT_TICKET_RESERVATION`),
and a grinder order + flight + headphones clump as `RECEIPT_OR_ORDER`. The questions become
incoherent (no single right answer) — which is why the fixed Work/Project/Research/Reference
choices felt wrong.

**Fix (rides the Phase B embedding keystone):** group by **semantic similarity** via
EmbeddingGemma instead of category. The existing `ClusterDetector` + `cluster/SimilarityEngine.kt`
already do embedding clustering — starved because `embed()` returns null. One embedding model
fixes grouping **and** recall **and** graph-relevance triage.

**Traceability is the #1 principle (elevated by the product owner):** *nothing Orbit infers
is un-inspectable.* Browseable sources on curious questions (shipped, commit `378279e`) is
what let the user catch the bad grouping. Next: a "these don't belong together" correction
that becomes a signal improving future grouping. Human-in-the-loop keeps the intelligence
honest. Apply this to every inferred thing: patterns, facts, recall answers.

## 8. Calendar integration — NOT complicated, already scaffolded

You do **not** integrate per-vendor. Android's single `CalendarContract` provider covers
Google/Samsung/Outlook via whatever accounts the user has synced. The codebase **already
has** `action/handler/CalendarActionHandler.kt` — fires `Intent.ACTION_INSERT` against
`CalendarContract.Events`, registered as `calendar.createEvent` in `ActionHandlerRegistry`
and `BuiltInAppFunctionSchemas`. The Intent path needs **zero permissions and zero network
from Orbit** (the user's calendar app does its own sync) — privacy-clean. What's missing:
wiring it to the **weekly plan / event facts** so Orbit *offers* "add to calendar." Highest-
ROI action (puts Orbit into a tool users live in). Belongs in Phase D. No owner setup needed
(no API keys/OAuth).

## 9. Privacy — the honest architecture (do not oversell)

`⚠ Correct any marketing that says "nothing in the cloud."` Verified defaults in
`settings/PrivacyPreferences.kt` (2026-07-07):
- `cloudAiRoutingEnabled` = **true** (default ON)
- `cloudAskSynthesisEnabled` = **true** (default ON)
- `memoryIndexingEnabled` = **false** (default OFF)
- `localAiEnabled` = **false** (default OFF)

**True and strong (architecturally enforced):** raw corpus, KG, and profile live in the
`:ml` process, which is **manifest-barred from the network** (a lint rule fails the build
if an HTTP client is constructed outside `:net`). Cross-process data goes as **compact
derived evidence**, never raw corpus. "The part that reads your content can't reach the
internet" is literally true.

**Overstated:** "nothing leaves the phone." By default, cloud AI routing + cloud Ask
synthesis are ON and send *derived* text (a question, a summary) to a gateway via `:net`.
It's minimized (never raw saves) and toggleable, and with `localAiEnabled` on + cloud
routing off it's fully local — but that's not the default yet. Honest framing:
**"Local-first, private by architecture; optional cloud help sends only derived snippets,
never your raw captures, and can be turned off."**

## 10. Session state (2026-07-07) — commits, tasks, what shipped

Shipped & device-validated this session (branch `feature/022-local-model-manager-20260614`):
- Orbit workspace Quiet-Almanac unification; Curious Agent lit up (spec-020).
- Library type badge — recipe reads as RECIPE not a note (`71aa64d`); classifier coverage
  widened + structural recipe signal (`a0e0462`, device-validated).
- Curious sources **browseable** + **custom "Something else" answer** (`378279e`).
- Roadmap + landing artifacts (§ links).

Open harness tasks: **#24** Phase A (in progress), **#25** Phase B, **#26** Phase C,
**#27** Phase D, **#28** semantic grouping.

**Recommended next build:** the EmbeddingGemma seam — it unlocks semantic grouping (#28),
recall (Phase B), and graph-relevance triage together. Or start Phase A deterministic
(fills the graph now, no model dependency). Both are defensible; A gives data to grow, the
embedding seam gives intelligence to use it.

## 10b. `embed()` implementation — definitive path (researched 2026-07-08)

The retrieval math is now in-repo (`memory/retrieval/ReciprocalRankFusion`,
`VectorSearch.cosineTopK`; clustering already exists in `cluster/SimilarityEngine`
+ `ClusterDetector`). The one remaining crux is a working `embed()` (returns null
on all local providers today). Verified path:

- **NOT LiteRT-LM** (`litertlm-android`): generation-only, no Kotlin embed/encode API.
- **NOT MediaPipe `tasks-text` `TextEmbedder`**: real API but cannot load
  EmbeddingGemma (metadata/tokenizer format mismatch; EmbeddingGemma support is an
  open MediaPipe issue). Only loads weak USE-class models.
- **USE the LiteRT `Interpreter`** (raw TFLite) + `litert-community/embeddinggemma-300m`
  `embeddinggemma-300M_seq256_mixed-precision.tflite` (~180MB, HF-gated) + a
  SentencePiece/DJL (`ai.djl.huggingface:tokenizers`, native `.so`) tokenizer.
  Deps: `com.google.ai.edge.litert:litert:1.4.0` (+ `litert-gpu` optional).
  **No single-engine-per-process constraint** — coexists with the MediaPipe engine
  in `:ml` (unlike `LlmInference`). New `EmbeddingGemmaProvider` in `:ml`.
- **Pipeline:** prefix (`task: search result | query: {text}` for queries;
  `title: none | text: {text}` for documents — **prefixes are load-bearing**) →
  tokenize → `Interpreter.run` → likely `[1,768]` pooled + L2-normalized in-graph →
  Matryoshka-truncate to 256 → **re-normalize**.
- **Must verify on-device (S24 Ultra):** (1) input signature (`[1,256]` int vs
  `{input_ids, attention_mask}`), (2) output rank (`[1,768]` pooled vs `[1,256,768]`
  → mean-pool yourself), (3) whether L2-norm already applied, (4) DJL tokenizer `.so`
  loads in `:ml` + APK-size delta, (5) GPU-delegate coexistence with MediaPipe (CPU
  is the safe default for a 300M embedder), (6) prompt prefixes present.
- **Fallback if the Interpreter path stalls:** `com.google.ai.edge.localagents:localagents-rag`
  ships `GeckoEmbeddingModel(modelPath, tokenizerPath, useGpu)` — but that's **Gecko**,
  not EmbeddingGemma (different quality/dims); use only to get *something* working.
- Model download reuses the existing `ModelDownloadStore` / `LocalModelsActivity`
  manager (same per-repo license-accept flow as the Gemma weights).

## 11. `⚠ VERIFY` before relying (drift-prone / lower-confidence)

- Constrained-decoding availability on the *current* on-device stack (verdict: NO as of
  the session; re-check the tasks-genai / LiteRT-LM versions in `app/build.gradle`).
- Exact seam line numbers in `EnvelopeRepositoryImpl.kt` — anchor by symbol, not line.
- Whether `add_to_pattern` has gained a synchronous API since (was none).
- Gemma 4 / LiteRT-LM API surface for vision + function-calling — moving fast; re-read
  current Google AI Edge docs before implementing multimodal or tool-calling.
