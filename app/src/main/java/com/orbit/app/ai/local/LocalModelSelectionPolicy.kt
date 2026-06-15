package com.orbit.app.ai.local

object LocalModelSelectionPolicy {
    private const val SPEED_MIN_RAM_MB = 4_096
    private const val INTELLIGENCE_MIN_RAM_MB = 6_144
    private const val LEGACY_NANO_LABEL = "legacy-nano"

    fun select(
        localFirstEnabled: Boolean,
        cloudRoutingEnabled: Boolean,
        hardware: DeviceAiHardwareProfile,
        installedModels: List<InstalledLocalModel>
    ): LocalModelSelection {
        if (!localFirstEnabled) {
            return if (cloudRoutingEnabled) {
                LocalModelSelection.cloud("cloud_default_local_first_disabled")
            } else {
                LocalModelSelection.unavailable("cloud_disabled_local_first_disabled")
            }
        }

        usableModel(
            tier = LocalModelTier.INTELLIGENCE,
            hardware = hardware,
            installedModels = installedModels,
        )?.let { return it }

        usableModel(
            tier = LocalModelTier.SPEED,
            hardware = hardware,
            installedModels = installedModels,
        )?.let { return it }

        if (hardware.legacyNanoCapable) {
            return LocalModelSelection(
                route = LocalModelRoute.LOCAL,
                tier = LocalModelTier.LEGACY_NANO,
                modelLabel = LEGACY_NANO_LABEL,
                capabilities = LocalModelCapabilities.LEGACY_NANO,
                reason = "legacy_nano_capable",
            )
        }

        return if (cloudRoutingEnabled) {
            LocalModelSelection.cloud("no_usable_local_model_cloud_fallback")
        } else {
            LocalModelSelection.unavailable("no_usable_local_model_cloud_disabled")
        }
    }

    private fun usableModel(
        tier: LocalModelTier,
        hardware: DeviceAiHardwareProfile,
        installedModels: List<InstalledLocalModel>
    ): LocalModelSelection? {
        val installed = installedModels.firstOrNull { it.tier == tier && it.downloaded }
            ?: return null
        val allowed = allowedCapabilitiesFor(tier)
        val eligible = when (tier) {
            LocalModelTier.SPEED -> hardware.totalRamMb >= SPEED_MIN_RAM_MB
            LocalModelTier.INTELLIGENCE ->
                hardware.totalRamMb >= INTELLIGENCE_MIN_RAM_MB && hardware.supportsVulkan
            LocalModelTier.LEGACY_NANO -> hardware.legacyNanoCapable
            LocalModelTier.CLOUD -> false
        }
        if (!eligible) return null
        val capabilities = installed.capabilities.intersect(allowed)
        if (capabilities.isEmpty()) return null
        return LocalModelSelection(
            route = LocalModelRoute.LOCAL,
            tier = tier,
            modelLabel = installed.modelLabel,
            capabilities = capabilities,
            reason = "${tier.name.lowercase()}_model_usable",
        )
    }

    private fun allowedCapabilitiesFor(tier: LocalModelTier): Set<LocalAiCapability> = when (tier) {
        LocalModelTier.SPEED -> LocalModelCapabilities.SPEED
        LocalModelTier.INTELLIGENCE -> LocalModelCapabilities.INTELLIGENCE
        LocalModelTier.LEGACY_NANO -> LocalModelCapabilities.LEGACY_NANO
        LocalModelTier.CLOUD -> emptySet()
    }
}
