import {
  ASK_SYNTHESIS_MODEL,
  ASK_SYNTHESIS_MODEL_LABEL,
  EMBEDDING_DIMENSIONS,
  EMBEDDING_MODEL,
  EMBEDDING_MODEL_LABEL,
} from "./embeddingPolicy.js";
import type { Citation } from "../types.js";

export class EmbeddingProviderError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "EmbeddingProviderError";
  }
}

function apiKey(): string {
  const key = process.env.OPENAI_API_KEY?.trim();
  if (!key) throw new EmbeddingProviderError("OPENAI_API_KEY is not configured");
  return key;
}

function ensureFiniteVector(vector: unknown): number[] {
  if (!Array.isArray(vector)) {
    throw new EmbeddingProviderError("embedding response did not include a vector");
  }
  const numbers = vector.map((value) => Number(value));
  if (numbers.length !== EMBEDDING_DIMENSIONS || numbers.some((value) => !Number.isFinite(value))) {
    throw new EmbeddingProviderError("embedding vector dimensions were invalid");
  }
  return numbers;
}

export async function embedText(text: string): Promise<{ vector: number[]; modelLabel: string; dimensions: number }> {
  const response = await fetch("https://api.openai.com/v1/embeddings", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${apiKey()}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model: EMBEDDING_MODEL,
      input: text,
      dimensions: EMBEDDING_DIMENSIONS,
    }),
  });
  if (!response.ok) {
    throw new EmbeddingProviderError(`embedding provider returned ${response.status}`);
  }
  const body = await response.json() as { data?: Array<{ embedding?: unknown }> };
  const vector = ensureFiniteVector(body.data?.[0]?.embedding);
  return { vector, modelLabel: EMBEDDING_MODEL_LABEL, dimensions: EMBEDDING_DIMENSIONS };
}

function responseText(body: unknown): string {
  const output = (body as { output_text?: unknown }).output_text;
  if (typeof output === "string" && output.trim()) return output.trim();
  const chunks = (body as { output?: unknown }).output;
  if (Array.isArray(chunks)) {
    const text = chunks
      .flatMap((chunk) => Array.isArray((chunk as { content?: unknown }).content) ? (chunk as { content: unknown[] }).content : [])
      .map((content) => (content as { text?: unknown }).text)
      .filter((text): text is string => typeof text === "string")
      .join(" ")
      .trim();
    if (text) return text;
  }
  throw new EmbeddingProviderError("answer provider returned no text");
}

export async function synthesizeGroundedAnswer(
  question: string,
  citations: Citation[],
): Promise<{ answer: string; modelLabel: string }> {
  const evidence = citations
    .map((citation) => `[${citation.citationId}] ${citation.title ?? "Saved capture"}: ${citation.excerpt ?? ""}`)
    .join("\n");
  const response = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${apiKey()}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model: ASK_SYNTHESIS_MODEL,
      input: [
        {
          role: "system",
          content: "Answer only from the cited saved-memory evidence. If the evidence is insufficient, say that. Keep the answer under 80 words and do not introduce uncited facts.",
        },
        {
          role: "user",
          content: `Question: ${question}\n\nEvidence:\n${evidence}`,
        },
      ],
      max_output_tokens: 160,
    }),
  });
  if (!response.ok) {
    throw new EmbeddingProviderError(`answer provider returned ${response.status}`);
  }
  return {
    answer: responseText(await response.json()),
    modelLabel: ASK_SYNTHESIS_MODEL_LABEL,
  };
}
