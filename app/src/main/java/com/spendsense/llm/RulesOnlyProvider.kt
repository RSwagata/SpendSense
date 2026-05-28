package com.spendsense.llm

import javax.inject.Inject

/**
 * Default provider — no API key, no network.
 * Returns empty string; agents fall back to "Unknown" or rule-based results.
 * This means the app is fully functional without any LLM setup.
 */
class RulesOnlyProvider @Inject constructor() : LlmProvider {
    override suspend fun complete(prompt: String, maxTokens: Int): String = ""
}
