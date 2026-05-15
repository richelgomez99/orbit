package com.orbit.app.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YoutubeUrlCanonicalizerTest {

    @Test
    fun canonicalize_expandsShortUrl() {
        assertEquals(
            "https://youtube.com/watch?v=dQw4w9WgXcQ",
            YoutubeUrlCanonicalizer.canonicalize("https://youtu.be/dQw4w9WgXcQ")
        )
    }

    @Test
    fun canonicalize_expandsShortsUrl() {
        assertEquals(
            "https://youtube.com/watch?v=dQw4w9WgXcQ",
            YoutubeUrlCanonicalizer.canonicalize("https://www.youtube.com/shorts/dQw4w9WgXcQ?feature=share")
        )
    }

    @Test
    fun canonicalize_stripsTrackingParamsViaCanonicalHasher() {
        assertEquals(
            "https://youtube.com/watch?v=dQw4w9WgXcQ",
            YoutubeUrlCanonicalizer.canonicalize("https://youtube.com/watch?v=dQw4w9WgXcQ&utm_source=share")
        )
    }

    @Test
    fun canonicalize_nonYoutubePassesThroughCanonicalForm() {
        assertEquals(
            "https://example.com/story",
            YoutubeUrlCanonicalizer.canonicalize("https://www.example.com/story/?utm_source=share")
        )
    }

    @Test
    fun canonicalize_blankReturnsNull() {
        assertNull(YoutubeUrlCanonicalizer.canonicalize("  "))
    }
}
