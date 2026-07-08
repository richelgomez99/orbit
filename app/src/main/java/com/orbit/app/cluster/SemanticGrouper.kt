package com.orbit.app.cluster

/**
 * Phase B / spec-020 (#28) — group items by *meaning* instead of by broad
 * category. This is the fix for the Curious Agent bug where a concert ticket
 * was lumped with startup/founder events (all `EVENT_TICKET_RESERVATION`) and a
 * grinder order with a flight receipt (all `RECEIPT_OR_ORDER`): category
 * grouping can't tell those apart, semantic grouping can.
 *
 * Greedy average-linkage agglomeration over embeddings: an item joins the
 * existing group it's most similar to when its mean cosine to that group clears
 * [threshold] (the same 0.7 the cluster subsystem uses), else it starts a new
 * group. Pure and deterministic given a stable input order — no model here, it
 * consumes vectors produced upstream by `embed()`.
 *
 * Dimension-mismatched items (different embedding model) never cross-compare
 * (FR-038/039): a candidate whose vector size differs is treated as similarity
 * 0, so it falls into its own group rather than corrupting a match.
 */
object SemanticGrouper {

    data class Embedded<T>(val item: T, val vector: FloatArray)

    fun <T> group(
        items: List<Embedded<T>>,
        threshold: Float = SimilarityEngine.Thresholds.COSINE,
    ): List<List<T>> {
        val groups = mutableListOf<MutableList<Embedded<T>>>()
        for (item in items) {
            var best: MutableList<Embedded<T>>? = null
            var bestSim = Float.NEGATIVE_INFINITY
            for (group in groups) {
                val sim = meanCosine(item.vector, group)
                if (sim > bestSim) {
                    bestSim = sim
                    best = group
                }
            }
            if (best != null && bestSim >= threshold) {
                best.add(item)
            } else {
                groups.add(mutableListOf(item))
            }
        }
        return groups.map { group -> group.map { it.item } }
    }

    private fun <T> meanCosine(vector: FloatArray, group: List<Embedded<T>>): Float {
        if (group.isEmpty()) return Float.NEGATIVE_INFINITY
        var sum = 0f
        for (member in group) {
            sum += if (member.vector.size == vector.size) {
                SimilarityEngine.cosine(vector, member.vector)
            } else {
                0f // never cross-model cosine
            }
        }
        return sum / group.size
    }
}
