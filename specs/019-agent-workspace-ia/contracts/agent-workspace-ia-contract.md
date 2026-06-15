# Contract: Agent Workspace IA

## Tab Exit Contract

When selected tab changes:

- Leaving `ORBIT`: call `AskOrbitViewModel.reset()`.
- Leaving `LIBRARY`: call `LibraryViewModel.reset()`.
- Leaving `DIARY`: no reset required.

## Persistence Contract

Tab switches must not call delete/archive/resolve/dismiss APIs. Repository-backed flows continue to emit current durable state.

## Test Contract

JVM tests should prove:

- `LibraryViewModel.reset()` cancels in-flight search and restores `LibraryUiState()`.
- `DiaryActivity` tab selection code includes reset calls for Library and Orbit exits, or equivalent extracted route controller tests exist.
