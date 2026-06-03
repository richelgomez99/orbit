import { MongoClient, type Collection, type Document } from "mongodb";
import type {
  MemoryIndexItem,
  MemoryIndexItemInput,
  MemorySearchFilters,
  MemorySearchResult,
  MemoryHealthReport,
} from "../types.js";

let clientPromise: Promise<MongoClient> | null = null;

function requiredEnv(name: string): string {
  const value = process.env[name]?.trim();
  if (!value) throw new Error(`${name} is required`);
  return value;
}

async function getClient(): Promise<MongoClient> {
  if (!clientPromise) {
    clientPromise = new MongoClient(requiredEnv("MONGODB_ATLAS_URI"), {
      serverSelectionTimeoutMS: 10_000,
    }).connect();
  }
  return clientPromise;
}

export async function memoryCollection(): Promise<Collection<MemoryIndexItem>> {
  const client = await getClient();
  const dbName = process.env.MONGODB_DB?.trim() || "orbit_dev";
  const collectionName =
    process.env.MONGODB_MEMORY_COLLECTION?.trim() || "memory_items";
  return client.db(dbName).collection<MemoryIndexItem>(collectionName);
}

export async function memoryHealthReport(): Promise<MemoryHealthReport> {
  const env = {
    mongodbAtlasUriConfigured: Boolean(process.env.MONGODB_ATLAS_URI?.trim()),
    mongodbDbConfigured: Boolean(process.env.MONGODB_DB?.trim()),
    mongodbMemoryCollectionConfigured: Boolean(process.env.MONGODB_MEMORY_COLLECTION?.trim()),
    supabaseUrlConfigured: Boolean(process.env.SUPABASE_URL?.trim()),
  };
  try {
    const collection = await memoryCollection();
    await collection.db.admin().ping();
    return { atlasOk: true, env };
  } catch (error) {
    return {
      atlasOk: false,
      atlasErrorName: error instanceof Error ? error.name : typeof error,
      atlasErrorMessage: error instanceof Error
        ? error.message.slice(0, 240)
        : "non-error thrown",
      env,
    };
  }
}

export function buildMemoryDocument(
  userId: string,
  item: MemoryIndexItemInput,
  nowMillis: number,
): MemoryIndexItem {
  return {
    ...item,
    userId,
    updatedAtMillis: nowMillis,
    tombstonedAt: null,
  };
}

export async function upsertMemoryItem(
  collection: Collection<MemoryIndexItem>,
  userId: string,
  item: MemoryIndexItemInput,
  nowMillis: number,
): Promise<{ upserted: boolean; indexedAtMillis: number }> {
  const doc = buildMemoryDocument(userId, item, nowMillis);
  const result = await collection.updateOne(
    { userId, envelopeId: item.envelopeId },
    { $set: doc },
    { upsert: true },
  );
  return {
    upserted: Boolean(result.upsertedId),
    indexedAtMillis: nowMillis,
  };
}

export async function tombstoneMemoryItem(
  collection: Collection<MemoryIndexItem>,
  userId: string,
  envelopeId: string,
  tombstonedAtMillis: number,
): Promise<{ matched: boolean; modified: boolean }> {
  const result = await collection.updateOne(
    { userId, envelopeId },
    { $set: { tombstonedAt: tombstonedAtMillis, updatedAtMillis: tombstonedAtMillis } },
  );
  return {
    matched: result.matchedCount > 0,
    modified: result.modifiedCount > 0,
  };
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function buildSearchFilter(
  userId: string,
  query: string,
  filters?: MemorySearchFilters,
): Document {
  const regex = new RegExp(escapeRegExp(query), "i");
  const mongoFilter: Document = {
    userId,
    $or: [{ tombstonedAt: null }, { tombstonedAt: { $exists: false } }],
    $and: [
      {
        $or: [
          { compactText: regex },
          { title: regex },
          { summary: regex },
          { tags: regex },
          { "evidence.excerpt": regex },
          { domain: regex },
          { sourceAppLabel: regex },
        ],
      },
    ],
  };

  if (filters?.dayLocalStart || filters?.dayLocalEnd) {
    mongoFilter.dayLocal = {};
    if (filters.dayLocalStart) mongoFilter.dayLocal.$gte = filters.dayLocalStart;
    if (filters.dayLocalEnd) mongoFilter.dayLocal.$lte = filters.dayLocalEnd;
  }
  if (filters?.intent) mongoFilter.intent = filters.intent;
  if (filters?.appCategory) mongoFilter.appCategory = filters.appCategory;

  return mongoFilter;
}

function scoreItem(item: MemoryIndexItem, query: string): number {
  const q = query.toLowerCase();
  let score = 0;
  if (item.title?.toLowerCase().includes(q)) score += 5;
  if (item.summary?.toLowerCase().includes(q)) score += 3;
  if (item.tags.some((tag) => tag.toLowerCase().includes(q))) score += 2;
  if (item.compactText.toLowerCase().includes(q)) score += 1;
  return score;
}

function matchedEvidence(item: MemoryIndexItem, query: string) {
  const q = query.toLowerCase();
  return item.evidence
    .filter((evidence) => evidence.excerpt?.toLowerCase().includes(q))
    .slice(0, 3);
}

export async function searchMemoryItems(
  collection: Collection<MemoryIndexItem>,
  userId: string,
  query: string,
  filters?: MemorySearchFilters,
  limit = 10,
): Promise<MemorySearchResult[]> {
  const docs = await collection
    .find(buildSearchFilter(userId, query, filters))
    .sort({ dayLocal: -1, createdAtMillis: -1 })
    .limit(Math.min(Math.max(limit, 1), 20))
    .toArray();

  return docs
    .map((item, index) => ({
      envelopeId: item.envelopeId,
      rank: index + 1,
      score: scoreItem(item, query),
      title: item.title,
      summary: item.summary,
      dayLocal: item.dayLocal,
      createdAtMillis: item.createdAtMillis,
      intent: item.intent,
      sourceAppLabel: item.sourceAppLabel,
      domain: item.domain,
      matchedEvidence: matchedEvidence(item, query),
    }))
    .sort((a, b) => b.score - a.score || a.rank - b.rank)
    .map((result, index) => ({ ...result, rank: index + 1 }));
}
