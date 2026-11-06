package com.saarthi.app.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.BreakIterator

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier, selected: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .border(if (selected) 1.5.dp else 1.dp, color.copy(alpha = if (selected) 0.95f else 0.45f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = text, color = color, fontSize = 13.sp)
    }
}

/**
 * Text that inscribes itself, letter by letter, like ink on a manuscript.
 * Hidden letters are transparent, so the layout doesn't jump while it writes.
 */
@Composable
fun InscribedText(
    text: String,
    key: Int,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val boundaries = remember(text) { characterBoundaries(text) }
    var shown by remember(key, text) { mutableIntStateOf(0) }

    LaunchedEffect(key, text) {
        shown = 0
        val stepMs = (2_400L / boundaries.size.coerceAtLeast(1)).coerceIn(14L, 40L)
        while (shown < boundaries.size) {
            delay(stepMs)
            shown++
        }
    }

    val cut = if (shown == 0) 0 else boundaries[shown - 1]
    val styled = buildAnnotatedString {
        withStyle(SpanStyle(color = color)) { append(text.substring(0, cut)) }
        withStyle(SpanStyle(color = Color.Transparent)) { append(text.substring(cut)) }
    }
    Text(text = styled, fontSize = fontSize, lineHeight = fontSize * 1.4f, textAlign = TextAlign.Center, modifier = modifier)
}

/** End index of each visible character, so Devanagari letters appear whole. */
private fun characterBoundaries(text: String): List<Int> {
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(text)
    val ends = mutableListOf<Int>()
    var end = iterator.next()
    while (end != BreakIterator.DONE) {
        ends.add(end)
        end = iterator.next()
    }
    return ends
}
