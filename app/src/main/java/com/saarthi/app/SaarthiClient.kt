package com.saarthi.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Turn(val role: String, val text: String)

class SetupNeededException : Exception("Saarthi isn't connected to its brain yet.")

class SaarthiException(message: String) : Exception(message)

/** Talks to Saarthi's cloud brain (the Cloudflare worker). */
class SaarthiClient(private val prefs: Prefs) {

    suspend fun ask(history: List<Turn>, isNight: Boolean): String = withContext(Dispatchers.IO) {
        if (!prefs.isConfigured) throw SetupNeededException()

        val messages = JSONArray()
        history.forEach { turn ->
            messages.put(JSONObject().put("role", turn.role).put("content", turn.text))
        }
        val body = JSONObject()
            .put("mode", if (isNight) "night" else "day")
            .put("localTime", SimpleDateFormat("EEEE, d MMMM yyyy, h:mm a", Locale.ENGLISH).format(Date()))
            .put("messages", messages)

        val json = request("POST", "/chat", prefs.serverUrl, prefs.accessCode, body.toString())
        json.optString("reply").ifBlank { throw SaarthiException("The brain sent back an empty reply.") }
    }

    /** Checks the given server details. Returns a short status line. */
    suspend fun test(serverUrl: String, accessCode: String): String = withContext(Dispatchers.IO) {
        val json = request("GET", "/health", serverUrl.trim().trimEnd('/'), accessCode.trim(), null)
        "Connected. Brain: ${json.optString("model", "unknown")}"
    }

    private fun request(method: String, path: String, base: String, code: String, body: String?): JSONObject {
        if (!base.startsWith("https://")) {
            throw SaarthiException("The server address should start with https://")
        }
        val connection = try {
            URL(base + path).openConnection() as HttpURLConnection
        } catch (e: Exception) {
            throw SaarthiException("That server address doesn't look right.")
        }
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 45_000
            connection.setRequestProperty("X-Saarthi-Code", code)
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(text) }.getOrNull()

            if (status !in 200..299) {
                val message = json?.optString("error")?.takeIf { it.isNotBlank() }
                throw SaarthiException(message ?: "The brain returned an error ($status).")
            }
            return json ?: throw SaarthiException("The brain's reply couldn't be read.")
        } catch (e: SaarthiException) {
            throw e
        } catch (e: IOException) {
            throw SaarthiException("Couldn't reach the brain. Check your internet connection.")
        } finally {
            connection.disconnect()
        }
    }
}
