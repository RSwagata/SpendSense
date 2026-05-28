package com.spendsense.llm

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Calls Ollama running on a local network machine.
 * Default URL assumes the user's PC is on the same WiFi.
 * The user configures the IP in Settings.
 */
class OllamaProvider(
    private val baseUrl: String = "http://192.168.1.100:11434",
    private val model: String = "llama3"
) : LlmProvider {

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun complete(prompt: String, maxTokens: Int): String {
        val body = """
            {
              "model": "$model",
              "prompt": ${Json.encodeToString(JsonPrimitive(prompt))},
              "stream": false,
              "options": {"num_predict": $maxTokens}
            }
        """.trimIndent()

        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/api/generate")
            .addHeader("content-type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return ""
            val parsed = json.decodeFromString<OllamaResponse>(responseBody)
            parsed.response.trim()
        } catch (e: Exception) {
            ""
        }
    }

    @Serializable
    private data class OllamaResponse(val response: String)
}
