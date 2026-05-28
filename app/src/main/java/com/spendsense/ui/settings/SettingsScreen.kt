package com.spendsense.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spendsense.data.repository.LlmProviderType
import com.spendsense.data.repository.LlmSettings

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    var draft by remember(settings) { mutableStateOf(settings) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("LLM Provider", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // Provider selector
        ProviderSelector(
            selected = draft.providerType,
            onSelect = { draft = draft.copy(providerType = it) }
        )

        // Provider-specific fields
        when (draft.providerType) {
            LlmProviderType.CLAUDE -> ClaudeFields(draft) { draft = it }
            LlmProviderType.OPENAI -> OpenAiFields(draft) { draft = it }
            LlmProviderType.OLLAMA -> OllamaFields(draft) { draft = it }
            LlmProviderType.RULES_ONLY -> RulesOnlyInfo()
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                viewModel.save(draft)
                saved = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Settings")
        }

        if (saved) {
            Text(
                "Settings saved. New provider active on next LLM call.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(24.dp))
        Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("SpendSense", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("Version 1.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("All data stays on your device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("No account numbers are ever sent to LLM providers.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ProviderSelector(selected: LlmProviderType, onSelect: (LlmProviderType) -> Unit) {
    val options = listOf(
        LlmProviderType.RULES_ONLY to "Rules Only (no API key)",
        LlmProviderType.CLAUDE to "Claude (Anthropic API)",
        LlmProviderType.OPENAI to "OpenAI / compatible (Groq, Together, etc.)",
        LlmProviderType.OLLAMA to "Ollama (local network)"
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(8.dp)) {
            options.forEach { (type, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    RadioButton(selected = selected == type, onClick = { onSelect(type) })
                    Column(Modifier.padding(start = 4.dp)) {
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                        if (type == LlmProviderType.RULES_ONLY) {
                            Text("Works fully offline. Merchants may show as 'Unknown'.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClaudeFields(draft: LlmSettings, onChange: (LlmSettings) -> Unit) {
    var showKey by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = draft.claudeApiKey,
            onValueChange = { onChange(draft.copy(claudeApiKey = it)) },
            label = { Text("Anthropic API Key") },
            placeholder = { Text("sk-ant-...") },
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { showKey = !showKey }) {
                    Text(if (showKey) "Hide" else "Show", style = MaterialTheme.typography.labelSmall)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        OutlinedTextField(
            value = draft.claudeModel,
            onValueChange = { onChange(draft.copy(claudeModel = it)) },
            label = { Text("Model") },
            placeholder = { Text("claude-haiku-4-5-20251001") },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Recommended: claude-haiku-4-5-20251001 for parsing (cheap), claude-sonnet-4-6 for weekly insights (better quality).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OpenAiFields(draft: LlmSettings, onChange: (LlmSettings) -> Unit) {
    var showKey by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = draft.openAiApiKey,
            onValueChange = { onChange(draft.copy(openAiApiKey = it)) },
            label = { Text("API Key") },
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { showKey = !showKey }) {
                    Text(if (showKey) "Hide" else "Show", style = MaterialTheme.typography.labelSmall)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        OutlinedTextField(
            value = draft.openAiBaseUrl,
            onValueChange = { onChange(draft.copy(openAiBaseUrl = it)) },
            label = { Text("Base URL") },
            placeholder = { Text("https://api.openai.com") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = draft.openAiModel,
            onValueChange = { onChange(draft.copy(openAiModel = it)) },
            label = { Text("Model") },
            placeholder = { Text("gpt-4o-mini") },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Compatible with Groq (api.groq.com/openai), Together AI, and any OpenAI-format endpoint.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OllamaFields(draft: LlmSettings, onChange: (LlmSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = draft.ollamaBaseUrl,
            onValueChange = { onChange(draft.copy(ollamaBaseUrl = it)) },
            label = { Text("Ollama URL") },
            placeholder = { Text("http://192.168.1.100:11434") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = draft.ollamaModel,
            onValueChange = { onChange(draft.copy(ollamaModel = it)) },
            label = { Text("Model") },
            placeholder = { Text("llama3") },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Run Ollama on a PC on the same WiFi network. Find your PC's IP and enter it above.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RulesOnlyInfo() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Rules-Only Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                "• All transaction parsing is done locally with regex patterns",
                style = MaterialTheme.typography.bodySmall
            )
            Text("• Known UPI merchants are resolved from a bundled lookup table", style = MaterialTheme.typography.bodySmall)
            Text("• Unknown merchants show as 'Unknown'", style = MaterialTheme.typography.bodySmall)
            Text("• Weekly insights are skipped", style = MaterialTheme.typography.bodySmall)
            Text("• No API keys required. Fully offline.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
