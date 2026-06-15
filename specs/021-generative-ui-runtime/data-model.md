# Data Model: Generative UI Runtime

## OrbitAgentUiDocument

Root renderable document.

| Field | Type | Notes |
| --- | --- | --- |
| `documentId` | String | Stable local id. |
| `components` | List<OrbitAgentUiComponent> | Known app-defined components only. |
| `fallbackText` | String | Deterministic bounded plain-text fallback. |

## OrbitAgentUiComponent

Sealed component family.

| Component | Fields | Notes |
| --- | --- | --- |
| `Title` | `text` | One-line heading. |
| `Body` | `text` | Bounded explanatory copy. |
| `Question` | `id`, `text`, `choices`, `evidenceIds` | Choice rendering only; no execution. |
| `Step` | `id`, `label`, `detail`, `requiresApproval`, `evidenceIds` | Action-like steps still require approval elsewhere. |
| `Evidence` | `id`, `label`, `sourceType`, `sourceId`, `dayLocal` | Opens source capture through existing handlers. |
| `Limitations` | `items` | Explains gaps/refusals. |

## OrbitAgentUiChoice

| Field | Type | Notes |
| --- | --- | --- |
| `id` | String | Stable within question. |
| `label` | String | Display label only. |

## Render Safety

- No arbitrary style fields.
- No arbitrary component names.
- No remote asset URL fields.
- No raw prompt/provider payload fields.
- No raw source body fields.
- Strings are capped by the adapter before rendering.
