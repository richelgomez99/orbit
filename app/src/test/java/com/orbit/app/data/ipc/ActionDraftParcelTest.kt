package com.orbit.app.data.ipc

import org.junit.Assert.assertEquals
import org.junit.Test

class ActionDraftParcelTest {

    @Test
    fun toProposalParcel_preservesExecutableProposalFields() {
        val draft = ActionDraftParcel(
            proposalId = "proposal-1",
            sourceEnvelopeId = "env-1",
            functionId = "tasks.createTodo",
            schemaVersion = 1,
            argsJson = """{"items":["buy salmon"]}""",
            previewTitle = "Create shopping list",
            previewSubtitle = "Recipe night",
            confidence = 0.88f,
            provenance = "OrbitManaged",
            state = "PROPOSED",
            sensitivityScope = "PERSONAL",
            createdAtMillis = 10L,
            stateChangedAtMillis = 20L,
            displayName = "Add to-do",
            sideEffects = "LOCAL_DB_WRITE",
            reversibility = "REVERSIBLE_24H",
            sourceTitle = "Shopping list for recipe night",
            sourceAppLabel = "Notes",
            sourceDayLocal = "2026-06-03",
        )

        val proposal = draft.toProposalParcel()

        assertEquals("proposal-1", proposal.id)
        assertEquals("env-1", proposal.envelopeId)
        assertEquals("tasks.createTodo", proposal.functionId)
        assertEquals("""{"items":["buy salmon"]}""", proposal.argsJson)
        assertEquals("Create shopping list", proposal.previewTitle)
        assertEquals("PROPOSED", proposal.state)
    }
}
