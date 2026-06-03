import type { MemorySearchResult } from "../types.js";

const SENSITIVE_TERMS = [
  "passport",
  "ssn",
  "social security",
  "credit card",
  "card number",
  "password",
  "api key",
  "access token",
  "recovery code",
];

export function isSensitiveQuestion(question: string): boolean {
  const q = question.toLowerCase();
  return SENSITIVE_TERMS.some((term) => q.includes(term));
}

export function resultContainsSensitiveEvidence(question: string, result: MemorySearchResult): boolean {
  const q = question.toLowerCase();
  const haystack = [
    result.title,
    result.summary,
    result.matchedEvidence.map((e) => e.excerpt ?? "").join(" "),
  ].filter(Boolean).join(" ").toLowerCase();
  if (q.includes("passport")) return haystack.includes("passport");
  if (q.includes("ssn") || q.includes("social security")) {
    return haystack.includes("ssn") || haystack.includes("social security");
  }
  if (q.includes("credit card") || q.includes("card number")) {
    return haystack.includes("credit card") || haystack.includes("card number");
  }
  return SENSITIVE_TERMS.some((term) => q.includes(term) && haystack.includes(term));
}
