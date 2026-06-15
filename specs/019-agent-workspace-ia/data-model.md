# Data Model: Agent Workspace IA

No database schema changes.

## Existing UI Model: OrbitHomeTab

Values:

- `DIARY`
- `LIBRARY`
- `ORBIT`

Rule: tab changes may reset transient ViewModel state but must not mutate persistent repository data.

## Existing UI Model: LibraryUiState

Transient fields reset on Library exit:

- `query`
- `results`
- `loading`
- `searched`
- unavailable/error fields
- `openEnvelopeId`

## Existing UI Model: AskOrbitUiState

Already reset on Orbit exit through `AskOrbitViewModel.reset()`.
