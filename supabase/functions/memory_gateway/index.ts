import { verifyJwt } from "./lib/auth.js";
import { ErrorCodes, UnauthorizedError, UNAUTHORIZED_MESSAGES } from "./lib/errors.js";
import { errorResponse, wireResponse } from "./lib/response.js";
import { MemoryGatewayRequestSchema } from "./lib/schemas.js";
import {
  embeddingInputFor,
  findStaleEmbeddingItems,
  markMemoryItemEmbedded,
  markMemoryItemEmbeddingFailed,
  memoryCollection,
  memoryHealthReport,
  searchMemoryItems,
  semanticSearchMemoryItems,
  tombstoneMemoryItem,
  upsertMemoryItem,
} from "./lib/atlas.js";
import { embedText } from "./lib/embeddings.js";
import { EMBEDDING_DIMENSIONS, EMBEDDING_MODEL_LABEL } from "./lib/embeddingPolicy.js";
import { buildGroundedAskAnswer } from "./lib/groundedAsk.js";
import type {
  AskOrbitAnswer,
  MemoryGatewayRequest,
  MemoryGatewaySuccessResponse,
  MemorySearchResult,
} from "./types.js";

function emitOperatorLog(line: Record<string, unknown>): void {
  console.log(JSON.stringify(line));
}

function buildExtractiveAnswer(
  question: string,
  results: MemorySearchResult[],
): AskOrbitAnswer {
  if (results.length === 0 || results[0]!.score <= 0) {
    return {
      status: "insufficient_evidence",
      answer: "I could not find enough saved evidence to answer that.",
      citations: [],
      candidates: results.slice(0, 5),
      modelLabel: "deterministic/extractive",
    };
  }

  const top = results.slice(0, 5);
  const citations = top.map((result, index) => ({
    citationId: `c${index + 1}`,
    envelopeId: result.envelopeId,
    title: result.title,
    excerpt: result.matchedEvidence[0]?.excerpt ?? result.summary,
    dayLocal: result.dayLocal,
    sourceAppLabel: result.sourceAppLabel,
  }));
  const subject = top[0]!.title || top[0]!.summary || "a saved memory";
  const evidence = citations[0]?.excerpt;
  return {
    status: "answered",
    answer: evidence
      ? `I found ${subject}. The strongest saved clue says: ${evidence}`
      : `I found ${subject} in your saved memories.`,
    citations,
    candidates: [],
    modelLabel: "deterministic/extractive",
  };
}

async function dispatch(
  req: MemoryGatewayRequest,
  userId: string,
): Promise<MemoryGatewaySuccessResponse> {
  const now = Date.now();

  switch (req.type) {
    case "memory_upsert": {
      const collection = await memoryCollection();
      const result = await upsertMemoryItem(collection, userId, req.payload.item, now);
      return {
        type: "memory_upsert_response",
        requestId: req.requestId,
        envelopeId: req.payload.item.envelopeId,
        ...result,
      };
    }
    case "memory_tombstone": {
      const collection = await memoryCollection();
      const result = await tombstoneMemoryItem(
        collection,
        userId,
        req.payload.envelopeId,
        req.payload.tombstonedAtMillis,
      );
      return {
        type: "memory_tombstone_response",
        requestId: req.requestId,
        envelopeId: req.payload.envelopeId,
        ...result,
      };
    }
    case "memory_search": {
      const collection = await memoryCollection();
      const results = await searchMemoryItems(
        collection,
        userId,
        req.payload.query,
        req.payload.filters,
        req.payload.limit ?? 10,
      );
      return {
        type: "memory_search_response",
        requestId: req.requestId,
        results,
      };
    }
    case "memory_embed_stale": {
      const collection = await memoryCollection();
      const items = await findStaleEmbeddingItems(collection, userId, req.payload.limit ?? 50);
      let embedded = 0;
      let skipped = 0;
      let failed = 0;
      for (const item of items) {
        const input = embeddingInputFor(item);
        if (!input.text.trim() || req.payload.dryRun) {
          skipped++;
          continue;
        }
        try {
          const result = await embedText(input.text);
          await markMemoryItemEmbedded(collection, userId, item, result.vector, input.hash, Date.now());
          embedded++;
        } catch {
          await markMemoryItemEmbeddingFailed(collection, userId, item, "EMBEDDING_PROVIDER_UNAVAILABLE", Date.now());
          failed++;
        }
      }
      return {
        type: "memory_embed_stale_response",
        requestId: req.requestId,
        embedded,
        skipped,
        failed,
        modelLabel: EMBEDDING_MODEL_LABEL,
        dimensions: EMBEDDING_DIMENSIONS,
      };
    }
    case "memory_semantic_search": {
      const collection = await memoryCollection();
      let queryVector: number[] | null = null;
      if (req.payload.mode !== "lexical_only") {
        try {
          queryVector = (await embedText(req.payload.query)).vector;
        } catch {
          queryVector = null;
        }
      }
      const results = await semanticSearchMemoryItems(
        collection,
        userId,
        req.payload.query,
        queryVector,
        req.payload.filters,
        req.payload.limit ?? 10,
      );
      return {
        type: "memory_semantic_search_response",
        requestId: req.requestId,
        results,
      };
    }
    case "memory_ask": {
      const collection = await memoryCollection();
      const results = await searchMemoryItems(
        collection,
        userId,
        req.payload.question,
        req.payload.filters,
        req.payload.limit ?? 5,
      );
      return {
        type: "memory_ask_response",
        requestId: req.requestId,
        answer: buildExtractiveAnswer(req.payload.question, results),
      };
    }
    case "memory_grounded_ask": {
      const collection = await memoryCollection();
      let queryVector: number[] | null = null;
      try {
        queryVector = (await embedText(req.payload.question)).vector;
      } catch {
        queryVector = null;
      }
      const results = await semanticSearchMemoryItems(
        collection,
        userId,
        req.payload.question,
        queryVector,
        req.payload.filters,
        req.payload.limit ?? 5,
      );
      return {
        type: "memory_grounded_ask_response",
        requestId: req.requestId,
        answer: await buildGroundedAskAnswer(
          req.payload.question,
          results,
          req.payload.allowSynthesis ?? false,
        ),
      };
    }
    case "memory_health": {
      return {
        type: "memory_health_response",
        requestId: req.requestId,
        health: await memoryHealthReport(),
      };
    }
  }
}

