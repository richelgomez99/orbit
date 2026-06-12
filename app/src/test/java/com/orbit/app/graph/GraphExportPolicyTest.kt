package com.orbit.app.graph

import org.junit.Assert.assertFalse
import org.junit.Test

class GraphExportPolicyTest {

    @Test
    fun graphExportModelDoesNotExposeRawCloudPayloadFields() {
        val exported = GraphExport(
            userId = "local-user",
            entities = listOf(
                GraphEntityDraft(
                    id = "topic-qr",
                    userId = "local-user",
                    type = GraphEntityType.TOPIC,
                    canonicalName = "QR code check-in",
                )
            ),
            facts = listOf(
                GraphFactDraft(
                    id = "fact-qr",
                    userId = "local-user",
                    subjectEntityId = "topic-qr",
                    predicate = "saved_for",
                    objectText = "event check-in",
                )
            ),
            relationships = emptyList(),
            provenance = listOf(
                GraphProvenanceDraft(
                    id = "support-qr",
                    userId = "local-user",
                    targetType = GraphTargetType.FACT,
                    targetId = "fact-qr",
                    sourceType = GraphSourceType.ENVELOPE,
                    sourceId = "env-qr",
                    supportKind = GraphSupportKind.ASSERTS,
                )
            ),
            feedback = emptyList(),
        )

        val rendered = exported.toString()
        listOf(
            "rawOcr",
            "ocrText",
            "screenshot",
            "imageBytes",
            "prompt",
            "embedding",
            "modelResponse",
            "accessToken",
            "apiKey",
            "jwt",
            "cookie",
        ).forEach { forbidden ->
            assertFalse("Graph export model must not expose $forbidden", rendered.contains(forbidden))
        }
    }
}
