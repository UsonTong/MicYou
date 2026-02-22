package com.lanrhyme.micyou

import java.io.BufferedReader
import java.io.InputStreamReader

actual fun readResourceFile(path: String): String? {
    return try {
        val classLoader = Thread.currentThread().contextClassLoader
        val fullPath = "composeResources/micyou.composeapp.generated.resources/files/$path"
        val inputStream = classLoader?.getResourceAsStream(fullPath)
        if (inputStream != null) {
            BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                reader.readText()
            }
        } else {
            null
        }
    } catch (e: Exception) {
        Logger.e("Localization", "Failed to read resource file: $path - ${e.message}")
        null
    }
}