export default async function handler(req: Request): Promise<Response> {
  if (req.method !== "POST") {
    return errorResponse(ErrorCodes.INTERNAL, "method not allowed", "", 405);
  }

  let userId: string;
  try {
    userId = (await verifyJwt(req.headers.get("Authorization"))).sub;
  } catch (e) {
    if (e instanceof UnauthorizedError) {
      return errorResponse(ErrorCodes.UNAUTHORIZED, e.publicMessage, "", 401);
    }
    return errorResponse(
      ErrorCodes.UNAUTHORIZED,
      UNAUTHORIZED_MESSAGES.TOKEN_VERIFICATION_FAILED,
      "",
      401,
    );
  }

  let raw: unknown;
  try {
    raw = await req.json();
  } catch {
    return errorResponse(
      ErrorCodes.VALIDATION_FAILED,
      "request body failed validation",
      "",
      200,
    );
  }

  const parsed = MemoryGatewayRequestSchema.safeParse(raw);
  if (!parsed.success) {
    return errorResponse(
      ErrorCodes.VALIDATION_FAILED,
      "request body failed validation",
      typeof raw === "object" && raw !== null && "requestId" in raw
        ? String((raw as { requestId?: unknown }).requestId ?? "")
        : "",
      200,
    );
  }

  const requestEnvelope = parsed.data;
  const start = performance.now();
  try {
    const response = await dispatch(requestEnvelope, userId);
    emitOperatorLog({
      requestId: requestEnvelope.requestId,
      userId,
      requestType: requestEnvelope.type,
      latencyMs: Math.round(performance.now() - start),
      success: true,
      resultCount:
        response.type === "memory_search_response"
          ? response.results.length
          : response.type === "memory_ask_response"
            ? response.answer.citations.length
            : undefined,
      atlasOk:
        response.type === "memory_health_response"
          ? response.health.atlasOk
          : undefined,
    });
    return wireResponse(response);
  } catch (error) {
    const atlasError = error instanceof Error
      ? { errorName: error.name, errorMessage: error.message.slice(0, 240) }
      : { errorName: typeof error, errorMessage: "non-error thrown" };
    emitOperatorLog({
      requestId: requestEnvelope.requestId,
      userId,
      requestType: requestEnvelope.type,
      latencyMs: Math.round(performance.now() - start),
      success: false,
      errorCode: ErrorCodes.ATLAS_UNAVAILABLE,
      ...atlasError,
    });
    return errorResponse(
      ErrorCodes.ATLAS_UNAVAILABLE,
      "memory index unavailable",
      requestEnvelope.requestId,
      200,
    );
  }
}
