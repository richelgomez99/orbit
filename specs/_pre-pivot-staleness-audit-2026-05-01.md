# Pre-pivot staleness audit
Date: 2026-05-01
Auditor: Agent A (read-only pass)
Pivot source of truth: ~/.gstack/projects/richelgomez99-capsule-app/orbit-pivot-plan-2026-04-28.md

## Summary
- Files scanned: 30
- Stale claims found: 31
- High-priority (front-page / spec headline impact): 14
- Medium-priority (body of spec, plan/quickstart): 13
- Low-priority (out-of-date but rarely read): 4

## High-priority findings

### README.md:7
**Current text (verbatim):** "Orbit catches what you screenshot, copy, and save across every app on your phone, understands it on-device, and helps you act on it — a daily diary of what your attention was actually on, calendar events extracted from flight confirmations, to-dos surfaced from screenshots, and eventually natural-language answers across everything you've ever saved."
**Why stale:** Cloud-mode default means Orbit may understand content through cloud LLM calls routed through `:net` to the Vercel AI Gateway; on-device understanding remains only the local-mode path.
**Recommended amendment:** "Orbit catches what you screenshot, copy, and save across every app on your phone, understands it through a cloud-first LLM route with an optional local AI mode, and helps you act on it..."

### README.md:58
**Current text (verbatim):** "| **v2.0+** | Cloud, but on infrastructure *you* own — BYO LLM key, BYO Postgres. Local stays the source of truth, always. | Long-term |"
**Why stale:** The pivot introduces Orbit-managed cloud LLM inference now, via Vercel AI Gateway, not only a long-term BYO-key/BYO-infrastructure future.
**Recommended amendment:** "| **Cloud pivot** | Orbit-managed LLM routing via Vercel AI Gateway, with local AI mode preserved behind `RuntimeFlags.useLocalAi=true`; BYO storage/LLM remains a sovereignty option. | Current pivot |"

### README.md:70
**Current text (verbatim):** "5. User-sovereign cloud escape hatch (LLM) — BYO API key, never Orbit's servers"
**Why stale:** Cloud-mode default uses Orbit-controlled gateway routing rather than requiring user-supplied API keys; prompts traverse Orbit/Vercel infrastructure.
**Recommended amendment:** "5. User-sovereign cloud controls — Orbit-managed gateway by default, optional local AI mode, and future BYO-provider routing for users who want it."

### README.md:77
**Current text (verbatim):** "- **On-device AI:** Gemini Nano 4 (via AICore, from April preview) · ML Kit for OCR"
**Why stale:** Gemini Nano/AICore becomes the local-mode option, while default LLM inference uses Anthropic/OpenAI models through the gateway.
**Recommended amendment:** "- **AI:** Cloud LLM routing by default (Anthropic/OpenAI via Vercel AI Gateway); optional local mode uses Gemini Nano/AICore where available; ML Kit remains on-device for OCR."

### specs/002-intent-envelope-and-diary/plan.md:20
**Current text (verbatim):** "envelope cards. All AI is on-device via Gemini Nano through AICore; no user"
**Why stale:** The claim is universal. After the pivot, default AI inference is cloud-routed; only local mode keeps the AICore/Nano path.
**Recommended amendment:** "envelope cards. By default, AI inference routes through `:net` to the Vercel AI Gateway; local mode uses Gemini Nano through AICore and keeps prompts on-device."

### specs/002-intent-envelope-and-diary/plan.md:69
**Current text (verbatim):** "- All AI on-device (Gemini Nano via AICore). No cloud AI anywhere."
**Why stale:** Directly contradicted by cloud-mode default.
**Recommended amendment:** "- AI routes through the cloud gateway by default; local AI mode preserves Gemini Nano via AICore for users/devices that require on-device inference."

