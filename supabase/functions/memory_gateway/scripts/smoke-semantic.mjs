#!/usr/bin/env node

import { randomUUID } from "node:crypto";
import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";

const ROOT = resolve(import.meta.dirname, "../../..", "..");
const GATEWAY_DIR = resolve(import.meta.dirname, "..");

function readProperties(path) {
  if (!existsSync(path)) return {};
  const out = {};
  for (const line of readFileSync(path, "utf8").split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const eq = trimmed.indexOf("=");
    if (eq < 0) continue;
    const key = trimmed.slice(0, eq).trim();
    let value = trimmed.slice(eq + 1).trim();
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }
    out[key] = value
      .replace(/\\:/g, ":")
      .replace(/\\=/g, "=")
      .replace(/\\ /g, " ")
      .replace(/\\\\/g, "\\");
  }
  return out;
}

function pick(...values) {
  return values.find((value) => typeof value === "string" && value.trim().length > 0)?.trim();
}

function required(name, value, hint) {
  if (!value) throw new Error(`${name} is required. ${hint}`);
  return value;
}

function decodeJwtPayload(token) {
  const [, payload] = token.split(".");
  if (!payload) throw new Error("Supabase token did not contain a JWT payload");
  const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
  return JSON.parse(Buffer.from(normalized, "base64").toString("utf8"));
}

async function signIn({ supabaseUrl, publishableKey, email, password }) {
  const response = await fetch(`${supabaseUrl}/auth/v1/token?grant_type=password`, {
    method: "POST",
    headers: {
      apikey: publishableKey,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ email, password }),
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok || typeof body.access_token !== "string") {
    throw new Error(`Supabase debug sign-in failed: HTTP ${response.status}`);
  }
  return {
    accessToken: body.access_token,
    userId: decodeJwtPayload(body.access_token).sub,
  };
}

async function callGateway({ gatewayUrl, accessToken }, envelope) {
  const response = await fetch(gatewayUrl, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
      "X-Orbit-Request-Id": envelope.requestId,
    },
    body: JSON.stringify(envelope),
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok || body.ok !== true) {
    const code = body?.error?.code ?? "HTTP_ERROR";
    const message = body?.error?.message ?? `HTTP ${response.status}`;
    throw new Error(`${envelope.type} failed: ${code}: ${message}`);
  }
  return body.data;
}

function semanticSearchEnvelope(query) {
  return {
    type: "memory_semantic_search",
    requestId: randomUUID(),
    payload: { query, limit: 5, mode: "hybrid" },
  };
}

function groundedAskEnvelope(question, allowSynthesis) {
  return {
    type: "memory_grounded_ask",
    requestId: randomUUID(),
    payload: { question, limit: 5, allowSynthesis },
  };
}

async function main() {
  const localProps = readProperties(resolve(ROOT, "local.properties"));
  const envLocal = readProperties(resolve(GATEWAY_DIR, ".env.local"));
  const supabaseUrl = required(
    "SUPABASE_URL",
    pick(process.env.SUPABASE_URL, localProps["supabase.url"], envLocal.SUPABASE_URL),
    "Set supabase.url in local.properties.",
  );
  const publishableKey = required(
    "SUPABASE_PUBLISHABLE_KEY",
    pick(
      process.env.SUPABASE_PUBLISHABLE_KEY,
      process.env.SUPABASE_ANON_KEY,
      localProps["supabase.publishable.key"],
      envLocal.SUPABASE_PUBLISHABLE_KEY,
      envLocal.SUPABASE_ANON_KEY,
    ),
    "Set supabase.publishable.key in local.properties.",
  );
  const email = required(
    "supabase.debug.email",
    pick(process.env.SUPABASE_DEBUG_EMAIL, localProps["supabase.debug.email"]),
    "Create a normal Supabase Auth debug user and add it to local.properties.",
  );
  const password = required(
    "supabase.debug.password",
    pick(process.env.SUPABASE_DEBUG_PASSWORD, localProps["supabase.debug.password"]),
    "Add the debug user's password to local.properties.",
  );
  const gatewayUrl = required(
    "memory.gateway.url",
    pick(process.env.MEMORY_GATEWAY_URL, localProps["memory.gateway.url"]),
    "Set memory.gateway.url in local.properties.",
  );

  const { accessToken, userId } = await signIn({
    supabaseUrl,
    publishableKey,
    email,
    password,
  });
  console.log(`supabase_session_user=${userId}`);

  for (const query of ["qr code", "reschedule", "flight receipt"]) {
    const result = await callGateway({ gatewayUrl, accessToken }, semanticSearchEnvelope(query));
    const top = result.results?.[0]?.envelopeId ?? "<none>";
    const mode = result.results?.[0]?.retrievalMode ?? "<none>";
    console.log(`semantic_search query="${query}" count=${result.results?.length ?? 0} top=${top} mode=${mode}`);
  }

  for (const question of ["Which flight receipt did I save?", "What is my passport number?"]) {
    const result = await callGateway({ gatewayUrl, accessToken }, groundedAskEnvelope(question, true));
    console.log(`grounded_ask question="${question}" status=${result.answer?.status ?? "<none>"} citations=${result.answer?.citations?.length ?? 0} model=${result.answer?.modelLabel ?? "<none>"}`);
  }

  console.log("semantic_smoke_ok=yes");
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : String(error));
  process.exitCode = 1;
});
