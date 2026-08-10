package com.capsule.app.understanding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SourceIdentityResolverTest {
    @Test
    fun providerUrlOutranksOriginAppAndPreservesBothFacts() {
        val resolved = SourceIdentityResolver.resolve(
            SourceIdentityResolver.Input(
                urls = listOf("https://www.youtube.com/watch?v=abc123"),
                foregroundAppLabel = "Brave",
                genericCategory = "BROWSER",
                evidenceIds = listOf("e1")
            )
        )

        assertEquals("youtube", resolved.providerKey)
        assertEquals("YouTube", resolved.displayLabel)
        assertEquals("via Brave", resolved.secondaryLabel)
        assertEquals(UnderstandingSourceGlyphKind.PROVIDER, resolved.glyphKind)
    }

    @Test
    fun appLabelOutranksCategoryWhenNoProviderUrlExists() {
        val resolved = SourceIdentityResolver.resolve(
            SourceIdentityResolver.Input(
                urls = emptyList(),
                foregroundAppLabel = "Messages",
                genericCategory = "MESSAGING",
                evidenceIds = listOf("e1")
            )
        )

        assertEquals("Messages", resolved.displayLabel)
        assertEquals(UnderstandingSourceGlyphKind.APP, resolved.glyphKind)
    }

    @Test
    fun categoryOnlyCaptureStaysGenericAndUnbranded() {
        val resolved = SourceIdentityResolver.resolve(
            SourceIdentityResolver.Input(
                urls = emptyList(),
                foregroundAppLabel = null,
                genericCategory = "VIDEO",
                evidenceIds = emptyList()
            )
        )

        assertNull(resolved.providerKey)
        assertNull(resolved.providerLabel)
        assertEquals("Video", resolved.displayLabel)
        assertEquals(UnderstandingSourceGlyphKind.GENERIC_CATEGORY, resolved.glyphKind)
    }
}