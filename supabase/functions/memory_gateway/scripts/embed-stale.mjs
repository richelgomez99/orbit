#!/usr/bin/env node

import { createHash } from "node:crypto";
import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";
import { MongoClient } from "mongodb";

const GATEWAY_DIR = resolve(import.meta.dirname, "..");
const DEFAULT_DB = "orbit_dev";
const DEFAULT_COLLECTION = "memory_items";
const DEFAULT_EMBEDDING_MODEL = "text-embedding-3-small";
const DEFAULT_EMBEDDING_DIMENSIONS = 1536;
const EMBEDDING_INPUT_MAX_CHARS = 3_000;

function loadDotEnv(path) {
  if (!existsSync(path)) return;
  for (const line of readFileSync(path, "utf8").split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const eq = trimmed.indexOf("=");
    if (eq < 0) continue;
    const key = trimmed.slice(0, eq).trim();
    if (process.env[key]) continue;
    let value = trimmed.slice(eq + 1).trim();
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }
    process.env[key] = value;
  }
}

function args() {
  const out = { limit: 50, allUsers: false, userId: null };
  for (let i = 2; i < process.argv.length; i += 1) {
    const arg = process.argv[i];
    if (arg === "--all-users") {
      out.allUsers = true;
    } else if (arg === "--user-id") {
      out.userId = process.argv[++i] ?? null;
    } else if (arg === "--limit") {
      out.limit = Number.parseInt(process.argv[++i] ?? "", 10);
    } else {
      throw new Error(`Unknown argument: ${arg}`);
    }
  }
  if (!out.allUsers && !out.userId) {
    throw new Error("Pass --user-id <supabase-user-id> or --all-users");
  }
  if (!Number.isFinite(out.limit) || out.limit < 1 || out.limit > 500) {
    throw new Error("--limit must be between 1 and 500");
  }
  return out;
}

function requiredEnv(name) {
  const value = process.env[name]?.trim();
  if (!value) throw new Error(`${name} is required in .env.local or the shell`);
  return value;
}

function intEnv(name, fallback) {
  const value = process.env[name]?.trim();
  if (!value) return fallback;
  const parsed = Number.parseInt(value, 10);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new Error(`${name} must be a positive integer`);
  }
  return parsed;
}

function modelLabel(model) {
  return model.includes(":") ? model : `openai:${model}`;
}

function clean(value) {
  const trimmed = typeof value === "string" ? value.replace(/\s+/g, " ").trim() : "";
  return trimmed || null;
}

function pushPart(parts, label, value) {
  const cleaned = clean(value);
  if (cleaned) parts.push(`${label}: ${cleaned}`);
}

function buildEmbeddingInput(item) {
  const parts = [];
  pushPart(parts, "title", item.title);
  pushPart(parts, "summary", item.summary);
  if (Array.isArray(item.tags) && item.tags.length > 0) pushPart(parts, "tags", item.tags.join(", "));
  pushPart(parts, "source", item.sourceAppLabel);
  pushPart(parts, "category", item.appCategory);
  pushPart(parts, "domain", item.domain);
  for (const evidence of Array.isArray(item.evidence) ? item.evidence : []) {
    pushPart(parts, `evidence ${evidence.label ?? "clue"}`, evidence.excerpt);
  }
  pushPart(parts, "compact", item.compactText);
  const text = parts.join("\n").slice(0, EMBEDDING_INPUT_MAX_CHARS);
  return {
    text,
    hash: createHash("sha256").update(text).digest("hex"),
  };
}

async function embedText({ apiKey, model, dimensions, text }) {
  const response = await fetch("https://api.openai.com/v1/embeddings", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model,
      input: text,
      dimensions,
    }),
  });
  if (!response.ok) {
    throw new Error(`OpenAI embeddings HTTP ${response.status}`);
  }
  const body = await response.json();
  const vector = body?.data?.[0]?.embedding;
  if (!Array.isArray(vector) || vector.length !== dimensions) {
    throw new Error(`Embedding vector had invalid dimensions: ${Array.isArray(vector) ? vector.length : "missing"}`);
  }
  return vector.map(Number);
}

loadDotEnv(resolve(GATEWAY_DIR, ".env.local"));

const options = args();
const embeddingModel = process.env.MEMORY_EMBEDDING_MODEL?.trim() || DEFAULT_EMBEDDING_MODEL;
const embeddingDimensions = intEnv("MEMORY_EMBEDDING_DIMENSIONS", DEFAULT_EMBEDDING_DIMENSIONS);
const embeddingModelLabel = modelLabel(embeddingModel);
const nowMillis = Date.now();

const client = new MongoClient(requiredEnv("MONGODB_ATLAS_URI"), {
  serverSelectionTimeoutMS: 10_000,
});

try {
  await client.connect();
  const dbName = process.env.MONGODB_DB?.trim() || DEFAULT_DB;
  const collectionName = process.env.MONGODB_MEMORY_COLLECTION?.trim() || DEFAULT_COLLECTION;
  const collection = client.db(dbName).collection(collectionName);
  const staleFilter = {
    ...(options.userId ? { userId: options.userId } : {}),
    $or: [{ tombstonedAt: null }, { tombstonedAt: { $exists: false } }],
    $and: [
      {
        $or: [
          { embeddingStatus: { $exists: false } },
          { embeddingStatus: { $in: ["none", "stale", "failed"] } },
          { embeddingModel: { $ne: embeddingModelLabel } },
          { embeddingDimensions: { $ne: embeddingDimensions } },
        ],
      },
    ],
  };
  const items = await collection
    .find(staleFilter)
    .sort({ updatedAtMillis: -1 })
    .limit(options.limit)
    .toArray();

  let embedded = 0;
  let failed = 0;
  for (const item of items) {
    const input = buildEmbeddingInput(item);
    try {
      const embedding = await embedText({
        apiKey: requiredEnv("OPENAI_API_KEY"),
        model: embeddingModel,
        dimensions: embeddingDimensions,
        text: input.text,
      });
      await collection.updateOne(
        { userId: item.userId, envelopeId: item.envelopeId },
        {
          $set: {
            embedding,
            embeddingModel: embeddingModelLabel,
            embeddingDimensions,
            embeddingInputHash: input.hash,
            embeddedAtMillis: nowMillis,
            embeddingStatus: "ready",
            updatedAtMillis: nowMillis,
          },
          $unset: { embeddingErrorCode: "" },
        },
      );
      embedded += 1;
    } catch (error) {
      failed += 1;
      await collection.updateOne(
        { userId: item.userId, envelopeId: item.envelopeId },
        {
          $set: {
            embeddingStatus: "failed",
            embeddingErrorCode: error instanceof Error ? error.message.slice(0, 64) : "unknown",
            updatedAtMillis: nowMillis,
          },
        },
      );
    }
  }

  console.log(JSON.stringify({
    ok: true,
    db: dbName,
    collection: collectionName,
    model: embeddingModelLabel,
    dimensions: embeddingDimensions,
    scanned: items.length,
    embedded,
    failed,
  }, null, 2));
} finally {
  await client.close();
}
