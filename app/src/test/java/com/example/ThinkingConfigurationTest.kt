package com.example

import com.example.data.local.model.ModelConfig
import com.example.data.local.model.ThinkingLevelConfig
import com.example.data.remote.dto.VertexGenerationConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ThinkingConfigurationTest {

    @Test
    fun testCustomArgumentKeyPayloadSerialization() {
        // Test custom argument key like 'premysleni_pro_api' with container 'thinkingConfig'
        val config = VertexGenerationConfig(
            temperature = 0.5f,
            maxOutputTokens = 4096,
            supportsThinking = true,
            thinkingContainerKey = "thinkingConfig",
            thinkingParamKey = "premysleni_pro_api",
            thinkingValue = "HIGH",
            isThinkingOff = false,
            isThinkingBudgetNumber = false
        )

        val json = config.toJson()
        assertTrue(json.has("thinkingConfig"))
        val containerObj = json.getJSONObject("thinkingConfig")
        assertTrue(containerObj.has("premysleni_pro_api"))
        assertEquals("HIGH", containerObj.getString("premysleni_pro_api"))
    }

    @Test
    fun testCustomArgumentKeyWithoutContainer() {
        // Test custom argument placed directly in root of generationConfig (when container key is empty)
        val config = VertexGenerationConfig(
            supportsThinking = true,
            thinkingContainerKey = "",
            thinkingParamKey = "premysleni_pro_api",
            thinkingValue = "ULTRA",
            isThinkingOff = false
        )

        val json = config.toJson()
        assertFalse(json.has("thinkingConfig"))
        assertTrue(json.has("premysleni_pro_api"))
        assertEquals("ULTRA", json.getString("premysleni_pro_api"))
    }

    @Test
    fun testThinkingOffDoesNotSerialize() {
        val config = VertexGenerationConfig(
            supportsThinking = true,
            thinkingContainerKey = "thinkingConfig",
            thinkingParamKey = "thinkingLevel",
            thinkingValue = "HIGH",
            isThinkingOff = true
        )

        val json = config.toJson()
        assertFalse(json.has("thinkingConfig"))
        assertFalse(json.has("thinkingLevel"))
    }

    @Test
    fun testIntegerBudgetSerialization() {
        val config = VertexGenerationConfig(
            supportsThinking = true,
            thinkingContainerKey = "thinkingConfig",
            thinkingParamKey = "thinkingBudget",
            thinkingValue = "4096",
            isThinkingOff = false,
            isThinkingBudgetNumber = true
        )

        val json = config.toJson()
        assertTrue(json.has("thinkingConfig"))
        val containerObj = json.getJSONObject("thinkingConfig")
        assertEquals(4096, containerObj.getInt("thinkingBudget"))
    }

    @Test
    fun testWordCountCalculationForThinkingBlock() {
        val sampleThinking = "První krok je analýza zadání. Následně vytvoříme plán a implementujeme čistý kód."
        val words = sampleThinking.trim().split("\\s+".toRegex()).count { it.isNotEmpty() }
        assertEquals(12, words)
    }

    @Test
    fun testModelConfigAndThinkingLevelConfigDefaults() {
        val model = ModelConfig(
            id = "gemini-2.5-flash",
            displayName = "Gemini 2.5 Flash",
            supportsThinking = true,
            thinkingParamKey = "thinkingLevel"
        )
        val level = ThinkingLevelConfig(
            id = "CUSTOM_1",
            label = "🧠 Vlastní úroveň",
            value = "HIGH",
            customParamKeyOverride = "premysleni_pro_api"
        )

        val effectiveKey = level.customParamKeyOverride.ifBlank { model.thinkingParamKey }
        assertEquals("premysleni_pro_api", effectiveKey)
    }
}
