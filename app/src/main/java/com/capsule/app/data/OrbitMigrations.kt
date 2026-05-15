package com.capsule.app.data

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

/** v8 — spec 004 Capture Understanding derived-record substrate. */
internal val MIGRATION_7_8: Migration = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS source_identity (
                id TEXT NOT NULL PRIMARY KEY,
                captureId TEXT NOT NULL,
                version INTEGER NOT NULL,
                providerKey TEXT,
                providerLabel TEXT,
                originAppLabel TEXT,
                genericCategory TEXT,
                displayLabel TEXT NOT NULL,
                secondaryLabel TEXT,
                glyphKind TEXT NOT NULL,
                confidence REAL,
                evidenceIdsJson TEXT NOT NULL,
                limitationsJson TEXT NOT NULL,
                resolverVersion INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                supersededAt INTEGER,
                invalidatedAt INTEGER,
                FOREIGN KEY(captureId) REFERENCES intent_envelope(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_source_identity_captureId ON source_identity(captureId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_source_identity_captureId_version ON source_identity(captureId, version)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_source_identity_captureId_invalidatedAt_supersededAt ON source_identity(captureId, invalidatedAt, supersededAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_source_identity_providerKey ON source_identity(providerKey)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS canonical_url (
                id TEXT NOT NULL PRIMARY KEY,
                captureId TEXT NOT NULL,
                originalUrl TEXT NOT NULL,
                normalizedUrl TEXT NOT NULL,
                canonicalUrlHash TEXT NOT NULL,
                role TEXT NOT NULL,
                providerFamily TEXT,
                host TEXT,
                normalizationVersion INTEGER NOT NULL,
                detectedAt INTEGER NOT NULL,
                hydratedAt INTEGER,
                isEligibleForDuplicateMatching INTEGER NOT NULL,
                invalidatedAt INTEGER,
                FOREIGN KEY(captureId) REFERENCES intent_envelope(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_canonical_url_captureId ON canonical_url(captureId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_canonical_url_canonicalUrlHash ON canonical_url(canonicalUrlHash)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_canonical_url_captureId_role_invalidatedAt ON canonical_url(captureId, role, invalidatedAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_canonical_url_canonicalUrlHash_isEligibleForDuplicateMatching ON canonical_url(canonicalUrlHash, isEligibleForDuplicateMatching)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_canonical_url_host ON canonical_url(host)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS evidence_bundle (
                id TEXT NOT NULL PRIMARY KEY,
                captureId TEXT NOT NULL,
                kind TEXT NOT NULL,
                sourceReference TEXT,
                acquisitionDepth TEXT NOT NULL,
                acquisitionMethod TEXT NOT NULL,
                confidence REAL,
                contentHash TEXT,
                retentionClass TEXT NOT NULL,
                status TEXT NOT NULL,
                limitationsJson TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                invalidatedAt INTEGER,
                FOREIGN KEY(captureId) REFERENCES intent_envelope(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_evidence_bundle_captureId ON evidence_bundle(captureId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_evidence_bundle_captureId_status_invalidatedAt ON evidence_bundle(captureId, status, invalidatedAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_evidence_bundle_kind ON evidence_bundle(kind)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_evidence_bundle_sourceReference ON evidence_bundle(sourceReference)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS understanding_depth_policy_override (
                id TEXT NOT NULL PRIMARY KEY,
                scope TEXT NOT NULL,
                captureId TEXT,
                depth TEXT NOT NULL,
                overrideKind TEXT,
                domainSuppressionKey TEXT,
                cloudAllowed INTEGER NOT NULL,
                reason TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY(captureId) REFERENCES intent_envelope(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_understanding_depth_policy_override_captureId ON understanding_depth_policy_override(captureId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_understanding_depth_policy_override_domainSuppressionKey ON understanding_depth_policy_override(domainSuppressionKey)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_understanding_depth_policy_override_scope_domainSuppressionKey_updatedAt ON understanding_depth_policy_override(scope, domainSuppressionKey, updatedAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_understanding_depth_policy_override_captureId_updatedAt ON understanding_depth_policy_override(captureId, updatedAt)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS understanding_job (
                id TEXT NOT NULL PRIMARY KEY,
                captureId TEXT NOT NULL,
                requestedDepth TEXT NOT NULL,
                effectiveDepth TEXT NOT NULL,
                status TEXT NOT NULL,
                policyDecision TEXT NOT NULL,
                retryEligibility TEXT NOT NULL,
                attemptCount INTEGER NOT NULL,
                maxAttempts INTEGER NOT NULL,
                traceIdsJson TEXT NOT NULL,
                failureCode TEXT,
                userVisibleReason TEXT,
                startedAt INTEGER,
                finishedAt INTEGER,
                createdAt INTEGER NOT NULL,
                invalidatedAt INTEGER,
                FOREIGN KEY(captureId) REFERENCES intent_envelope(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_understanding_job_captureId ON understanding_job(captureId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_understanding_job_captureId_status_createdAt ON understanding_job(captureId, status, createdAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_understanding_job_policyDecision ON understanding_job(policyDecision)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS capture_understanding (
                id TEXT NOT NULL PRIMARY KEY,
                captureId TEXT NOT NULL,
                jobId TEXT,
                version INTEGER NOT NULL,
                status TEXT NOT NULL,
                title TEXT,
                compactSummary TEXT,
                basedOnEvidenceIdsJson TEXT NOT NULL,
                sourceIdentityId TEXT,
                confidence REAL,
                limitationsJson TEXT NOT NULL,
                depthUsed TEXT NOT NULL,
                extractorProvenance TEXT,
                producedAt INTEGER NOT NULL,
                supersededAt INTEGER,
                invalidatedAt INTEGER,
                FOREIGN KEY(captureId) REFERENCES intent_envelope(id) ON DELETE CASCADE,
                FOREIGN KEY(jobId) REFERENCES understanding_job(id) ON DELETE SET NULL,
                FOREIGN KEY(sourceIdentityId) REFERENCES source_identity(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_capture_understanding_captureId ON capture_understanding(captureId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_capture_understanding_jobId ON capture_understanding(jobId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_capture_understanding_sourceIdentityId ON capture_understanding(sourceIdentityId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_capture_understanding_captureId_version ON capture_understanding(captureId, version)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_capture_understanding_captureId_status_invalidatedAt_supersededAt ON capture_understanding(captureId, status, invalidatedAt, supersededAt)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS correction_feedback (
                id TEXT NOT NULL PRIMARY KEY,
                captureId TEXT NOT NULL,
                understandingId TEXT,
                sourceIdentityId TEXT,
                feedbackKind TEXT NOT NULL,
                targetReference TEXT,
                note TEXT,
                createdAt INTEGER NOT NULL,
                resolvedByUnderstandingId TEXT,
                FOREIGN KEY(captureId) REFERENCES intent_envelope(id) ON DELETE CASCADE,
                FOREIGN KEY(understandingId) REFERENCES capture_understanding(id) ON DELETE SET NULL,
                FOREIGN KEY(sourceIdentityId) REFERENCES source_identity(id) ON DELETE SET NULL,
                FOREIGN KEY(resolvedByUnderstandingId) REFERENCES capture_understanding(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_correction_feedback_captureId ON correction_feedback(captureId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_correction_feedback_understandingId ON correction_feedback(understandingId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_correction_feedback_sourceIdentityId ON correction_feedback(sourceIdentityId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_correction_feedback_resolvedByUnderstandingId ON correction_feedback(resolvedByUnderstandingId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_correction_feedback_captureId_feedbackKind_createdAt ON correction_feedback(captureId, feedbackKind, createdAt)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS deletion_invalidation (
                id TEXT NOT NULL PRIMARY KEY,
                captureId TEXT NOT NULL,
                reason TEXT NOT NULL,
                affectedRecordRefsJson TEXT NOT NULL,
                downstreamEligibility TEXT NOT NULL,
                auditTraceId TEXT,
                createdAt INTEGER NOT NULL,
                cloudReceiptStatus TEXT,
                FOREIGN KEY(captureId) REFERENCES intent_envelope(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_deletion_invalidation_captureId ON deletion_invalidation(captureId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_deletion_invalidation_captureId_createdAt ON deletion_invalidation(captureId, createdAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_deletion_invalidation_auditTraceId ON deletion_invalidation(auditTraceId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_deletion_invalidation_cloudReceiptStatus ON deletion_invalidation(cloudReceiptStatus)")
    }
}
