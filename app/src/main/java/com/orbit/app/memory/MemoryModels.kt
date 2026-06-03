package com.orbit.app.memory

import kotlinx.serialization.Serializable

@Serializable
data class MemoryEvidenceSnippet(
    val kind: String,
    val label: String,
    val excerpt: String? = null,
    val source: String,
    val confidence: Float? = null,
    val hash: String? = null,
    val createdAtMillis: Long? = null
)

@Serializable
data class MemoryIndexItem(
    val envelopeId: String,
    val schemaVersion: Int = 1,
    val kind: String,
    val dayLocal: String,
    val createdAtMillis: Long,
    val intent: String,
    val contentType: String,
    val title: String? = null,
    val summary: String? = null,
    val sourceAppLabel: String? = null,
    val appCategory: String? = null,
    val canonicalUrl: String? = null,
    val domain: String? = null,
    val tags: List<String>,
    val evidence: List<MemoryEvidenceSnippet>,
    val compactText: String,
    val localContentHash: String? = null
)

@Serializable
data class MemorySearchResult(
    val envelopeId: String,
    val rank: Int,
    val score: Float,
    val semanticScore: Float? = null,
    val lexicalScore: Float? = null,
    val contextScore: Float? = null,
    val recencyScore: Float? = null,
    val retrievalMode: String? = null,
    val embeddingModel: String? = null,
    val title: String? = null,
    val summary: String? = null,
    val dayLocal: String,
    val createdAtMillis: Long,
    val intent: String,
    val sourceAppLabel: String? = null,
    val domain: String? = null,
    val matchedEvidence: List<MemoryEvidenceSnippet> = emptyList()
)

@Serializable
data class AskOrbitCitation(
    val citationId: String,
    val envelopeId: String,
    val title: String? = null,
    val excerpt: String? = null,
    val dayLocal: String,
    val sourceAppLabel: String? = null
)

@Serializable
data class AskOrbitAnswer(
    val status: String,
    val answer: String,
    val citations: List<AskOrbitCitation>,
    val candidates: List<MemorySearchResult>,
    val modelLabel: String,
    val retrievalMode: String? = null,
    val confidence: Float? = null,
    val limitations: List<String> = emptyList()
)
