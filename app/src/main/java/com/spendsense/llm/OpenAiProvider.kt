package com.spendsense.llm

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * OpenAI-compatible provider. Works with:
 * - OpenAI (api.openai.com)
 * - Groq (api.groq.com/openai)
 * - Together AI (api.together.xyz)
 * - Any other OpenAI-compatible endpoint
 */
class OpenAiProvider(
    private val apiKey: String,
    private val baseUrl: String = "https://api.openai.com",
    private val model: String = "gpt-4o-mini"
) : LlmProvider {

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun complete(prompt: String, maxTokens: Int): String {
        val body = """
            {
              "model": "$model",
              "max_tokens": $maxTokens,
              "messages": [{"role": "user", "content": ${Json.encodeToString(JsonPrimitive(prompt))}}]
            }
        """.trimIndent()

        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("content-type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        return withContext(Dispatchers.IO) {
        try {
            Log.i("OpenAiProvider", "calling baseUrl=$baseUrl model=$model")
            val response = client.newCall(request).execute()
            val responseBody = response.body.string() ?: return@withContext ""
            Log.i("OpenAiProvider", "response code=${response.code} body=$responseBody")
            val parsed = json.decodeFromString<OpenAiResponse>(responseBody)
            parsed.choices.firstOrNull()?.message?.content?.trim() ?: ""
        } catch (e: Exception) {
            Log.e("OpenAiProvider", "request failed", e)
            ""
        }
        }
    }

    @Serializable
    private data class OpenAiResponse(val choices: List<Choice>)

    @Serializable
    private data class Choice(val message: Message)

    @Serializable
    private data class Message(val content: String)
}
