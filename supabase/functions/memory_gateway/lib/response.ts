import type { ErrorCode } from "./errors.js";
import type { MemoryGatewayResponse } from "../types.js";

export function toWireBody(response: MemoryGatewayResponse): Record<string, unknown> {
  if (response.type === "error") {
    return {
      type: "error",
      requestId: response.requestId,
      ok: false,
      error: { code: response.code, message: response.message },
    };
  }

  const { type, requestId, ...rest } = response as MemoryGatewayResponse &
    Record<string, unknown>;
  return {
    type,
    requestId,
    ok: true,
    data: rest,
  };
}

export function wireResponse(response: MemoryGatewayResponse): Response {
  return new Response(JSON.stringify(toWireBody(response)), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
}

export function errorResponse(
  code: ErrorCode,
  message: string,
  requestId: string,
  status: 200 | 401 | 405 | 500 = 200,
): Response {
  return new Response(
    JSON.stringify({
      type: "error",
      requestId,
      ok: false,
      error: { code, message },
    }),
    {
      status,
      headers: { "Content-Type": "application/json" },
    },
  );
}
