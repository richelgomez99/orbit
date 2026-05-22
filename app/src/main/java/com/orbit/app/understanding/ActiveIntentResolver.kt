package com.orbit.app.understanding

import com.orbit.app.data.entity.ActiveIntentEntity
import com.orbit.app.understanding.domain.ActiveIntentStatus
import com.orbit.app.understanding.domain.ResolutionReason

class ActiveIntentResolver {

    fun resolve(
        entity: ActiveIntentEntity,
        reason: ResolutionReason,
        resolvedAt: Long,
        userConfirmed: Boolean = true
    ): ActiveIntentEntity = entity.copy(
        status = statusFor(reason),
        resolutionReason = reason,
        resolvedAt = resolvedAt,
        userConfirmed = userConfirmed,
        updatedAt = resolvedAt
    )

    fun expireIfNeeded(entity: ActiveIntentEntity, nowMillis: Long): ActiveIntentEntity {
        val expiresAt = entity.expiresAt ?: return entity
        if (entity.status != ActiveIntentStatus.ACTIVE || expiresAt > nowMillis) return entity
        return resolve(
            entity = entity,
            reason = ResolutionReason.AUTO_EXPIRED,
            resolvedAt = nowMillis,
            userConfirmed = false
        )
    }

    private fun statusFor(reason: ResolutionReason): ActiveIntentStatus = when (reason) {
        ResolutionReason.NOT_INTERESTED,
        ResolutionReason.USER_ARCHIVED -> ActiveIntentStatus.ARCHIVED
        ResolutionReason.AUTO_EXPIRED -> ActiveIntentStatus.EXPIRED
        ResolutionReason.INVALIDATED,
        ResolutionReason.SOURCE_DELETED -> ActiveIntentStatus.INVALIDATED
        ResolutionReason.BOUGHT,
        ResolutionReason.COOKED,
        ResolutionReason.READ_OR_WATCHED,
        ResolutionReason.VISITED,
        ResolutionReason.REPLIED_OR_DONE -> ActiveIntentStatus.RESOLVED
    }
}