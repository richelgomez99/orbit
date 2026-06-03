#!/usr/bin/env node

import { randomUUID } from "node:crypto";
import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";
import { MongoClient } from "mongodb";

const ROOT = resolve(import.meta.dirname, "../../..", "..");
const GATEWAY_DIR = resolve(import.meta.dirname, "..");
const DEFAULT_SEARCHES = ["startup event", "flight receipt", "recipe"];

const BANNED_KEYS = new Set([
  "rawScreenshot",
  "imageBytes",
  "rawOcr",
  "ocrText",
  "rawHtml",
  "clipboardText",
  "prompt",
  "modelResponse",
  "auditLog",
  "accessToken",
  "refreshToken",
  "mongodbUri",
]);

function readProperties(path) {
  if (!existsSync(path)) return {};
  const out = {};
  const lines = readFileSync(path, "utf8").split(/\r?\n/);
  for (const line of lines) {
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
    out[key] = unescapePropertyValue(value);
  }
  return out;
}

function unescapePropertyValue(value) {
  return value
    .replace(/\\:/g, ":")
    .replace(/\\=/g, "=")
    .replace(/\\ /g, " ")
    .replace(/\\\\/g, "\\");
}

function pick(...values) {
  return values.find((value) => typeof value === "string" && value.trim().length > 0)?.trim();
}

