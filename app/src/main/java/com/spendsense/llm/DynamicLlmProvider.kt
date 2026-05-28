package com.spendsense.llm

import com.spendsense.data.repository.LlmProviderType
import com.spendsense.data.repository.LlmSettings
import com.spendsense.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the user's LLM choice from DataStore and delegates to the correct provider.
 * This is what gets injected into all agents — they never know which provider is active.
 * Switching the provider in Settings takes effect on the next agent call.
 */
@Singleton
class DynamicLlmProvider @Inject constructor(
    private val settingsRepository: SettingsRepository
) : LlmProvider {

    override suspend fun complete(prompt: String, maxTokens: Int): String {
        val settings = settingsRepository.settingsFlow.first()
        return getDelegate(settings).complete(prompt, maxTokens)
    }

    private fun getDelegate(settings: LlmSettings): LlmProvider = when (settings.providerType) {
        LlmProviderType.CLAUDE -> ClaudeProvider(
            apiKey = settings.claudeApiKey,
            model = settings.claudeModel
        )
        LlmProviderType.OPENAI -> OpenAiProvider(
            apiKey = settings.openAiApiKey,
            baseUrl = settings.openAiBaseUrl,
            model = settings.openAiModel
        )
        LlmProviderType.OLLAMA -> OllamaProvider(
            baseUrl = settings.ollamaBaseUrl,
            model = settings.ollamaModel
        )
        LlmProviderType.RULES_ONLY -> RulesOnlyProvider()
    }
}
