package com.spendsense.llm

/**
 * Single interface all agents use to call the LLM.
 * Swap implementations in Settings without touching any agent code.
 */
interface LlmProvider {
    suspend fun complete(prompt: String, maxTokens: Int = 200): String
}
