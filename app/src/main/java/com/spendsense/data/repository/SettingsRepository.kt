package com.spendsense.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class LlmProviderType { RULES_ONLY, CLAUDE, OPENAI, OLLAMA }

data class LlmSettings(
    val providerType: LlmProviderType = LlmProviderType.RULES_ONLY,
    val claudeApiKey: String = "",
    val claudeModel: String = "claude-haiku-4-5-20251001",
    val openAiApiKey: String = "",
    val openAiBaseUrl: String = "https://api.openai.com",
    val openAiModel: String = "gpt-4o-mini",
    val ollamaBaseUrl: String = "http://192.168.1.100:11434",
    val ollamaModel: String = "llama3"
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val PROVIDER_TYPE = stringPreferencesKey("provider_type")
        val CLAUDE_API_KEY = stringPreferencesKey("claude_api_key")
        val CLAUDE_MODEL = stringPreferencesKey("claude_model")
        val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        val OPENAI_BASE_URL = stringPreferencesKey("openai_base_url")
        val OPENAI_MODEL = stringPreferencesKey("openai_model")
        val OLLAMA_BASE_URL = stringPreferencesKey("ollama_base_url")
        val OLLAMA_MODEL = stringPreferencesKey("ollama_model")
    }

    val settingsFlow: Flow<LlmSettings> = context.dataStore.data.map { prefs ->
        LlmSettings(
            providerType = prefs[Keys.PROVIDER_TYPE]
                ?.let { LlmProviderType.valueOf(it) } ?: LlmProviderType.RULES_ONLY,
            claudeApiKey = prefs[Keys.CLAUDE_API_KEY] ?: "",
            claudeModel = prefs[Keys.CLAUDE_MODEL] ?: "claude-haiku-4-5-20251001",
            openAiApiKey = prefs[Keys.OPENAI_API_KEY] ?: "",
            openAiBaseUrl = prefs[Keys.OPENAI_BASE_URL] ?: "https://api.openai.com",
            openAiModel = prefs[Keys.OPENAI_MODEL] ?: "gpt-4o-mini",
            ollamaBaseUrl = prefs[Keys.OLLAMA_BASE_URL] ?: "http://192.168.1.100:11434",
            ollamaModel = prefs[Keys.OLLAMA_MODEL] ?: "llama3"
        )
    }

    suspend fun save(settings: LlmSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PROVIDER_TYPE] = settings.providerType.name
            prefs[Keys.CLAUDE_API_KEY] = settings.claudeApiKey
            prefs[Keys.CLAUDE_MODEL] = settings.claudeModel
            prefs[Keys.OPENAI_API_KEY] = settings.openAiApiKey
            prefs[Keys.OPENAI_BASE_URL] = settings.openAiBaseUrl
            prefs[Keys.OPENAI_MODEL] = settings.openAiModel
            prefs[Keys.OLLAMA_BASE_URL] = settings.ollamaBaseUrl
            prefs[Keys.OLLAMA_MODEL] = settings.ollamaModel
        }
    }
}
