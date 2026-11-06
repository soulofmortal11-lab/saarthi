package com.saarthi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SaarthiTestScreen() }
    }
}

/** SAARTHI's colours: palm-leaf parchment by day, starry indigo sky by night. */
private object Palette {
    val parchment = Color(0xFFEFE3C8)
    val leaf = Color(0xFFE2CFA2)
    val leafPin = Color(0xFFA9773F)
    val bronze = Color(0xFF9A6A1C)
    val bronzeHub = Color(0xFFB8862E)
    val copperInk = Color(0xFF8A4B22)
    val ink = Color(0xFF6B3A18)
    val night = Color(0xFF120E2A)
    val gold = Color(0xFFD4A24C)
    val goldBright = Color(0xFFF2C66D)
    val starlight = Color(0xFFE8E2FF)
    val lavender = Color(0xFFC9BEFF)
}

private data class Star(val x: Float, val y: Float, val radiusDp: Float, val phase: Float)

private const val MODE_SWITCH_MS = 1400

@Composable
fun SaarthiTestScreen() {
    var isNight by rememberSaveable { mutableStateOf(false) }

    // Dusk and dawn: every colour glides between the two worlds.
    val background by animateColorAsState(
        if (isNight) Palette.night else Palette.parchment, tween(MODE_SWITCH_MS), label = "background"
    )
    val chakraColor by animateColorAsState(
        if (isNight) Palette.gold else Palette.bronze, tween(MODE_SWITCH_MS), label = "chakra"
    )
    val hubColor by animateColorAsState(
        if (isNight) Palette.goldBright else Palette.bronzeHub, tween(MODE_SWITCH_MS), label = "hub"
    )
    val titleColor by animateColorAsState(
        if (isNight) Palette.goldBright else Palette.copperInk, tween(MODE_SWITCH_MS), label = "title"
    )
    val textColor by animateColorAsState(
        if (isNight) Palette.lavender else Palette.ink, tween(MODE_SWITCH_MS), label = "text"
    )
    val nightness by animateFloatAsState(
        if (isNight) 1f else 0f, tween(MODE_SWITCH_MS), label = "nightness"
    )

    val motion = rememberInfiniteTransition(label = "motion")
    val rotation by motion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing)),
        label = "rotation"
    )
    val twinkle by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3_200, easing = LinearEasing), RepeatMode.Reverse),
        label = "twinkle"
    )
    val drift by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(90_000, easing = LinearEasing)),
        label = "drift"
    )

    val stars = remember {
        val random = Random(7)
        List(80) {
            Star(random.nextFloat(), random.nextFloat(), 0.6f + random.nextFloat() * 1.6f, random.nextFloat())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (nightness > 0f) drawStars(stars, twinkle, drift, nightness)
            val dayAlpha = 1f - nightness
            if (dayAlpha > 0f) {
                drawPalmLeaf(top = size.height * 0.12f, alpha = dayAlpha)
                drawPalmLeaf(top = size.height * 0.84f, alpha = dayAlpha)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                ModeToggle(isNight = isNight, color = textColor) { isNight = !isNight }
            }

            Spacer(modifier = Modifier.weight(1f))

            Chakra(
                rotation = rotation,
                color = chakraColor,
                hubColor = hubColor,
                background = background,
                modifier = Modifier.size(250.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(text = "सारथी", color = titleColor, fontSize = 46.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isNight) "Shubh ratri. Pipeline kaam kar rahi hai." else "Pranam. The pipeline works.",
                color = textColor,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Test build ${BuildConfig.VERSION_NAME}",
                color = textColor.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ModeToggle(isNight: Boolean, color: Color, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .border(1.dp, color.copy(alpha = 0.5f), shape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = if (isNight) "Day mode" else "Night mode", color = color, fontSize = 13.sp)
    }
}

/** The golden chariot wheel: SAARTHI's core. */
@Composable
private fun Chakra(
    rotation: Float,
    color: Color,
    hubColor: Color,
    background: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val centre = center
        val radius = size.minDimension / 2f

        drawCircle(
            color = color.copy(alpha = 0.45f),
            radius = radius * 0.98f,
            center = centre,
            style = Stroke(
                width = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 7.dp.toPx()))
            )
        )

        rotate(degrees = rotation, pivot = centre) {
            drawCircle(color, radius * 0.84f, centre, style = Stroke(2.5.dp.toPx()))
            drawCircle(color, radius * 0.72f, centre, style = Stroke(1.dp.toPx()))

            repeat(16) { i ->
                val angle = (i * 22.5 * PI / 180).toFloat()
                val inner = radius * 0.16f
                val outer = radius * 0.72f
                drawLine(
                    color = color,
                    start = Offset(centre.x + cos(angle) * inner, centre.y + sin(angle) * inner),
                    end = Offset(centre.x + cos(angle) * outer, centre.y + sin(angle) * outer),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            repeat(24) { i ->
                val angle = (i * 15.0 * PI / 180).toFloat()
                drawCircle(
                    color = color,
                    radius = 2.dp.toPx(),
                    center = Offset(centre.x + cos(angle) * radius * 0.78f, centre.y + sin(angle) * radius * 0.78f)
                )
            }
        }

        drawCircle(background, radius * 0.16f, centre)
        drawCircle(hubColor, radius * 0.16f, centre, style = Stroke(2.5.dp.toPx()))
        drawCircle(hubColor, radius * 0.07f, centre)
    }
}

private fun DrawScope.drawStars(stars: List<Star>, twinkle: Float, drift: Float, nightness: Float) {
    stars.forEach { star ->
        val glow = 0.3f + 0.7f * abs(sin((twinkle + star.phase) * PI.toFloat()))
        val x = ((star.x + drift * 0.08f) % 1f) * size.width
        drawCircle(
            color = Palette.starlight.copy(alpha = glow * nightness),
            radius = star.radiusDp.dp.toPx(),
            center = Offset(x, star.y * size.height)
        )
    }
}

private fun DrawScope.drawPalmLeaf(top: Float, alpha: Float) {
    val left = size.width * 0.07f
    val width = size.width * 0.86f
    val height = 26.dp.toPx()
    val pinInset = 18.dp.toPx()

    drawRoundRect(
        color = Palette.leaf.copy(alpha = alpha),
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(height / 2f)
    )
    listOf(0.32f, 0.52f, 0.68f).forEach { fraction ->
        val y = top + height * fraction
        drawLine(
            color = Palette.leafPin.copy(alpha = alpha * 0.25f),
            start = Offset(left + pinInset * 1.8f, y),
            end = Offset(left + width - pinInset * 1.8f, y),
            strokeWidth = 0.6.dp.toPx()
        )
    }
    drawCircle(Palette.leafPin.copy(alpha = alpha), 3.5.dp.toPx(), Offset(left + pinInset, top + height / 2f))
    drawCircle(Palette.leafPin.copy(alpha = alpha), 3.5.dp.toPx(), Offset(left + width - pinInset, top + height / 2f))
}
