export type MemoryRequestType =
  | "memory_upsert"
  | "memory_tombstone"
  | "memory_search"
  | "memory_ask"
  | "memory_embed_stale"
  | "memory_semantic_search"
  | "memory_grounded_ask"
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
  embedding?: number[];
  embeddingModel?: string;
  embeddingDimensions?: number;
  embeddingInputHash?: string;
  embeddedAtMillis?: number;
  embeddingStatus?: "none" | "ready" | "failed" | "stale" | "disabled";
  embeddingErrorCode?: string;
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

export type SemanticSearchMode = "hybrid" | "vector_only" | "lexical_only";
export type RetrievalMode = SemanticSearchMode | "local_fallback";

export interface MemoryEmbedStaleRequest {
  type: "memory_embed_stale";
  requestId: string;
  payload: {
    limit?: number;
    dryRun?: boolean;
  };
}

export interface MemorySemanticSearchRequest {
  type: "memory_semantic_search";
  requestId: string;
  payload: {
    query: string;
    filters?: MemorySearchFilters;
    limit?: number;
    mode?: SemanticSearchMode;
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

export interface MemoryGroundedAskRequest {
  type: "memory_grounded_ask";
  requestId: string;
  payload: {
    question: string;
    filters?: MemorySearchFilters;
    limit?: number;
    allowSynthesis?: boolean;
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
  | MemoryEmbedStaleRequest
  | MemorySemanticSearchRequest
  | MemoryGroundedAskRequest
  | MemoryHealthRequest;

export interface MemorySearchResult {
  envelopeId: string;
  rank: number;
  score: number;
  semanticScore?: number;
  lexicalScore?: number;
  contextScore?: number;
  recencyScore?: number;
  retrievalMode?: RetrievalMode;
  embeddingModel?: string;
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
  status:
    | "answered"
    | "insufficient_evidence"
    | "sensitive_refusal"
    | "provider_unavailable";
  answer: string;
  citations: Citation[];
  candidates: MemorySearchResult[];
  modelLabel: string;
  retrievalMode?: RetrievalMode;
  confidence?: number;
  limitations?: string[];
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
      type: "memory_embed_stale_response";
      requestId: string;
      embedded: number;
      skipped: number;
      failed: number;
      modelLabel: string;
      dimensions: number;
    }
  | {
      type: "memory_semantic_search_response";
      requestId: string;
      results: MemorySearchResult[];
    }
  | {
      type: "memory_ask_response";
      requestId: string;
      answer: AskOrbitAnswer;
    }
  | {
      type: "memory_grounded_ask_response";
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
    | "EMBEDDING_PROVIDER_UNAVAILABLE"
    | "VECTOR_INDEX_UNAVAILABLE"
    | "VECTOR_DIMENSION_MISMATCH"
    | "SEMANTIC_SEARCH_UNAVAILABLE"
    | "GROUNDED_ASK_UNAVAILABLE"
    | "NOT_FOUND"
    | "INSUFFICIENT_EVIDENCE"
    | "INTERNAL";
  message: string;
}

export type MemoryGatewayResponse =
  | MemoryGatewaySuccessResponse
  | MemoryGatewayErrorResponse;
