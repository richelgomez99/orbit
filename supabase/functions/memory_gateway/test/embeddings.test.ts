import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { embedText, EmbeddingProviderError, synthesizeGroundedAnswer } from "../lib/embeddings.js";
import { ASK_SYNTHESIS_MODEL, EMBEDDING_DIMENSIONS, EMBEDDING_MODEL, EMBEDDING_MODEL_LABEL } from "../lib/embeddingPolicy.js";

const OLD_OPENAI_API_KEY = process.env.OPENAI_API_KEY;

describe("embedding provider", () => {
  beforeEach(() => {
    process.env.OPENAI_API_KEY = "test-openai-key";
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    if (OLD_OPENAI_API_KEY === undefined) {
      delete process.env.OPENAI_API_KEY;
    } else {
      process.env.OPENAI_API_KEY = OLD_OPENAI_API_KEY;
    }
  });

  it("calls OpenAI embeddings with locked model, dimensions, input, and backend-only auth", async () => {
    const vector = Array.from({ length: EMBEDDING_DIMENSIONS }, (_, index) => index / EMBEDDING_DIMENSIONS);
    const bodies: unknown[] = [];
    const headers: HeadersInit[] = [];
    vi.stubGlobal("fetch", vi.fn(async (_url: string, init?: RequestInit) => {
      bodies.push(JSON.parse(String(init?.body)));
      headers.push(init?.headers ?? {});
      return new Response(JSON.stringify({ data: [{ embedding: vector }] }), { status: 200 });
    }));

    const result = await embedText("QR code for first 1000 customers");

    expect(result.modelLabel).toBe(EMBEDDING_MODEL_LABEL);
    expect(result.dimensions).toBe(EMBEDDING_DIMENSIONS);
    expect(result.vector).toHaveLength(EMBEDDING_DIMENSIONS);
    expect(bodies).toEqual([
      {
        model: EMBEDDING_MODEL,
        input: "QR code for first 1000 customers",
        dimensions: EMBEDDING_DIMENSIONS,
      },
    ]);
    expect(headers[0]).toMatchObject({
      Authorization: "Bearer test-openai-key",
      "Content-Type": "application/json",
    });
  });

  it("rejects missing provider credentials before fetch", async () => {
    delete process.env.OPENAI_API_KEY;
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    await expect(embedText("startup event")).rejects.toThrow(EmbeddingProviderError);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("rejects malformed embedding dimensions", async () => {
    vi.stubGlobal("fetch", vi.fn(async () =>
      new Response(JSON.stringify({ data: [{ embedding: [0.1, 0.2, 0.3] }] }), { status: 200 })
    ));

    await expect(embedText("flight receipt")).rejects.toThrow("embedding vector dimensions were invalid");
  });

  it("rejects non-finite embedding values", async () => {
    const vector: Array<number | string> = Array.from({ length: EMBEDDING_DIMENSIONS }, () => 0.1);
    vector[42] = "not-a-number";
    vi.stubGlobal("fetch", vi.fn(async () =>
      new Response(JSON.stringify({ data: [{ embedding: vector }] }), { status: 200 })
    ));

    await expect(embedText("recipe")).rejects.toThrow("embedding vector dimensions were invalid");
  });

  it("surfaces provider HTTP failures", async () => {
    vi.stubGlobal("fetch", vi.fn(async () =>
      new Response(JSON.stringify({ error: "rate limited" }), { status: 429 })
    ));

    await expect(embedText("reschedule dentist")).rejects.toThrow("embedding provider returned 429");
  });

  it("calls OpenAI responses with cited evidence for grounded synthesis", async () => {
    const bodies: unknown[] = [];
    vi.stubGlobal("fetch", vi.fn(async (_url: string, init?: RequestInit) => {
      bodies.push(JSON.parse(String(init?.body)));
      return new Response(JSON.stringify({ output_text: "You saved the flight receipt." }), { status: 200 });
    }));

    const result = await synthesizeGroundedAnswer("Which receipt?", [
      {
        citationId: "c1",
        envelopeId: "env-flight",
        title: "Flight receipt",
        excerpt: "Flight receipt: NYC to San Francisco.",
        dayLocal: "2026-05-30",
      },
    ]);

    expect(result).toEqual({
      answer: "You saved the flight receipt.",
      modelLabel: `openai:${ASK_SYNTHESIS_MODEL}`,
    });
    expect(bodies[0]).toMatchObject({
      model: ASK_SYNTHESIS_MODEL,
      max_output_tokens: 160,
    });
    expect(JSON.stringify(bodies[0])).toContain("[c1] Flight receipt: Flight receipt: NYC to San Francisco.");
  });
});
