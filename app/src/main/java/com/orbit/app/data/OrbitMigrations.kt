package com.orbit.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room schema migrations for `OrbitDatabase`.
 *
 * v1 (002 baseline):
 *   `intent_envelope`, `continuation`, `continuation_result`, `audit_log`.
 *
 * v2 (003 v1.1 — Orbit Actions):
 *   - extends `intent_envelope` with `kind`, `derivedFromEnvelopeIdsJson`, `todoMetaJson`
 *   - adds `action_proposal`, `action_execution`, `appfunction_skill`, `skill_usage`
 *   - adds the partial unique index `index_digest_unique_per_day` to enforce
 *     "exactly one DIGEST per Sunday" idempotency for `WeeklyDigestWorker`
 *
 * The migration is purely additive: every 002 query continues to return the
 * same shape because the new columns have NULL or string defaults.
 *
 * Verified by `OrbitDatabaseMigrationV1toV2Test` (T029) on a 1000-envelope
 * fixture and on-device by T111. See `specs/003-orbit-actions/data-model.md` §7.
 */
internal val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Extend intent_envelope. The kind column gets a NOT NULL DEFAULT so
        //    existing rows back-fill to REGULAR. derivedFromEnvelopeIdsJson and
        //    todoMetaJson are nullable — only DIGEST/DERIVED rows or todo-derived
        //    envelopes set them.
        db.execSQL("ALTER TABLE intent_envelope ADD COLUMN kind TEXT NOT NULL DEFAULT 'REGULAR'")
        db.execSQL("ALTER TABLE intent_envelope ADD COLUMN derivedFromEnvelopeIdsJson TEXT")
        db.execSQL("ALTER TABLE intent_envelope ADD COLUMN todoMetaJson TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_intent_envelope_kind_day_local ON intent_envelope(kind, day_local)")

        // 2. action_proposal — extracted candidates per envelope.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS action_proposal (
                id TEXT PRIMARY KEY NOT NULL,
                envelopeId TEXT NOT NULL,
                functionId TEXT NOT NULL,
                schemaVersion INTEGER NOT NULL,
                argsJson TEXT NOT NULL,
                previewTitle TEXT NOT NULL,
                previewSubtitle TEXT,
                confidence REAL NOT NULL,
                provenance TEXT NOT NULL,
                state TEXT NOT NULL,
                sensitivityScope TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                stateChangedAt INTEGER NOT NULL,
                FOREIGN KEY(envelopeId) REFERENCES intent_envelope(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_action_proposal_envelopeId ON action_proposal(envelopeId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_action_proposal_state ON action_proposal(state)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_action_proposal_createdAt ON action_proposal(createdAt)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_action_proposal_envelopeId_functionId ON action_proposal(envelopeId, functionId)")

        // 3. action_execution — one row per Confirm tap. Cascade-deletes with
        //    its proposal, which itself cascades from the source envelope.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS action_execution (
                id TEXT PRIMARY KEY NOT NULL,
                proposalId TEXT NOT NULL,
                functionId TEXT NOT NULL,
                outcome TEXT NOT NULL,
                outcomeReason TEXT,
                dispatchedAt INTEGER NOT NULL,
                completedAt INTEGER,
                latencyMs INTEGER,
                episodeId TEXT,
                FOREIGN KEY(proposalId) REFERENCES action_proposal(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_action_execution_proposalId ON action_execution(proposalId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_action_execution_outcome ON action_execution(outcome)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_action_execution_dispatchedAt ON action_execution(dispatchedAt)")

        // 4. appfunction_skill — registry of agent-callable functions. PK is
        //    `functionId` (soft-supersede via REPLACE on schema bumps); the
        //    composite unique index documents the schemaVersion semantics.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS appfunction_skill (
                functionId TEXT PRIMARY KEY NOT NULL,
                appPackage TEXT NOT NULL,
                displayName TEXT NOT NULL,
                description TEXT NOT NULL,
                schemaVersion INTEGER NOT NULL,
                argsSchemaJson TEXT NOT NULL,
                sideEffects TEXT NOT NULL,
                reversibility TEXT NOT NULL,
                sensitivityScope TEXT NOT NULL,
                registeredAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_appfunction_skill_functionId_schemaVersion ON appfunction_skill(functionId, schemaVersion)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_appfunction_skill_appPackage ON appfunction_skill(appPackage)")

        // 5. skill_usage — per-execution row consumed by the Settings stats UI
        //    and (forward) the v1.2 agent's planner heuristics.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS skill_usage (
                id TEXT PRIMARY KEY NOT NULL,
                skillId TEXT NOT NULL,
                executionId TEXT NOT NULL,
                proposalId TEXT NOT NULL,
                episodeId TEXT,
                outcome TEXT NOT NULL,
                latencyMs INTEGER NOT NULL,
                invokedAt INTEGER NOT NULL,
                FOREIGN KEY(skillId) REFERENCES appfunction_skill(functionId),
                FOREIGN KEY(executionId) REFERENCES action_execution(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_skill_usage_skillId ON skill_usage(skillId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_skill_usage_executionId ON skill_usage(executionId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_skill_usage_invokedAt ON skill_usage(invokedAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_skill_usage_outcome ON skill_usage(outcome)")

        // 6. Idempotency: at most one DIGEST envelope per local day. Two
        //    concurrent WeeklyDigestWorker runs (e.g., job-cancel + retry)
        //    will see one INSERT succeed and the other observe a UNIQUE
        //    constraint failure, which the worker maps to DIGEST_SKIPPED
        //    (T074 / weekly-digest-contract.md §3).
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_digest_unique_per_day " +
                "ON intent_envelope(day_local) WHERE kind = 'DIGEST'"
        )
    }
}

/**
 * v3 (002 amendment Phase 11 — Cluster Engine T120):
 *   - adds `cluster` + `cluster_member` tables with FK CASCADE on both
 *     parents (cluster.id and intent_envelope.id).
 *   - adds the four indexes per FR-026..FR-040: state filter,
 *     time-bucket range scan, and member-by-envelope reverse lookup.
 *
 * Procedural orphan-trigger NOT used here. FK CASCADE handles row
 * deletes; the count-based DISMISS rule (FR-038 — "surviving members
 * < 3 → auto-DISMISS reason=orphaned") lives in app code (T153) since
 * Room aggregations don't compose with FK triggers.
 *
 * Migration is purely additive — every v2 query continues unchanged.
 */
internal val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. cluster — agent-detected grouping with modelLabel stamping per
        //    Principle IX (LLM Sovereignty).
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS cluster (
                id TEXT PRIMARY KEY NOT NULL,
                cluster_type TEXT NOT NULL,
                state TEXT NOT NULL,
                timeBucketStart INTEGER NOT NULL,
                timeBucketEnd INTEGER NOT NULL,
                similarityScore REAL NOT NULL,
                model_label TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                stateChangedAt INTEGER NOT NULL,
                dismissedAt INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_cluster_state ON cluster(state)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_cluster_time_bucket ON cluster(timeBucketStart, timeBucketEnd)")

        // 2. cluster_member — composite PK + double FK CASCADE so a hard
        //    delete on either parent (cluster or envelope) cleans up the
        //    junction row without leaving dangling references. The
        //    surviving-count check that drives FR-038 lives in app code.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS cluster_member (
                clusterId TEXT NOT NULL,
                envelopeId TEXT NOT NULL,
                memberIndex INTEGER NOT NULL,
                PRIMARY KEY(clusterId, envelopeId),
                FOREIGN KEY(clusterId) REFERENCES cluster(id) ON DELETE CASCADE,
                FOREIGN KEY(envelopeId) REFERENCES intent_envelope(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_cluster_member_clusterId ON cluster_member(clusterId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_cluster_member_envelope ON cluster_member(envelopeId)")
    }
}

/**
 * Spec 002 Phase 11 Block 13 / spec 012 FR-012-011 — additive column on
 * `intent_envelope` recording the AppFunction `function_id` that produced
 * a DERIVED row (e.g. `'cluster_summarize'`). NULL for REGULAR + DIGEST
 * + every existing row, so this is a strictly additive ALTER. Pre-Phase-11
 * derived rows (DIGESTs landed before v4) keep `derivedVia = NULL`; spec
 * 012 readers treat NULL on a DERIVED row as "unknown derivation path"
 * and fall back to row-shape inference.
 *
 * Spec 012 schema uses snake_case (`derived_via`); we keep the column
 * name camelCase here to match the rest of `intent_envelope`. The
 * stored *value* is what carries semantic meaning across surfaces.
 */
internal val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE intent_envelope ADD COLUMN derivedVia TEXT")
    }
}

/**
 * v5 — capture the user-facing foreground app label when Usage Access can
 * resolve one. `appCategory` remains the durable grouping/classification field;
 * this nullable label is display-only and lets the Diary render "from YouTube"
 * instead of the broader "from Video" bucket for new captures.
 */
internal val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE intent_envelope ADD COLUMN sourceAppLabel TEXT")
    }
}

/**
 * v6 — spec 017 envelope-level duplicate key for URL captures. Existing hydrated
 * rows inherit their continuation-result URL hash so the first post-upgrade
 * duplicate attempt still resolves to Already Saved.
 */
internal val MIGRATION_5_6: Migration = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE intent_envelope ADD COLUMN primaryCanonicalUrlHash TEXT")
        db.execSQL("ALTER TABLE intent_envelope ADD COLUMN activePrimaryCanonicalUrlHash TEXT")
        db.execSQL("ALTER TABLE intent_envelope ADD COLUMN activeTextContentSha256 TEXT")
        db.execSQL(
            """
            UPDATE intent_envelope
            SET primaryCanonicalUrlHash = (
                SELECT canonicalUrlHash
                FROM continuation_result
                WHERE continuation_result.envelopeId = intent_envelope.id
                  AND canonicalUrlHash IS NOT NULL
                ORDER BY producedAt DESC
                LIMIT 1
            )
            WHERE primaryCanonicalUrlHash IS NULL
              AND EXISTS (
                SELECT 1
                FROM continuation_result
                WHERE continuation_result.envelopeId = intent_envelope.id
                  AND canonicalUrlHash IS NOT NULL
              )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_intent_envelope_primaryCanonicalUrlHash " +
                "ON intent_envelope(primaryCanonicalUrlHash)"
        )
        db.execSQL(
            """
            UPDATE intent_envelope
            SET activePrimaryCanonicalUrlHash = primaryCanonicalUrlHash
            WHERE primaryCanonicalUrlHash IS NOT NULL
              AND deletedAt IS NULL
              AND isDeleted = 0
              AND isArchived = 0
              AND NOT EXISTS (
                  SELECT 1
                  FROM intent_envelope AS earlier
                  WHERE earlier.primaryCanonicalUrlHash = intent_envelope.primaryCanonicalUrlHash
                    AND earlier.deletedAt IS NULL
                    AND earlier.isDeleted = 0
                    AND earlier.isArchived = 0
                    AND (
                        earlier.createdAt < intent_envelope.createdAt
                        OR (earlier.createdAt = intent_envelope.createdAt AND earlier.id < intent_envelope.id)
                    )
              )
            """.trimIndent()
        )
        db.execSQL(
            """
            UPDATE intent_envelope
            SET activeTextContentSha256 = textContentSha256
            WHERE textContentSha256 IS NOT NULL
              AND primaryCanonicalUrlHash IS NULL
              AND deletedAt IS NULL
              AND isDeleted = 0
              AND isArchived = 0
              AND NOT EXISTS (
                  SELECT 1
                  FROM intent_envelope AS earlier
                  WHERE earlier.textContentSha256 = intent_envelope.textContentSha256
                    AND earlier.primaryCanonicalUrlHash IS NULL
                    AND earlier.deletedAt IS NULL
                    AND earlier.isDeleted = 0
                    AND earlier.isArchived = 0
                    AND (
                        earlier.createdAt < intent_envelope.createdAt
                        OR (earlier.createdAt = intent_envelope.createdAt AND earlier.id < intent_envelope.id)
                    )
              )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_intent_envelope_activePrimaryCanonicalUrlHash " +
                "ON intent_envelope(activePrimaryCanonicalUrlHash)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_intent_envelope_activeTextContentSha256 " +
                "ON intent_envelope(activeTextContentSha256)"
        )
    }
}

