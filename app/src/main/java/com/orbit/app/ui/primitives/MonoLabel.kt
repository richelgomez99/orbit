package com.orbit.app.ui.primitives

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbit.app.ui.tokens.OrbitPalette
import com.orbit.app.ui.tokens.OrbitType
import java.util.Locale

@Composable
fun MonoLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    size: TextUnit = 10.sp,
) {
    val palette = OrbitPalette.current(dark = isSystemInDarkTheme())
    val resolvedColor = color ?: palette.inkFaint
    val letterSpacing = (size.value * 0.18f).sp

    Text(
        text = text.uppercase(Locale.ROOT),
        modifier = modifier,
        color = resolvedColor,
        style = TextStyle(
            fontFamily = OrbitType.QuietAlmanac.captionMono,
            fontSize = size,
            lineHeight = (size.value * 1.4f).sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = letterSpacing,
        ),
    )
}

@Preview(name = "mono label")
@Composable
private fun MonoLabelPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        MonoLabel("// Orbit noticed")
    }
}