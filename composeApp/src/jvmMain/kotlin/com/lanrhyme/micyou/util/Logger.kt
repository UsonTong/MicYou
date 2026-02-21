package com.lanrhyme.micyou.util

import com.lanrhyme.micyou.LogLevel
import com.lanrhyme.micyou.LoggerImpl
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

object JvmLogger : LoggerImpl {
    private var logFile: File? = null
    private var logWriter: PrintWriter? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    private val logDir: File by lazy {
        val userHome = System.getProperty("user.home")
        File(userHome, ".micyou").apply {
            if (!exists()) mkdirs()
        }
    }
    
    fun init() {
        try {
            logFile = File(logDir, "micyou.log")
            logWriter = PrintWriter(FileWriter(logFile, true), true)
        } catch (e: Exception) {
            System.err.println("Failed to initialize logger: ${e.message}")
        }
    }
    
    private fun formatMessage(level: LogLevel, tag: String, message: String): String {
        val timestamp = dateFormat.format(Date())
        return "[$timestamp] ${level.name}/$tag: $message"
    }
    
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val formattedMessage = formatMessage(level, tag, message)
        
        println(formattedMessage)
        
        try {
            logWriter?.println(formattedMessage)
            throwable?.let { 
                it.printStackTrace(System.out)
                it.printStackTrace(logWriter)
            }
            logWriter?.flush()
        } catch (e: Exception) {
        }
    }
    
    override fun getLogFilePath(): String? {
        return logFile?.absolutePath
    }
    
    fun release() {
        try {
            logWriter?.flush()
            logWriter?.close()
            logWriter = null
        } catch (e: Exception) {
        }
    }
}
