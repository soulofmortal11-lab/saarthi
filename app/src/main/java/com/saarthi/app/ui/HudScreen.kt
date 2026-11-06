package com.saarthi.app.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saarthi.app.BuildConfig
import com.saarthi.app.Phase
import com.saarthi.app.SaarthiViewModel

const val MODE_SWITCH_MS = 1400

@Composable
fun HudScreen(vm: SaarthiViewModel, onOpenSettings: () -> Unit, onCoreTap: () -> Unit) {
    val state = vm.state
    val isNight = state.isNight
    val view = LocalView.current

    // Dusk and dawn: every colour glides between the two worlds.
    val background by animateColorAsState(if (isNight) Palette.night else Palette.parchment, tween(MODE_SWITCH_MS), label = "bg")
    val wheel by animateColorAsState(if (isNight) Palette.gold else Palette.bronze, tween(MODE_SWITCH_MS), label = "wheel")
    val hub by animateColorAsState(if (isNight) Palette.goldBright else Palette.bronzeHub, tween(MODE_SWITCH_MS), label = "hub")
    val title by animateColorAsState(if (isNight) Palette.goldBright else Palette.copperInk, tween(MODE_SWITCH_MS), label = "title")
    val text by animateColorAsState(if (isNight) Palette.lavender else Palette.ink, tween(MODE_SWITCH_MS), label = "text")
    val alert by animateColorAsState(if (isNight) Palette.alertNight else Palette.alertDay, tween(MODE_SWITCH_MS), label = "alert")
    val nightness by animateFloatAsState(if (isNight) 1f else 0f, tween(MODE_SWITCH_MS), label = "nightness")

    // One shared clock for every animation on screen.
    val clock = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now -> clock.floatValue = (now - start) / 1_000_000_000f }
        }
    }
    val time = { clock.floatValue }

    val status = when (state.phase) {
        Phase.Idle -> if (vm.prefs.isConfigured) "Tap the chakra to speak" else "Tap the chakra · brain not connected yet"
        Phase.Listening -> "Listening…"
        Phase.Thinking -> "Thinking…"
        Phase.Speaking -> "Tap to stop"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Sky(nightness = nightness, isNight = isNight, time = time, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Pill(text = "Settings", color = text, onClick = onOpenSettings)
                Pill(text = if (isNight) "Day mode" else "Night mode", color = text, onClick = vm::toggleNight)
            }

            Spacer(modifier = Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .size(300.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onCoreTap()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Chakra(
                    phase = state.phase,
                    level = state.level,
                    nightness = nightness,
                    wheelColor = wheel,
                    hubColor = hub,
                    background = background,
                    time = time,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Text(text = "सारथी", color = title, fontSize = 36.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = status, color = text.copy(alpha = 0.75f), fontSize = 13.sp, letterSpacing = 0.5.sp)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (state.heard.isNotBlank()) {
                    Text(
                        text = "“${state.heard}”",
                        color = text.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                InscribedText(text = state.reply, key = state.replyKey, color = title, fontSize = 19.sp)
                state.notice?.let {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = it, color = alert, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }

            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                color = text.copy(alpha = 0.5f),
                fontSize = 11.sp,
            )
        }
    }
}
