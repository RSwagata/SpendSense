package com.spendsense.llm

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Calls the Anthropic Messages API (Claude).
 * Uses claude-haiku-4-5 for parser/classifier (fast, cheap) and
 * the user can upgrade to Sonnet for the Insight Agent via Settings.
 */
class ClaudeProvider(
    private val apiKey: String,
    private val model: String = "claude-haiku-4-5-20251001"
) : LlmProvider {

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun complete(prompt: String, maxTokens: Int): String {
        val body = """
            {
              "model": "$model",
              "max_tokens": $maxTokens,
              "messages": [{"role": "user", "content": ${kotlinx.serialization.json.Json.encodeToString(kotlinx.serialization.json.JsonPrimitive(prompt))}]}
            }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return ""
            val parsed = json.decodeFromString<ClaudeResponse>(responseBody)
            parsed.content.firstOrNull()?.text?.trim() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    @Serializable
    private data class ClaudeResponse(val content: List<ContentBlock>)

    @Serializable
    private data class ContentBlock(val type: String, val text: String = "")
}
