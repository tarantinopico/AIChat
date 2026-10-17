package com.example.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.model.ModelConfig
import com.example.data.local.model.ThinkingLevelConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.dataStore by preferencesDataStore(name = "vertex_ai_settings")

data class AppSettings(
    val serviceAccountJson: String = "",
    val region: String = "us-central1",
    val activeModelId: String = "gemini-2.5-pro",
    val models: List<ModelConfig> = ModelConfig.defaultModels(),
    val thinkingLevels: List<ThinkingLevelConfig> = ThinkingLevelConfig.defaultLevels(),
    val activeThinkingLevelId: String = "HIGH",
    val enableThinkingFeature: Boolean = true,
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
    val appTheme: String = "SLATE" // "SLATE", "OBSIDIAN", "CYBERPUNK", "EMERALD"
) {
    val activeModelConfig: ModelConfig
        get() = models.find { it.id == activeModelId } ?: ModelConfig(
            id = activeModelId,
            displayName = activeModelId,
            supportsThinking = activeModelId.contains("2.5") || activeModelId.contains("thinking"),
            thinkingParamKey = "thinkingLevel"
        )

    val activeThinkingLevel: ThinkingLevelConfig
        get() = thinkingLevels.find { it.id == activeThinkingLevelId } ?: (
            thinkingLevels.find { it.isOff } ?: ThinkingLevelConfig("OFF", "Vypnuto", isOff = true)
        )

    val allAvailableModelIds: List<String>
        get() = models.map { it.id }.distinct()
}

class SettingsDataStore(private val context: Context) {

    private object PreferencesKeys {
        val SERVICE_ACCOUNT_JSON = stringPreferencesKey("service_account_json")
        val REGION = stringPreferencesKey("region")
        val ACTIVE_MODEL_ID = stringPreferencesKey("active_model_id")
        val MODELS_JSON = stringPreferencesKey("models_json_v2")
        val THINKING_LEVELS_JSON = stringPreferencesKey("thinking_levels_json_v2")
        val ACTIVE_THINKING_LEVEL_ID = stringPreferencesKey("active_thinking_level_id")
        val ENABLE_THINKING_FEATURE = booleanPreferencesKey("enable_thinking_feature")
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
    }

    private fun serializeModels(models: List<ModelConfig>): String {
        val arr = JSONArray()
        models.forEach { arr.put(it.toJson()) }
        return arr.toString()
    }

