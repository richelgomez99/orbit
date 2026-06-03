import { describe, expect, it } from "vitest";
import { mergeHybridResults } from "../lib/hybridSearch.js";
import type { MemoryIndexItem } from "../types.js";

function item(id: string, text: string, title = id): MemoryIndexItem {
  return {
    userId: "user-1",
    envelopeId: id,
    schemaVersion: 1,
    kind: "REGULAR",
    dayLocal: "2026-05-30",
    createdAtMillis: 1780150000000,
    intent: "REFERENCE",
    contentType: "text",
    title,
    summary: text,
    sourceAppLabel: "Chrome",
    appCategory: "browser",
    tags: [],
    evidence: [
      {
        kind: "compact_clue",
        label: "Summary",
        excerpt: text,
        source: "derived",
      },
    ],
    compactText: text,
    localContentHash: id,
    updatedAtMillis: 1780150000000,
    tombstonedAt: null,
  };
}

describe("hybrid search", () => {
  it("lets semantic results win when the query uses a related form", () => {
    const dentist = item(
      "env-dentist-reschedule",
      "Dental office message about rescheduling the appointment after the original slot changed.",
      "Dentist appointment update",
    );
    const lexicalNoise = item(
      "env-reschedule-noise",
      "Remember to reschedule a package pickup.",
      "Package pickup",
    );

    const results = mergeHybridResults(
      "reschedule dentist",
      [lexicalNoise],
      [{ item: dentist, semanticScore: 0.91 }],
      5,
    );

    expect(results[0]?.envelopeId).toBe("env-dentist-reschedule");
    expect(results[0]?.retrievalMode).toBe("hybrid");
  });

  it("falls back to lexical-only results when no vector result exists", () => {
    const qr = item(
      "env-qr-customers",
      "Context note says this is the QR code for first 1000 customers.",
      "How to get first 1000 customers QR code",
    );

    const results = mergeHybridResults("qr code", [qr], [], 5);

    expect(results[0]?.envelopeId).toBe("env-qr-customers");
    expect(results[0]?.retrievalMode).toBe("lexical_only");
  });
});
