# Data Model: Local Model Manager

## LocalModelTier

| Value | Meaning |
| --- | --- |
| `SPEED` | Small local model for extraction/basic understanding/embeddings. |
| `INTELLIGENCE` | Larger local model for offline Ask, planning, and structured UI. |
| `LEGACY_NANO` | Current AICore/Gemini Nano provider path. |
| `CLOUD` | Zero-download cloud gateway path. |

## LocalAiCapability

- `BASIC_UNDERSTANDING`
- `EMBEDDINGS`
- `ACTION_EXTRACTION`
- `GROUNDED_ASK`
- `GENERATIVE_UI`

## DeviceAiHardwareProfile

| Field | Type | Notes |
| --- | --- | --- |
| `totalRamMb` | Int | Conservative total system RAM signal. |
| `availableRamMb` | Int? | Optional runtime free-memory signal. |
| `apiLevel` | Int | Android SDK level. |
| `supportsVulkan` | Boolean | Required by MLC-style GPU path. |
| `legacyNanoCapable` | Boolean | Current Nano compatibility signal. |

## InstalledLocalModel

| Field | Type | Notes |
| --- | --- | --- |
| `tier` | LocalModelTier | Speed or Intelligence in first BYOM path. |
| `modelLabel` | String | Version/label for audit and embedding compatibility. |
| `downloaded` | Boolean | False means not usable. |
| `capabilities` | Set<LocalAiCapability> | Explicit feature set. |

## LocalModelSelection

| Field | Type | Notes |
| --- | --- | --- |
| `route` | Enum | `LOCAL`, `CLOUD`, `UNAVAILABLE`. |
| `tier` | LocalModelTier? | Present for local/cloud route. |
| `modelLabel` | String? | Present for local route. |
| `capabilities` | Set<LocalAiCapability> | Empty for unavailable. |
| `reason` | String | Bounded diagnostic reason. |

## Policy Defaults

- Speed requires 4GB+ total RAM and a downloaded Speed model.
- Intelligence requires 6GB+ total RAM, Vulkan support, and a downloaded Intelligence model.
- Legacy Nano requires `legacyNanoCapable = true`.
- If local-first is off and cloud routing is enabled, select Cloud.
- If local-first is on but no local model is usable, select Cloud only when cloud routing is enabled; otherwise select Unavailable.
