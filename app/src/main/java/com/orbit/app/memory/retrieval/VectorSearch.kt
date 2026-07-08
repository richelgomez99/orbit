package com.orbit.app.memory.retrieval

import com.orbit.app.cluster.SimilarityEngine

/**
 * Phase B — brute-force cosine top-K over stored embeddings. At Orbit's corpus
 * size (hundreds–low thousands) a linear scan is sub-millisecond, so no ANN
 * index is needed (see roadmap §6). Reuses the existing [SimilarityEngine]
 * cosine so there is one cosine implementation in the app.
 *
 * Candidates whose dimensionality does not match the query are skipped (never
 * cross-model cosine — FR-038/039), so a mixed store degrades gracefully rather
 * than throwing.
 */
object VectorSearch {

    data class Scored(val id: String, val score: Float)

    fun cosineTopK(
        query: FloatArray,
        candidates: Map<String, FloatArray>,
        k: Int,
        minScore: Float = Float.NEGATIVE_INFINITY,
    ): List<Scored> {
        if (query.isEmpty() || k <= 0) return emptyList()
        return candidates.asSequence()
            .filter { (_, vec) -> vec.size == query.size }
            .map { (id, vec) -> Scored(id, SimilarityEngine.cosine(query, vec)) }
            .filter { it.score >= minScore }
            .sortedByDescending { it.score }
            .take(k)
            .toList()
    }
}
