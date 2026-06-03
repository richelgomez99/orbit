import { z } from "zod";

const UUID_V4_REGEX =
  /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

const RequestId = z.string().regex(UUID_V4_REGEX);
const DayLocal = z.string().regex(/^\d{4}-\d{2}-\d{2}$/);

const TITLE_MAX = 140;
const SUMMARY_MAX = 500;
const EVIDENCE_EXCERPT_MAX = 240;
const EVIDENCE_MAX = 5;
const TAG_MAX = 64;
const TAGS_MAX = 20;
const COMPACT_TEXT_MAX = 1600;
const QUERY_MAX = 500;
const QUESTION_MAX = 1000;
const LIMIT_MAX = 20;
const EMBED_BATCH_LIMIT_MAX = 100;

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
  "apiKey",
  "openaiApiKey",
  "openai_api_key",
  "mongodbUri",
  "embeddingProviderSecret",
]);

function containsBannedKey(value: unknown): string | null {
  if (Array.isArray(value)) {
    for (const item of value) {
      const found = containsBannedKey(item);
      if (found) return found;
    }
    return null;
  }
  if (value !== null && typeof value === "object") {
    for (const [key, nested] of Object.entries(value as Record<string, unknown>)) {
      if (
        BANNED_KEYS.has(key) ||
        /(?:Secret|Token|Key)$/i.test(key)
      ) {
        return key;
      }
      const found = containsBannedKey(nested);
      if (found) return found;
    }
  }
  return null;
}

export const MemoryEvidenceSnippetSchema = z.object({
  kind: z.string().min(1).max(64),
  label: z.string().min(1).max(80),
  excerpt: z.string().max(EVIDENCE_EXCERPT_MAX).optional(),
  source: z.enum(["envelope", "understanding", "continuation", "note", "active_intent", "derived"]),
  confidence: z.number().min(0).max(1).optional(),
  hash: z.string().max(128).optional(),
  createdAtMillis: z.number().int().nonnegative().optional(),
}).strict();

export const MemoryIndexItemInputSchema = z.object({
  envelopeId: z.string().min(1).max(256),
  schemaVersion: z.literal(1),
  kind: z.string().min(1).max(64),
  dayLocal: DayLocal,
  createdAtMillis: z.number().int().nonnegative(),
  intent: z.string().min(1).max(128),
  contentType: z.string().min(1).max(64),
  title: z.string().max(TITLE_MAX).optional(),
  summary: z.string().max(SUMMARY_MAX).optional(),
  sourceAppLabel: z.string().max(128).optional(),
  appCategory: z.string().max(128).optional(),
  canonicalUrl: z.string().url().max(2048).optional(),
  domain: z.string().max(255).optional(),
  tags: z.array(z.string().min(1).max(TAG_MAX)).max(TAGS_MAX),
  evidence: z.array(MemoryEvidenceSnippetSchema).max(EVIDENCE_MAX),
  compactText: z.string().min(1).max(COMPACT_TEXT_MAX),
  localContentHash: z.string().max(128).optional(),
}).strict();

const FiltersSchema = z.object({
  dayLocalStart: DayLocal.nullable().optional(),
  dayLocalEnd: DayLocal.nullable().optional(),
  intent: z.string().max(128).nullable().optional(),
  appCategory: z.string().max(128).nullable().optional(),
}).strict().optional();

const UpsertRequestSchema = z.object({
  type: z.literal("memory_upsert"),
  requestId: RequestId,
  payload: z.object({ item: MemoryIndexItemInputSchema }).strict(),
}).strict();

const TombstoneRequestSchema = z.object({
  type: z.literal("memory_tombstone"),
  requestId: RequestId,
  payload: z.object({
    envelopeId: z.string().min(1).max(256),
    reason: z.enum(["LOCAL_DELETE", "USER_DISABLED_CLOUD", "SOURCE_INVALIDATED", "HARD_PURGE"]),
    tombstonedAtMillis: z.number().int().nonnegative(),
  }).strict(),
}).strict();

const SearchRequestSchema = z.object({
  type: z.literal("memory_search"),
  requestId: RequestId,
  payload: z.object({
    query: z.string().min(1).max(QUERY_MAX),
    filters: FiltersSchema,
    limit: z.number().int().min(1).max(LIMIT_MAX).optional(),
  }).strict(),
}).strict();

const SemanticSearchModeSchema = z.enum(["hybrid", "vector_only", "lexical_only"]);

const EmbedStaleRequestSchema = z.object({
  type: z.literal("memory_embed_stale"),
  requestId: RequestId,
  payload: z.object({
    limit: z.number().int().min(1).max(EMBED_BATCH_LIMIT_MAX).optional(),
    dryRun: z.boolean().optional(),
  }).strict(),
}).strict();

const SemanticSearchRequestSchema = z.object({
  type: z.literal("memory_semantic_search"),
  requestId: RequestId,
  payload: z.object({
    query: z.string().min(1).max(QUERY_MAX),
    filters: FiltersSchema,
    limit: z.number().int().min(1).max(LIMIT_MAX).optional(),
    mode: SemanticSearchModeSchema.optional(),
  }).strict(),
}).strict();

const AskRequestSchema = z.object({
  type: z.literal("memory_ask"),
  requestId: RequestId,
  payload: z.object({
    question: z.string().min(1).max(QUESTION_MAX),
    filters: FiltersSchema,
    limit: z.number().int().min(1).max(10).optional(),
  }).strict(),
}).strict();

const GroundedAskRequestSchema = z.object({
  type: z.literal("memory_grounded_ask"),
  requestId: RequestId,
  payload: z.object({
    question: z.string().min(1).max(QUESTION_MAX),
    filters: FiltersSchema,
    limit: z.number().int().min(1).max(10).optional(),
    allowSynthesis: z.boolean().optional(),
  }).strict(),
}).strict();

const HealthRequestSchema = z.object({
  type: z.literal("memory_health"),
  requestId: RequestId,
  payload: z.object({}).strict(),
}).strict();

export const MemoryGatewayRequestSchema = z.discriminatedUnion("type", [
  UpsertRequestSchema,
  TombstoneRequestSchema,
  SearchRequestSchema,
  AskRequestSchema,
  EmbedStaleRequestSchema,
  SemanticSearchRequestSchema,
  GroundedAskRequestSchema,
  HealthRequestSchema,
]).superRefine((value, ctx) => {
  const bannedKey = containsBannedKey(value);
  if (bannedKey) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      message: `banned field: ${bannedKey}`,
    });
  }
});

export { containsBannedKey };
