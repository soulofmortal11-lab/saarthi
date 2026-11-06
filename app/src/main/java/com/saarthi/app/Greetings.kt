package com.saarthi.app

/** Lines that appear on screen, mixing Hindi and English at random in both modes. */
object Greetings {
    private val day = listOf(
        "Pranam. How may I help you today?",
        "नमस्ते। आज मैं आपकी क्या सहायता करूँ?",
        "Ready when you are. Just tap the chakra.",
        "सुप्रभात का समय हो या दोपहर, मैं हाज़िर हूँ।",
        "At your service, Sir.",
        "बताइए, आज क्या करना है?",
    )

    private val night = listOf(
        "The stars are out. How can I help?",
        "शुभ रात्रि। बताइए, क्या सेवा करूँ?",
        "Still up? I'm here if you need me.",
        "रात हो गई है, पर मैं जाग रहा हूँ।",
        "Evening, Sir. What shall we do?",
        "तारों के नीचे, आपकी सेवा में।",
    )

    private val toNight = listOf(
        "Switching to the night sky.",
        "रात का आसमान, आपके लिए।",
    )

    private val toDay = listOf(
        "Back to the manuscript.",
        "दिन का उजाला, हाज़िर है।",
    )

    fun random(isNight: Boolean): String = (if (isNight) night else day).random()

    fun modeSwitch(isNight: Boolean): String = (if (isNight) toNight else toDay).random()

    const val NOT_CONNECTED =
        "I'm not connected to my brain yet. Open Settings and add the server details."

    /** Returns true for night, false for day, or null when [heard] isn't a mode command. */
    fun modeCommand(heard: String): Boolean? {
        val text = heard.lowercase()
        val night = listOf("night mode", "dark mode", "नाइट मोड", "रात मोड")
        val day = listOf("day mode", "light mode", "डे मोड", "दिन मोड")
        return when {
            night.any { text.contains(it) } -> true
            day.any { text.contains(it) } -> false
            else -> null
        }
    }
}
