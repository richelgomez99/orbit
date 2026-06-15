package com.orbit.app.ai.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalModelSelectionPolicyTest {

    @Test
    fun localFirstOffUsesCloudWhenCloudEnabled() {
        val selection = LocalModelSelectionPolicy.select(
            localFirstEnabled = false,
            cloudRoutingEnabled = true,
            hardware = hardware(totalRamMb = 8_192),
            installedModels = emptyList(),
        )

        assertEquals(LocalModelRoute.CLOUD, selection.route)
        assertEquals(LocalModelTier.CLOUD, selection.tier)
    }

    @Test
    fun speedModelSupportsExtractionButNotGenerativeUi() {
        val selection = LocalModelSelectionPolicy.select(
            localFirstEnabled = true,
            cloudRoutingEnabled = true,
            hardware = hardware(totalRamMb = 4_096),
            installedModels = listOf(
                installed(
                    LocalModelTier.SPEED,
                    "speed-test",
                    LocalModelCapabilities.INTELLIGENCE,
                )
            ),
        )

        assertEquals(LocalModelRoute.LOCAL, selection.route)
        assertEquals(LocalModelTier.SPEED, selection.tier)
        assertTrue(selection.capabilities.contains(LocalAiCapability.ACTION_EXTRACTION))
        assertFalse(selection.capabilities.contains(LocalAiCapability.GENERATIVE_UI))
    }

    @Test
    fun intelligenceRequiresVulkanAndRam() {
        val noVulkan = LocalModelSelectionPolicy.select(
            localFirstEnabled = true,
            cloudRoutingEnabled = false,
            hardware = hardware(totalRamMb = 8_192, supportsVulkan = false),
            installedModels = listOf(installed(LocalModelTier.INTELLIGENCE, "intel-test")),
        )
        val enoughHardware = LocalModelSelectionPolicy.select(
            localFirstEnabled = true,
            cloudRoutingEnabled = false,
            hardware = hardware(totalRamMb = 8_192, supportsVulkan = true),
            installedModels = listOf(installed(LocalModelTier.INTELLIGENCE, "intel-test")),
        )

        assertEquals(LocalModelRoute.UNAVAILABLE, noVulkan.route)
        assertEquals(LocalModelRoute.LOCAL, enoughHardware.route)
        assertEquals(LocalModelTier.INTELLIGENCE, enoughHardware.tier)
        assertTrue(enoughHardware.capabilities.contains(LocalAiCapability.GENERATIVE_UI))
    }

    @Test
    fun intelligenceWinsOverSpeedWhenBothUsable() {
        val selection = LocalModelSelectionPolicy.select(
            localFirstEnabled = true,
            cloudRoutingEnabled = true,
            hardware = hardware(totalRamMb = 8_192, supportsVulkan = true),
            installedModels = listOf(
                installed(LocalModelTier.SPEED, "speed-test"),
                installed(LocalModelTier.INTELLIGENCE, "intel-test"),
            ),
        )

        assertEquals(LocalModelTier.INTELLIGENCE, selection.tier)
        assertEquals("intel-test", selection.modelLabel)
    }

    @Test
    fun legacyNanoIsFallbackLocalCandidate() {
        val selection = LocalModelSelectionPolicy.select(
            localFirstEnabled = true,
            cloudRoutingEnabled = false,
            hardware = hardware(totalRamMb = 4_096, legacyNanoCapable = true),
            installedModels = emptyList(),
        )

        assertEquals(LocalModelRoute.LOCAL, selection.route)
        assertEquals(LocalModelTier.LEGACY_NANO, selection.tier)
        assertFalse(selection.capabilities.contains(LocalAiCapability.GENERATIVE_UI))
    }

    @Test
    fun cloudDisabledWithoutUsableLocalModelFailsClosed() {
        val selection = LocalModelSelectionPolicy.select(
            localFirstEnabled = true,
            cloudRoutingEnabled = false,
            hardware = hardware(totalRamMb = 3_000),
            installedModels = listOf(
                installed(LocalModelTier.SPEED, "speed-test", downloaded = false),
            ),
        )

        assertEquals(LocalModelRoute.UNAVAILABLE, selection.route)
        assertTrue(selection.capabilities.isEmpty())
    }

    private fun hardware(
        totalRamMb: Int,
        supportsVulkan: Boolean = true,
        legacyNanoCapable: Boolean = false,
    ): DeviceAiHardwareProfile = DeviceAiHardwareProfile(
        totalRamMb = totalRamMb,
        availableRamMb = null,
        apiLevel = 35,
        supportsVulkan = supportsVulkan,
        legacyNanoCapable = legacyNanoCapable,
    )

    private fun installed(
        tier: LocalModelTier,
        label: String,
        capabilities: Set<LocalAiCapability> = when (tier) {
            LocalModelTier.SPEED -> LocalModelCapabilities.SPEED
            LocalModelTier.INTELLIGENCE -> LocalModelCapabilities.INTELLIGENCE
            LocalModelTier.LEGACY_NANO -> LocalModelCapabilities.LEGACY_NANO
            LocalModelTier.CLOUD -> LocalModelCapabilities.CLOUD
        },
        downloaded: Boolean = true,
    ): InstalledLocalModel = InstalledLocalModel(
        tier = tier,
        modelLabel = label,
        downloaded = downloaded,
        capabilities = capabilities,
    )
}
