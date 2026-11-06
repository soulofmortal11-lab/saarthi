package com.saarthi.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

private data class Star(val x: Float, val y: Float, val radiusDp: Float, val phase: Float, val speed: Float)

private data class ShootingStar(val x: Float, val y: Float, val angleDeg: Float, val travel: Float)

/**
 * The backdrop: palm-leaf strips by day, a drifting, twinkling star field by night,
 * with an occasional shooting star.
 */
@Composable
fun Sky(nightness: Float, isNight: Boolean, time: () -> Float, modifier: Modifier = Modifier) {
    val stars = remember {
        val random = Random(7)
        List(110) {
            Star(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radiusDp = 0.5f + random.nextFloat().pow(2) * 1.9f,
                phase = random.nextFloat() * 6.28f,
                speed = 0.6f + random.nextFloat() * 1.6f,
            )
        }
    }

    var shooting by remember { mutableStateOf<ShootingStar?>(null) }
    val streak = remember { Animatable(0f) }

    LaunchedEffect(isNight) {
        if (!isNight) {
            shooting = null
            return@LaunchedEffect
        }
        delay(4_000)
        while (true) {
            launchStreak(streak) { shooting = it }
            // Now and then, a second one follows close behind.
            if (Random.nextFloat() < 0.3f) {
                delay(Random.nextLong(300, 900))
                launchStreak(streak) { shooting = it }
            }
            shooting = null
            delay(Random.nextLong(10_000, 28_000))
        }
    }

    Canvas(modifier = modifier) {
        val t = time()
        if (nightness > 0f) {
            drawStars(stars, t, nightness)
            shooting?.let { drawShootingStar(it, streak.value, nightness) }
        }
        val dayAlpha = 1f - nightness
        if (dayAlpha > 0f) {
            drawPalmLeaf(top = size.height * 0.105f, alpha = dayAlpha)
            drawPalmLeaf(top = size.height * 0.875f, alpha = dayAlpha)
        }
    }
}

private suspend fun launchStreak(streak: Animatable<Float, AnimationVector1D>, show: (ShootingStar) -> Unit) {
    val random = Random.Default
    show(
        ShootingStar(
            x = 0.15f + random.nextFloat() * 0.75f,
            y = 0.04f + random.nextFloat() * 0.35f,
            angleDeg = 140f + random.nextFloat() * 30f,
            travel = 0.35f + random.nextFloat() * 0.25f,
        )
    )
    streak.snapTo(0f)
    streak.animateTo(1f, tween(durationMillis = random.nextInt(900, 1400), easing = FastOutSlowInEasing))
}

private fun DrawScope.drawStars(stars: List<Star>, t: Float, nightness: Float) {
    val drift = (t / 900f) % 1f
    stars.forEach { star ->
        val glow = 0.25f + 0.75f * abs(sin(t * star.speed * 0.9f + star.phase))
        val x = ((star.x + drift * 0.12f) % 1f) * size.width
        val center = Offset(x, star.y * size.height)
        val radius = star.radiusDp.dp.toPx()
        if (star.radiusDp > 1.6f) {
            drawCircle(Palette.starlight.copy(alpha = 0.12f * glow * nightness), radius * 3.2f, center)
        }
        drawCircle(Palette.starlight.copy(alpha = glow * nightness), radius, center)
    }
}

private fun DrawScope.drawShootingStar(star: ShootingStar, progress: Float, nightness: Float) {
    val angle = (star.angleDeg * PI / 180).toFloat()
    val direction = Offset(cos(angle), sin(angle))
    val start = Offset(star.x * size.width, star.y * size.height)
    val distance = star.travel * size.width
    val head = start + direction * (distance * progress)
    val tailLength = distance * 0.45f * (1f - progress * 0.4f)
    val tail = head - direction * tailLength
    val fade = (1f - progress.pow(3)) * nightness

    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(Color.Transparent, Palette.starlight.copy(alpha = 0.9f * fade)),
            start = tail,
            end = head,
        ),
        start = tail,
        end = head,
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawCircle(Palette.starlight.copy(alpha = 0.25f * fade), 5.dp.toPx(), head)
    drawCircle(Color.White.copy(alpha = fade), 1.6.dp.toPx(), head)
}

private fun DrawScope.drawPalmLeaf(top: Float, alpha: Float) {
    val left = size.width * 0.07f
    val width = size.width * 0.86f
    val height = 24.dp.toPx()
    val pinInset = 18.dp.toPx()

    drawRoundRect(
        color = Palette.leaf.copy(alpha = alpha),
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(height / 2f),
    )
    listOf(0.32f, 0.52f, 0.68f).forEach { fraction ->
        val y = top + height * fraction
        drawLine(
            color = Palette.leafPin.copy(alpha = alpha * 0.25f),
            start = Offset(left + pinInset * 1.8f, y),
            end = Offset(left + width - pinInset * 1.8f, y),
            strokeWidth = 0.6.dp.toPx(),
        )
    }
    drawCircle(Palette.leafPin.copy(alpha = alpha), 3.5.dp.toPx(), Offset(left + pinInset, top + height / 2f))
    drawCircle(Palette.leafPin.copy(alpha = alpha), 3.5.dp.toPx(), Offset(left + width - pinInset, top + height / 2f))
}
