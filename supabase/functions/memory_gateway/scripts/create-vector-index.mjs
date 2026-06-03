#!/usr/bin/env node

import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";
import { MongoClient } from "mongodb";

const GATEWAY_DIR = resolve(import.meta.dirname, "..");
const INDEX_NAME = "memory_embedding_v1";
const DEFAULT_DB = "orbit_dev";
const DEFAULT_COLLECTION = "memory_items";
const DEFAULT_DIMENSIONS = 1536;

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

loadDotEnv(resolve(GATEWAY_DIR, ".env.local"));

const client = new MongoClient(requiredEnv("MONGODB_ATLAS_URI"), {
  serverSelectionTimeoutMS: 10_000,
});

try {
  await client.connect();
  const dbName = process.env.MONGODB_DB?.trim() || DEFAULT_DB;
  const collectionName = process.env.MONGODB_MEMORY_COLLECTION?.trim() || DEFAULT_COLLECTION;
  const dimensions = intEnv("MEMORY_EMBEDDING_DIMENSIONS", DEFAULT_DIMENSIONS);
  const collection = client.db(dbName).collection(collectionName);

  const existing = await collection.listSearchIndexes(INDEX_NAME).toArray();
  if (existing.length > 0) {
    console.log(JSON.stringify({
      ok: true,
      status: "exists",
      db: dbName,
      collection: collectionName,
      index: INDEX_NAME,
    }, null, 2));
    process.exit(0);
  }

  await collection.createSearchIndex({
    name: INDEX_NAME,
    type: "vectorSearch",
    definition: {
      fields: [
        {
          type: "vector",
          path: "embedding",
          numDimensions: dimensions,
          similarity: "cosine",
        },
        { type: "filter", path: "userId" },
        { type: "filter", path: "tombstonedAt" },
        { type: "filter", path: "embeddingStatus" },
        { type: "filter", path: "embeddingModel" },
        { type: "filter", path: "embeddingDimensions" },
        { type: "filter", path: "dayLocal" },
        { type: "filter", path: "intent" },
        { type: "filter", path: "appCategory" },
      ],
    },
  });

  console.log(JSON.stringify({
    ok: true,
    status: "created",
    db: dbName,
    collection: collectionName,
    index: INDEX_NAME,
    dimensions,
  }, null, 2));
} finally {
  await client.close();
}
