package com.orbit.app.understanding

import com.orbit.app.understanding.domain.EvidenceLevel
import com.orbit.app.understanding.domain.UnderstandingMode
import com.orbit.app.understanding.domain.UnderstandingStatus
import com.orbit.app.understanding.engine.BasicCaptureInput
import com.orbit.app.understanding.engine.BasicUnderstandingEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BasicUnderstandingEngineTest {

    @Test
    fun understand_urlDefaultsToBasicWithoutNetworkWork() = runTest {
        val result = BasicUnderstandingEngine().understand(
            BasicCaptureInput(
                captureId = "capture-1",
                rawUrl = "https://example.com/story?utm_source=share",
                foregroundPackageName = "com.android.chrome"
            )
        )

        assertEquals(UnderstandingMode.BASIC, result.mode)
        assertEquals(UnderstandingStatus.READY, result.status)
        assertEquals("https://example.com/story", result.canonicalUrl)
        assertNotNull(result.contentHashHex)
        assertTrue(result.groundingConstraints.constraints.contains("basic-local-only"))
        assertEquals(EvidenceLevel.METADATA_ONLY, result.groundingConstraints.evidenceLevel)
    }

    @Test
    fun understand_screenshotOnlyIsLimitedVisualOnly() = runTest {
        val result = BasicUnderstandingEngine().understand(
            BasicCaptureInput(
                captureId = "capture-2",
                imageUri = "content://media/screenshot/1"
            )
        )

        assertEquals(UnderstandingMode.BASIC, result.mode)
        assertEquals(UnderstandingStatus.READY, result.status)
        assertEquals(EvidenceLevel.VISUAL_ONLY, result.groundingConstraints.evidenceLevel)
        assertTrue(result.groundingConstraints.constraints.contains("Orbit read only visual content"))
    }
}
