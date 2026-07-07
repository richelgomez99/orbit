package com.orbit.app.ui.primitives

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import com.orbit.app.data.model.Intent

/**
 * The five actionable intent seals, in canonical order, paired with their
 * chip kind. AMBIGUOUS has no chip — it's the "none selected" / defer state.
 * Single source of truth for both the overlay capture flow and the manual
 * "Save to Orbit" sheet, so the chips never drift apart.
 */
val INTENT_CHIP_ORDER: List<Pair<Intent, IntentChipKind>> = listOf(
    Intent.WANT_IT to IntentChipKind.wantIt,
    Intent.REFERENCE to IntentChipKind.reference,
    Intent.READ_LATER to IntentChipKind.readLater,
    Intent.FOR_SOMEONE to IntentChipKind.forSomeone,
    Intent.INTERESTING to IntentChipKind.interesting,
)

/**
 * A wrapping row of the five [IntentChip]s. Shared by capture surfaces.
 *
 * @param selected the currently-active intent (null = none highlighted). The
 *   overlay capture pass leaves this null (tap = immediate seal); the manual
 *   sheet passes the user's persistent pick so the chosen chip stays lit.
 * @param haptics fire a long-press haptic on tap (the overlay does; the sheet
 *   doesn't need to).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IntentChipRow(
    onSelect: (Intent) -> Unit,
    modifier: Modifier = Modifier,
    selected: Intent? = null,
    haptics: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        INTENT_CHIP_ORDER.forEach { (intent, kind) ->
            IntentChip(
                intent = kind,
                active = selected == intent,
                onClick = {
                    if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelect(intent)
                },
            )
        }
    }
}
