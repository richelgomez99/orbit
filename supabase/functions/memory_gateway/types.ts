export type MemoryRequestType =
  | "memory_upsert"
  | "memory_tombstone"
  | "memory_search"
  | "memory_ask"
  | "memory_health";

export interface MemoryEvidenceSnippet {
  kind: string;
  label: string;
  excerpt?: string;
  source: string;
  confidence?: number;
  hash?: string;
  createdAtMillis?: number;
}

export interface MemoryIndexItemInput {
  envelopeId: string;
  schemaVersion: 1;
  kind: string;
  dayLocal: string;
  createdAtMillis: number;
  intent: string;
  contentType: string;
  title?: string;
  summary?: string;
  sourceAppLabel?: string;
  appCategory?: string;
  canonicalUrl?: string;
  domain?: string;
  tags: string[];
  evidence: MemoryEvidenceSnippet[];
  compactText: string;
  localContentHash?: string;
}

export interface MemoryIndexItem extends MemoryIndexItemInput {
  userId: string;
  updatedAtMillis: number;
  tombstonedAt?: number | null;
}

export interface MemoryUpsertRequest {
  type: "memory_upsert";
  requestId: string;
  payload: { item: MemoryIndexItemInput };
}

export interface MemoryTombstoneRequest {
  type: "memory_tombstone";
  requestId: string;
  payload: {
    envelopeId: string;
    reason: "LOCAL_DELETE" | "USER_DISABLED_CLOUD" | "SOURCE_INVALIDATED" | "HARD_PURGE";
    tombstonedAtMillis: number;
  };
}

export interface MemorySearchFilters {
  dayLocalStart?: string | null;
  dayLocalEnd?: string | null;
  intent?: string | null;
  appCategory?: string | null;
}

export interface MemorySearchRequest {
  type: "memory_search";
  requestId: string;
  payload: {
    query: string;
    filters?: MemorySearchFilters;
    limit?: number;
  };
}

export interface MemoryAskRequest {
  type: "memory_ask";
  requestId: string;
  payload: {
    question: string;
    filters?: MemorySearchFilters;
    limit?: number;
  };
}

export interface MemoryHealthRequest {
  type: "memory_health";
  requestId: string;
  payload: Record<string, never>;
}

export type MemoryGatewayRequest =
  | MemoryUpsertRequest
  | MemoryTombstoneRequest
  | MemorySearchRequest
  | MemoryAskRequest
  | MemoryHealthRequest;

export interface MemorySearchResult {
  envelopeId: string;
  rank: number;
  score: number;
  title?: string;
  summary?: string;
  dayLocal: string;
  createdAtMillis: number;
  intent: string;
  sourceAppLabel?: string;
  domain?: string;
  matchedEvidence: MemoryEvidenceSnippet[];
}

export interface Citation {
  citationId: string;
  envelopeId: string;
  title?: string;
  excerpt?: string;
  dayLocal: string;
  sourceAppLabel?: string;
}

export interface AskOrbitAnswer {
  status: "answered" | "insufficient_evidence";
  answer: string;
  citations: Citation[];
  candidates: MemorySearchResult[];
  modelLabel: "deterministic/extractive";
}

export interface MemoryHealthReport {
  atlasOk: boolean;
  atlasErrorName?: string;
  atlasErrorMessage?: string;
  env: {
    mongodbAtlasUriConfigured: boolean;
    mongodbDbConfigured: boolean;
    mongodbMemoryCollectionConfigured: boolean;
    supabaseUrlConfigured: boolean;
  };
}

export type MemoryGatewaySuccessResponse =
  | {
      type: "memory_upsert_response";
      requestId: string;
      envelopeId: string;
      upserted: boolean;
      indexedAtMillis: number;
    }
  | {
      type: "memory_tombstone_response";
      requestId: string;
      envelopeId: string;
      matched: boolean;
      modified: boolean;
    }
  | {
      type: "memory_search_response";
      requestId: string;
      results: MemorySearchResult[];
    }
  | {
      type: "memory_ask_response";
      requestId: string;
      answer: AskOrbitAnswer;
    }
  | {
      type: "memory_health_response";
      requestId: string;
      health: MemoryHealthReport;
    };

export interface MemoryGatewayErrorResponse {
  type: "error";
  requestId: string;
  code:
    | "UNAUTHORIZED"
    | "VALIDATION_FAILED"
    | "ATLAS_UNAVAILABLE"
    | "NOT_FOUND"
    | "INSUFFICIENT_EVIDENCE"
    | "INTERNAL";
  message: string;
}

export type MemoryGatewayResponse =
  | MemoryGatewaySuccessResponse
  | MemoryGatewayErrorResponse;
