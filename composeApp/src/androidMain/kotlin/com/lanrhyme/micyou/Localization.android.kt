package com.lanrhyme.micyou

import java.io.BufferedReader
import java.io.InputStreamReader

actual fun readResourceFile(path: String): String? {
    return try {
        val context = ContextHelper.getContext() ?: return null
        val assetManager = context.assets
        val fullPath = "composeResources/micyou.composeapp.generated.resources/files/$path"
        BufferedReader(InputStreamReader(assetManager.open(fullPath), "UTF-8")).use { reader ->
            reader.readText()
        }
    } catch (e: Exception) {
        Logger.e("Localization", "Failed to read resource file: $path - ${e.message}")
        null
    }
}
