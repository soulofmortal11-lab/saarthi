package com.saarthi.app

import android.content.Context

/** Settings saved on the phone. The server details never go into the code. */
class Prefs(context: Context) {
    private val store = context.getSharedPreferences("saarthi", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = store.getString("server_url", "") ?: ""
        set(value) = store.edit().putString("server_url", value.trim().trimEnd('/')).apply()

    var accessCode: String
        get() = store.getString("access_code", "") ?: ""
        set(value) = store.edit().putString("access_code", value.trim()).apply()

    var isNight: Boolean
        get() = store.getBoolean("is_night", false)
        set(value) = store.edit().putBoolean("is_night", value).apply()

    /** "en-IN" or "hi-IN": the language the speech recogniser listens for. */
    var listenLanguage: String
        get() = store.getString("listen_language", "en-IN") ?: "en-IN"
        set(value) = store.edit().putString("listen_language", value).apply()

    var englishVoice: String?
        get() = store.getString("english_voice", null)
        set(value) = store.edit().putString("english_voice", value).apply()

    var hindiVoice: String?
        get() = store.getString("hindi_voice", null)
        set(value) = store.edit().putString("hindi_voice", value).apply()

    val isConfigured: Boolean
        get() = serverUrl.isNotBlank() && accessCode.isNotBlank()
}
