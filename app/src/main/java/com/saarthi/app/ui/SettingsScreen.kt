package com.saarthi.app.ui

import android.speech.tts.Voice
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saarthi.app.BuildConfig
import com.saarthi.app.CrashLog
import com.saarthi.app.SaarthiViewModel
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(vm: SaarthiViewModel, onBack: () -> Unit) {
    val isNight = vm.state.isNight
    val background = if (isNight) Palette.night else Palette.parchment
    val panel = if (isNight) Palette.nightPanel else Palette.leaf.copy(alpha = 0.55f)
    val accent = if (isNight) Palette.goldBright else Palette.copperInk
    val text = if (isNight) Palette.lavender else Palette.ink
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var url by remember { mutableStateOf(vm.prefs.serverUrl) }
    var code by remember { mutableStateOf(vm.prefs.accessCode) }
    var listenLanguage by remember { mutableStateOf(vm.prefs.listenLanguage) }
    var englishVoice by remember { mutableStateOf(vm.prefs.englishVoice) }
    var hindiVoice by remember { mutableStateOf(vm.prefs.hindiVoice) }
    var crash by remember { mutableStateOf(CrashLog.read(context)) }

    // The voice engine starts up in the background, so check again for a few seconds.
    var englishVoices by remember { mutableStateOf<List<Voice>>(emptyList()) }
    var hindiVoices by remember { mutableStateOf<List<Voice>>(emptyList()) }
    LaunchedEffect(Unit) {
        repeat(10) {
            englishVoices = vm.voices("en-IN")
            hindiVoices = vm.voices("hi-IN")
            if (englishVoices.isNotEmpty() && hindiVoices.isNotEmpty()) return@LaunchedEffect
            delay(500)
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = text,
        unfocusedTextColor = text,
        focusedBorderColor = accent,
        unfocusedBorderColor = text.copy(alpha = 0.4f),
        focusedLabelColor = accent,
        unfocusedLabelColor = text.copy(alpha = 0.7f),
        cursorColor = accent,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(text = "Back", color = text, onClick = onBack)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "Settings", color = accent, fontSize = 24.sp)
        }

        Section("Brain connection", panel, accent) {
            Hint("Paste the server address and access code from your Cloudflare setup.", text)
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Server address") },
                placeholder = { Text("https://saarthi.yourname.workers.dev") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Access code") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Pill(text = "Save and test", color = accent, selected = true) { vm.saveServer(url, code) }
            vm.testResult?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = text, fontSize = 13.sp)
            }
        }

        Section("Listening language", panel, accent) {
            Hint("Hindi and Hinglish usually work best with English (India).", text)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("English (India)", text, selected = listenLanguage == "en-IN") {
                    listenLanguage = "en-IN"
                    vm.setListenLanguage("en-IN")
                }
                Pill("हिन्दी", text, selected = listenLanguage == "hi-IN") {
                    listenLanguage = "hi-IN"
                    vm.setListenLanguage("hi-IN")
                }
            }
        }

        Section("English voice", panel, accent) {
            Hint("Tap a voice to hear it and choose it.", text)
            VoiceList(englishVoices, englishVoice, text, accent) { name ->
                englishVoice = name
                vm.chooseVoice("en-IN", name)
            }
        }

        Section("Hindi voice", panel, accent) {
            Hint("Used whenever Saarthi replies in Hindi.", text)
            VoiceList(hindiVoices, hindiVoice, text, accent) { name ->
                hindiVoice = name
                vm.chooseVoice("hi-IN", name)
            }
        }

        Section("Conversation", panel, accent) {
            Hint("Saarthi remembers the conversation until you start a new one or close the app.", text)
            Pill("Start a new conversation", text) {
                vm.newConversation()
                onBack()
            }
        }

        Section("Crash report", panel, accent) {
            val report = crash
            if (report == null) {
                Hint("No crashes recorded.", text)
            } else {
                Hint("The app crashed last time. Copy this and send it to Claude.", text)
                Text(
                    text = report.take(1500),
                    color = text,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 14.sp,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Pill("Copy", accent) { clipboard.setText(AnnotatedString(report)) }
                    Pill("Clear", text) {
                        CrashLog.clear(context)
                        crash = null
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "सारथी v${BuildConfig.VERSION_NAME}",
            color = text.copy(alpha = 0.6f),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun Section(title: String, panel: Color, accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Spacer(modifier = Modifier.height(18.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(panel)
            .padding(16.dp),
    ) {
        Text(text = title, color = accent, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun Hint(text: String, color: Color) {
    Text(text = text, color = color.copy(alpha = 0.8f), fontSize = 13.sp, lineHeight = 18.sp)
    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
private fun VoiceList(voices: List<Voice>, selected: String?, text: Color, accent: Color, onPick: (String) -> Unit) {
    if (voices.isEmpty()) {
        Text("Loading voices… If none appear, install voice data in your phone's text-to-speech settings.", color = text, fontSize = 13.sp)
        return
    }
    val active = selected ?: voices.first().name
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        voices.forEachIndexed { index, voice ->
            val isActive = voice.name == active
            val shape = RoundedCornerShape(12.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .border(if (isActive) 1.5.dp else 1.dp, (if (isActive) accent else text).copy(alpha = if (isActive) 0.9f else 0.3f), shape)
                    .clickable { onPick(voice.name) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Voice ${index + 1}" + if (voice.isNetworkConnectionRequired) " · online" else " · offline",
                        color = if (isActive) accent else text,
                        fontSize = 14.sp,
                    )
                    Text(text = voice.name, color = text.copy(alpha = 0.55f), fontSize = 11.sp)
                }
                if (isActive) Text("Chosen", color = accent, fontSize = 12.sp)
            }
        }
    }
}