### specs/002-intent-envelope-and-diary/plan.md:91
**Current text (verbatim):** "| I | Local-First Supremacy | All envelope content in SQLCipher. AI on-device only. Only outbound call is `:net` → public HTTPS URLs user captured. Audit log exposes every network call to the user. | ✅ PASS |"
**Why stale:** Default cloud AI adds outbound LLM calls through `:net`; outbound network is no longer limited to public URL hydration.
**Recommended amendment:** "| I | Local-First / Cloud-Routed AI Boundary | Envelope content remains in SQLCipher locally; default AI prompts route through audited `:net` gateway calls, while local mode keeps inference on-device. Audit log exposes URL fetches and LLM gateway calls. | NEEDS UPDATE |"

### specs/002-intent-envelope-and-diary/spec.md:481
**Current text (verbatim):** "- **FR-013**: System MUST NOT transmit raw envelope content, state snapshot"
**Why stale:** This requirement continues on the next lines to ban derived inferences from remote servers. Cloud-mode default sends prompts or prompt excerpts to the gateway.
**Recommended amendment:** "- **FR-013**: In local AI mode, system MUST NOT transmit raw envelope content, state snapshots, or derived inferences to remote servers. In cloud AI mode, system MAY transmit minimally scoped prompt payloads through the audited `:net` gateway path under the configured cloud privacy controls."

### specs/002-intent-envelope-and-diary/spec.md:483
**Current text (verbatim):** "  signals, or any derived inferences (summaries, tags, embeddings) to"
**Why stale:** Cloud-mode default includes cloud summaries/actions/embeddings, so derived inference inputs and outputs are no longer universally local-only.
**Recommended amendment:** "  signals, or derived inferences to remote servers except when cloud AI mode is enabled and the payload is routed through the audited gateway contract."

### specs/002-intent-envelope-and-diary/spec.md:716
**Current text (verbatim):** "  runs on-device. No cloud AI is used anywhere in Orbit v1."
**Why stale:** Cloud AI becomes the default for pivoted v1 work.
**Recommended amendment:** "  runs through the configured LLM route. Orbit defaults to cloud AI via gateway; local mode preserves the on-device Nano path."

### specs/003-orbit-actions/plan.md:122
**Current text (verbatim):** "- All AI on-device. No cloud LLM in the v1.1 path; spec 005 BYOK is"
**Why stale:** Action extraction will use the cloud default unless local mode is enabled.
**Recommended amendment:** "- AI uses the shared LLM router. Cloud mode is default; local mode uses Nano. BYOK remains a future/user-selected provider route."

### specs/003-orbit-actions/plan.md:156
**Current text (verbatim):** "| I | Local-First Supremacy | Proposals are produced on-device by Nano via the existing `LlmProvider` interface. Execution fires Android `Intent`s — IPC only, never network. The audit log records every proposal/confirm/dismiss/execute event locally. No data leaves the device through any 003 code path. | ✅ PASS |"
**Why stale:** Proposal generation can now route through cloud LLMs; prompt data can leave the device via `:net`.
**Recommended amendment:** "| I | Routed AI Boundary | Proposals are produced through `LlmProviderRouter`: cloud gateway by default, Nano in local mode. Execution remains IPC-only and local. Audit rows distinguish proposal route/provider and execution events. | NEEDS UPDATE |"

### specs/005-cloud-boost-byok-llm/spec.md:89
**Current text (verbatim):** "- Orbit never operates its own LLM keys. There is no \"Orbit Plus\" tier that bills for cloud inference."
**Why stale:** The pivot uses Orbit-managed/Vercel AI Gateway credentials and routing. The user does not supply vendor keys for the default cloud path.
**Recommended amendment:** "- Orbit-managed gateway routing is allowed for the default cloud path. Users do not provide vendor keys for this path; BYOK remains optional. Billing/quota policy is tracked separately."

