package com.orbit.app.memory.retrieval

/**
 * Phase B — Reciprocal Rank Fusion. Blends several independently-ranked result
 * lists (keyword, graph facts, vector similarity) into one ranking without any
 * tuning or a reranker model — the 2026-standard fusion for small hybrid
 * retrieval (see `docs/agentic-memory-roadmap`).
 *
 * Each input list is ordered best-first. An item's fused score is the sum over
 * the lists it appears in of `1 / (k + rank)`, so appearing high in multiple
 * lists beats ranking first in only one. Pure and deterministic.
 */
object ReciprocalRankFusion {

    /** Standard RRF damping constant. Larger k flattens the contribution of top ranks. */
    const val DEFAULT_K: Int = 60

    fun <T> fuseScored(rankedLists: List<List<T>>, k: Int = DEFAULT_K): List<Scored<T>> {
        require(k > 0) { "k must be positive" }
        val scores = LinkedHashMap<T, Double>()
        for (list in rankedLists) {
            list.forEachIndexed { index, item ->
                scores[item] = (scores[item] ?: 0.0) + 1.0 / (k + index + 1)
            }
        }
        return scores.entries
            .map { Scored(it.key, it.value) }
            .sortedByDescending { it.score }
    }

    /** Convenience: the fused ranking as a plain ordered list. */
    fun <T> fuse(rankedLists: List<List<T>>, k: Int = DEFAULT_K): List<T> =
        fuseScored(rankedLists, k).map { it.item }

    data class Scored<T>(val item: T, val score: Double)
}
