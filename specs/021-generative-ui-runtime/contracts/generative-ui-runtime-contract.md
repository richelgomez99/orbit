# Contract: Generative UI Runtime

## Boundary

The runtime is UI/default-process presentation code over existing compact projections. It does not read Room directly and does not add network access.

## Adapter Contract

```kotlin
fun AgentPlanParcel.toOrbitAgentUiDocument(): OrbitAgentUiDocument
```

Rules:

- Preserve all cited evidence refs present in `AgentPlanParcel`.
- Cap display strings before building components.
- Do not include raw source text beyond labels already present in compact parcels.
- Do not create actions or function ids that are not already present in the plan.
- Create fallback text from the same components, not from a separate model call.

## Renderer Contract

```kotlin
@Composable
fun OrbitAgentUiRenderer(
    document: OrbitAgentUiDocument,
    onOpenEvidence: (OrbitAgentUiEvidence) -> Unit
)
```

Rules:

- Render known components only.
- Use Orbit theme tokens and Compose primitives.
- Do not accept arbitrary styling from the document.
- Evidence taps delegate to the caller's existing open-capture path.

## Fallback Contract

```kotlin
fun OrbitAgentUiDocument.asPlainText(): String
```

Rules:

- Include title, summary/body, questions, steps, limitations, and evidence labels when present.
- Preserve citations by listing evidence labels/source ids.
- Stay bounded for display/logging.
