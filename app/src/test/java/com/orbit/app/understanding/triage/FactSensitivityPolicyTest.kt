package com.orbit.app.understanding.triage

import com.orbit.app.data.model.IntentSource
import com.orbit.app.data.model.MemorySensitivity
import com.orbit.app.understanding.domain.CompletionKey
import com.orbit.app.understanding.domain.CompletionKeyKind
import com.orbit.app.understanding.domain.IntentCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class FactSensitivityPolicyTest {

    @Test
    fun redactionMarkerMakesItSensitive() {
        assertEquals(
            MemorySensitivity.SENSITIVE,
            FactSensitivityPolicy.sensitivityOf(input(text = "call me at [REDACTED_PHONE] tomorrow")),
        )
    }

    @Test
    fun financialCategoryIsSensitive() {
        assertEquals(
            MemorySensitivity.SENSITIVE,
            FactSensitivityPolicy.sensitivityOf(input(category = IntentCategory.RECEIPT_OR_ORDER)),
        )
    }

    @Test
    fun priceKeyIsSensitive() {
        assertEquals(
            MemorySensitivity.SENSITIVE,
            FactSensitivityPolicy.sensitivityOf(input(key = key(CompletionKeyKind.PRICE))),
        )
    }

    @Test
    fun qrIsLocalOnly() {
        assertEquals(
            MemorySensitivity.LOCAL_ONLY,
            FactSensitivityPolicy.sensitivityOf(input(category = IntentCategory.QR_OR_BARCODE)),
        )
    }

    @Test
    fun addressKeyIsLocalOnly() {
        assertEquals(
            MemorySensitivity.LOCAL_ONLY,
            FactSensitivityPolicy.sensitivityOf(input(key = key(CompletionKeyKind.ADDRESS))),
        )
    }

    @Test
    fun benignRecipeIsNormal() {
        assertEquals(
            MemorySensitivity.NORMAL,
            FactSensitivityPolicy.sensitivityOf(input(category = IntentCategory.RECIPE)),
        )
    }

    private fun input(
        category: IntentCategory = IntentCategory.RECIPE,
        key: CompletionKey? = null,
        text: String? = "benign text",
    ) = TriageInput(
        captureId = "cap-1",
        text = text,
        category = category,
        categoryConfidence = 0.9f,
        completionKey = key,
        canonicalUrl = null,
        contentHashHex = "hash",
        intentSource = IntentSource.AUTO_AMBIGUOUS,
        redactionMarkersPresent = text?.contains("[REDACTED_") == true,
    )

    private fun key(kind: CompletionKeyKind, label: String = "x") =
        CompletionKey(kind, label, "test", 0.9f, null)
}
