import type { MemoryIndexItem, MemorySearchFilters, MemorySearchResult, RetrievalMode } from "../types.js";
import { EMBEDDING_MODEL_LABEL } from "./embeddingPolicy.js";

function lowerText(item: MemoryIndexItem): string {
  return [
    item.title,
    item.summary,
    item.sourceAppLabel,
    item.domain,
    item.intent,
    item.tags.join(" "),
    item.evidence.map((e) => e.excerpt ?? "").join(" "),
    item.compactText,
  ].filter(Boolean).join(" ").toLowerCase();
}

export function lexicalScore(item: MemoryIndexItem, query: string): number {
  const terms = query.toLowerCase().split(/[^a-z0-9]+/).filter((term) => term.length >= 3);
  if (terms.length === 0) return 0;
  const haystack = lowerText(item);
  const title = item.title?.toLowerCase() ?? "";
  const evidence = item.evidence.map((e) => e.excerpt ?? "").join(" ").toLowerCase();
  let score = 0;
  for (const term of new Set(terms)) {
    if (haystack.includes(term)) score += 1;
    if (title.includes(term)) score += 2;
    if (evidence.includes(term)) score += 1.5;
  }
  if (haystack.includes(terms.join(" "))) score += 3;
  return score;
}

function matchedEvidence(item: MemoryIndexItem, query: string) {
  const q = query.toLowerCase();
  const terms = q.split(/[^a-z0-9]+/).filter((term) => term.length >= 3);
  const matches = item.evidence
    .filter((evidence) => {
      const text = evidence.excerpt?.toLowerCase() ?? "";
      return text.includes(q) || terms.some((term) => text.includes(term));
    })
    .slice(0, 3);
  return matches.length > 0 ? matches : item.evidence.slice(0, 1);
}

export function toSearchResult(
  item: MemoryIndexItem,
  index: number,
  query: string,
  scores: {
    finalScore: number;
    semanticScore?: number;
    lexicalScore?: number;
    contextScore?: number;
    retrievalMode: RetrievalMode;
  },
): MemorySearchResult {
  return {
    envelopeId: item.envelopeId,
    rank: index + 1,
    score: scores.finalScore,
    semanticScore: scores.semanticScore,
    lexicalScore: scores.lexicalScore,
    contextScore: scores.contextScore,
    retrievalMode: scores.retrievalMode,
    embeddingModel: item.embeddingModel ?? EMBEDDING_MODEL_LABEL,
    title: item.title,
    summary: item.summary,
    dayLocal: item.dayLocal,
    createdAtMillis: item.createdAtMillis,
    intent: item.intent,
    sourceAppLabel: item.sourceAppLabel,
    domain: item.domain,
    matchedEvidence: matchedEvidence(item, query),
  };
}

export function mergeHybridResults(
  query: string,
  lexicalItems: MemoryIndexItem[],
  vectorItems: Array<{ item: MemoryIndexItem; semanticScore: number }>,
  limit: number,
): MemorySearchResult[] {
  const byEnvelope = new Map<string, { item: MemoryIndexItem; semanticScore?: number }>();
  for (const item of lexicalItems) byEnvelope.set(item.envelopeId, { item });
  for (const vector of vectorItems) {
    byEnvelope.set(vector.item.envelopeId, {
      item: vector.item,
      semanticScore: Math.max(byEnvelope.get(vector.item.envelopeId)?.semanticScore ?? 0, vector.semanticScore),
    });
  }

  return [...byEnvelope.values()]
    .map(({ item, semanticScore }) => {
      const lex = lexicalScore(item, query);
      const contextScore = item.evidence.some((e) => e.source === "note" && (e.excerpt ?? "").toLowerCase().includes(query.toLowerCase()))
        ? 2
        : 0;
      const finalScore = (semanticScore ?? 0) * 10 + lex + contextScore;
      return { item, semanticScore, lexical: lex, contextScore, finalScore };
    })
    .filter((entry) => entry.finalScore > 0)
    .sort((a, b) => b.finalScore - a.finalScore || b.item.createdAtMillis - a.item.createdAtMillis)
    .slice(0, Math.min(Math.max(limit, 1), 20))
    .map((entry, index) => toSearchResult(entry.item, index, query, {
      finalScore: entry.finalScore,
      semanticScore: entry.semanticScore,
      lexicalScore: entry.lexical,
      contextScore: entry.contextScore,
      retrievalMode: entry.semanticScore === undefined ? "lexical_only" : "hybrid",
    }));
}

export function applyFilters(item: MemoryIndexItem, filters?: MemorySearchFilters): boolean {
  if (item.tombstonedAt) return false;
  if (filters?.dayLocalStart && item.dayLocal < filters.dayLocalStart) return false;
  if (filters?.dayLocalEnd && item.dayLocal > filters.dayLocalEnd) return false;
  if (filters?.intent && item.intent !== filters.intent) return false;
  if (filters?.appCategory && item.appCategory !== filters.appCategory) return false;
  return true;
}
