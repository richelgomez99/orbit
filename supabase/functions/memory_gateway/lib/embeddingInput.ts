import { createHash } from "node:crypto";
import type { MemoryIndexItemInput } from "../types.js";
import { EMBEDDING_INPUT_MAX_CHARS } from "./embeddingPolicy.js";

export interface CompactEmbeddingInput {
  text: string;
  hash: string;
}

function clean(value: string | undefined): string | null {
  const trimmed = value?.replace(/\s+/g, " ").trim();
  return trimmed ? trimmed : null;
}

function pushPart(parts: string[], label: string, value: string | undefined): void {
  const cleaned = clean(value);
  if (cleaned) parts.push(`${label}: ${cleaned}`);
}

export function buildCompactEmbeddingInput(item: MemoryIndexItemInput): CompactEmbeddingInput {
  const parts: string[] = [];
  pushPart(parts, "title", item.title);
  pushPart(parts, "summary", item.summary);
  if (item.tags.length > 0) pushPart(parts, "tags", item.tags.join(", "));
  pushPart(parts, "source", item.sourceAppLabel);
  pushPart(parts, "category", item.appCategory);
  pushPart(parts, "domain", item.domain);
  for (const evidence of item.evidence) {
    pushPart(parts, `evidence ${evidence.label}`, evidence.excerpt);
  }
  pushPart(parts, "compact", item.compactText);

  const text = parts.join("\n").slice(0, EMBEDDING_INPUT_MAX_CHARS);
  return {
    text,
    hash: createHash("sha256").update(text).digest("hex"),
  };
}
