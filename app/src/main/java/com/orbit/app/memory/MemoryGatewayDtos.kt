package com.orbit.app.memory

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MemorySearchFilters(
    val dayLocalStart: String? = null,
    val dayLocalEnd: String? = null,
    val intent: String? = null,
    val appCategory: String? = null
)

@Serializable
sealed class MemoryGatewayRequest {
    abstract val requestId: String

    @Serializable
    @SerialName("memory_upsert")
    data class Upsert(
        override val requestId: String,
        val item: MemoryIndexItem
    ) : MemoryGatewayRequest()

    @Serializable
    @SerialName("memory_tombstone")
    data class Tombstone(
        override val requestId: String,
        val envelopeId: String,
        val reason: String,
        val tombstonedAtMillis: Long
    ) : MemoryGatewayRequest()

    @Serializable
    @SerialName("memory_search")
    data class Search(
        override val requestId: String,
        val query: String,
        val filters: MemorySearchFilters? = null,
        val limit: Int = 10
    ) : MemoryGatewayRequest()

    @Serializable
    @SerialName("memory_semantic_search")
    data class SemanticSearch(
        override val requestId: String,
        val query: String,
        val filters: MemorySearchFilters? = null,
        val limit: Int = 10,
        val mode: String = "hybrid"
    ) : MemoryGatewayRequest()

    @Serializable
    @SerialName("memory_ask")
    data class Ask(
        override val requestId: String,
        val question: String,
        val filters: MemorySearchFilters? = null,
        val limit: Int = 5
    ) : MemoryGatewayRequest()

    @Serializable
    @SerialName("memory_grounded_ask")
    data class GroundedAsk(
        override val requestId: String,
        val question: String,
        val filters: MemorySearchFilters? = null,
        val limit: Int = 5,
        val allowSynthesis: Boolean = true
    ) : MemoryGatewayRequest()
}

@Serializable
sealed class MemoryGatewayResponse {
    abstract val requestId: String

    @Serializable
    @SerialName("memory_upsert_response")
    data class UpsertResponse(
        override val requestId: String,
        val envelopeId: String,
        val upserted: Boolean,
        val indexedAtMillis: Long
    ) : MemoryGatewayResponse()

    @Serializable
    @SerialName("memory_tombstone_response")
    data class TombstoneResponse(
        override val requestId: String,
        val envelopeId: String,
        val matched: Boolean,
        val modified: Boolean
    ) : MemoryGatewayResponse()

    @Serializable
    @SerialName("memory_search_response")
    data class SearchResponse(
        override val requestId: String,
        val results: List<MemorySearchResult>
    ) : MemoryGatewayResponse()

    @Serializable
    @SerialName("memory_semantic_search_response")
    data class SemanticSearchResponse(
        override val requestId: String,
        val results: List<MemorySearchResult>
    ) : MemoryGatewayResponse()

    @Serializable
    @SerialName("memory_ask_response")
    data class AskResponse(
        override val requestId: String,
        val answer: AskOrbitAnswer
    ) : MemoryGatewayResponse()

    @Serializable
    @SerialName("memory_grounded_ask_response")
    data class GroundedAskResponse(
        override val requestId: String,
        val answer: AskOrbitAnswer
    ) : MemoryGatewayResponse()

    @Serializable
    @SerialName("error")
    data class Error(
        override val requestId: String,
        val code: String,
        val message: String
    ) : MemoryGatewayResponse()
}
