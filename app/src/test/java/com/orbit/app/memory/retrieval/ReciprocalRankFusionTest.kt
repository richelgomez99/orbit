package com.orbit.app.memory.retrieval

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReciprocalRankFusionTest {

    @Test
    fun appearingInMultipleListsBeatsRankingFirstInOne() {
        // "b" is 2nd in both lists; "a" is 1st in one only. Consensus wins.
        val fused = ReciprocalRankFusion.fuse(
            listOf(
                listOf("a", "b", "c"),
                listOf("d", "b", "e"),
            )
        )
        assertEquals("b", fused.first())
    }

    @Test
    fun singleListPreservesOrder() {
        assertEquals(
            listOf("x", "y", "z"),
            ReciprocalRankFusion.fuse(listOf(listOf("x", "y", "z"))),
        )
    }

    @Test
    fun emptyInputYieldsEmpty() {
        assertTrue(ReciprocalRankFusion.fuse<String>(emptyList()).isEmpty())
        assertTrue(ReciprocalRankFusion.fuse(listOf(emptyList<String>())).isEmpty())
    }

    @Test
    fun scoresAccumulateAcrossLists() {
        val scored = ReciprocalRankFusion.fuseScored(
            listOf(listOf("a"), listOf("a"))
        )
        // "a" first in two lists: 2 * 1/(60+1)
        assertEquals(2.0 / 61.0, scored.single().score, 1e-9)
    }

    @Test
    fun smallerKSharpensTopRankAdvantage() {
        val lists = listOf(listOf("top", "second"))
        val sharp = ReciprocalRankFusion.fuseScored(lists, k = 1)
        val flat = ReciprocalRankFusion.fuseScored(lists, k = 1000)
        val sharpGap = sharp[0].score - sharp[1].score
        val flatGap = flat[0].score - flat[1].score
        assertTrue("small k should widen the top-rank gap", sharpGap > flatGap)
    }
}
