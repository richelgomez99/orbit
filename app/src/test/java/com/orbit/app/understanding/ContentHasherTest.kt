package com.orbit.app.understanding

import com.orbit.app.understanding.engine.ContentHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ContentHasherTest {

    @Test
    fun hash_isStableForIdenticalBytes() {
        val bytes = "orbit".toByteArray()
        assertEquals(ContentHasher.hash(bytes), ContentHasher.hash(bytes))
    }

    @Test
    fun hash_changesWhenBytesChange() {
        assertNotEquals(
            ContentHasher.hash("orbit".toByteArray()),
            ContentHasher.hash("orbits".toByteArray())
        )
    }

    @Test
    fun hash_matchesKnownSha256Vector() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            ContentHasher.hash("abc".toByteArray())
        )
    }

    @Test
    fun normalizedTextHash_lowercasesAndCollapsesWhitespace() {
        assertEquals(
            ContentHasher.normalizedTextHash("  Same   Article\nText "),
            ContentHasher.normalizedTextHash("same article text")
        )
    }
}
