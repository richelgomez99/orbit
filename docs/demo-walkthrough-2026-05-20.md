# Orbit Class Demo Walkthrough - 2026-05-20

## 30-second opener

Orbit is a personal AI agent for Android: a mobile attention memory system that catches what I save across apps, preserves why it mattered, and brings it back through a quiet Diary when it can help me understand, retrieve, or act.

I built it because every modern phone is a graveyard of intentions. Screenshots, copied links, recipes, confirmations, group-chat snippets: they mattered for a second, then they disappeared. Orbit is the inverse: your attention compounds for you.

## 6-8 minute flow

1. Open Orbit to the Diary.
   Say: The Diary is the return surface. Orbit is not a generic list of screenshots and it is not a chatbot-first product. It is a daybook of what my attention was actually on.

2. Show `Needs follow-up`.
   Say: This panel is the first visible agentic loop. Orbit is looking at captures as intent envelopes: where they came from, what text or source clues exist, and whether they might still represent motion.

3. Collapse and reopen the panel.
   Say: I added this because the queue can get overwhelming. The header gives me the gist, and I can hide it when I just want the diary.

4. Use filters: `Needs context`, `Ready`, `Maybe old`.
   Say: These filters make review faster. Needs context means Orbit needs a human hint. Ready means I can decide now. Maybe old means it may be safe to clear.

5. Tap `View capture`.
   Say: Source of truth matters. The original capture stays inspectable; Orbit's understanding is attached to it, not a replacement for it.

6. Tap `Add context` and save a note.
   Say: If Orbit cannot know why this matters, I can add the missing human context. That turns the system from “AI guessed something” into a collaborative loop.

7. Tap `Ask Orbit`.
   Say: Right now this writes a local decision brief and audit event. The cloud gateway contract is ready for a real model-backed review, but I am keeping the phone build conservative until auth and keys are configured safely.

8. Resolve a row with handled / not needed.
   Say: Orbit does not execute external actions silently. Rows leave the queue only after I decide.

## LLMs, Agents, And Tools

- In the phone build shown today, Basic understanding is deterministic and local. It does not call an LLM.
- The cloud gateway path is prepared for `active_intent_review` using Anthropic Haiku through the existing authenticated gateway, with strict compact-context validation.
- The broader gateway also has model routing for Anthropic Claude Sonnet/Haiku and OpenAI embeddings, but real dispatch from this Active Intent button is still gated behind auth/session and provider keys.
- For development, I used GitHub Copilot in VS Code, Speckit-style planning, Gradle/Kotlin tests, ADB device testing on an S24, and Vitest/typecheck for the TypeScript gateway.
- I would describe the current product as agentic but not autonomous: it observes saved captures, proposes what may need attention, asks for context, and records decisions. The next step is user-confirmed action drafts, not silent execution.

## Why This Project

I chose Orbit because the phone is the place where attention leaks. We save things because they might matter later, but the OS gives us infinite capture and almost no surfacing. Orbit's bet is capture -> intent -> surface: if the system preserves why something mattered at save time, it can later become memory, retrieval, and user-confirmed action.

## Challenges

- Android multi-process boundaries: capture, UI, data, and network code have to stay separated so raw screenshots and large evidence do not leak across Binder.
- Device reality: the same build behaved differently across devices, and screenshot observation/OCR timing had to be validated on the actual S24.
- UX trust: early versions were technically correct but not user-friendly. The app showed classifier language before showing the capture, which made the experience feel opaque.
- Scope control: it is tempting to jump straight to a full agent, but the product needs capture grounding, context, resolution, and confirmation first.
- Privacy and cloud readiness: I want cloud AI where it helps, but not by casually sending raw screenshots or secrets around.

## Next Work

- Wire the real model-backed Active Intent review through the authenticated gateway once keys and sessions are configured outside chat.
- Add confirmed action drafts: calendar event, generic to-do, reminder, reading list, or save-for-later item.
- Improve source/app recognition so screenshots from YouTube, messages, browsers, and shopping apps get better names and reasons.
- Add feedback controls so users can say “wrong reason,” “not useful,” or “this is still important.”
- Keep tightening the demo loop: capture -> understand -> view source -> add context -> review -> decide.

## Feedback To Ask The Group

- Does this feel more useful as a cleanup queue, a memory/search app, or an action assistant?
- When should Orbit ask for context versus just staying quiet?
- Which action draft would be most impressive and useful next: calendar, to-do, reminder, or message draft?
- Does the product feel trustworthy when it shows source, clue, and why before asking you to act?

## Backup One-liner

Orbit is a personal AI agent for Android that turns saved attention into memory: capture from anywhere, preserve intent, and surface the right thing when I can act on it.
