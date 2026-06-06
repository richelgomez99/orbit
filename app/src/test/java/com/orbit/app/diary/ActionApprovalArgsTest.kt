package com.orbit.app.diary

import com.orbit.app.data.ipc.ActionProposalParcel
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ActionApprovalArgsTest {

    @Test
    fun calendarArgs_passThroughUnchanged() {
        val proposal = proposal(
            functionId = "calendar.createEvent",
            argsJson = """{"title":"Coffee","startEpochMillis":1745164800000}"""
        )

        val result = ActionApprovalArgs.forExecution(proposal)

        assertEquals(proposal.argsJson, result)
    }

    @Test
    fun todoArgs_injectRuntimeIdsAndDefaultTarget() {
        val proposal = proposal(
            id = "proposal-1",
            envelopeId = "env-1",
            functionId = "tasks.createTodo",
            argsJson = """{"items":["buy salmon"]}"""
        )

        val result = JSONObject(ActionApprovalArgs.forExecution(proposal))

        assertEquals("proposal-1", result.getString("proposalId"))
        assertEquals("env-1", result.getString("parentEnvelopeId"))
        assertEquals("local", result.getString("target"))
        assertEquals("buy salmon", result.getJSONArray("items").getString(0))
    }

    @Test
    fun todoArgs_preserveEditedParentAndTargetButReplaceProposalId() {
        val proposal = proposal(
            id = "proposal-new",
            envelopeId = "env-source",
            functionId = "tasks.createTodo",
            argsJson = """{"items":["fallback"]}"""
        )
        val edited = """
            {
              "parentEnvelopeId": "env-edited",
              "proposalId": "stale",
              "target": "external",
              "items": ["send list"]
            }
        """.trimIndent()

        val result = JSONObject(ActionApprovalArgs.forExecution(proposal, edited))

        assertEquals("proposal-new", result.getString("proposalId"))
        assertEquals("env-edited", result.getString("parentEnvelopeId"))
        assertEquals("external", result.getString("target"))
    }

    @Test
    fun malformedTodoArgs_passThroughForExecutorFailurePath() {
        val proposal = proposal(
            functionId = "tasks.createTodo",
            argsJson = "not json"
        )

        val result = ActionApprovalArgs.forExecution(proposal)

        assertEquals("not json", result)
    }

    @Test
    fun todoModelFacingArgsDoNotNeedProposalId() {
        val proposal = proposal(
            id = "proposal-1",
            functionId = "tasks.createTodo",
            argsJson = """{"items":["buy salmon"]}"""
        )

        val modelFacing = JSONObject(proposal.argsJson)

        assertFalse(modelFacing.has("proposalId"))
        assertEquals("proposal-1", JSONObject(ActionApprovalArgs.forExecution(proposal)).getString("proposalId"))
    }

    private fun proposal(
        id: String = "p1",
        envelopeId: String = "env-1",
        functionId: String,
        argsJson: String
    ) = ActionProposalParcel(
        id = id,
        envelopeId = envelopeId,
        functionId = functionId,
        schemaVersion = 1,
        argsJson = argsJson,
        previewTitle = "Preview",
        previewSubtitle = null,
        confidence = 0.9f,
        provenance = "LocalNano",
        state = "PROPOSED",
        sensitivityScope = "PUBLIC",
        createdAtMillis = 0L,
        stateChangedAtMillis = 0L
    )
}
