package com.orbit.app.understanding.triage

import com.orbit.app.data.model.MemorySensitivity
import com.orbit.app.understanding.domain.CompletionKeyKind
import com.orbit.app.understanding.domain.IntentCategory

/**
 * Phase A — deterministic sensitivity classification for extracted facts.
 *
 * Intentionally does NOT call the on-device LLM `scanSensitivity` (M2 finding:
 * a 1B grossly over-triggers). Sensitivity gates auto-promotion: only NORMAL
 * facts may auto-promote; SENSITIVE/LOCAL_ONLY are at most review candidates
 * and never leave the device.
 */
object FactSensitivityPolicy {

    private val FINANCIAL_CATEGORIES = setOf(
        IntentCategory.RECEIPT_OR_ORDER,
        IntentCategory.COUPON_OR_PROMO,
    )

    private val FINANCIAL_KEYS = setOf(
        CompletionKeyKind.PRICE,
        CompletionKeyKind.ORDER_ID,
        CompletionKeyKind.COUPON_CODE,
    )

    fun sensitivityOf(input: TriageInput): MemorySensitivity {
        if (input.redactionMarkersPresent) return MemorySensitivity.SENSITIVE
        val keyKind = input.completionKey?.kind
        if (input.category in FINANCIAL_CATEGORIES || keyKind in FINANCIAL_KEYS) {
            return MemorySensitivity.SENSITIVE
        }
        if (input.category == IntentCategory.QR_OR_BARCODE || keyKind == CompletionKeyKind.ADDRESS) {
            return MemorySensitivity.LOCAL_ONLY
        }
        return MemorySensitivity.NORMAL
    }
}
