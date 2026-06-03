import { describe, expect, it } from "vitest";
import { isSensitiveQuestion, resultContainsSensitiveEvidence } from "../lib/sensitiveQuestionPolicy.js";
import type { MemorySearchResult } from "../types.js";

describe("sensitive question policy", () => {
  it("detects sensitive identifier questions", () => {
    expect(isSensitiveQuestion("What is my passport number?")).toBe(true);
    expect(isSensitiveQuestion("Do I have a saved access token?")).toBe(true);
    expect(isSensitiveQuestion("Which recipe did I save?")).toBe(false);
  });

  it("requires exact passport evidence for passport questions", () => {
    expect(resultContainsSensitiveEvidence("What is my passport number?", result("Passport renewal form"))).toBe(true);
    expect(resultContainsSensitiveEvidence("What is my passport number?", result("Tailoring should take 15 minutes"))).toBe(false);
  });

  it("matches SSN and social security evidence", () => {
    expect(resultContainsSensitiveEvidence("What is my SSN?", result("Social Security card scan"))).toBe(true);
    expect(resultContainsSensitiveEvidence("What is my social security number?", result("SSN document"))).toBe(true);
  });

  it("matches credit card evidence without treating generic numbers as enough", () => {
    expect(resultContainsSensitiveEvidence("What is my credit card?", result("Credit card statement"))).toBe(true);
    expect(resultContainsSensitiveEvidence("What is my card number?", result("Order number 12345"))).toBe(false);
  });

  function result(text: string): MemorySearchResult {
    return {
      envelopeId: "env-sensitive",
      rank: 1,
      score: 8,
      title: text,
      summary: text,
      dayLocal: "2026-05-30",
      createdAtMillis: 1780150000000,
      intent: "REFERENCE",
      matchedEvidence: [
        {
          kind: "SUMMARY",
          label: "Summary",
          excerpt: text,
          source: "derived",
        },
      ],
    };
  }
});
