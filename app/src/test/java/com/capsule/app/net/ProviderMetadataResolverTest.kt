package com.capsule.app.net

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderMetadataResolverTest {
    @Test
    fun youtubeRecognitionCoversFamilyHostsAndSchemeLessLinks() {
        assertTrue(ProviderMetadataResolver.isYouTubeUrl("https://youtu.be/abc123"))
        assertTrue(ProviderMetadataResolver.isYouTubeUrl("www.youtube.com/watch?v=abc123"))
        assertTrue(ProviderMetadataResolver.isYouTubeUrl("m.youtube.com/shorts/abc123"))
        assertTrue(ProviderMetadataResolver.isYouTubeUrl("music.youtube.com/watch?v=abc123"))
        assertTrue(ProviderMetadataResolver.isYouTubeUrl("https://www.youtube.com/live/abc123"))
        assertTrue(ProviderMetadataResolver.isYouTubeUrl("https://www.youtube.com/embed/abc123"))
        assertTrue(ProviderMetadataResolver.isYouTubeUrl("https://www.youtube-nocookie.com/embed/abc123"))
    }

    @Test
    fun youtubeRecognitionRejectsLookalikeHosts() {
        assertFalse(ProviderMetadataResolver.isYouTubeUrl("https://notyoutube.com/watch?v=abc123"))
        assertFalse(ProviderMetadataResolver.isYouTubeUrl("https://youtube.example.com/watch?v=abc123"))
    }
}