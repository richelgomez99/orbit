package com.orbit.app.ai.local

enum class LocalModelTier {
    SPEED,
    INTELLIGENCE,
    LEGACY_NANO,
    CLOUD
}

enum class LocalAiCapability {
    BASIC_UNDERSTANDING,
    EMBEDDINGS,
    ACTION_EXTRACTION,
    GROUNDED_ASK,
    GENERATIVE_UI
}

data class DeviceAiHardwareProfile(
    val totalRamMb: Int,
    val availableRamMb: Int? = null,
    val apiLevel: Int,
    val supportsVulkan: Boolean,
    val legacyNanoCapable: Boolean
)

data class InstalledLocalModel(
    val tier: LocalModelTier,
    val modelLabel: String,
    val downloaded: Boolean,
    val capabilities: Set<LocalAiCapability>
)

enum class LocalModelRoute {
    LOCAL,
    CLOUD,
    UNAVAILABLE
}

data class LocalModelSelection(
    val route: LocalModelRoute,
    val tier: LocalModelTier?,
    val modelLabel: String?,
    val capabilities: Set<LocalAiCapability>,
    val reason: String
) {
    companion object {
        fun cloud(reason: String): LocalModelSelection = LocalModelSelection(
            route = LocalModelRoute.CLOUD,
            tier = LocalModelTier.CLOUD,
            modelLabel = "cloud-gateway",
            capabilities = LocalModelCapabilities.CLOUD,
            reason = reason,
        )

        fun unavailable(reason: String): LocalModelSelection = LocalModelSelection(
            route = LocalModelRoute.UNAVAILABLE,
            tier = null,
            modelLabel = null,
            capabilities = emptySet(),
            reason = reason,
        )
    }
}

object LocalModelCapabilities {
    val SPEED: Set<LocalAiCapability> = setOf(
        LocalAiCapability.BASIC_UNDERSTANDING,
        LocalAiCapability.EMBEDDINGS,
        LocalAiCapability.ACTION_EXTRACTION,
    )

    val INTELLIGENCE: Set<LocalAiCapability> = setOf(
        LocalAiCapability.BASIC_UNDERSTANDING,
        LocalAiCapability.EMBEDDINGS,
        LocalAiCapability.ACTION_EXTRACTION,
        LocalAiCapability.GROUNDED_ASK,
        LocalAiCapability.GENERATIVE_UI,
    )

    val LEGACY_NANO: Set<LocalAiCapability> = SPEED

    val CLOUD: Set<LocalAiCapability> = INTELLIGENCE
}
