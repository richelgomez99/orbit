package com.orbit.app.understanding.triage

import com.orbit.app.data.MemoryRepositoryDelegate
import com.orbit.app.data.model.IntentSource
import com.orbit.app.understanding.BasicUnderstandingResult

/**
 * Phase A — the capture-agent orchestrator. Runs at seal in `:ml` (no network):
 * triage gate → tool router → dispatch the one chosen action to the existing
 * memory/graph write path. The whole decision is deterministic and unit-tested
 * (see the `triage` package); this class only wires it to the delegate.
 *
 * Failure must never break seal — the caller wraps this in `runCatching`.
 */
class CaptureMemoryAgent(
    private val memory: MemoryRepositoryDelegate,
) {
    suspend fun onSealed(
        result: BasicUnderstandingResult,
        text: String?,
        intentSource: IntentSource,
    ) {
        val input = TriageInput.from(result, text, intentSource)
        val verdict = CaptureTriageGate.evaluate(input)
        when (val action = MemoryAgentRouter.route(input, verdict)) {
            is TriageAction.SaveToMemory ->
                memory.ingestExtractedFact(action.fact, input.captureId, action.autoPromote)
            is TriageAction.DraftConversation -> Unit // durable draft store lands in the next slice
            TriageAction.Ignore -> Unit
        }
    }
}
