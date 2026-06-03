// Spec 005 - Vercel Node Function entry for the memory gateway.
//
// MongoDB's Node driver requires TCP/TLS modules that are not available in
// Vercel Edge Runtime. Keep the tested gateway core as Web Request/Response,
// but adapt Vercel's Node request/response objects at the boundary.

import handlerImpl from "../index.js";
import type { IncomingMessage, ServerResponse } from "node:http";

export default async function handler(req: IncomingMessage, res: ServerResponse): Promise<void> {
  const body = await readBody(req);
  const url = absoluteUrl(req);
  const headers = new Headers();
  for (const [key, value] of Object.entries(req.headers)) {
    if (Array.isArray(value)) {
      value.forEach((v) => headers.append(key, v));
    } else if (value !== undefined) {
      headers.set(key, value);
    }
  }

  const request = new Request(url, {
    method: req.method ?? "GET",
    headers,
    body: body.length > 0 && req.method !== "GET" && req.method !== "HEAD"
      ? new Uint8Array(body)
      : undefined,
  });
  const response = await handlerImpl(request);

  res.statusCode = response.status;
  response.headers.forEach((value, key) => {
    res.setHeader(key, value);
  });
  const responseBody = Buffer.from(await response.arrayBuffer());
  res.end(responseBody);
}

function absoluteUrl(req: IncomingMessage): string {
  const proto = headerValue(req, "x-forwarded-proto") ?? "https";
  const host = headerValue(req, "host") ?? "localhost";
  return `${proto}://${host}${req.url ?? "/memory"}`;
}

function headerValue(req: IncomingMessage, key: string): string | undefined {
  const value = req.headers[key];
  return Array.isArray(value) ? value[0] : value;
}

async function readBody(req: IncomingMessage): Promise<Buffer> {
  const chunks: Buffer[] = [];
  for await (const chunk of req) {
    chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk));
  }
  return Buffer.concat(chunks);
}
