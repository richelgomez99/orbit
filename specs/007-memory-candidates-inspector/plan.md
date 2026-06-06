# Implementation Plan: Memory Candidates Inspector

**Branch**: `feature/007-memory-candidates-inspector-20260605`  
**Spec**: [spec.md](spec.md)  
**Base**: Spec 006 checkpoint `ba2dcf7`

## Technical Summary

Add an additive Room v9 memory candidate layer, queryable support/provenance junction tables, compact Binder projections, repository decision methods, debug seeding, and an Orbit Memory Review UI. Accept/reject/edit decisions are local, audited, and provenance-backed. Candidates are not used as facts by Ask/actions and are not synced to Atlas.

## Constitution Check

- **I Local-First Supremacy**: PASS. Room remains canonical; no cloud requirement.
- **III Intent Before Artifact**: PASS. Candidate memory is grounded in source captures/actions and user decisions.
- **VI Privilege Separation**: PASS. DB work in `:ml`; UI through Binder; no network added.
- **VIII Collect Only What You Use**: PASS. Add only candidate/promoted rows needed for user review.
- **IX Cloud Escape Hatch**: PASS. Candidate review works offline.
- **XII Provenance**: PASS. Every candidate/promoted memory carries supporting source ids and audit rows.

## Project Structure

```text
specs/007-memory-candidates-inspector/
├── spec.md
├── research.md
├── data-model.md
├── plan.md
├── quickstart.md
├── contracts/
│   └── memory-candidate-contract.md
└── tasks.md

app/src/main/java/com/orbit/app/data/entity/
├── MemoryCandidateEntity.kt
├── MemoryCandidateSupportEntity.kt
├── PromotedMemoryEntity.kt
└── PromotedMemorySupportEntity.kt

app/src/main/java/com/orbit/app/data/dao/
├── MemoryCandidateDao.kt
├── MemoryCandidateSupportDao.kt
├── PromotedMemoryDao.kt
└── PromotedMemorySupportDao.kt

app/src/main/java/com/orbit/app/data/ipc/
├── MemoryCandidateParcel.kt
├── PromotedMemoryParcel.kt
└── MemoryDecisionResultParcel.kt

app/src/main/aidl/com/orbit/app/data/ipc/
├── MemoryCandidateParcel.aidl
├── PromotedMemoryParcel.aidl
├── MemoryDecisionResultParcel.aidl
├── IMemoryCandidateObserver.aidl
└── IPromotedMemoryObserver.aidl

app/src/main/java/com/orbit/app/data/
└── MemoryRepositoryDelegate.kt

app/src/main/java/com/orbit/app/diary/
├── DiaryRepository.kt
├── BinderDiaryRepository.kt
└── DiaryViewModel.kt

app/src/main/java/com/orbit/app/diary/ui/
└── OrbitCleanupScreen.kt
```

## Implementation Phases

1. **Spec lock and schema design**: final artifacts, enum choices, migration shape.
2. **Room foundation**: entities, DAOs, migration v8->v9, schema export, migration tests.
3. **Repository and Binder**: compact list projections, accept/reject/edit methods, audit writes.
4. **UI**: Orbit Memory Review section and edit/decision flows.
5. **Debug seed and tests**: deterministic candidates, repository tests, Compose tests.
6. **Boundary and closeout**: cloud payload exclusion tests, network/secret scans, docs, commit.

## Risk Controls

- Do not use pending candidates in Ask/action answer generation.
- Do not sync candidate text to Atlas.
- Keep support ids compact and bounded.
- Use support junction tables for deletion/provenance queries; JSON arrays are display/cache fields only.
- Keep Diary free of candidate queue pressure.
- Keep generated candidate seeding debug-only.

## Engineering Review Pivots - 2026-06-05

- Add support junction tables now. Relying only on JSON support arrays would make deletion, stale-source invalidation, and source-count queries brittle.
- Keep the first UI to pending candidates and accepted-memory projection. A complete Settings memory manager is out of scope until promoted memory use exists.
- Make source deletion behavior conservative: if all support rows disappear, mark candidate/promoted memory invalidated instead of showing source-less memory as trusted.
- Treat edited acceptance as a user-confirmed memory source, not a model-promoted source.
- Do not add any cloud memory sync shape for promoted memory in this branch; Spec 008 owns user-visible cloud controls first.