### specs/005-cloud-boost-byok-llm/spec.md:120
**Current text (verbatim):** "- **FR-005-004**: System MUST gate each capability with an independent user toggle persisted in SharedPreferences; the default for every toggle is `LOCAL_NANO`. Available routes per capability: `LOCAL_NANO | ORBIT_MANAGED | BYOK(provider)`."
**Why stale:** The pivot default is cloud (`RuntimeFlags.useLocalAi=false`), not `LOCAL_NANO`.
**Recommended amendment:** "- **FR-005-004**: System MUST route capabilities through `LlmProviderRouter`; default route is cloud gateway unless `RuntimeFlags.useLocalAi=true`, which forces `LOCAL_NANO`. BYOK/provider-specific routing remains an explicit user setting."

## Medium-priority findings

### specs/002-intent-envelope-and-diary/data-model.md:320
**Current text (verbatim):** "    val summary: String?,       // 2-3 sentence Nano summary"
**Why stale:** Summary provenance is no longer necessarily Nano; cloud provider/model metadata must be expressible.
**Recommended amendment:** "    val summary: String?,       // 2-3 sentence LLM summary (cloud by default, Nano in local mode)"

### specs/002-intent-envelope-and-diary/data-model.md:362
**Current text (verbatim):** "| `INFERENCE_RUN` | `NanoClient.*` wrappers | \"Summarized article from {domain}\" / \"Predicted intent from preview\" / \"Generated day summary for {date}\" |"
**Why stale:** Inference events will originate from a router/cloud provider as well as Nano.
**Recommended amendment:** "| `INFERENCE_RUN` | `LlmProviderRouter` / provider wrappers | Include provider, model, route (`cloud_gateway` or `local_nano`), prompt digest, and capability. |"

### specs/002-intent-envelope-and-diary/quickstart.md:91
**Current text (verbatim):** "     with the title + 2–3 sentence Nano summary."
**Why stale:** Default quickstart builds will produce a cloud-generated summary unless local mode is forced.
**Recommended amendment:** "     with the title + 2–3 sentence LLM summary (cloud by default; Nano when local mode is enabled)."

### specs/002-intent-envelope-and-diary/research.md:182
**Current text (verbatim):** "- Principle I forbids cloud fallback, so \"Nano or nothing\" is the only"
**Why stale:** The pivot explicitly permits cloud-mode default and preserves Nano as local mode rather than requiring Nano-or-nothing.
**Recommended amendment:** "- Local AI mode preserves the Nano-or-nothing privacy guarantee. Cloud mode uses the gateway route with explicit audit/provider metadata."

### specs/002-intent-envelope-and-diary/research.md:196
**Current text (verbatim):** "Nano prompts are crafted on-device only. The prompt + user content are"
**Why stale:** Prompt assembly may still happen locally, but default execution sends prompt payloads to the gateway.
**Recommended amendment:** "Prompts are assembled on-device. In cloud mode, minimally scoped prompt payloads cross to `:net` for gateway inference; in local mode, prompt + content remain inside the Nano runtime."

### specs/002-intent-envelope-and-diary/research.md:225
**Current text (verbatim):** "**jsoup + Readability algorithm (ported) in `:net` process; Nano summary in"
**Why stale:** URL hydration summary generation will no longer necessarily be Nano in `:ml`; cloud-mode summaries route through `:net`.
**Recommended amendment:** "**jsoup + Readability algorithm in `:net`; summary generation through `LlmProviderRouter` (cloud gateway by default, Nano local mode).**"

### specs/003-orbit-actions/plan.md:164
**Current text (verbatim):** "| IX | User-Sovereign Cloud Escape Hatch | `LlmProvider.extractActions` accepts `OrbitManaged` / `Byok` provenance like every other Nano call site. Per-capability cloud routing (FR-003-005) is a settings toggle that defaults OFF; falls back to Nano with no feature loss, only quality change. | ✅ PASS |"
**Why stale:** Cloud routing defaults on; Nano is the opt-in/flagged local mode path.
**Recommended amendment:** "| IX | User-Sovereign AI Routing | `LlmProvider.extractActions` routes through cloud by default, can be forced to `LocalNano` with `RuntimeFlags.useLocalAi=true`, and records provider/provenance on every call. | NEEDS UPDATE |"

