# Research: Manual Compose And Capture Context

## Decision 1: Reuse `envelope_note` For Capture Context

**Decision**: Store user-provided capture context in the existing `envelope_note` table and repository methods.

**Rationale**: The existing note path already feeds capture detail, Library local search, compact memory index snapshots, and link rehydration context. A second "context" table would fragment the product and create sync/search/KG ambiguity.

**Alternatives considered**:

- New `capture_context` table: rejected for v1 because it duplicates note semantics and requires another migration.
- Store context in `IntentEnvelope.textContent`: rejected because it pollutes the artifact with the user's reason for saving it.
- Store context only in Active Intent sidecars: rejected because manual compose and Library need context even when no follow-up exists.

## Decision 2: Save Capture First, Then Attach Context

**Decision**: The overlay context affordance targets an already-saved envelope id and attaches a note after seal succeeds.

**Rationale**: Existing duplicate detection, scrub, audit, undo, and continuation scheduling already happen in the seal path. Trying to gather context before seal would increase overlay friction and complicate duplicate matching. Attaching after save also lets duplicates route context to the existing envelope.

**Alternatives considered**:

- Pre-seal context field in the capture sheet: attractive but higher friction and riskier while the overlay already has chip-row timing logic.
- Separate transparent activity before save: matches long-term vision but needs careful overlay/window testing. Can be a later polish step after the safe post-save context path lands.

## Decision 3: Manual Compose Uses Existing Repository Seal Path

**Decision**: Manual compose creates a normal text `IntentEnvelopeDraftParcel` and calls the existing repository seal method through `BinderDiaryRepository` or a narrow UI repository method.

**Rationale**: Constitution Principle II says deliberate compose and reactive capture converge on the same seal contract. Reusing repository seal preserves duplicate suppression, audit, sensitivity scrub preconditions where available, and future storage backends.

**Alternatives considered**:

- Direct DAO insert from UI: rejected because it violates process boundaries and bypasses audit/storage abstractions.
- New `manual_note` entity: rejected because manual compose is still an IntentEnvelope, not a notes app.

## Decision 4: Backfill Is Display-Day Only, Not Time Travel

**Decision**: Backfilled manual compose may set/display `dayLocal` for the selected Diary day, but audit/created timestamps must use wall-clock now.

**Rationale**: The constitution explicitly requires wall-clock honesty. Orbit can support reflective notes without pretending they were captured in the past.

**Alternative considered**:

- Override `createdAt` to the selected day: rejected because it corrupts provenance and future KG timelines.

## Decision 5: No Model Dependency

**Decision**: Spec 011 ships deterministic/offline first. Notes may later improve LLM prompts/search, but creating and using context cannot require embeddings or a model.

**Rationale**: The product value is the user's explicit intent. Model-generated titles and KG extraction become better downstream, but the capture-context loop must work on every device and in airplane mode.
