package com.orbit.app.ai.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalModelCatalogTest {

    private fun hardware(ramMb: Int) = DeviceAiHardwareProfile(
        totalRamMb = ramMb,
        apiLevel = 34,
        supportsVulkan = true,
        legacyNanoCapable = false,
    )

    @Test
    fun speedTierOffered_intelligenceNotOfferedUntilLoadableSource() {
        val fourGb = LocalModelCatalog.offerableFor(hardware(4_096)).map { it.id }
        assertTrue("Speed tier must be offered on 4GB", fourGb.contains("gemma-3-1b-it-int4"))
        assertFalse("Intelligence tier must NOT be offered on 4GB", fourGb.contains("gemma-3-4b-it-int4"))

        // Even with ample RAM, the 4B is NOT offered: its only source is a raw
        // TFL3 flatbuffer, not an Android MediaPipe .task (T022-026 finding,
        // androidTaskAvailable=false). RAM threshold + capabilities still hold
        // for the tier record — see the capability tests below.
        val eightGb = LocalModelCatalog.offerableFor(hardware(8_192)).map { it.id }
        assertTrue(eightGb.contains("gemma-3-1b-it-int4"))
        assertFalse(
            "4B must not be offered until a loadable Android .task source exists",
            eightGb.contains("gemma-3-4b-it-int4"),
        )
        assertFalse("4B is flagged not-Android-loadable", LocalModelCatalog.GEMMA_3_4B_INT4.androidTaskAvailable)
    }

    @Test
    fun lowRamDeviceGetsNoLocalModels() {
        assertTrue(LocalModelCatalog.offerableFor(hardware(2_048)).isEmpty())
    }

    @Test
    fun speedTierCapabilitiesExcludeDeepAskAndGenerativeUi() {
        val speed = LocalModelCatalog.GEMMA_3_1B_INT4.capabilities
        assertFalse(speed.contains(LocalAiCapability.GROUNDED_ASK))
        assertFalse(speed.contains(LocalAiCapability.GENERATIVE_UI))
        assertTrue(speed.contains(LocalAiCapability.BASIC_UNDERSTANDING))
        assertTrue(speed.contains(LocalAiCapability.ACTION_EXTRACTION))
    }

    @Test
    fun intelligenceTierUnlocksDeepCapabilities() {
        val intel = LocalModelCatalog.GEMMA_3_4B_INT4.capabilities
        assertTrue(intel.contains(LocalAiCapability.GROUNDED_ASK))
        assertTrue(intel.contains(LocalAiCapability.GENERATIVE_UI))
    }

    @Test
    fun downloadLabelIsHumanReadable() {
        assertEquals("529 MB", LocalModelCatalog.GEMMA_3_1B_INT4.approxDownloadLabel)
        assertEquals("gemma-3-1b-it-int4", LocalModelCatalog.byId("gemma-3-1b-it-int4")?.id)
    }
}