### specs/003-orbit-actions/quickstart.md:293
**Current text (verbatim):** "| XI | Consent-aware prompts | When BYOK is enabled, the `:agent` consent filter logs every outbound prompt's category set. With BYOK off, no `:net` traffic at all from 003 paths. |"
**Why stale:** BYOK off no longer implies no `:net` LLM traffic; default cloud route still uses `:net`.
**Recommended amendment:** "| XI | Consent-aware prompts | Cloud-mode prompts route through `:net` with provider/model/audit metadata; local mode (`RuntimeFlags.useLocalAi=true`) produces no LLM `:net` traffic from 003 paths. |"

### specs/003-orbit-actions/research.md:123
**Current text (verbatim):** "Phase B — Nano extraction (runs as `ACTION_EXTRACT` continuation"
**Why stale:** Action extraction becomes routed inference, cloud by default, Nano only in local mode.
**Recommended amendment:** "Phase B — routed LLM extraction (runs as `ACTION_EXTRACT` continuation; cloud gateway by default, Nano in local mode)."

### specs/004-ask-orbit/spec.md:16
**Current text (verbatim):** "1. **On-device text embeddings** (Gemini Nano embedding endpoint on capable devices) produced at envelope-seal time, stored in a local vector table."
**Why stale:** Pivot uses OpenAI `text-embedding-3-small` through the gateway for default embeddings.
**Recommended amendment:** "1. **Text embeddings** produced through the embedding route (OpenAI `text-embedding-3-small` via gateway by default; Nano embeddings in local mode when available), stored with provider/model metadata."

### specs/004-ask-orbit/spec.md:47
**Current text (verbatim):** "- **FR-004-004**: System MUST synthesize the response through the `LlmProvider` interface, defaulting to `NanoLlmProvider`. Users with BYOK enabled for Ask Orbit (per spec 005) route synthesis to their configured cloud provider."
**Why stale:** The default provider is cloud, not `NanoLlmProvider`.
**Recommended amendment:** "- **FR-004-004**: System MUST synthesize the response through `LlmProviderRouter`, defaulting to cloud gateway and using `NanoLlmProvider` only when local mode is enabled."

### specs/008-orbit-agent/spec.md:84
**Current text (verbatim):** "- **FR-008-V1-002 (privacy lock, no cloud routing in v1)**: The v1"
**Why stale:** Cloud routing is now part of the pivoted v1 path; this lock conflicts with default cloud AI.
**Recommended amendment:** "- **FR-008-V1-002 (privacy lock, routed AI in v1)**: The v1 agent must use the configured LLM route; local mode forbids cloud routing, cloud mode routes through `:net` with audit/provider metadata."

### specs/012-resolution-semantics/spec.md:184
**Current text (verbatim):** "- **FR-012-022 (Sunday review is the satisfaction beat)**: The weekly digest's body should feel like a small letter-to-self about the week's intent fulfillment. Tone: warm, brief, factual. Generated by Nano on-device per spec 003 FR-003-004."
**Why stale:** Weekly digest generation may route through cloud by default.
**Recommended amendment:** "- **FR-012-022**: Generated through the configured LLM route; cloud by default, Nano when local mode is enabled. Tone remains warm, brief, factual."

## Low-priority findings

### specs/006-orbit-cloud-storage/spec.md:266
**Current text (verbatim):** "    vector VECTOR(768),                  -- computed on-device, uploaded plaintext"
**Why stale:** Default embeddings may be computed by OpenAI through the gateway, not on-device.
**Recommended amendment:** "    vector VECTOR(768),                  -- computed by configured embedding route; stored/uploaded with provider/model metadata"

