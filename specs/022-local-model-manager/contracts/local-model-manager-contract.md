# Contract: Local Model Manager

## Selection Contract

```kotlin
fun LocalModelSelectionPolicy.select(
    localFirstEnabled: Boolean,
    cloudRoutingEnabled: Boolean,
    hardware: DeviceAiHardwareProfile,
    installedModels: List<InstalledLocalModel>
): LocalModelSelection
```

Rules:

- Never select a model whose `downloaded` flag is false.
- Never select Intelligence without Vulkan support.
- Never select Speed/Intelligence below their RAM floor.
- Prefer Intelligence over Speed when both are usable and local-first is enabled.
- Prefer Speed over legacy Nano for BYOM extraction capabilities.
- Preserve Cloud as the default when local-first is disabled and cloud routing is enabled.
- Return Unavailable when no local route is usable and cloud routing is disabled.

## Router Seam Contract

`LlmProviderRouter.resolve` may accept an optional local model selection and optional BYOM provider factory for tests/future integration.

Rules:

- Existing production defaults must remain unchanged.
- A selected BYOM provider must implement `LlmProvider`.
- If local-first selection is local but no provider implementation is supplied, fail closed rather than routing to cloud when cloud routing is disabled.
- `NanoLlmProvider` remains available through the existing legacy branch.

## Process Contract

- Local provider implementations run in `:ml`.
- Native engines must not open network sockets.
- Model downloads, when implemented later, must route through explicit user-controlled download/storage policy and not through inference providers.