function required(name, value, hint) {
  if (!value) {
    throw new Error(`${name} is required. ${hint}`);
  }
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

function item({
  id,
  day,
  intent,
  title,
  summary,
  source,
  category,
  tags,
  text,
  domain,
  contentType = "text",
}) {
  const createdAt = Date.parse(`${day}T12:00:00.000Z`);
  return {
    envelopeId: id,
    schemaVersion: 1,
    kind: "REGULAR",
    dayLocal: day,
    createdAtMillis: createdAt,
    intent,
    contentType,
    title,
    summary,
    sourceAppLabel: source,
    appCategory: category,
    domain,
    tags,
    evidence: [
      {
        kind: "compact_clue",
        label: "Demo clue",
        excerpt: text.slice(0, 220),
        source: "derived",
        confidence: 0.9,
        hash: id,
        createdAtMillis: createdAt,
      },
    ],
    compactText: text,
    localContentHash: `demo-${id}`,
  };
}

function demoItems() {
  const day = "2026-05-30";
  return [
    item({
      id: "demo-startup-event-01",
      day,
      intent: "REFERENCE",
      title: "Startup event ticket",
      summary: "Ticket and notes for a startup event with founder office hours.",
      source: "Gmail",
      category: "event",
      tags: ["startup", "event", "yc", "office-hours"],
      text: "Startup event ticket for Monday founder office hours. Venue doors open at 6pm with a panel about MVP scope and traction.",
      domain: "lu.ma",
    }),
    item({
      id: "demo-flight-receipt-01",
      day,
      intent: "REFERENCE",
      title: "Flight receipt to San Francisco",
      summary: "Flight receipt for a Monday trip to San Francisco.",
      source: "Gmail",
      category: "receipt",
      tags: ["flight", "receipt", "travel", "san-francisco"],
      text: "Flight receipt: NYC to San Francisco, confirmation ORB123, departs Monday morning. Total paid $248.40.",
      domain: "delta.com",
    }),
    item({
      id: "demo-recipe-01",
      day,
      intent: "REFERENCE",
      title: "Miso salmon recipe",
      summary: "Recipe saved for miso salmon with ginger rice.",
      source: "Chrome",
      category: "recipe",
      tags: ["recipe", "salmon", "dinner"],
      text: "Recipe to try: miso salmon with ginger rice. Marinade uses miso, mirin, soy sauce, and honey.",
      domain: "cooking.example",
    }),
    item({
      id: "demo-order-espresso-01",
      day,
      intent: "REFERENCE",
      title: "Espresso grinder order",
      summary: "Order confirmation for a hand grinder.",
      source: "Gmail",
      category: "order",
      tags: ["receipt", "coffee", "order"],
      text: "Order confirmation for hand espresso grinder. Estimated delivery Tuesday. Return window closes June 15.",
      domain: "shop.example",
    }),
    item({
      id: "demo-place-hotel-01",
      day,
      intent: "REFERENCE",
      title: "Hotel address near Moscone",
      summary: "Travel note with hotel address near Moscone Center.",
      source: "Maps",
      category: "place",
      tags: ["travel", "hotel", "san-francisco"],
      text: "Saved hotel address near Moscone Center for the startup event trip. Check-in starts at 3pm.",
      domain: "maps.google.com",
    }),
    item({
      id: "demo-chat-reply-01",
      day,
      intent: "FOLLOW_UP",
      title: "Reply to Raizy",
      summary: "Message follow-up asking about Monday timing.",
      source: "Messages",
      category: "chat",
      tags: ["reply", "message", "raizy"],
      text: "Raizy asked if Monday afternoon works after the startup event. Need to reply with availability.",
    }),
    item({
      id: "demo-read-later-01",
      day,
      intent: "READ_LATER",
      title: "Article about mobile memory systems",
      summary: "Saved article about attention and memory on mobile.",
      source: "Chrome",
      category: "read-later",
      tags: ["read-later", "memory", "mobile"],
      text: "Article saved for later: mobile attention memory systems and quiet daybook design patterns.",
      domain: "research.example",
    }),
    item({
      id: "demo-calendar-01",
      day,
      intent: "FOLLOW_UP",
      title: "Dentist appointment reminder",
      summary: "Reminder to move dentist appointment.",
      source: "Calendar",
      category: "event",
      tags: ["appointment", "calendar", "health"],
      text: "Dentist appointment reminder conflicts with Monday travel. Need to reschedule before flight.",
    }),
    item({
      id: "demo-receipt-books-01",
      day,
      intent: "REFERENCE",
      title: "Book receipt",
      summary: "Receipt for product strategy books.",
      source: "Gmail",
      category: "receipt",
      tags: ["receipt", "books", "startup"],
      text: "Receipt for product strategy books, including a founder manual and user research guide.",
      domain: "bookshop.example",
    }),
    item({
      id: "demo-note-mvp-01",
      day,
      intent: "REFERENCE",
      title: "Monday MVP checklist",
      summary: "Checklist for Orbit MVP before Monday.",
      source: "Keep",
      category: "note",
      tags: ["mvp", "orbit", "startup"],
      text: "Monday MVP checklist: Diary works, Library search opens captures, cloud index stays compact, no raw screenshots in Atlas.",
    }),
    item({
      id: "demo-ticket-concert-01",
      day,
      intent: "REFERENCE",
      title: "Concert ticket",
      summary: "Concert ticket for next Friday.",
      source: "Gmail",
      category: "event",
      tags: ["ticket", "music", "event"],
      text: "Concert ticket saved for next Friday. Doors at 8pm. Add to shared calendar.",
      domain: "tickets.example",
    }),
    item({
      id: "demo-recipe-pasta-01",
      day,
      intent: "REFERENCE",
      title: "Lemon pasta recipe",
      summary: "Simple lemon pasta recipe.",
      source: "Instagram",
      category: "recipe",
      tags: ["recipe", "pasta", "dinner"],
      text: "Recipe clip: lemon pasta with parmesan, black pepper, and a little pasta water.",
    }),
    item({
      id: "demo-return-label-01",
      day,
      intent: "FOLLOW_UP",
      title: "Return label deadline",
      summary: "Return label expires soon.",
      source: "Gmail",
      category: "order",
      tags: ["return", "order", "deadline"],
      text: "Return label for headphones expires Monday. Print label or drop off before 5pm.",
      domain: "returns.example",
    }),
    item({
      id: "demo-place-ramen-01",
      day,
      intent: "REFERENCE",
      title: "Ramen place near hotel",
      summary: "Saved ramen place near the hotel.",
      source: "Maps",
      category: "place",
      tags: ["restaurant", "travel", "ramen"],
      text: "Ramen restaurant near the hotel, open late after startup event. Try spicy miso bowl.",
      domain: "maps.google.com",
    }),
    item({
      id: "demo-video-ai-01",
      day,
      intent: "READ_LATER",
      title: "Video about on-device AI",
      summary: "Watch later video about local AI routing.",
      source: "YouTube",
      category: "watch-later",
      tags: ["ai", "local-model", "watch-later"],
      text: "Watch later: on-device AI routing, local model manager, and cloud fallback design.",
      domain: "youtube.com",
    }),
    item({
      id: "demo-invoice-01",
      day,
      intent: "REFERENCE",
      title: "Cloud tools invoice",
      summary: "Invoice for developer tools.",
      source: "Gmail",
      category: "receipt",
      tags: ["invoice", "developer-tools", "receipt"],
      text: "Developer tools invoice for May. Includes hosting, API credits, and database usage.",
      domain: "billing.example",
    }),
    item({
      id: "demo-chat-mom-01",
      day,
      intent: "FOLLOW_UP",
      title: "Call mom about travel",
      summary: "Message asks for travel details.",
      source: "WhatsApp",
      category: "chat",
      tags: ["reply", "family", "travel"],
      text: "Mom asked for flight arrival time and hotel address for San Francisco travel.",
    }),
    item({
      id: "demo-bookmark-kg-01",
      day,
      intent: "READ_LATER",
      title: "Knowledge graph design note",
      summary: "Saved note about KG-backed agents.",
      source: "Chrome",
      category: "read-later",
      tags: ["knowledge-graph", "agent", "orbit"],
      text: "Knowledge graph design note: ground agent actions in captured evidence and cite source envelopes.",
      domain: "docs.example",
    }),
    item({
      id: "demo-photo-context-01",
      day,
      intent: "REFERENCE",
      title: "Whiteboard capture",
      summary: "Whiteboard notes from planning session.",
      source: "Photos",
      category: "image",
      tags: ["whiteboard", "planning", "mvp"],
      text: "Whiteboard capture summarized locally: MVP means Library search with cited local capture links before Monday.",
      contentType: "image_summary",
    }),
    item({
      id: "demo-shopping-list-01",
      day,
      intent: "FOLLOW_UP",
      title: "Dinner shopping list",
      summary: "Ingredients for recipe night.",
      source: "Notes",
      category: "todo",
      tags: ["shopping", "recipe", "dinner"],
      text: "Shopping list for recipe night: salmon, miso, ginger, rice, lemons, parmesan, pasta.",
    }),
  ];
}

function upsertEnvelope(item) {
  return {
    type: "memory_upsert",
    requestId: randomUUID(),
    payload: { item },
  };
}

function searchEnvelope(query) {
  return {
    type: "memory_search",
    requestId: randomUUID(),
    payload: { query, limit: 10 },
  };
}

function healthEnvelope() {
  return {
    type: "memory_health",
    requestId: randomUUID(),
    payload: {},
  };
}

function findBannedKey(value, path = "$") {
  if (Array.isArray(value)) {
    for (let i = 0; i < value.length; i += 1) {
      const found = findBannedKey(value[i], `${path}[${i}]`);
      if (found) return found;
    }
    return null;
  }
  if (value !== null && typeof value === "object") {
    for (const [key, nested] of Object.entries(value)) {
      if (BANNED_KEYS.has(key) || /(?:Secret|Token|Key)$/i.test(key)) {
        return `${path}.${key}`;
      }
      const found = findBannedKey(nested, `${path}.${key}`);
      if (found) return found;
    }
  }
  return null;
}

async function inspectAtlas({ atlasUri, dbName, collectionName, userId, envelopeIds }) {
  if (!atlasUri) {
    console.log("atlas_inspection=skipped_missing_mongodb_atlas_uri");
    return;
  }
  const client = new MongoClient(atlasUri, { serverSelectionTimeoutMS: 10_000 });
  await client.connect();
  try {
    const docs = await client
      .db(dbName)
      .collection(collectionName)
      .find({ userId, envelopeId: { $in: envelopeIds } })
      .toArray();
    const banned = docs
      .map((doc) => ({ envelopeId: doc.envelopeId, path: findBannedKey(doc) }))
      .filter((result) => result.path);
    console.log(`atlas_docs_found=${docs.length}`);
    console.log(`atlas_banned_field_count=${banned.length}`);
    for (const item of banned) {
      console.log(`atlas_banned_field=${item.envelopeId}:${item.path}`);
    }
    if (banned.length > 0) {
      throw new Error("Atlas inspection found banned fields");
    }
  } finally {
    await client.close();
  }
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

  const dbName = pick(process.env.MONGODB_DB, envLocal.MONGODB_DB) ?? "orbit_dev";
  const collectionName =
    pick(process.env.MONGODB_MEMORY_COLLECTION, envLocal.MONGODB_MEMORY_COLLECTION) ??
    "memory_items";
  const atlasUri = pick(process.env.MONGODB_ATLAS_URI, envLocal.MONGODB_ATLAS_URI);

  const { accessToken, userId } = await signIn({
    supabaseUrl,
    publishableKey,
    email,
    password,
  });
  console.log(`supabase_session_user=${userId}`);

  const health = await callGateway(
    { gatewayUrl, accessToken },
    healthEnvelope(),
  );
  console.log(`gateway_health_atlas_ok=${health.health?.atlasOk === true ? "yes" : "no"}`);
  if (health.health?.atlasOk !== true) {
    console.log(`gateway_health_env=${JSON.stringify(health.health?.env ?? {})}`);
    console.log(`gateway_health_error_name=${health.health?.atlasErrorName ?? "<none>"}`);
    console.log(`gateway_health_error_message=${health.health?.atlasErrorMessage ?? "<none>"}`);
  }

  const items = demoItems();
  for (const demoItem of items) {
    const result = await callGateway(
      { gatewayUrl, accessToken },
      upsertEnvelope(demoItem),
    );
    console.log(`upserted=${result.envelopeId}`);
  }

  for (const query of DEFAULT_SEARCHES) {
    const result = await callGateway(
      { gatewayUrl, accessToken },
      searchEnvelope(query),
    );
    const count = Array.isArray(result.results) ? result.results.length : 0;
    const top = result.results?.[0]?.envelopeId ?? "<none>";
    console.log(`search="${query}" count=${count} top=${top}`);
    if (count === 0) {
      throw new Error(`Expected at least one result for "${query}"`);
    }
  }

  await inspectAtlas({
    atlasUri,
    dbName,
    collectionName,
    userId,
    envelopeIds: items.map((it) => it.envelopeId),
  });

  console.log("demo_seed_ok=yes");
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : String(error));
  process.exitCode = 1;
});
