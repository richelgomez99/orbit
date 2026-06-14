package com.orbit.app.diary

import com.orbit.app.data.ipc.IntentEnvelopeDraftParcel
import com.orbit.app.data.ipc.SealResultParcel
import com.orbit.app.data.ipc.StateSnapshotParcel
import com.orbit.app.data.model.ActivityState
import com.orbit.app.data.model.AppCategory
import com.orbit.app.data.model.ContentType
import com.orbit.app.data.model.Intent
import com.orbit.app.data.model.IntentSource
import java.time.Instant
import java.time.ZoneId

class ManualComposeSaver(
    private val sealWithResult: suspend (IntentEnvelopeDraftParcel, StateSnapshotParcel) -> SealResultParcel,
    private val createOrUpdateLatestNote: suspend (String, String) -> Boolean,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val zoneId: () -> ZoneId = { ZoneId.systemDefault() },
) {

    suspend fun save(draft: ManualComposeDraft): ManualComposeResult {
        val body = draft.bodyText.trim()
        if (body.isBlank()) return ManualComposeResult.Blocked("blank_body")

        val sealResult = runCatching {
            sealWithResult(toEnvelopeDraft(body, draft.intentName), stateSnapshot())
        }.getOrElse { error ->
            return ManualComposeResult.Blocked(error.message ?: "seal_failed")
        }

        return when (sealResult.status) {
            SealResultParcel.STATUS_CREATED -> {
                val context = draft.contextText?.trim()?.takeIf { it.isNotBlank() }
                val attached = context?.let { note ->
                    runCatching { createOrUpdateLatestNote(sealResult.envelopeId, note) }
                        .getOrDefault(false)
                } ?: false
                ManualComposeResult.Saved(
                    envelopeId = sealResult.envelopeId,
                    contextAttached = attached,
                )
            }
            SealResultParcel.STATUS_ALREADY_SAVED -> ManualComposeResult.AlreadySaved(
                existingEnvelopeId = sealResult.envelopeId,
                matchedBy = sealResult.matchedBy ?: "UNKNOWN",
            )
            else -> ManualComposeResult.Blocked("unknown_seal_status")
        }
    }

    private fun toEnvelopeDraft(body: String, intentName: String?): IntentEnvelopeDraftParcel {
        val intent = intentName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: Intent.AMBIGUOUS.name
        return IntentEnvelopeDraftParcel(
            contentType = ContentType.TEXT.name,
            textContent = body,
            imageUri = null,
            intent = intent,
            intentConfidence = intentName?.let { 1.0f },
            intentSource = if (intentName == null) {
                IntentSource.FALLBACK.name
            } else {
                IntentSource.USER_CHIP.name
            },
        )
    }

    private fun stateSnapshot(): StateSnapshotParcel {
        val zone = zoneId()
        val now = Instant.ofEpochMilli(clock()).atZone(zone)
        return StateSnapshotParcel(
            appCategory = AppCategory.OTHER.name,
            activityState = ActivityState.STILL.name,
            tzId = zone.id,
            hourLocal = now.hour,
            dayOfWeekLocal = now.dayOfWeek.value,
            sourceAppLabel = "Orbit",
        )
    }
}
