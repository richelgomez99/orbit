package com.orbit.app.memory.retrieval

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VectorSearchTest {

    @Test
    fun ranksByCosineAndTakesK() {
        val query = floatArrayOf(1f, 0f)
        val candidates = mapOf(
            "same" to floatArrayOf(1f, 0f),        // cosine 1.0
            "diagonal" to floatArrayOf(1f, 1f),    // cosine ~0.707
            "orthogonal" to floatArrayOf(0f, 1f),  // cosine 0.0
        )
        val top = VectorSearch.cosineTopK(query, candidates, k = 2)
        assertEquals(listOf("same", "diagonal"), top.map { it.id })
        assertEquals(1.0f, top.first().score, 1e-4f)
    }

    @Test
    fun mismatchedDimensionIsSkipped() {
        val query = floatArrayOf(1f, 0f)
        val candidates = mapOf(
            "ok" to floatArrayOf(1f, 0f),
            "wrongDim" to floatArrayOf(1f, 0f, 0f),
        )
        val top = VectorSearch.cosineTopK(query, candidates, k = 5)
        assertEquals(listOf("ok"), top.map { it.id })
    }

    @Test
    fun minScoreFiltersWeakMatches() {
        val query = floatArrayOf(1f, 0f)
        val candidates = mapOf(
            "strong" to floatArrayOf(1f, 0f),
            "weak" to floatArrayOf(0f, 1f),
        )
        val top = VectorSearch.cosineTopK(query, candidates, k = 5, minScore = 0.5f)
        assertEquals(listOf("strong"), top.map { it.id })
    }

    @Test
    fun emptyQueryOrZeroKYieldsEmpty() {
        assertTrue(VectorSearch.cosineTopK(floatArrayOf(), mapOf("a" to floatArrayOf(1f)), 3).isEmpty())
        assertTrue(VectorSearch.cosineTopK(floatArrayOf(1f), mapOf("a" to floatArrayOf(1f)), 0).isEmpty())
    }
}