### specs/007-knowledge-graph/spec.md:63
**Current text (verbatim):** "   a `ContinuationType.ENTITY_EXTRACT` runnable on Nano (default),"
**Why stale:** Entity extraction default becomes cloud route, not Nano.
**Recommended amendment:** "   a `ContinuationType.ENTITY_EXTRACT` runnable through the configured LLM route (cloud default, Nano local mode),"

### specs/010-visual-polish-pass/spec.md:139
**Current text (verbatim):** "- **FR-010-024**: System MUST render `ClusterSuggestionCard` in 6 states. **SURFACED** (default): ✦ + body italic + action row with hairline rules. **ACTING** (during Nano 4 inference, ~2-3 s after Summarize tap): action labels replaced by Newsreader italic 14 sp ellipsis cycling at 600 ms intervals; ✦ does not animate (remains anchored). No spinner, no skeleton shimmer (Material chrome banned). **FAILED** (Nano returns error/null/timeout): body copy replaced by Newsreader italic 16 sp `Orbit couldn't reach all 4 captures. Try again?` with a single ↻ retry affordance in the action row position. After 3 retries: Berkeley Mono 10 sp `--ink-faint` `Retried. Try again later, or open captures individually.` **STALE** (>6 h old when user opens Orbit): action row's right margin gains a Berkeley Mono 10 sp `· 9:14A` timestamp marker in `--ink-faint`. **REPEAT-TAP**: 1 s debounce; second tap during ACTING is a visual no-op (ellipsis continues). **SLOW-NETWORK** (one or more constituent URL captures never hydrated): body soft-degrades to `3 of 4 captures synthesized. The 4th couldn't be reached.` — the card never lies about coverage."
**Why stale:** UI copy/state semantics are pinned to Nano and 2-3s local inference; cloud latency and failure modes differ.
**Recommended amendment:** "Replace provider-specific `Nano` wording with configured-route wording (`LLM inference`, `provider unavailable`, `gateway timeout`, `local Nano unavailable`) and revisit latency/state copy for cloud mode."

### specs/012-resolution-semantics/spec.md:258
**Current text (verbatim):** "- **FR-012-031 (ACTING is disk-persisted)**: When the user taps Summarize, the cluster transitions to `ACTING` and the new state is **written to disk before** Nano inference begins. This guarantees that backgrounding the app mid-inference resumes correctly: on foreground, the user sees either the ACTED result OR a \"tap to retry\" affordance, never a frozen ACTING ellipsis."
**Why stale:** Cluster summarize inference is not necessarily Nano; default route may be cloud gateway.
**Recommended amendment:** "...before configured LLM inference begins..."

## Files scanned (full list with path + line count)
- specs/001-core-capture-overlay/data-model.md — 175 lines
- specs/001-core-capture-overlay/plan.md — 114 lines
- specs/001-core-capture-overlay/quickstart.md — 179 lines
- specs/001-core-capture-overlay/research.md — 387 lines
- specs/001-core-capture-overlay/spec.md — 326 lines
- specs/001-core-capture-overlay/tasks.md — 269 lines (skimmed for context only)
- specs/002-intent-envelope-and-diary/data-model.md — 530 lines
- specs/002-intent-envelope-and-diary/plan.md — 302 lines
- specs/002-intent-envelope-and-diary/quickstart.md — 261 lines
- specs/002-intent-envelope-and-diary/research.md — 664 lines
- specs/002-intent-envelope-and-diary/spec.md — 733 lines
- specs/002-intent-envelope-and-diary/tasks.md — 957 lines (skimmed for context only)
- specs/003-orbit-actions/data-model.md — 465 lines
- specs/003-orbit-actions/plan.md — 339 lines
- specs/003-orbit-actions/quickstart.md — 313 lines
- specs/003-orbit-actions/research.md — 509 lines
- specs/003-orbit-actions/spec.md — 88 lines
- specs/003-orbit-actions/tasks.md — 637 lines (skimmed for context only)
- specs/004-ask-orbit/spec.md — 63 lines
- specs/005-cloud-boost-byok-llm/spec.md — 146 lines
- specs/006-orbit-cloud-storage/spec.md — 447 lines
- specs/007-knowledge-graph/spec.md — 187 lines
- specs/008-orbit-agent/spec.md — 357 lines
- specs/009-byoc-sovereign-storage/spec.md — 219 lines
- specs/010-visual-polish-pass/spec.md — 173 lines
- specs/011-manual-compose/spec.md — 134 lines
- specs/012-resolution-semantics/spec.md — 376 lines
- specs/contracts/envelope-content-encryption-contract.md — 174 lines
- specs/contracts/orbit-cloud-api-contract.md — 179 lines
- README.md — 122 lines

