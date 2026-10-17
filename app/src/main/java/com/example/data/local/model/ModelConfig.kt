package com.example.data.local.model

import org.json.JSONObject

data class ModelConfig(
    val id: String, // e.g. "gemini-2.5-pro", "gemini-1.5-flash", "projects/xyz/endpoints/123"
    val displayName: String,
    val description: String = "",
    val supportsThinking: Boolean = false,
    val thinkingContainerKey: String = "thinkingConfig", // e.g. "thinkingConfig", "thinking_config", or "" for root
    val thinkingParamKey: String = "thinkingLevel", // e.g. "thinkingLevel", "thinking_level", "premysleni_pro_api", "thinkingBudget"
    val defaultThinkingLevelId: String = "HIGH",
    val maxOutputTokens: Int = 8192,
    val temperature: Float = 0.7f,
    val topP: Float = 0.95f,
    val topK: Int = 40,
    val isCustom: Boolean = false
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("displayName", displayName)
            put("description", description)
            put("supportsThinking", supportsThinking)
            put("thinkingContainerKey", thinkingContainerKey)
            put("thinkingParamKey", thinkingParamKey)
            put("defaultThinkingLevelId", defaultThinkingLevelId)
            put("maxOutputTokens", maxOutputTokens)
            put("temperature", temperature.toDouble())
            put("topP", topP.toDouble())
            put("topK", topK)
            put("isCustom", isCustom)
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): ModelConfig {
            return ModelConfig(
                id = obj.optString("id", "gemini-2.5-pro"),
                displayName = obj.optString("displayName", obj.optString("id")),
                description = obj.optString("description", ""),
                supportsThinking = obj.optBoolean("supportsThinking", false),
                thinkingContainerKey = obj.optString("thinkingContainerKey", "thinkingConfig"),
                thinkingParamKey = obj.optString("thinkingParamKey", "thinkingLevel"),
                defaultThinkingLevelId = obj.optString("defaultThinkingLevelId", "HIGH"),
                maxOutputTokens = obj.optInt("maxOutputTokens", 8192),
                temperature = obj.optDouble("temperature", 0.7).toFloat(),
                topP = obj.optDouble("topP", 0.95).toFloat(),
                topK = obj.optInt("topK", 40),
                isCustom = obj.optBoolean("isCustom", false)
            )
        }

        fun defaultModels(): List<ModelConfig> = listOf(
            ModelConfig(
                id = "gemini-2.5-pro",
                displayName = "Gemini 2.5 Pro",
                description = "Špičkový model s uvažováním (thinkingLevel)",
                supportsThinking = true,
                thinkingContainerKey = "thinkingConfig",
                thinkingParamKey = "thinkingLevel",
                defaultThinkingLevelId = "HIGH",
                maxOutputTokens = 8192,
                isCustom = false
            ),
            ModelConfig(
                id = "gemini-2.5-flash",
                displayName = "Gemini 2.5 Flash",
                description = "Bleskový model s volitelným uvažováním",
                supportsThinking = true,
                thinkingContainerKey = "thinkingConfig",
                thinkingParamKey = "thinkingLevel",
                defaultThinkingLevelId = "LOW",
                maxOutputTokens = 8192,
                isCustom = false
            ),
            ModelConfig(
                id = "gemini-2.0-flash",
                displayName = "Gemini 2.0 Flash",
                description = "Ultra rychlý obecný model bez uvažování",
                supportsThinking = false,
                thinkingContainerKey = "thinkingConfig",
                thinkingParamKey = "thinkingLevel",
                defaultThinkingLevelId = "OFF",
                maxOutputTokens = 8192,
                isCustom = false
            ),
            ModelConfig(
                id = "gemini-1.5-pro",
                displayName = "Gemini 1.5 Pro",
                description = "Výkonný model pro dlouhý kontext bez uvažování",
                supportsThinking = false,
                thinkingContainerKey = "thinkingConfig",
                thinkingParamKey = "thinkingLevel",
                defaultThinkingLevelId = "OFF",
                maxOutputTokens = 8192,
                isCustom = false
            ),
            ModelConfig(
                id = "gemini-1.5-flash",
                displayName = "Gemini 1.5 Flash",
                description = "Rychlý úsporný model bez uvažování",
                supportsThinking = false,
                thinkingContainerKey = "thinkingConfig",
                thinkingParamKey = "thinkingLevel",
                defaultThinkingLevelId = "OFF",
                maxOutputTokens = 8192,
                isCustom = false
            )
        )
    }
}
