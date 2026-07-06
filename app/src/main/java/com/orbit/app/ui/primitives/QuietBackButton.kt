package com.orbit.app.ui.primitives

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbit.app.ui.tokens.OrbitType

/**
 * Shared "‹" back affordance for Quiet Almanac screens.
 *
 * Polish audit 2026-07-06 (Batch A): the four Quiet screens each
 * hand-rolled `Text("‹")` + `.clickable` with no semantics, so TalkBack
 * announced nothing actionable — a screen-reader user could not
 * navigate back. This primitive carries a "Back" content description
 * and Button role, so every current and future Quiet screen inherits an
 * accessible back button from one definition. Visual output is identical
 * to the copies it replaces (28sp cream chevron, circular tap target).
 *
 * @param label spoken description; defaults to "Back". Screens with a
 *   more specific destination (e.g. "Back to Diary") may override.
 */
@Composable
fun QuietBackButton(
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier,
    label: String = "Back",
) {
    Text(
        text = "‹",
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = label
                role = Role.Button
            }
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = color,
        style = TextStyle(
            fontFamily = OrbitType.QuietAlmanac.bodySans,
            fontSize = 28.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Normal,
        ),
    )
}
