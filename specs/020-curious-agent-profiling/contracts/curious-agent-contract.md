# Contract: Curious Agent Profiling

## Generator Contract

Input:

- compact evidence refs;
- existing dismissed/answered question ids;
- max active question count;
- minimum supporting source count.

Output:

- `List<CuriousQuestionCandidate>`

Rules:

- Emit no question if supporting evidence count is below threshold.
- Emit no raw source content.
- Do not emit dismissed or answered questions as active.
- Cap output before UI.

## UI Contract

The Orbit surface may show active curious questions with:

- question text;
- choices;
- source count and source labels;
- dismiss action;
- answer action.

## Answer Contract

Answer/dismiss records are user-authored feedback. They do not directly write profile facts unless a later task routes them through existing memory candidate/KG provenance paths.
