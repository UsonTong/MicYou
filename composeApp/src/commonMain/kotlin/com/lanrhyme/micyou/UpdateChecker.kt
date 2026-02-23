package com.lanrhyme.micyou

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("body") val body: String
)

class UpdateChecker {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    suspend fun checkUpdate(): Result<GitHubRelease?> {
        return try {
            val currentVersion = getAppVersion()
            if (currentVersion == "dev") return Result.success(null)

            val response = client.get("https://api.github.com/repos/LanRhyme/MicYou/releases/latest") {
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                header(HttpHeaders.Accept, "application/vnd.github+json")
            }
            
            if (!response.status.isSuccess()) {
                val errorMsg = "HTTP Error: ${response.status.value}"
                Logger.e("UpdateChecker", errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val latestRelease: GitHubRelease = response.body()
            val latestVersion = latestRelease.tagName.removePrefix("v")
            if (isNewerVersion(currentVersion, latestVersion)) {
                Result.success(latestRelease)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Logger.e("UpdateChecker", "Failed to check for updates", e)
            Result.failure(e)
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }

        val size = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until size) {
            val curr = currentParts.getOrNull(i) ?: 0
            val late = latestParts.getOrNull(i) ?: 0
            if (late > curr) return true
            if (late < curr) return false
        }
        return false
    }
}
