# Research: Generative UI Runtime

## Decision 1: Start With Internal Typed Components

**Decision**: Build Orbit's first generative UI runtime around app-defined typed Kotlin models, not an external A2UI dependency or model-emitted JSON parser.

**Rationale**: Current Orbit agent output is already a compact `AgentPlanParcel` produced through `:ml` and Binder. Rendering that as typed components validates the product surface while preserving process boundaries and avoiding premature dependency risk.

**Alternatives Considered**

- External A2UI renderer now: deferred because current repo has no concrete dependency, no local model manager, and no trusted model JSON validation path.
- Raw JSON-to-Compose parser: rejected because arbitrary provider payloads should not be renderable UI.

## Decision 2: Adapter, Renderer, Fallback

**Decision**: Split the feature into three pure layers:

1. `AgentPlanParcel` to `OrbitAgentUiDocument`.
2. Compose renderer for known components.
3. Text fallback renderer for the same document.

**Rationale**: This keeps business trust rules separate from presentation and makes degradation testable.

## Decision 3: No New Persistence

**Decision**: Do not add Room or Binder state in this branch.

**Rationale**: The runtime is a presentation layer over existing compact projections. Durable chat/workbench sessions belong to a later spec after renderer safety is proven.

## Decision 4: Model Output Is Not Authoritative

**Decision**: If a future model proposes UI, it must be normalized into the same typed components and validated against local evidence/action ids before rendering.

**Rationale**: This preserves Principle XII provenance and prevents visual hallucinations from becoming product state.
