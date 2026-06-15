# Research: Agent Workspace IA

## Decision 1: Harden Existing Tab Host

**Decision**: Keep `DiaryActivity` as the MVP tab host and harden its state boundaries.

**Rationale**: The app already has Diary, Library, and Orbit tabs. Replacing this with a navigation framework would add risk without changing the user-facing IA.

## Decision 2: Reset Transient State On Route Exit

**Decision**: Reset Library query/results/errors when leaving Library, and keep existing Ask reset when leaving Orbit.

**Rationale**: Search and Ask are ephemeral workbench states. Durable items come from repositories and should remain.

## Decision 3: Do Not Persist Chat Sessions Here

**Decision**: Durable chat/workbench sessions are out of scope.

**Rationale**: The vision includes dedicated sessions, but they require a data model, memory policy, and citations. This branch is shell hygiene only.
