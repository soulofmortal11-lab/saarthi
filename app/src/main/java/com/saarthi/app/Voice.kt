package com.saarthi.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale

/** Listens through the phone's speech recognition service. */
class VoiceInput(private val context: Context) {

    interface Listener {
        fun onLevel(level: Float)
        fun onPartial(text: String)
        fun onResult(text: String)
        fun onError(message: String)
    }

    private var recognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(language: String, listener: Listener) {
        cancel()
        val speech = SpeechRecognizer.createSpeechRecognizer(context)
        var finished = false

        speech.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                listener.onLevel(((rmsdB + 2f) / 12f).coerceIn(0f, 1f))
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                if (finished) return
                finished = true
                listener.onError(describe(error))
            }

            override fun onResults(results: Bundle?) {
                if (finished) return
                finished = true
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (text.isNullOrBlank()) listener.onError("I didn't catch that. Tap and try again.")
                else listener.onResult(text)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank()) listener.onPartial(text)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        recognizer = speech
        speech.startListening(intent)
    }

    fun stop() {
        recognizer?.stopListening()
    }

    fun cancel() {
        recognizer?.let {
            it.cancel()
            it.destroy()
        }
        recognizer = null
    }

    private fun describe(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't catch that. Tap and try again."
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition needs internet. Check your connection."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Saarthi needs microphone access to hear you."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "The speech service is busy. Try again in a moment."
        SpeechRecognizer.ERROR_AUDIO -> "The microphone couldn't be used. Is another app using it?"
        12, 13 -> "This language isn't available for speech recognition. Try the other one in Settings."
        else -> "Speech recognition stopped (code $error). Tap to try again."
    }
}

/** Speaks through the phone's text-to-speech engine. */
class VoiceOutput(context: Context) : TextToSpeech.OnInitListener {

    var onStart: () -> Unit = {}
    var onDone: () -> Unit = {}

    private val main = Handler(Looper.getMainLooper())
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var counter = 0
    private var pending: (() -> Unit)? = null

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (!ready) return
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                main.post { onStart() }
            }

            override fun onDone(utteranceId: String?) {
                main.post { onDone() }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                main.post { onDone() }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                main.post { onDone() }
            }
        })
        pending?.invoke()
        pending = null
    }

    /** Voices for a language tag such as "en-IN" or "hi-IN". */
    fun voices(tag: String): List<Voice> {
        if (!ready) return emptyList()
        val wanted = Locale.forLanguageTag(tag)
        return runCatching {
            tts.voices.orEmpty()
                .filter { it.locale.language == wanted.language && it.locale.country == wanted.country }
                .filter { !it.features.orEmpty().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
                .sortedWith(compareBy({ it.isNetworkConnectionRequired }, { it.name }))
        }.getOrDefault(emptyList())
    }

    /** Speaks [text], choosing the Hindi voice when the text has Devanagari letters. */
    fun speak(text: String, englishVoice: String?, hindiVoice: String?): Boolean {
        val clean = cleanForSpeech(text)
        if (clean.isBlank()) return false
        if (!ready) {
            pending = { speak(text, englishVoice, hindiVoice) }
            return true
        }
        val isHindi = clean.any { it in 'ऀ'..'ॿ' }
        val tag = if (isHindi) "hi-IN" else "en-IN"
        val preferred = if (isHindi) hindiVoice else englishVoice
        return speakWith(tag, preferred, clean)
    }

    /** Plays a sample in a specific voice, used by Settings. */
    fun preview(tag: String, voiceName: String, sample: String) {
        if (ready) speakWith(tag, voiceName, sample)
    }

    private fun speakWith(tag: String, voiceName: String?, text: String): Boolean {
        val available = voices(tag)
        val voice = available.firstOrNull { it.name == voiceName } ?: available.firstOrNull()
        if (voice != null) tts.voice = voice else tts.language = Locale.forLanguageTag(tag)
        tts.setPitch(0.92f)
        tts.setSpeechRate(1.0f)
        val limited = text.take(TextToSpeech.getMaxSpeechInputLength() - 1)
        val result = tts.speak(limited, TextToSpeech.QUEUE_FLUSH, null, "saarthi-${counter++}")
        return result == TextToSpeech.SUCCESS
    }

    fun stop() {
        pending = null
        if (ready) tts.stop()
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }

    private fun cleanForSpeech(text: String): String =
        text.replace(Regex("[*_`#>~|]"), " ")
            .replace(Regex("\\p{So}"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
}
