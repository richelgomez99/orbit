export const EMBEDDING_PROVIDER = "openai";
export const EMBEDDING_MODEL = process.env.MEMORY_EMBEDDING_MODEL?.trim() || "text-embedding-3-small";
export const EMBEDDING_MODEL_LABEL = `${EMBEDDING_PROVIDER}:${EMBEDDING_MODEL}`;
export const EMBEDDING_DIMENSIONS = Number.parseInt(
  process.env.MEMORY_EMBEDDING_DIMENSIONS?.trim() || "1536",
  10,
);
export const EMBEDDING_INPUT_MAX_CHARS = 2_000;

export const ASK_SYNTHESIS_MODEL = process.env.MEMORY_ASK_MODEL?.trim() || "gpt-4.1-mini";
export const ASK_SYNTHESIS_MODEL_LABEL = `openai:${ASK_SYNTHESIS_MODEL}`;

export function assertEmbeddingPolicy(): void {
  if (EMBEDDING_MODEL !== "text-embedding-3-small") {
    throw new Error(`unsupported embedding model: ${EMBEDDING_MODEL}`);
  }
  if (EMBEDDING_DIMENSIONS !== 1536) {
    throw new Error(`unsupported embedding dimensions: ${EMBEDDING_DIMENSIONS}`);
  }
}
