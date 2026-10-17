package com.example.data.local.model

import org.json.JSONObject

data class ThinkingLevelConfig(
    val id: String, // e.g. "OFF", "LOW", "MEDIUM", "HIGH", "MAX", or custom ID
    val label: String, // e.g. "Vypnuto (Off)", "Nízká (Low)", "Vysoká (High)", "Ultra"
    val description: String = "",
    val value: String = "", // Raw value sent in payload: e.g. "LOW", "HIGH", "2048"
    val customParamKeyOverride: String = "", // e.g. "premysleni_pro_api", "thinkingLevel", "thinking_level"
    val customContainerKeyOverride: String = "", // e.g. "thinkingConfig", "thinking_config", or "" to use model default
    val isOff: Boolean = false, // When true, thinkingConfig is omitted from request payload
    val isBudgetNumber: Boolean = false // When true, value is parsed as integer (for thinkingBudget)
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("label", label)
            put("description", description)
            put("value", value)
            put("customParamKeyOverride", customParamKeyOverride)
            put("customContainerKeyOverride", customContainerKeyOverride)
            put("isOff", isOff)
            put("isBudgetNumber", isBudgetNumber)
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): ThinkingLevelConfig {
            return ThinkingLevelConfig(
                id = obj.optString("id", "OFF"),
                label = obj.optString("label", "Vypnuto"),
                description = obj.optString("description", ""),
                value = obj.optString("value", ""),
                customParamKeyOverride = obj.optString("customParamKeyOverride", ""),
                customContainerKeyOverride = obj.optString("customContainerKeyOverride", ""),
                isOff = obj.optBoolean("isOff", false),
                isBudgetNumber = obj.optBoolean("isBudgetNumber", false)
            )
        }

        fun defaultLevels(): List<ThinkingLevelConfig> = listOf(
            ThinkingLevelConfig(
                id = "OFF",
                label = "❌ Vypnuto (Off)",
                description = "Odpověď bez uvažování, maximální rychlost",
                value = "",
                customParamKeyOverride = "",
                isOff = true
            ),
            ThinkingLevelConfig(
                id = "LOW",
                label = "⚡ Nízká (Low)",
                description = "Rychlé odlehčené uvažování",
                value = "LOW",
                customParamKeyOverride = "",
                isOff = false
            ),
            ThinkingLevelConfig(
                id = "HIGH",
                label = "🧠 Vysoká (High)",
                description = "Hloubkové uvažování pro komplexní kód",
                value = "HIGH",
                customParamKeyOverride = "",
                isOff = false
            )
        )
    }
}
