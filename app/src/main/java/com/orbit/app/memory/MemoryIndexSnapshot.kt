package com.orbit.app.memory

import com.orbit.app.data.entity.CaptureUnderstandingEntity
import com.orbit.app.data.entity.ContinuationResultEntity
import com.orbit.app.data.entity.EnvelopeNoteEntity
import com.orbit.app.data.entity.EvidenceBundleEntity
import com.orbit.app.data.entity.IntentEnvelopeEntity

data class MemoryIndexSnapshot(
    val envelope: IntentEnvelopeEntity,
    val latestResult: ContinuationResultEntity?,
    val note: EnvelopeNoteEntity?,
    val understanding: CaptureUnderstandingEntity?,
    val evidenceBundles: List<EvidenceBundleEntity>,
)
