package com.saarthi.app

import android.app.Application
import android.speech.tts.Voice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

enum class Phase { Idle, Listening, Thinking, Speaking }

data class HudState(
    val phase: Phase = Phase.Idle,
    val isNight: Boolean = false,
    val heard: String = "",
    val reply: String = "",
    val replyKey: Int = 0,
    val notice: String? = null,
    val level: Float = 0f,
)

class SaarthiViewModel(app: Application) : AndroidViewModel(app) {

    val prefs = Prefs(app)
    private val client = SaarthiClient(prefs)
    private val input = VoiceInput(app)
    private val output = VoiceOutput(app)
    private val history = mutableListOf<Turn>()

    var state by mutableStateOf(HudState(isNight = prefs.isNight, reply = Greetings.random(prefs.isNight)))
        private set

    var testResult by mutableStateOf<String?>(null)
        private set

    init {
        output.onStart = {
            if (state.phase == Phase.Thinking) state = state.copy(phase = Phase.Speaking)
        }
        output.onDone = {
            if (state.phase == Phase.Speaking) state = state.copy(phase = Phase.Idle)
        }
    }

    /** Tap on the chakra: start listening, stop listening, or stop speaking. */
    fun onCoreTapped() {
        when (state.phase) {
            Phase.Idle -> startListening()
            Phase.Listening -> input.stop()
            Phase.Thinking -> Unit
            Phase.Speaking -> {
                output.stop()
                state = state.copy(phase = Phase.Idle)
            }
        }
    }

    fun showNotice(message: String) {
        state = state.copy(notice = message)
    }

    fun toggleNight() = setNight(!state.isNight, announce = false)

    private fun setNight(night: Boolean, announce: Boolean) {
        prefs.isNight = night
        val line = if (announce) Greetings.modeSwitch(night) else Greetings.random(night)
        state = state.copy(isNight = night, reply = line, replyKey = state.replyKey + 1, notice = null)
        if (announce) say(line)
    }

    fun newConversation() {
        history.clear()
        output.stop()
        state = state.copy(
            phase = Phase.Idle,
            heard = "",
            reply = Greetings.random(state.isNight),
            replyKey = state.replyKey + 1,
            notice = null,
        )
    }

    private fun startListening() {
        if (!input.isAvailable()) {
            showNotice("No speech recognition service was found. Install or update the Google app.")
            return
        }
        state = state.copy(phase = Phase.Listening, heard = "", notice = null, level = 0f)
        input.start(prefs.listenLanguage, object : VoiceInput.Listener {
            override fun onLevel(level: Float) {
                if (state.phase == Phase.Listening) state = state.copy(level = level)
            }

            override fun onPartial(text: String) {
                if (state.phase == Phase.Listening) state = state.copy(heard = text)
            }

            override fun onResult(text: String) {
                state = state.copy(heard = text, level = 0f)
                handleHeard(text)
            }

            override fun onError(message: String) {
                state = state.copy(phase = Phase.Idle, level = 0f, notice = message)
            }
        })
    }

    private fun handleHeard(text: String) {
        val modeCommand = Greetings.modeCommand(text)
        if (modeCommand != null) {
            state = state.copy(phase = Phase.Thinking)
            setNight(modeCommand, announce = true)
            return
        }

        history.add(Turn("user", text))
        state = state.copy(phase = Phase.Thinking)

        viewModelScope.launch {
            try {
                val reply = client.ask(history.takeLast(20), state.isNight)
                history.add(Turn("assistant", reply))
                state = state.copy(reply = reply, replyKey = state.replyKey + 1)
                say(reply)
            } catch (e: SetupNeededException) {
                history.removeAt(history.lastIndex)
                state = state.copy(reply = Greetings.NOT_CONNECTED, replyKey = state.replyKey + 1)
                say(Greetings.NOT_CONNECTED)
            } catch (e: Exception) {
                history.removeAt(history.lastIndex)
                state = state.copy(phase = Phase.Idle, notice = e.message ?: "Something went wrong.")
            }
        }
    }

    private fun say(text: String) {
        state = state.copy(phase = Phase.Thinking)
        val started = output.speak(text, prefs.englishVoice, prefs.hindiVoice)
        if (!started) {
            state = state.copy(phase = Phase.Idle, notice = "The voice engine isn't available. Check text-to-speech in phone settings.")
        }
    }

    // ----- Settings -----

    fun saveServer(url: String, code: String) {
        prefs.serverUrl = url
        prefs.accessCode = code
        testResult = "Testing…"
        viewModelScope.launch {
            testResult = try {
                client.test(url, code)
            } catch (e: Exception) {
                "Not connected. ${e.message ?: ""}".trim()
            }
        }
    }

    fun voices(tag: String): List<Voice> = output.voices(tag)

    fun chooseVoice(tag: String, name: String) {
        if (tag == "hi-IN") prefs.hindiVoice = name else prefs.englishVoice = name
        val sample = if (tag == "hi-IN") "नमस्ते, मैं सारथी हूँ। आपकी सेवा में।" else "Pranam. I am Saarthi, at your service."
        output.preview(tag, name, sample)
    }

    fun setListenLanguage(tag: String) {
        prefs.listenLanguage = tag
    }

    override fun onCleared() {
        input.cancel()
        output.shutdown()
    }
}
