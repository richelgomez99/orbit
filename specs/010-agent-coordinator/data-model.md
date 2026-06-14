# Data Model: Agent Coordinator

## AgentRequest

Inbound coordinator request.

| Field | Type | Notes |
| --- | --- | --- |
| `requestId` | String | Client/generated id for audit correlation. |
| `query` | String | User text, capped before Binder/provider use. |
| `attachedEnvelopeIds` | List<String> | Optional explicit context selected by user. |
| `maxEvidence` | Int | Bounded retrieval/plan evidence count. |
| `allowModelAssist` | Boolean | Must still respect Spec 008 and router policy. |

## AgentPlanDraft

Coordinator output.

| Field | Type | Notes |
| --- | --- | --- |
| `planId` | String | Local projection id, not persistent v1. |
| `outcome` | Enum | `PLAN`, `ASK_USER`, `REFUSE`, `ERROR`. |
| `title` | String | Compact user-facing label. |
| `summary` | String? | Cited, bounded summary. |
| `steps` | List<AgentPlanStep> | Max 5 in v1. |
| `questions` | List<AgentQuestion> | Max 3 in v1. |
| `evidence` | List<AgentEvidenceRef> | Max 10 in v1. |
| `limitations` | List<String> | Why the agent cannot continue or what it needs. |
| `modelLabel` | String? | Null for deterministic local mode. |
| `createdAtMillis` | Long | Local time. |

## AgentPlanStep

| Field | Type | Notes |
| --- | --- | --- |
| `stepId` | String | Stable within plan. |
| `kind` | Enum | `REVIEW_EVIDENCE`, `ASK_USER`, `DRAFT_ACTION`, `OPEN_CAPTURE`, `REFUSE`. |
| `label` | String | Compact display copy. |
| `detail` | String? | Bounded explanation. |
| `requiredApproval` | Boolean | True for action steps. |
| `actionProposalId` | String? | Existing proposal when available. |
| `functionId` | String? | Registered AppFunction id when relevant. |
| `evidenceIds` | List<String> | References `AgentEvidenceRef.evidenceId`. |

## AgentEvidenceRef

| Field | Type | Notes |
| --- | --- | --- |
| `evidenceId` | String | Local plan-scoped id. |
| `sourceType` | Enum | `ENVELOPE`, `GRAPH_FACT`, `GRAPH_RELATIONSHIP`, `PROMOTED_MEMORY`, `ACTION_PROPOSAL`. |
| `sourceId` | String | Local id only. |
| `label` | String | Compact title/label. |
| `dayLocal` | String? | Optional date for memory sources. |
| `whyThisTargetType` | String? | For graph sources. |
| `whyThisTargetId` | String? | For graph sources. |

## AgentQuestion

| Field | Type | Notes |
| --- | --- | --- |
| `questionId` | String | Stable within plan. |
| `text` | String | One focused question. Avoid naming this `prompt` so local UI text is not confused with model/provider prompts. |
| `choices` | List<AgentChoice> | Empty for free-text; max 5 for choices. |
| `evidenceIds` | List<String> | Why this question is being asked. |

## AgentTraceReceipt

Bounded audit/trace metadata only.

| Field | Type | Notes |
| --- | --- | --- |
| `requestId` | String | Correlates local request/result. |
| `queryDigest` | String | Hash only, no raw query. |
| `outcome` | String | Plan/ask/refuse/error. |
| `evidenceCount` | Int | Count only. |
| `stepCount` | Int | Count only. |
| `modelLabel` | String? | Optional. |
| `latencyMs` | Long | Local measured latency. |
| `createdAtMillis` | Long | Local time. |

## Persistence Decision

No new Room table is planned for `AgentPlanDraft` in v1. The coordinator returns compact projections through Binder and writes bounded audit metadata only if needed by implementation. Durable chat/workbench sessions belong to a later spec after the UI interaction model is proven.
