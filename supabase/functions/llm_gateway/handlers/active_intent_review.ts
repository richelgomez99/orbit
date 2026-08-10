// Ask Orbit active-intent review handler.
// Accepts only the compact ActiveIntentReviewContext packet from Android;
// never raw screenshots, raw OCR bodies, HTML, prompts, embeddings, or model
// responses. The gateway audit layer records only metadata.

import type {
  ActiveIntentReviewRequest,
  HandlerContext,
  HandlerResult,
} from "../types.js";
import {
  callAnthropic,
  cachedSystemPrompt,
  AnthropicCallError,
} from "../lib/anthropic.js";
import {
  MODEL_HAIKU,
  MODEL_HAIKU_LABEL,
  TIMEOUT_DEFAULT_MS,
  failureToErrorCode,
  failureToMessage,
} from "../lib/models.js";
import { ActiveIntentReviewResultSchema } from "../lib/schemas.js";

const SYSTEM_PREFIX =
  "You are Orbit, a calm decision assistant for screenshot cleanup. " +
  "You receive a compact, redacted active-intent context only. " +
  "Do not invent hidden screenshot details. Decide whether the user should keep following up, mark handled, clear as not needed, or add context. " +
  'Respond with STRICT JSON only: {"decision":"KEEP_FOLLOWING|MARK_HANDLED|NOT_NEEDED|ADD_CONTEXT","confidence":0..1,"rationale":"one short user-facing sentence","suggestedResolution":string|null}. ' +
  "Output JSON only — no markdown, no preamble.";

export async function handle(
  req: ActiveIntentReviewRequest,
  _ctx: HandlerContext,
): Promise<HandlerResult> {
  const userText = JSON.stringify(req.payload.reviewContext);

  try {
    const { text, cacheHit, tokensIn, tokensOut } = await callAnthropic({
      model: MODEL_HAIKU,
      system: cachedSystemPrompt(SYSTEM_PREFIX),
      userText,
      maxTokens: 160,
      timeoutMs: TIMEOUT_DEFAULT_MS,
      cacheBeta: true,
    });

    let raw: unknown;
    try {
      raw = JSON.parse(stripCodeFence(text));
    } catch {
      return malformed(req.requestId, cacheHit, tokensIn, tokensOut);
    }

    const parsed = ActiveIntentReviewResultSchema.safeParse(raw);
    if (!parsed.success) {
      return malformed(req.requestId, cacheHit, tokensIn, tokensOut);
    }

    return {
      response: {
        type: "active_intent_review_response",
        requestId: req.requestId,
        decision: parsed.data.decision,
        confidence: parsed.data.confidence,
        rationale: parsed.data.rationale,
        suggestedResolution: parsed.data.suggestedResolution,
        modelLabel: MODEL_HAIKU_LABEL,
      },
      model: MODEL_HAIKU,
      modelLabel: MODEL_HAIKU_LABEL,
      tokensIn,
      tokensOut,
      cacheHit,
    };
  } catch (e) {
    if (e instanceof AnthropicCallError) {
      return {
        response: {
          type: "error",
          requestId: req.requestId,
          code: failureToErrorCode(e.failure),
          message: failureToMessage(e.failure),
        },
        model: MODEL_HAIKU,
        modelLabel: MODEL_HAIKU_LABEL,
        tokensIn: 0,
        tokensOut: 0,
        cacheHit: false,
      };
    }
    throw e;
  }
}

function malformed(
  requestId: string,
  cacheHit: boolean,
  tokensIn: number,
  tokensOut: number,
): HandlerResult {
  return {
    response: {
      type: "error",
      requestId,
      code: "MALFORMED_RESPONSE",
      message: "Upstream returned malformed response",
    },
    model: MODEL_HAIKU,
    modelLabel: MODEL_HAIKU_LABEL,
    tokensIn,
    tokensOut,
    cacheHit,
  };
}

function stripCodeFence(s: string): string {
  const trimmed = s.trim();
  const fenced = /^```(?:json)?\s*([\s\S]*?)\s*```$/i.exec(trimmed);
  return fenced && fenced[1] ? fenced[1].trim() : trimmed;
}