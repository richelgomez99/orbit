package com.orbit.app.resolution

enum class ResolutionKind {
    DUPLICATE_RECAPTURE,
    DISMISSED,
    NOT_NOW,
    SNOOZED,
    DONE,
    REOPENED,
    RESOLVED,
    STALE,
    INVALIDATED,
    SOURCE_DELETED,
    CONFLICT
}

enum class ResolutionActor {
    USER,
    SYSTEM,
    DUPLICATE_DETECTOR,
    ACTION_RUNTIME,
    RETENTION,
    AGENT
}

enum class ResolutionTargetType {
    ENVELOPE,
    ACTIVE_INTENT,
    ACTION_PROPOSAL,
    TODO_LIST,
    TODO_ITEM,
    MEMORY_CANDIDATE,
    GRAPH_FACT,
    DUPLICATE_ATTEMPT
}

enum class ResolutionSurface {
    CLEANUP_QUEUE,
    MEMORY_BROWSING,
    AGENT_PLANNING
}

enum class ResolutionSurfacingVerdict {
    ACTIVE,
    HIDDEN_UNTIL,
    DISMISSED,
    RESOLVED,
    INVALIDATED,
    STALE
}

data class ResolutionReceipt(
    val id: String,
    val targetType: ResolutionTargetType,
    val targetId: String,
    val kind: ResolutionKind,
    val actor: ResolutionActor,
    val occurredAtMillis: Long,
    val envelopeId: String? = null,
    val relatedType: ResolutionTargetType? = null,
    val relatedId: String? = null,
    val reason: String? = null,
    val effectiveUntilMillis: Long? = null,
    val invalidatesReceiptId: String? = null,
    val metadataJson: String? = null,
)

data class ResolutionVerdict(
    val verdict: ResolutionSurfacingVerdict,
    val receiptId: String? = null,
    val kind: ResolutionKind? = null,
    val hiddenUntilMillis: Long? = null,
)

class ResolutionVerdictResolver {

    fun resolve(
        receipts: List<ResolutionReceipt>,
        nowMillis: Long,
        surface: ResolutionSurface = ResolutionSurface.CLEANUP_QUEUE,
    ): ResolutionVerdict {
        if (receipts.isEmpty()) return active()
        val ordered = receipts.sortedWith(
            compareBy<ResolutionReceipt> { it.occurredAtMillis }.thenBy { it.id }
        )

        latestOf(ordered, ResolutionKind.INVALIDATED, ResolutionKind.SOURCE_DELETED)?.let {
            return ResolutionVerdict(
                verdict = ResolutionSurfacingVerdict.INVALIDATED,
                receiptId = it.id,
                kind = it.kind,
            )
        }

        latestOf(ordered, ResolutionKind.STALE, ResolutionKind.CONFLICT)?.let {
            return ResolutionVerdict(
                verdict = ResolutionSurfacingVerdict.STALE,
                receiptId = it.id,
                kind = it.kind,
            )
        }

        val latestCompletion = latestOf(ordered, ResolutionKind.DONE, ResolutionKind.RESOLVED)
        val latestReopen = latestOf(ordered, ResolutionKind.REOPENED)
        if (latestCompletion != null &&
            (latestReopen == null || latestCompletion.occurredAtMillis >= latestReopen.occurredAtMillis)
        ) {
            return ResolutionVerdict(
                verdict = ResolutionSurfacingVerdict.RESOLVED,
                receiptId = latestCompletion.id,
                kind = latestCompletion.kind,
            )
        }

        latestOf(ordered, ResolutionKind.SNOOZED)
            ?.takeIf { (it.effectiveUntilMillis ?: Long.MIN_VALUE) > nowMillis }
            ?.let {
                return ResolutionVerdict(
                    verdict = ResolutionSurfacingVerdict.HIDDEN_UNTIL,
                    receiptId = it.id,
                    kind = it.kind,
                    hiddenUntilMillis = it.effectiveUntilMillis,
                )
            }

        latestOf(ordered, ResolutionKind.DISMISSED)?.let {
            return ResolutionVerdict(
                verdict = ResolutionSurfacingVerdict.DISMISSED,
                receiptId = it.id,
                kind = it.kind,
            )
        }

        latestOf(ordered, ResolutionKind.NOT_NOW)
            ?.takeIf { surface != ResolutionSurface.MEMORY_BROWSING }
            ?.let {
                return ResolutionVerdict(
                    verdict = ResolutionSurfacingVerdict.DISMISSED,
                    receiptId = it.id,
                    kind = it.kind,
                )
            }

        return active()
    }

    private fun active() = ResolutionVerdict(ResolutionSurfacingVerdict.ACTIVE)

    private fun latestOf(
        receipts: List<ResolutionReceipt>,
        vararg kinds: ResolutionKind,
    ): ResolutionReceipt? {
        val wanted = kinds.toSet()
        return receipts.lastOrNull { it.kind in wanted }
    }
}
