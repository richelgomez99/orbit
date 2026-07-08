package com.orbit.app.cluster

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticGrouperTest {

    private fun <T> emb(item: T, vararg v: Float) = SemanticGrouper.Embedded(item, v)

    @Test
    fun separatesTheConcertFromTheWorkEvents() {
        // The exact real-world case: two professional events cluster; the
        // concert (a different direction in embedding space) falls out — where
        // category grouping ("event or reservation") wrongly merged all three.
        val groups = SemanticGrouper.group(
            listOf(
                emb("founder office hours", 0.98f, 0.10f, 0.0f),
                emb("startup showcase", 0.95f, 0.20f, 0.0f),
                emb("concert ticket", 0.0f, 0.10f, 0.99f),
            )
        )
        assertEquals(2, groups.size)
        val work = groups.first { it.contains("founder office hours") }
        assertTrue(work.contains("startup showcase"))
        assertTrue(work.none { it == "concert ticket" })
    }

    @Test
    fun identicalVectorsGroupTogether() {
        val groups = SemanticGrouper.group(
            listOf(
                emb("a", 1f, 0f),
                emb("b", 1f, 0f),
                emb("c", 1f, 0f),
            )
        )
        assertEquals(1, groups.size)
        assertEquals(3, groups.single().size)
    }

    @Test
    fun orthogonalVectorsEachGetTheirOwnGroup() {
        val groups = SemanticGrouper.group(
            listOf(
                emb("x", 1f, 0f, 0f),
                emb("y", 0f, 1f, 0f),
                emb("z", 0f, 0f, 1f),
            )
        )
        assertEquals(3, groups.size)
    }

    @Test
    fun mismatchedDimensionNeverMergesIntoAGroup() {
        val groups = SemanticGrouper.group(
            listOf(
                emb("2d", 1f, 0f),
                emb("3d", 1f, 0f, 0f),
            )
        )
        assertEquals(2, groups.size)
    }

    @Test
    fun looserThresholdMergesMore() {
        val items = listOf(
            emb("a", 1f, 0f),
            emb("b", 0.6f, 0.8f), // cosine to a = 0.6
        )
        assertEquals(2, SemanticGrouper.group(items, threshold = 0.7f).size)
        assertEquals(1, SemanticGrouper.group(items, threshold = 0.5f).size)
    }
}