/** v7 — spec 017 note persistence for duplicate feedback actions. */
internal val MIGRATION_6_7: Migration = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS envelope_note (
                id TEXT NOT NULL PRIMARY KEY,
                envelopeId TEXT NOT NULL,
                text TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER,
                FOREIGN KEY(envelopeId) REFERENCES intent_envelope(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_envelope_note_envelopeId_updatedAt " +
                "ON envelope_note(envelopeId, updatedAt)"
        )
    }
}

/** v8 — spec 004 Screenshot Cleanup + Active Intent sidecars. */
internal val MIGRATION_7_8: Migration = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS capture_understanding (
                captureId TEXT NOT NULL PRIMARY KEY,
                mode TEXT NOT NULL,
                status TEXT NOT NULL,
                category TEXT NOT NULL,
                categoryConfidence REAL NOT NULL,
                title TEXT,
                summaryText TEXT,
                completionKeyJson TEXT,
                completionKeyStatus TEXT NOT NULL,
                sourceIdentityJson TEXT,
                contentHashHex TEXT,
                canonicalUrl TEXT,
                groundingConstraintsJson TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                invalidatedAt INTEGER,
                FOREIGN KEY(captureId) REFERENCES intent_envelope(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_capture_understanding_category ON capture_understanding(category)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_capture_understanding_completionKeyStatus ON capture_understanding(completionKeyStatus)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_capture_understanding_contentHashHex ON capture_understanding(contentHashHex)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_capture_understanding_canonicalUrl ON capture_understanding(canonicalUrl)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS evidence_bundle (
                id TEXT NOT NULL PRIMARY KEY,
                captureId TEXT NOT NULL,
                bundleType TEXT NOT NULL,
                payloadJson TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(captureId) REFERENCES intent_envelope(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_evidence_bundle_captureId ON evidence_bundle(captureId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_evidence_bundle_bundleType ON evidence_bundle(bundleType)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS invalidation_record (
                captureId TEXT NOT NULL PRIMARY KEY,
                invalidatedAt INTEGER NOT NULL,
                reason TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS active_intent (
                intentId TEXT NOT NULL PRIMARY KEY,
                captureId TEXT NOT NULL,
                intentType TEXT NOT NULL,
                status TEXT NOT NULL,
                completionKeyJson TEXT,
                completionKeyStatus TEXT NOT NULL,
                primaryEvidenceJson TEXT NOT NULL,
                primaryAction TEXT,
                dueAt INTEGER,
                expiresAt INTEGER,
                resolutionReason TEXT,
                resolvedAt INTEGER,
                userConfirmed INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY(captureId) REFERENCES intent_envelope(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_active_intent_captureId ON active_intent(captureId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_active_intent_status ON active_intent(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_active_intent_intentType ON active_intent(intentType)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_active_intent_dueAt ON active_intent(dueAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_active_intent_expiresAt ON active_intent(expiresAt)")
    }
}

/** v9 — spec 007 Memory Candidates Inspector sidecars. */
internal val MIGRATION_8_9: Migration = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS memory_candidate (
                id TEXT NOT NULL PRIMARY KEY,
                candidateKind TEXT NOT NULL,
                state TEXT NOT NULL,
                displayLabel TEXT NOT NULL,
                subject TEXT NOT NULL,
                predicate TEXT NOT NULL,
                objectValue TEXT NOT NULL,
                confidence REAL NOT NULL,
                sensitivity TEXT NOT NULL,
                supportingEnvelopeIdsJson TEXT NOT NULL,
                supportingEvidenceIdsJson TEXT,
                supportingFeedbackIdsJson TEXT,
                askUserCopy TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                expiresAt INTEGER,
                decidedAt INTEGER,
                decisionReason TEXT,
                modelLabel TEXT,
                promptVersion TEXT,
                source TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_memory_candidate_state ON memory_candidate(state)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_memory_candidate_candidateKind ON memory_candidate(candidateKind)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_memory_candidate_sensitivity ON memory_candidate(sensitivity)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_memory_candidate_createdAt ON memory_candidate(createdAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_memory_candidate_expiresAt ON memory_candidate(expiresAt)")
        // NOTE (2026-07-06 hotfix): this migration originally also created a
        // partial-unique index `index_memory_candidate_active_fact_key`
        // (`WHERE state IN ('PENDING','ASKED')`) that MemoryCandidateEntity
        // never declared — Room's @Index cannot express partial indexes, so
        // schema validation failed and crashed :ml on launch. The creation is
        // removed here (fresh upgrades never get the index) and
        // MIGRATION_11_12 drops it from devices that already ran the old
        // version of this migration. Active-duplicate uniqueness is enforced
        // in code via MemoryCandidateDao.findActiveDuplicate.

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS memory_candidate_support (
                candidateId TEXT NOT NULL,
                envelopeId TEXT NOT NULL,
                supportType TEXT NOT NULL,
                evidenceId TEXT,
                createdAt INTEGER NOT NULL,
                PRIMARY KEY(candidateId, envelopeId, supportType),
                FOREIGN KEY(candidateId) REFERENCES memory_candidate(id) ON DELETE CASCADE,
                FOREIGN KEY(envelopeId) REFERENCES intent_envelope(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_memory_candidate_support_candidateId ON memory_candidate_support(candidateId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_memory_candidate_support_envelopeId ON memory_candidate_support(envelopeId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS promoted_memory (
                id TEXT NOT NULL PRIMARY KEY,
                candidateId TEXT,
                memoryKind TEXT NOT NULL,
                state TEXT NOT NULL,
                displayLabel TEXT NOT NULL,
                subject TEXT NOT NULL,
                predicate TEXT NOT NULL,
                objectValue TEXT NOT NULL,
                confidenceLabel TEXT NOT NULL,
                sensitivity TEXT NOT NULL,
                source TEXT NOT NULL,
                supportingEnvelopeIdsJson TEXT NOT NULL,
                supportingEvidenceIdsJson TEXT,
                supportingFeedbackIdsJson TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                validFrom INTEGER NOT NULL,
                validTo INTEGER,
                invalidatedAt INTEGER,
                lastUsedAt INTEGER,
                useCount INTEGER NOT NULL,
                FOREIGN KEY(candidateId) REFERENCES memory_candidate(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_promoted_memory_state ON promoted_memory(state)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_promoted_memory_memoryKind ON promoted_memory(memoryKind)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_promoted_memory_sensitivity ON promoted_memory(sensitivity)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_promoted_memory_candidateId ON promoted_memory(candidateId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_promoted_memory_updatedAt ON promoted_memory(updatedAt)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS promoted_memory_support (
                memoryId TEXT NOT NULL,
                envelopeId TEXT NOT NULL,
                supportType TEXT NOT NULL,
                evidenceId TEXT,
                createdAt INTEGER NOT NULL,
                PRIMARY KEY(memoryId, envelopeId, supportType),
                FOREIGN KEY(memoryId) REFERENCES promoted_memory(id) ON DELETE CASCADE,
                FOREIGN KEY(envelopeId) REFERENCES intent_envelope(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_promoted_memory_support_memoryId ON promoted_memory_support(memoryId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_promoted_memory_support_envelopeId ON promoted_memory_support(envelopeId)")
    }
}

/** v10 — spec 009 Local-first Knowledge Graph backend POC sidecars. */
internal val MIGRATION_9_10: Migration = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS graph_entity (
                id TEXT NOT NULL PRIMARY KEY,
                userId TEXT NOT NULL,
                type TEXT NOT NULL,
                canonicalName TEXT NOT NULL,
                normalizedName TEXT NOT NULL,
                description TEXT,
                status TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                invalidatedAt INTEGER,
                invalidatedReason TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_entity_userId ON graph_entity(userId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_entity_type ON graph_entity(type)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_entity_normalizedName ON graph_entity(normalizedName)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_entity_status ON graph_entity(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_entity_invalidatedAt ON graph_entity(invalidatedAt)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS graph_mention (
                id TEXT NOT NULL PRIMARY KEY,
                userId TEXT NOT NULL,
                entityId TEXT NOT NULL,
                sourceType TEXT NOT NULL,
                sourceId TEXT NOT NULL,
                label TEXT,
                excerptDigest TEXT,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(entityId) REFERENCES graph_entity(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_mention_userId ON graph_mention(userId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_mention_entityId ON graph_mention(entityId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_mention_sourceType_sourceId ON graph_mention(sourceType, sourceId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS graph_fact (
                id TEXT NOT NULL PRIMARY KEY,
                userId TEXT NOT NULL,
                subjectEntityId TEXT NOT NULL,
                predicate TEXT NOT NULL,
                objectText TEXT,
                objectEntityId TEXT,
                confidence REAL NOT NULL,
                status TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                invalidatedAt INTEGER,
                invalidatedReason TEXT,
                FOREIGN KEY(subjectEntityId) REFERENCES graph_entity(id) ON DELETE CASCADE,
                FOREIGN KEY(objectEntityId) REFERENCES graph_entity(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_fact_userId ON graph_fact(userId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_fact_status ON graph_fact(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_fact_subjectEntityId ON graph_fact(subjectEntityId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_fact_objectEntityId ON graph_fact(objectEntityId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_fact_predicate ON graph_fact(predicate)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_fact_invalidatedAt ON graph_fact(invalidatedAt)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS graph_relationship (
                id TEXT NOT NULL PRIMARY KEY,
                userId TEXT NOT NULL,
                fromEntityId TEXT NOT NULL,
                toEntityId TEXT NOT NULL,
                relationshipType TEXT NOT NULL,
                confidence REAL NOT NULL,
                status TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                invalidatedAt INTEGER,
                invalidatedReason TEXT,
                FOREIGN KEY(fromEntityId) REFERENCES graph_entity(id) ON DELETE CASCADE,
                FOREIGN KEY(toEntityId) REFERENCES graph_entity(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_relationship_userId ON graph_relationship(userId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_relationship_status ON graph_relationship(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_relationship_fromEntityId ON graph_relationship(fromEntityId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_relationship_toEntityId ON graph_relationship(toEntityId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_relationship_relationshipType ON graph_relationship(relationshipType)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_relationship_invalidatedAt ON graph_relationship(invalidatedAt)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS graph_provenance (
                id TEXT NOT NULL PRIMARY KEY,
                userId TEXT NOT NULL,
                targetType TEXT NOT NULL,
                targetId TEXT NOT NULL,
                sourceType TEXT NOT NULL,
                sourceId TEXT NOT NULL,
                supportKind TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                invalidatedAt INTEGER,
                invalidatedReason TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_provenance_userId ON graph_provenance(userId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_provenance_targetType_targetId ON graph_provenance(targetType, targetId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_provenance_sourceType_sourceId ON graph_provenance(sourceType, sourceId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_provenance_supportKind ON graph_provenance(supportKind)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_provenance_invalidatedAt ON graph_provenance(invalidatedAt)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS graph_feedback (
                id TEXT NOT NULL PRIMARY KEY,
                userId TEXT NOT NULL,
                targetType TEXT NOT NULL,
                targetId TEXT NOT NULL,
                feedbackType TEXT NOT NULL,
                replacementText TEXT,
                replacementEntityId TEXT,
                sourceType TEXT NOT NULL,
                sourceId TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(replacementEntityId) REFERENCES graph_entity(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_feedback_userId ON graph_feedback(userId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_feedback_targetType_targetId ON graph_feedback(targetType, targetId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_feedback_feedbackType ON graph_feedback(feedbackType)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_feedback_replacementEntityId ON graph_feedback(replacementEntityId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_feedback_sourceType_sourceId ON graph_feedback(sourceType, sourceId)")
    }
}

/**
 * v11 — spec 012 resolution semantics.
 *
 * Adds a compact local receipt table for durable semantic state such as
 * duplicate recapture, dismissed, not-now, snoozed, done, reopened, stale,
 * invalidated, source-deleted, and conflict. The table intentionally avoids
 * FK constraints to preserve history even when the referenced target is later
 * invalidated or hard-deleted.
 */
/**
 * v12 — hotfix (2026-07-02): MIGRATION_8_9 (spec 007) created a partial-unique
 * index `index_memory_candidate_active_fact_key` (`WHERE state IN ('PENDING',
 * 'ASKED')`) that the [com.orbit.app.data.entity.MemoryCandidateEntity]
 * annotation never declared, because Room's `@Index` does not support partial
 * `WHERE` clauses. Room 2.7+ validates schema at build-time in production,
 * detects the mismatch, and crashes the `:ml` process during app launch on
 * every device that had a pre-v11 database. Uniqueness is now enforced in
 * code (see `MemoryCandidateDao.findActiveDuplicate` + delegate insert path);
 * this migration drops the orphan index so devices already carrying it come
 * back into agreement with the entity. Idempotent on fresh installs — the
 * index will never have existed there.
 */
/**
 * v13 — hotfix (2026-07-06): drop the legacy partial-unique index
 * `index_digest_unique_per_day` (created by MIGRATION_1_2, `WHERE
 * kind='DIGEST'`). Same disease as the v12 `active_fact_key` fix:
 * Room's @Entity cannot declare partial indexes, so the index existed
 * ONLY on v1-upgraded databases — where Room 2.7's strict schema
 * validation rejects it and crashes the open — while fresh installs
 * never had it, meaning the digest race-guard that leaned on it was
 * silently absent everywhere it mattered. Enforcement now lives in
 * code: `LocalRoomBackend.insertDigestTransaction` does an atomic
 * check-then-insert via `IntentEnvelopeDao.countDigestsForDay`.
 * Idempotent on fresh installs.
 */
internal val MIGRATION_12_13: Migration = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP INDEX IF EXISTS index_digest_unique_per_day")
    }
}

internal val MIGRATION_11_12: Migration = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP INDEX IF EXISTS index_memory_candidate_active_fact_key")
    }
}

internal val MIGRATION_10_11: Migration = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS resolution_receipt (
                id TEXT NOT NULL PRIMARY KEY,
                targetType TEXT NOT NULL,
                targetId TEXT NOT NULL,
                envelopeId TEXT,
                relatedType TEXT,
                relatedId TEXT,
                kind TEXT NOT NULL,
                actor TEXT NOT NULL,
                reason TEXT,
                occurredAtMillis INTEGER NOT NULL,
                effectiveUntilMillis INTEGER,
                invalidatesReceiptId TEXT,
                metadataJson TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_resolution_receipt_targetType_targetId_occurredAtMillis " +
                "ON resolution_receipt(targetType, targetId, occurredAtMillis)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_resolution_receipt_envelopeId_occurredAtMillis " +
                "ON resolution_receipt(envelopeId, occurredAtMillis)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_resolution_receipt_kind_occurredAtMillis " +
                "ON resolution_receipt(kind, occurredAtMillis)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_resolution_receipt_effectiveUntilMillis " +
                "ON resolution_receipt(effectiveUntilMillis)"
        )
    }
}

/**
 * All registered [OrbitDatabase] migrations, ordered by source version.
 *
 * Tests that reopen the DB via `Room.databaseBuilder(...)` after a
 * [androidx.room.testing.MigrationTestHelper] step must pass this to
 * `.addMigrations(*ALL_MIGRATIONS)` so Room can bring the DB up to the
 * current entity version. Otherwise Room fails with
 * `"A migration from X to Y was required but not found"`.
 */
internal val ALL_MIGRATIONS: Array<Migration> = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7,
    MIGRATION_7_8,
    MIGRATION_8_9,
    MIGRATION_9_10,
    MIGRATION_10_11,
    MIGRATION_11_12,
    MIGRATION_12_13,
)