## Files with no stale claims (clean — good)
- specs/001-core-capture-overlay/data-model.md — Phase 1 has no AI inference architecture.
- specs/001-core-capture-overlay/plan.md — Offline/no-network claims are scoped to the capture overlay primitive, not all future AI.
- specs/001-core-capture-overlay/quickstart.md — No cloud-pivot-relevant AI claims.
- specs/001-core-capture-overlay/research.md — Android service/clipboard research remains scoped to capture mechanics.
- specs/001-core-capture-overlay/spec.md — No universal AI routing claim.
- specs/003-orbit-actions/data-model.md — Provenance field already allows non-Nano routes.
- specs/003-orbit-actions/spec.md — On-device-only claims are about AppFunction execution/tool invocation, not LLM inference routing.
- specs/009-byoc-sovereign-storage/spec.md — BYOC storage process/secret claims remain compatible with cloud LLM routing.
- specs/011-manual-compose/spec.md — Manual composition no-network claims are scoped to the compose action itself, not post-seal AI routing.
- specs/contracts/envelope-content-encryption-contract.md — Encryption/upload boundaries remain compatible with gateway inference if prompt payloads are separately contracted.
- specs/contracts/orbit-cloud-api-contract.md — Already includes `intent_source: user|nano|managed|byok`; no direct on-device-only LLM claim found.
- specs/*/tasks.md — skimmed only for cross-reference context per instructions; no findings emitted from task files.

## Specs that may need a section-level rewrite (not just inline amendments)
- README.md — Front-page positioning is still on-device/Nano/BYOK-future; pivot makes cloud LLM default now.
- specs/002-intent-envelope-and-diary/plan.md — Technical context, constitution table, process tree, and complexity/risk sections assume all AI is local and only URL hydration touches `:net`.
- specs/002-intent-envelope-and-diary/research.md — Process architecture and Gemini Nano sections are built around Principle I forbidding cloud fallback; this needs a new routed-AI interpretation rather than isolated line edits.
- specs/002-intent-envelope-and-diary/spec.md — Privacy/network FRs and assumptions need a cohesive cloud-mode/local-mode split.
- specs/003-orbit-actions/plan.md — Action extraction plan assumes Nano-only v1.1 and `:net` untouched; pivot changes proposal-generation transport and audit/provenance requirements.
- specs/003-orbit-actions/research.md — Action extraction and date parsing decisions are framed as Nano-first; needs a routed-provider update.
- specs/004-ask-orbit/spec.md — Embeddings and synthesis defaults change materially from Nano to OpenAI/cloud gateway.
- specs/005-cloud-boost-byok-llm/spec.md — Existing cloud model is opt-in/BYOK/local-default; pivot makes Orbit-managed gateway default and should likely supersede this spec or become its amendment.
- specs/008-orbit-agent/spec.md — Agent privacy lock and prompt routing sections conflict with cloud-by-default v1 inference.
- specs/010-visual-polish-pass/spec.md — Cluster card state copy is provider/latency-specific to Nano; cloud gateway states need provider-neutral copy.
- specs/012-resolution-semantics/spec.md — Weekly digest and cluster ACTING/FAILED semantics should be provider-neutral.
