import { beforeEach, describe, expect, it, vi } from "vitest";
import type { MemoryIndexItemInput } from "../types.js";

const mocks = vi.hoisted(() => ({
  collection: {},
  upsertMemoryItem: vi.fn(),
  tombstoneMemoryItem: vi.fn(),
  searchMemoryItems: vi.fn(),
  findStaleEmbeddingItems: vi.fn(),
  markMemoryItemEmbedded: vi.fn(),
  markMemoryItemEmbeddingFailed: vi.fn(),
  embeddingInputFor: vi.fn(),
  semanticSearchMemoryItems: vi.fn(),
  memoryHealthReport: vi.fn(),
  embedText: vi.fn(),
}));

vi.mock("../lib/auth.js", () => ({
  verifyJwt: vi.fn(async () => ({ sub: "11111111-1111-4111-8111-111111111111" })),
  UnauthorizedError: class UnauthorizedError extends Error {
    publicMessage: string;
    constructor(publicMessage: string) {
      super(publicMessage);
      this.publicMessage = publicMessage;
    }
  },
}));

vi.mock("../lib/atlas.js", () => ({
  memoryCollection: vi.fn(async () => mocks.collection),
  findStaleEmbeddingItems: mocks.findStaleEmbeddingItems,
  markMemoryItemEmbedded: mocks.markMemoryItemEmbedded,
  markMemoryItemEmbeddingFailed: mocks.markMemoryItemEmbeddingFailed,
  embeddingInputFor: mocks.embeddingInputFor,
  upsertMemoryItem: mocks.upsertMemoryItem,
  tombstoneMemoryItem: mocks.tombstoneMemoryItem,
  searchMemoryItems: mocks.searchMemoryItems,
  semanticSearchMemoryItems: mocks.semanticSearchMemoryItems,
  memoryHealthReport: mocks.memoryHealthReport,
}));

vi.mock("../lib/embeddings.js", () => ({
  embedText: mocks.embedText,
  synthesizeGroundedAnswer: vi.fn(async () => ({
    answer: "Synthesized answer from cited evidence.",
    modelLabel: "openai:gpt-4.1-mini",
  })),
}));

const RID = "550e8400-e29b-41d4-a716-446655440000";

const item: MemoryIndexItemInput = {
  envelopeId: "env-1",
  schemaVersion: 1,
  kind: "REGULAR",
  dayLocal: "2026-05-30",
  createdAtMillis: 1780150000000,
  intent: "READ_OR_WATCH_LATER",
  contentType: "image",
  title: "Startup event flyer",
  summary: "Saved event details for later.",
  sourceAppLabel: "Chrome",
  appCategory: "browser",
  tags: ["event", "startup"],
  evidence: [],
  compactText: "Startup event flyer",
};

async function post(body: object | string): Promise<Response> {
  const mod = await import("../index.js");
  return mod.default(
    new Request("https://test.local/memory", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer test",
      },
      body: typeof body === "string" ? body : JSON.stringify(body),
    }),
  );
}

