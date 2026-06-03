import type { AskOrbitAnswer, Citation, MemorySearchResult } from "../types.js";
import { synthesizeGroundedAnswer } from "./embeddings.js";
import { isSensitiveQuestion, resultContainsSensitiveEvidence } from "./sensitiveQuestionPolicy.js";

function citationsFrom(results: MemorySearchResult[]): Citation[] {
  return results.slice(0, 5).map((result, index) => ({
    citationId: `c${index + 1}`,
    envelopeId: result.envelopeId,
    title: result.title,
    excerpt: result.matchedEvidence[0]?.excerpt ?? result.summary,
    dayLocal: result.dayLocal,
    sourceAppLabel: result.sourceAppLabel,
  }));
}

function extractiveAnswer(citations: Citation[]): string {
  const first = citations[0];
  if (!first) return "I could not find enough saved evidence to answer that.";
  return first.excerpt
    ? `Best saved evidence: ${first.excerpt}`
    : `Best saved evidence: ${first.title ?? "saved capture"}.`;
}

export async function buildGroundedAskAnswer(
  question: string,
  results: MemorySearchResult[],
  allowSynthesis = false,
): Promise<AskOrbitAnswer> {
  const top = results[0];
  if (!top || top.score <= 0) {
    return {
      status: "insufficient_evidence",
      answer: "I could not find enough saved evidence to answer that.",
      citations: [],
      candidates: results.slice(0, 5),
      modelLabel: "hybrid/extractive",
      retrievalMode: "hybrid",
      confidence: 0,
      limitations: ["No sufficiently relevant saved memory was retrieved."],
    };
  }

  if (isSensitiveQuestion(question) && !resultContainsSensitiveEvidence(question, top)) {
    return {
      status: "sensitive_refusal",
      answer: "I do not have a saved capture that explicitly contains that, so I will not guess. For sensitive details, Orbit only answers when the exact value is present in saved evidence.",
      citations: [],
      candidates: [],
      modelLabel: "hybrid/sensitive-policy",
      retrievalMode: top.retrievalMode ?? "hybrid",
      confidence: 0,
      limitations: ["Capture or add the document first if you want Orbit to recall that detail later."],
    };
  }

  const citations = citationsFrom(results);
  if (!allowSynthesis) {
    return {
      status: "answered",
      answer: extractiveAnswer(citations),
      citations,
      candidates: [],
      modelLabel: "hybrid/extractive",
      retrievalMode: top.retrievalMode ?? "hybrid",
      confidence: Math.min(1, top.score / 10),
      limitations: ["Extractive answer from retrieved saved-memory evidence."],
    };
  }

  try {
    const synthesized = await synthesizeGroundedAnswer(question, citations);
    return {
      status: "answered",
      answer: synthesized.answer,
      citations,
      candidates: [],
      modelLabel: synthesized.modelLabel,
      retrievalMode: top.retrievalMode ?? "hybrid",
      confidence: Math.min(1, top.score / 10),
      limitations: ["Generated only from cited compact saved-memory evidence."],
    };
  } catch {
    return {
      status: "answered",
      answer: extractiveAnswer(citations),
      citations,
      candidates: [],
      modelLabel: "hybrid/extractive-fallback",
      retrievalMode: top.retrievalMode ?? "hybrid",
      confidence: Math.min(1, top.score / 10),
      limitations: ["LLM synthesis unavailable; returned extractive cited answer."],
    };
  }
}
