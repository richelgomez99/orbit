import { afterEach, describe, expect, it, vi } from "vitest";
import { buildGroundedAskAnswer } from "../lib/groundedAsk.js";
import type { MemorySearchResult } from "../types.js";

const receipt: MemorySearchResult = {
  envelopeId: "env-flight",
  rank: 1,
  score: 8,
  title: "Flight receipt",
  summary: "Flight receipt for NYC to San Francisco.",
  dayLocal: "2026-05-30",
  createdAtMillis: 1780150000000,
  intent: "REFERENCE",
  sourceAppLabel: "Gmail",
  matchedEvidence: [
    {
      kind: "SUMMARY",
      label: "Summary",
      excerpt: "Flight receipt: NYC to San Francisco, confirmation ORB123.",
      source: "derived",
    },
  ],
  retrievalMode: "hybrid",
};

const numberNoise: MemorySearchResult = {
  ...receipt,
  envelopeId: "env-number-noise",
  title: "Tailoring time",
  summary: "Tailoring should take 15 minutes.",
  matchedEvidence: [
    {
      kind: "SUMMARY",
      label: "Summary",
      excerpt: "Tailoring should take 15 minutes, not two hours.",
      source: "derived",
    },
  ],
};

describe("grounded Ask", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    delete process.env.OPENAI_API_KEY;
  });

  it("refuses sensitive identifier questions without exact evidence", async () => {
    const answer = await buildGroundedAskAnswer("What is my passport number?", [numberNoise], false);
    expect(answer.status).toBe("sensitive_refusal");
    expect(answer.citations).toHaveLength(0);
  });

  it("returns extractive cited answers without synthesis", async () => {
    const answer = await buildGroundedAskAnswer("Which flight receipt did I save?", [receipt], false);
    expect(answer.status).toBe("answered");
    expect(answer.citations[0]?.envelopeId).toBe("env-flight");
    expect(answer.modelLabel).toBe("hybrid/extractive");
  });

  it("uses OpenAI synthesis only after cited retrieval passes", async () => {
    process.env.OPENAI_API_KEY = "test-key";
    vi.stubGlobal("fetch", vi.fn(async () => new Response(JSON.stringify({
      output_text: "You saved the NYC to San Francisco flight receipt.",
    }), { status: 200 })));

    const answer = await buildGroundedAskAnswer("Which flight receipt did I save?", [receipt], true);
    expect(answer.status).toBe("answered");
    expect(answer.answer).toContain("NYC to San Francisco");
    expect(answer.modelLabel).toMatch(/^openai:/);
    expect(fetch).toHaveBeenCalledOnce();
  });
});
