package com.example.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "vertex_ai_settings")

data class AppSettings(
    val serviceAccountJson: String = "",
    val region: String = "us-central1",
    val activeModelId: String = "gemini-2.5-pro",
    val customModels: Set<String> = setOf("gemini-2.5-pro", "gemini-2.5-flash", "gemini-4-argon"),
    val apiModels: Set<String> = emptySet(),
    val systemInstructions: String = "Jsi špičkový softwarový inženýr a programátor. Piš kompletní, čistý a funkční kód.",
    val temperature: Float = 0.7f,
    val topP: Float = 0.95f,
    val topK: Int = 40,
    val maxOutputTokens: Int = 8192,
    val contextLimit: Int = 20,
    val isStreaming: Boolean = true,
    val isBlockNoneSafety: Boolean = true,
    val autoContinueMode: String = "AUTO", // "AUTO" or "MANUAL"
    val costPerMillionInputTokens: Double = 1.25,
    val costPerMillionOutputTokens: Double = 5.00,
    val initialCreditUsd: Double = 300.0,
    val appTheme: String = "SLATE", // "SLATE", "OBSIDIAN", "CYBERPUNK", "EMERALD"
    val thinkingLevel: String = "HIGH" // "OFF", "LOW", "HIGH"
) {
    val allAvailableModels: List<String>
        get() = (customModels + apiModels).toList().distinct().sorted()
}

class SettingsDataStore(private val context: Context) {