    private fun deserializeModels(jsonStr: String?): List<ModelConfig> {
        if (jsonStr.isNullOrBlank()) return ModelConfig.defaultModels()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<ModelConfig>()
            for (i in 0 until arr.length()) {
                list.add(ModelConfig.fromJson(arr.getJSONObject(i)))
            }
            if (list.isEmpty()) ModelConfig.defaultModels() else list
        } catch (_: Exception) {
            ModelConfig.defaultModels()
        }
    }

    private fun serializeThinkingLevels(levels: List<ThinkingLevelConfig>): String {
        val arr = JSONArray()
        levels.forEach { arr.put(it.toJson()) }
        return arr.toString()
    }

    private fun deserializeThinkingLevels(jsonStr: String?): List<ThinkingLevelConfig> {
        if (jsonStr.isNullOrBlank()) return ThinkingLevelConfig.defaultLevels()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<ThinkingLevelConfig>()
            for (i in 0 until arr.length()) {
                list.add(ThinkingLevelConfig.fromJson(arr.getJSONObject(i)))
            }
            if (list.isEmpty()) ThinkingLevelConfig.defaultLevels() else list
        } catch (_: Exception) {
            ThinkingLevelConfig.defaultLevels()
        }
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            serviceAccountJson = prefs[PreferencesKeys.SERVICE_ACCOUNT_JSON].orEmpty(),
            region = prefs[PreferencesKeys.REGION] ?: "us-central1",
            activeModelId = prefs[PreferencesKeys.ACTIVE_MODEL_ID] ?: "gemini-2.5-pro",
            models = deserializeModels(prefs[PreferencesKeys.MODELS_JSON]),
            thinkingLevels = deserializeThinkingLevels(prefs[PreferencesKeys.THINKING_LEVELS_JSON]),
            activeThinkingLevelId = prefs[PreferencesKeys.ACTIVE_THINKING_LEVEL_ID] ?: "HIGH",
            enableThinkingFeature = prefs[PreferencesKeys.ENABLE_THINKING_FEATURE] ?: true,
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
            appTheme = prefs[PreferencesKeys.APP_THEME] ?: "SLATE"
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

    suspend fun updateActiveThinkingLevelId(levelId: String) {
        context.dataStore.edit { it[PreferencesKeys.ACTIVE_THINKING_LEVEL_ID] = levelId }
    }

    suspend fun updateEnableThinkingFeature(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.ENABLE_THINKING_FEATURE] = enabled }
    }

    // --- Model CRUD Operations ---
    suspend fun saveModel(model: ModelConfig) {
        context.dataStore.edit { prefs ->
            val current = deserializeModels(prefs[PreferencesKeys.MODELS_JSON]).toMutableList()
            val index = current.indexOfFirst { it.id == model.id }
            if (index >= 0) {
                current[index] = model
            } else {
                current.add(model)
            }
            prefs[PreferencesKeys.MODELS_JSON] = serializeModels(current)
        }
    }

    suspend fun deleteModel(modelId: String) {
        context.dataStore.edit { prefs ->
            val current = deserializeModels(prefs[PreferencesKeys.MODELS_JSON])
            val filtered = current.filter { it.id != modelId }
            prefs[PreferencesKeys.MODELS_JSON] = serializeModels(filtered)
            if (prefs[PreferencesKeys.ACTIVE_MODEL_ID] == modelId) {
                prefs[PreferencesKeys.ACTIVE_MODEL_ID] = filtered.firstOrNull()?.id ?: "gemini-2.5-pro"
            }
        }
    }

    suspend fun setModelSupportsThinking(modelId: String, supportsThinking: Boolean) {
        context.dataStore.edit { prefs ->
            val current = deserializeModels(prefs[PreferencesKeys.MODELS_JSON]).toMutableList()
            val index = current.indexOfFirst { it.id == modelId }
            if (index >= 0) {
                current[index] = current[index].copy(supportsThinking = supportsThinking)
                prefs[PreferencesKeys.MODELS_JSON] = serializeModels(current)
            }
        }
    }

    suspend fun syncApiModels(modelIds: List<String>) {
        context.dataStore.edit { prefs ->
            val current = deserializeModels(prefs[PreferencesKeys.MODELS_JSON]).toMutableList()
            val existingIds = current.map { it.id }.toSet()
            for (id in modelIds) {
                if (id !in existingIds) {
                    val is25 = id.contains("2.5") || id.contains("thinking")
                    current.add(
                        ModelConfig(
                            id = id,
                            displayName = id,
                            description = "Importováno z Vertex AI API",
                            supportsThinking = is25,
                            isCustom = false
                        )
                    )
                }
            }
            prefs[PreferencesKeys.MODELS_JSON] = serializeModels(current)
        }
    }

    suspend fun resetModelsToDefault() {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.MODELS_JSON] = serializeModels(ModelConfig.defaultModels())
        }
    }

    suspend fun updateActiveModelThinkingParamKey(paramKey: String, containerKey: String? = null) {
        context.dataStore.edit { prefs ->
            val current = deserializeModels(prefs[PreferencesKeys.MODELS_JSON]).toMutableList()
            val activeId = prefs[PreferencesKeys.ACTIVE_MODEL_ID] ?: "gemini-2.5-pro"
            val index = current.indexOfFirst { it.id == activeId }
            if (index >= 0) {
                val model = current[index]
                current[index] = model.copy(
                    thinkingParamKey = paramKey.trim().ifBlank { "thinkingLevel" },
                    thinkingContainerKey = if (containerKey != null) containerKey.trim() else model.thinkingContainerKey
                )
                prefs[PreferencesKeys.MODELS_JSON] = serializeModels(current)
            }
        }
    }

    suspend fun updateActiveModelThinkingContainerKey(containerKey: String) {
        context.dataStore.edit { prefs ->
            val current = deserializeModels(prefs[PreferencesKeys.MODELS_JSON]).toMutableList()
            val activeId = prefs[PreferencesKeys.ACTIVE_MODEL_ID] ?: "gemini-2.5-pro"
            val index = current.indexOfFirst { it.id == activeId }
            if (index >= 0) {
                val model = current[index]
                current[index] = model.copy(thinkingContainerKey = containerKey.trim())
                prefs[PreferencesKeys.MODELS_JSON] = serializeModels(current)
            }
        }
    }

    // --- Thinking Level CRUD Operations ---
    suspend fun saveThinkingLevel(level: ThinkingLevelConfig) {
        context.dataStore.edit { prefs ->
            val current = deserializeThinkingLevels(prefs[PreferencesKeys.THINKING_LEVELS_JSON]).toMutableList()
            val index = current.indexOfFirst { it.id == level.id }
            if (index >= 0) {
                current[index] = level
            } else {
                current.add(level)
            }
            prefs[PreferencesKeys.THINKING_LEVELS_JSON] = serializeThinkingLevels(current)
        }
    }

    suspend fun deleteThinkingLevel(levelId: String) {
        context.dataStore.edit { prefs ->
            val current = deserializeThinkingLevels(prefs[PreferencesKeys.THINKING_LEVELS_JSON])
            val filtered = current.filter { it.id != levelId }
            prefs[PreferencesKeys.THINKING_LEVELS_JSON] = serializeThinkingLevels(filtered)
            if (prefs[PreferencesKeys.ACTIVE_THINKING_LEVEL_ID] == levelId) {
                prefs[PreferencesKeys.ACTIVE_THINKING_LEVEL_ID] = "OFF"
            }
        }
    }

    suspend fun resetThinkingLevelsToDefault() {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.THINKING_LEVELS_JSON] = serializeThinkingLevels(ThinkingLevelConfig.defaultLevels())
        }
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
}
