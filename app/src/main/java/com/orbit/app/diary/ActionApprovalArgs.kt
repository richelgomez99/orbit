package com.orbit.app.diary

import com.orbit.app.data.ipc.ActionProposalParcel
import org.json.JSONObject

/**
 * Prepares proposal args for execution after the user approves a draft.
 *
 * Extraction-time model output cannot know the persisted proposal id. Keep
 * that out of the model-facing schema and inject it here, at the point where
 * the runtime has both the persisted proposal and the edited args.
 */
internal object ActionApprovalArgs {
    fun forExecution(
        proposal: ActionProposalParcel,
        editedArgsJson: String? = null
    ): String {
        val base = editedArgsJson ?: proposal.argsJson
        if (proposal.functionId != "tasks.createTodo") return base

        return runCatching {
            JSONObject(base).apply {
                if (!has("parentEnvelopeId")) put("parentEnvelopeId", proposal.envelopeId)
                put("proposalId", proposal.id)
                if (!has("target")) put("target", "local")
            }.toString()
        }.getOrElse {
            base
        }
    }
}