describe("memory gateway router", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.upsertMemoryItem.mockResolvedValue({ upserted: true, indexedAtMillis: 123 });
    mocks.tombstoneMemoryItem.mockResolvedValue({ matched: true, modified: true });
    mocks.searchMemoryItems.mockResolvedValue([
      {
        envelopeId: "env-1",
        rank: 1,
        score: 5,
        title: "Startup event flyer",
        summary: "Saved event details for later.",
        dayLocal: "2026-05-30",
        createdAtMillis: 1780150000000,
        intent: "READ_OR_WATCH_LATER",
        sourceAppLabel: "Chrome",
        matchedEvidence: [],
      },
    ]);
    mocks.semanticSearchMemoryItems.mockResolvedValue([
      {
        envelopeId: "env-1",
        rank: 1,
        score: 8,
        title: "Startup event flyer",
        summary: "Saved event details for later.",
        dayLocal: "2026-05-30",
        createdAtMillis: 1780150000000,
        intent: "READ_OR_WATCH_LATER",
        sourceAppLabel: "Chrome",
        matchedEvidence: [
          {
            kind: "SUMMARY",
            label: "Summary",
            excerpt: "Startup event flyer for demo day.",
            source: "derived",
          },
        ],
        retrievalMode: "hybrid",
      },
    ]);
    mocks.findStaleEmbeddingItems.mockResolvedValue([{ ...item, userId: "user-1", updatedAtMillis: 123 }]);
    mocks.markMemoryItemEmbedded.mockResolvedValue(undefined);
    mocks.markMemoryItemEmbeddingFailed.mockResolvedValue(undefined);
    mocks.embeddingInputFor.mockReturnValue({ text: "Startup event flyer", hash: "abc123" });
    mocks.embedText.mockResolvedValue({
      vector: Array.from({ length: 1536 }, () => 0.01),
      modelLabel: "openai:text-embedding-3-small",
      dimensions: 1536,
    });
    mocks.memoryHealthReport.mockResolvedValue({
      atlasOk: true,
      env: {
        mongodbAtlasUriConfigured: true,
        mongodbDbConfigured: true,
        mongodbMemoryCollectionConfigured: true,
        supabaseUrlConfigured: true,
      },
    });
  });

  it("routes memory_upsert", async () => {
    const res = await post({ type: "memory_upsert", requestId: RID, payload: { item } });
    expect(res.status).toBe(200);
    const body = await res.json();
    expect(body.type).toBe("memory_upsert_response");
    expect(body.ok).toBe(true);
    expect(mocks.upsertMemoryItem).toHaveBeenCalledOnce();
  });

  it("routes memory_tombstone", async () => {
    const res = await post({
      type: "memory_tombstone",
      requestId: RID,
      payload: {
        envelopeId: "env-1",
        reason: "LOCAL_DELETE",
        tombstonedAtMillis: 1780150000001,
      },
    });
    const body = await res.json();
    expect(body.type).toBe("memory_tombstone_response");
    expect(mocks.tombstoneMemoryItem).toHaveBeenCalledOnce();
  });

  it("routes memory_search", async () => {
    const res = await post({
      type: "memory_search",
      requestId: RID,
      payload: { query: "startup", limit: 10 },
    });
    const body = await res.json();
    expect(body.type).toBe("memory_search_response");
    expect(body.data.results).toHaveLength(1);
    expect(mocks.searchMemoryItems).toHaveBeenCalledOnce();
  });

  it("keeps memory_ask retrieval-grounded when stretch endpoint is used", async () => {
    const res = await post({
      type: "memory_ask",
      requestId: RID,
      payload: { question: "startup event", limit: 5 },
    });
    const body = await res.json();
    expect(body.type).toBe("memory_ask_response");
    expect(body.data.answer.citations[0].envelopeId).toBe("env-1");
    expect(mocks.searchMemoryItems).toHaveBeenCalledOnce();
  });

  it("routes memory_embed_stale", async () => {
    const res = await post({
      type: "memory_embed_stale",
      requestId: RID,
      payload: { limit: 5 },
    });
    const body = await res.json();
    expect(body.type).toBe("memory_embed_stale_response");
    expect(body.data.embedded).toBe(1);
    expect(mocks.embedText).toHaveBeenCalledOnce();
    expect(mocks.markMemoryItemEmbedded).toHaveBeenCalledOnce();
  });

  it("routes memory_semantic_search", async () => {
    const res = await post({
      type: "memory_semantic_search",
      requestId: RID,
      payload: { query: "startup event", limit: 5, mode: "hybrid" },
    });
    const body = await res.json();
    expect(body.type).toBe("memory_semantic_search_response");
    expect(body.data.results[0].envelopeId).toBe("env-1");
    expect(mocks.semanticSearchMemoryItems).toHaveBeenCalledOnce();
  });

  it("routes memory_grounded_ask with cited answer", async () => {
    const res = await post({
      type: "memory_grounded_ask",
      requestId: RID,
      payload: { question: "What startup event did I save?", allowSynthesis: true },
    });
    const body = await res.json();
    expect(body.type).toBe("memory_grounded_ask_response");
    expect(body.data.answer.status).toBe("answered");
    expect(body.data.answer.citations[0].envelopeId).toBe("env-1");
  });

  it("routes authenticated memory_health diagnostics", async () => {
    const res = await post({
      type: "memory_health",
      requestId: RID,
      payload: {},
    });
    const body = await res.json();
    expect(body.type).toBe("memory_health_response");
    expect(body.data.health.atlasOk).toBe(true);
    expect(mocks.memoryHealthReport).toHaveBeenCalledOnce();
  });

  it("rejects malformed bodies", async () => {
    const res = await post("{bad json");
    const body = await res.json();
    expect(body.type).toBe("error");
    expect(body.error.code).toBe("VALIDATION_FAILED");
  });
});
