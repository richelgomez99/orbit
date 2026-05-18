package com.orbit.app.understanding

import com.orbit.app.data.model.AppCategory
import com.orbit.app.understanding.engine.SourceIdentityDomainResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SourceIdentityDomainResolverTest {

    @Test
    fun resolve_youtubeUrlUsesProviderEvidence() {
        val result = SourceIdentityDomainResolver.resolve(
            canonicalUrl = "https://youtube.com/watch?v=abc",
            foregroundPackageName = null
        )
        assertEquals("YouTube", result.provider)
        assertEquals(AppCategory.VIDEO, result.category)
    }

    @Test
    fun resolve_twitterUrlUsesProviderEvidence() {
        val result = SourceIdentityDomainResolver.resolve(
            canonicalUrl = "https://x.com/example/status/1",
            foregroundPackageName = null
        )
        assertEquals("Twitter/X", result.provider)
        assertEquals(AppCategory.SOCIAL, result.category)
    }

    @Test
    fun resolve_unknownDomainDoesNotInventProvider() {
        val result = SourceIdentityDomainResolver.resolve(
            canonicalUrl = "https://example.invalid/story",
            foregroundPackageName = null
        )
        assertNull(result.provider)
        assertEquals(AppCategory.OTHER, result.category)
    }

    @Test
    fun resolve_appOnlyUsesForegroundAppLabel() {
        val result = SourceIdentityDomainResolver.resolve(
            canonicalUrl = null,
            foregroundPackageName = "com.instagram.android"
        )
        assertNull(result.provider)
        assertEquals("Instagram", result.appLabel)
        assertEquals(AppCategory.SOCIAL, result.category)
    }

    @Test
    fun resolve_noEvidenceIsUnknown() {
        val result = SourceIdentityDomainResolver.resolve(
            canonicalUrl = null,
            foregroundPackageName = null
        )
        assertNull(result.provider)
        assertNull(result.appLabel)
        assertEquals(AppCategory.UNKNOWN_SOURCE, result.category)
    }
}
