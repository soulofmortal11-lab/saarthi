package com.saarthi.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.saarthi.app.Phase
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private data class Ember(
    val baseAngle: Float,
    val orbit: Float,
    val speed: Float,
    val sizeDp: Float,
    val phase: Float,
)

/**
 * The golden chariot wheel, alive: a breathing glow, orbiting embers, a light that runs
 * along the spokes, ripples that follow your voice, and a counter-turning outer dial.
 */
@Composable
fun Chakra(
    phase: Phase,
    level: Float,
    nightness: Float,
    wheelColor: Color,
    hubColor: Color,
    background: Color,
    time: () -> Float,
    modifier: Modifier = Modifier,
) {
    val embers = remember {
        val random = Random(11)
        List(44) {
            Ember(
                baseAngle = random.nextFloat() * 6.283f,
                orbit = 1.08f + random.nextFloat() * 0.42f,
                speed = (0.12f + random.nextFloat() * 0.38f) * (if (random.nextBoolean()) 1f else -1f),
                sizeDp = 0.9f + random.nextFloat() * 1.8f,
                phase = random.nextFloat() * 6.283f,
            )
        }
    }

    val listening by animateFloatAsState(if (phase == Phase.Listening) 1f else 0f, tween(500), label = "listening")
    val thinking by animateFloatAsState(if (phase == Phase.Thinking) 1f else 0f, tween(500), label = "thinking")
    val speaking by animateFloatAsState(if (phase == Phase.Speaking) 1f else 0f, tween(500), label = "speaking")
    val voiceLevel by animateFloatAsState(level, tween(120), label = "level")
    val spin by animateFloatAsState(
        when (phase) {
            Phase.Idle -> 14f
            Phase.Listening -> 26f
            Phase.Thinking -> 150f
            Phase.Speaking -> 40f
        },
        tween(900),
        label = "spin",
    )

    // The wheel's angle keeps accumulating, so speed changes stay smooth.
    val angle = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) angle.floatValue = (angle.floatValue + spin * (now - last) / 1_000_000_000f) % 360f
                last = now
            }
        }
    }

    val glowColor = lerp(Palette.ember, Palette.goldBright, nightness)
    val emberColor = lerp(Palette.ember, Palette.goldBright, nightness)

    Canvas(modifier = modifier) {
        val t = time()
        val center = this.center
        val radius = min(size.width, size.height) / 2f * 0.6f
        val voice = 0.5f + 0.5f * sin(t * 9f) * sin(t * 3.7f)
        val wheelAngle = angle.floatValue

        // Breathing glow.
        val glowAlpha = (0.16f + 0.16f * nightness) + 0.06f * sin(t * 1.6f) +
            listening * voiceLevel * 0.3f + speaking * (0.1f + 0.12f * voice) + thinking * 0.1f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    glowColor.copy(alpha = glowAlpha.coerceIn(0f, 1f)),
                    glowColor.copy(alpha = (glowAlpha * 0.35f).coerceIn(0f, 1f)),
                    Color.Transparent,
                ),
                center = center,
                radius = radius * 1.75f,
            ),
            radius = radius * 1.75f,
            center = center,
        )

        // Ripples that grow with your voice while listening.
        if (listening > 0.01f) {
            repeat(3) { k ->
                val p = (t * 0.8f + k / 3f) % 1f
                val rippleRadius = radius * (1.02f + p * 0.5f * (0.6f + voiceLevel))
                drawCircle(
                    color = wheelColor.copy(alpha = ((1f - p) * 0.55f * listening * (0.35f + voiceLevel)).coerceIn(0f, 1f)),
                    radius = rippleRadius,
                    center = center,
                    style = Stroke(1.5.dp.toPx()),
                )
            }
        }

        // Outer dial turning the other way.
        rotate(degrees = -wheelAngle * 0.6f, pivot = center) {
            drawCircle(
                color = wheelColor.copy(alpha = 0.45f),
                radius = radius * 1.2f,
                center = center,
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 8.dp.toPx())),
                ),
            )
        }
        rotate(degrees = -wheelAngle * 0.3f, pivot = center) {
            drawTicks(center, radius * 1.1f, radius * 1.14f, wheelColor.copy(alpha = 0.35f))
        }

        // Embers drifting around the wheel.
        embers.forEach { ember ->
            val a = ember.baseAngle + t * ember.speed * (1f + thinking * 3f)
            val pull = thinking * 0.32f * (0.5f + 0.5f * sin(t * 2f + ember.phase))
            val push = speaking * 0.1f * voice + listening * 0.08f * voiceLevel
            val wobble = sin(t * 1.3f + ember.phase) * 0.03f
            val r = radius * (ember.orbit - pull + push + wobble)
            val position = Offset(center.x + cos(a) * r, center.y + sin(a) * r)
            val twinkle = 0.35f + 0.65f * abs(sin(t * 0.9f + ember.phase))
            val alpha = twinkle * (0.55f + 0.35f * nightness)
            val emberRadius = ember.sizeDp.dp.toPx()
            drawCircle(emberColor.copy(alpha = alpha * 0.18f), emberRadius * 3f, position)
            drawCircle(emberColor.copy(alpha = alpha), emberRadius, position)
        }

        // The wheel itself, with a light running along the spokes.
        val sweep = (t * 110f) % 360f
        rotate(degrees = wheelAngle, pivot = center) {
            drawCircle(wheelColor, radius, center, style = Stroke(2.5.dp.toPx()))
            drawCircle(wheelColor, radius * 0.86f, center, style = Stroke(1.dp.toPx()))
            drawCircle(wheelColor.copy(alpha = 0.6f), radius * 0.32f, center, style = Stroke(0.8.dp.toPx()))

            repeat(16) { i ->
                val spokeDegrees = i * 22.5f
                val worldDegrees = (spokeDegrees + wheelAngle) % 360f
                val distance = abs(((worldDegrees - sweep + 540f) % 360f) - 180f)
                val highlight = (1f - distance / 40f).coerceAtLeast(0f)
                val brightness = (highlight * 0.85f + speaking * voice * 0.45f).coerceIn(0f, 1f)
                val spokeColor = lerp(wheelColor, hubColor, brightness)
                val rad = (spokeDegrees * PI / 180).toFloat()
                drawLine(
                    color = spokeColor,
                    start = Offset(center.x + cos(rad) * radius * 0.19f, center.y + sin(rad) * radius * 0.19f),
                    end = Offset(center.x + cos(rad) * radius * 0.86f, center.y + sin(rad) * radius * 0.86f),
                    strokeWidth = (1.5f + highlight * 1.2f).dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            repeat(24) { i ->
                val rad = (i * 15.0 * PI / 180).toFloat()
                drawCircle(
                    color = wheelColor,
                    radius = 2.dp.toPx(),
                    center = Offset(center.x + cos(rad) * radius * 0.93f, center.y + sin(rad) * radius * 0.93f),
                )
            }
        }

        // Hub, pulsing with the voice.
        drawCircle(background, radius * 0.19f, center)
        drawCircle(hubColor, radius * 0.19f, center, style = Stroke(2.5.dp.toPx()))
        val hubDot = radius * (0.075f + 0.015f * sin(t * 3f) + speaking * voice * 0.035f + listening * voiceLevel * 0.035f)
        drawCircle(hubColor.copy(alpha = 0.3f), hubDot * 1.9f, center)
        drawCircle(hubColor, hubDot, center)
    }
}

private fun DrawScope.drawTicks(center: Offset, inner: Float, outer: Float, color: Color) {
    repeat(72) { i ->
        val rad = (i * 5.0 * PI / 180).toFloat()
        val long = i % 6 == 0
        val start = if (long) inner - (outer - inner) else inner
        drawLine(
            color = color,
            start = Offset(center.x + cos(rad) * start, center.y + sin(rad) * start),
            end = Offset(center.x + cos(rad) * outer, center.y + sin(rad) * outer),
            strokeWidth = (if (long) 1.2f else 0.7f).dp.toPx(),
        )
    }
}