    private object PreferencesKeys {
        val SERVICE_ACCOUNT_JSON = stringPreferencesKey("service_account_json")
        val REGION = stringPreferencesKey("region")
        val ACTIVE_MODEL_ID = stringPreferencesKey("active_model_id")
        val CUSTOM_MODELS = stringSetPreferencesKey("custom_models")
        val API_MODELS = stringSetPreferencesKey("api_models")
        val SYSTEM_INSTRUCTIONS = stringPreferencesKey("system_instructions")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val TOP_P = floatPreferencesKey("top_p")
        val TOP_K = intPreferencesKey("top_k")
        val MAX_OUTPUT_TOKENS = intPreferencesKey("max_output_tokens")
        val CONTEXT_LIMIT = intPreferencesKey("context_limit")
        val IS_STREAMING = booleanPreferencesKey("is_streaming")
        val IS_BLOCK_NONE_SAFETY = booleanPreferencesKey("is_block_none_safety")
        val AUTO_CONTINUE_MODE = stringPreferencesKey("auto_continue_mode")
        val COST_PER_MILLION_INPUT = doublePreferencesKey("cost_per_million_input")
        val COST_PER_MILLION_OUTPUT = doublePreferencesKey("cost_per_million_output")
        val INITIAL_CREDIT_USD = doublePreferencesKey("initial_credit_usd")
        val APP_THEME = stringPreferencesKey("app_theme")
        val THINKING_LEVEL = stringPreferencesKey("thinking_level")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            serviceAccountJson = prefs[PreferencesKeys.SERVICE_ACCOUNT_JSON].orEmpty(),
            region = prefs[PreferencesKeys.REGION] ?: "us-central1",
            activeModelId = prefs[PreferencesKeys.ACTIVE_MODEL_ID] ?: "gemini-2.5-pro",
            customModels = prefs[PreferencesKeys.CUSTOM_MODELS] ?: setOf("gemini-2.5-pro", "gemini-2.5-flash", "gemini-4-argon"),
            apiModels = prefs[PreferencesKeys.API_MODELS] ?: emptySet(),
            systemInstructions = prefs[PreferencesKeys.SYSTEM_INSTRUCTIONS] ?: "Jsi špičkový softwarový inženýr a programátor. Piš kompletní, čistý a funkční kód.",
            temperature = prefs[PreferencesKeys.TEMPERATURE] ?: 0.7f,
            topP = prefs[PreferencesKeys.TOP_P] ?: 0.95f,
            topK = prefs[PreferencesKeys.TOP_K] ?: 40,
            maxOutputTokens = prefs[PreferencesKeys.MAX_OUTPUT_TOKENS] ?: 8192,
            contextLimit = prefs[PreferencesKeys.CONTEXT_LIMIT] ?: 20,
            isStreaming = prefs[PreferencesKeys.IS_STREAMING] ?: true,
            isBlockNoneSafety = prefs[PreferencesKeys.IS_BLOCK_NONE_SAFETY] ?: true,
            autoContinueMode = prefs[PreferencesKeys.AUTO_CONTINUE_MODE] ?: "AUTO",
            costPerMillionInputTokens = prefs[PreferencesKeys.COST_PER_MILLION_INPUT] ?: 1.25,
            costPerMillionOutputTokens = prefs[PreferencesKeys.COST_PER_MILLION_OUTPUT] ?: 5.00,
            initialCreditUsd = prefs[PreferencesKeys.INITIAL_CREDIT_USD] ?: 300.0,
            appTheme = prefs[PreferencesKeys.APP_THEME] ?: "SLATE",
            thinkingLevel = prefs[PreferencesKeys.THINKING_LEVEL] ?: "HIGH"
        )
    }

    suspend fun updateServiceAccountJson(json: String) {
        context.dataStore.edit { it[PreferencesKeys.SERVICE_ACCOUNT_JSON] = json }
    }

    suspend fun updateRegion(region: String) {
        context.dataStore.edit { it[PreferencesKeys.REGION] = region }
    }

    suspend fun updateActiveModelId(modelId: String) {
        context.dataStore.edit { it[PreferencesKeys.ACTIVE_MODEL_ID] = modelId }
    }

    suspend fun addCustomModel(modelId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.CUSTOM_MODELS] ?: setOf("gemini-2.5-pro", "gemini-2.5-flash", "gemini-4-argon")
            prefs[PreferencesKeys.CUSTOM_MODELS] = current + modelId.trim()
        }
    }

    suspend fun removeCustomModel(modelId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.CUSTOM_MODELS] ?: emptySet()
            prefs[PreferencesKeys.CUSTOM_MODELS] = current - modelId
        }
    }

    suspend fun setApiModels(models: Set<String>) {
        context.dataStore.edit { it[PreferencesKeys.API_MODELS] = models }
    }

    suspend fun updateSystemInstructions(instructions: String) {
        context.dataStore.edit { it[PreferencesKeys.SYSTEM_INSTRUCTIONS] = instructions }
    }

    suspend fun updateParameters(
        temperature: Float,
        topP: Float,
        topK: Int,
        maxOutputTokens: Int,
        contextLimit: Int
    ) {
        context.dataStore.edit {
            it[PreferencesKeys.TEMPERATURE] = temperature
            it[PreferencesKeys.TOP_P] = topP
            it[PreferencesKeys.TOP_K] = topK
            it[PreferencesKeys.MAX_OUTPUT_TOKENS] = maxOutputTokens
            it[PreferencesKeys.CONTEXT_LIMIT] = contextLimit
        }
    }

    suspend fun updateToggles(
        isStreaming: Boolean,
        isBlockNoneSafety: Boolean,
        autoContinueMode: String
    ) {
        context.dataStore.edit {
            it[PreferencesKeys.IS_STREAMING] = isStreaming
            it[PreferencesKeys.IS_BLOCK_NONE_SAFETY] = isBlockNoneSafety
            it[PreferencesKeys.AUTO_CONTINUE_MODE] = autoContinueMode
        }
    }

    suspend fun updatePricing(
        costPerMillionInput: Double,
        costPerMillionOutput: Double,
        initialCreditUsd: Double
    ) {
        context.dataStore.edit {
            it[PreferencesKeys.COST_PER_MILLION_INPUT] = costPerMillionInput
            it[PreferencesKeys.COST_PER_MILLION_OUTPUT] = costPerMillionOutput
            it[PreferencesKeys.INITIAL_CREDIT_USD] = initialCreditUsd
        }
    }

    suspend fun updateAppTheme(theme: String) {
        context.dataStore.edit { it[PreferencesKeys.APP_THEME] = theme }
    }

    suspend fun updateThinkingLevel(level: String) {
        context.dataStore.edit { it[PreferencesKeys.THINKING_LEVEL] = level }
    }
}
