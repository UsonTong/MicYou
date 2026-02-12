package com.lanrhyme.micyou

import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JvmLogger : LoggerImpl {
    private val logFile: File by lazy {
        val userHome = System.getProperty("user.home")
        val appDir = File(userHome, ".micyou").apply { if (!exists()) mkdirs() }
        File(appDir, "micyou.log")
    }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "$timestamp [$level][$tag] $message${throwable?.let { "\n${getStackTraceString(it)}" } ?: ""}\n"
        
        // Console
        print(logEntry)

        // File
        try {
            FileOutputStream(logFile, true).use {
                it.write(logEntry.toByteArray())
            }
        } catch (e: Exception) {
            System.err.println("Failed to write log to file: ${e.message}")
        }
    }

    private fun getStackTraceString(t: Throwable): String {
        val sw = java.io.StringWriter()
        val pw = java.io.PrintWriter(sw)
        t.printStackTrace(pw)
        pw.flush()
        return sw.toString()
    }

    override fun getLogFilePath(): String? {
        return logFile.absolutePath
    }
}
