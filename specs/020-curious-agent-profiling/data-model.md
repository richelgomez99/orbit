# Data Model: Curious Agent Profiling

## CuriousQuestionCandidate

Initial implementation may be pure domain or Room-backed depending on implementation risk.

Fields:

- `id: String`
- `questionText: String`
- `choices: List<CuriousQuestionChoice>`
- `sourceRefs: List<CuriousEvidenceRef>`
- `confidence: Float`
- `status: ACTIVE | DISMISSED | ANSWERED | STALE`
- `createdAtMillis: Long`
- `updatedAtMillis: Long`

## CuriousQuestionChoice

- `id: String`
- `label: String`
- `meaning: String`

## CuriousEvidenceRef

- `sourceType: ENVELOPE | PROMOTED_MEMORY | GRAPH_FACT | GRAPH_ENTITY`
- `sourceId: String`
- `label: String`

## CuriousQuestionAnswer

- `questionId: String`
- `choiceId: String?`
- `freeText: String?`
- `dismissed: Boolean`
- `createdAtMillis: Long`

Invariant: answers are explicit user evidence. They may create memory candidates or graph feedback in later phases, but they are not silent facts.
