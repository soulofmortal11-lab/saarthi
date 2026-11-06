package com.saarthi.app

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.util.Date

class SaarthiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            CrashLog.save(this, error)
            previous?.uncaughtException(thread, error)
        }
    }
}

/** Saves the last crash so it can be read and copied from Settings. */
object CrashLog {
    private const val FILE_NAME = "last_crash.txt"

    fun save(context: Context, error: Throwable) {
        runCatching {
            val report = buildString {
                appendLine("Time: ${Date()}")
                appendLine("Version: ${BuildConfig.VERSION_NAME}")
                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}")
                appendLine()
                append(Log.getStackTraceString(error))
            }
            File(context.filesDir, FILE_NAME).writeText(report)
        }
    }

    fun read(context: Context): String? =
        runCatching { File(context.filesDir, FILE_NAME).takeIf { it.exists() }?.readText() }.getOrNull()

    fun clear(context: Context) {
        runCatching { File(context.filesDir, FILE_NAME).delete() }
    }
}
