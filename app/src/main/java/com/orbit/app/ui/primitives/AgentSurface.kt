package com.orbit.app.ui.primitives

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbit.app.ui.tokens.OrbitType

/**
 * Spec 019 — shared Quiet Almanac primitives for the Orbit agent workspace,
 * so every surfaced thing (action drafts, memory review, follow-ups, curious
 * questions) renders as one coherent feed instead of Material one-offs.
 */
object AgentSurface {
    val Panel = Color(0x14F3EAD8)      // faint cream fill (hairline card)
    val Cream = Color(0xFFF3EAD8)
    val CreamDim = Color(0xB3F3EAD8)
    val CreamFaint = Color(0x66F3EAD8)
    val Accent = Color(0xFFE8B06A)
    val AccentInk = Color(0xFF211607)
    val Rule = Color(0x29F3EAD8)
}

@Composable
fun AgentSectionHeader(kicker: String, title: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        MonoLabel(text = kicker, color = AgentSurface.Accent, size = 9.5.sp)
        Text(
            text = title,
            color = AgentSurface.Cream,
            style = TextStyle(
                fontFamily = OrbitType.QuietAlmanac.displaySerif,
                fontSize = 19.sp,
                lineHeight = 24.sp,
            ),
        )
    }
}

@Composable
fun SurfacedCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AgentSurface.Panel)
            .border(BorderStroke(1.dp, AgentSurface.Rule), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
fun AgentCardTitle(text: String) {
    Text(
        text = text,
        color = AgentSurface.Cream,
        style = TextStyle(
            fontFamily = OrbitType.QuietAlmanac.displaySerif,
            fontSize = 17.sp,
            lineHeight = 22.sp,
        ),
    )
}

@Composable
fun AgentCardBody(text: String, dim: Boolean = false) {
    Text(
        text = text,
        color = if (dim) AgentSurface.CreamDim else AgentSurface.Cream,
        style = TextStyle(
            fontFamily = OrbitType.QuietAlmanac.bodySans,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        ),
    )
}

@Composable
fun AgentCardMeta(text: String) {
    MonoLabel(text = text.uppercase(), color = AgentSurface.CreamFaint, size = 8.5.sp)
}

@Composable
fun AgentActionRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun AgentSecondaryAction(label: String, onClick: () -> Unit, testTag: String? = null) {
    Text(
        text = label,
        modifier = Modifier
            .let { if (testTag != null) it.testTag(testTag) else it }
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = AgentSurface.CreamDim,
        style = TextStyle(fontFamily = OrbitType.QuietAlmanac.bodySans, fontSize = 13.sp),
    )
}

@Composable
fun AgentPrimaryAction(label: String, onClick: () -> Unit, testTag: String? = null) {
    Box(
        modifier = Modifier
            .let { if (testTag != null) it.testTag(testTag) else it }
            .clip(RoundedCornerShape(999.dp))
            .background(AgentSurface.Accent)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp),
    ) {
        Text(
            text = label,
            color = AgentSurface.AccentInk,
            style = TextStyle(
                fontFamily = OrbitType.QuietAlmanac.displaySerif,
                fontSize = 15.sp,
                fontStyle = FontStyle.Italic,
            ),
        )
    }
}
