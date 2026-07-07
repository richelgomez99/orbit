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
    fun speedTierOfferedOn4gb_intelligenceRequires6gb() {
        val fourGb = LocalModelCatalog.offerableFor(hardware(4_096)).map { it.id }
        assertTrue("Speed tier must be offered on 4GB", fourGb.contains("gemma-3-1b-it-int4"))
        assertFalse("Intelligence tier must NOT be offered on 4GB", fourGb.contains("gemma-3-4b-it-int4"))

        val eightGb = LocalModelCatalog.offerableFor(hardware(8_192)).map { it.id }
        assertTrue(eightGb.contains("gemma-3-1b-it-int4"))
        assertTrue("Intelligence tier available on 8GB", eightGb.contains("gemma-3-4b-it-int4"))
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
