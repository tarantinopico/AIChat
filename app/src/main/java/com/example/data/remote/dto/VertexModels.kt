package com.example.data.remote.dto

import org.json.JSONArray
import org.json.JSONObject

data class VertexContentPart(
    val text: String,
    val isThought: Boolean = false
)

data class VertexContent(
    val role: String, // "user" or "model"
    val parts: List<VertexContentPart>
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("role", role)
        val partsArray = JSONArray()
        for (part in parts) {
            val partObj = JSONObject()
            partObj.put("text", part.text)
            partsArray.put(partObj)
        }
        json.put("parts", partsArray)
        return json
    }
}

data class VertexGenerationConfig(
    val temperature: Float = 0.7f,
    val topP: Float = 0.95f,
    val topK: Int = 40,
    val maxOutputTokens: Int = 8192
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("temperature", temperature)
            put("topP", topP)
            put("topK", topK)
            put("maxOutputTokens", maxOutputTokens)
        }
    }
}

data class VertexSafetySetting(
    val category: String,
    val threshold: String
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("category", category)
            put("threshold", threshold)
        }
    }

    companion object {
        fun createBlockNoneList(): List<VertexSafetySetting> = listOf(
            VertexSafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_NONE"),
            VertexSafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_NONE"),
            VertexSafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_NONE"),
            VertexSafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_NONE"),
            VertexSafetySetting("HARM_CATEGORY_CIVIC_INTEGRITY", "BLOCK_NONE")
        )
    }
}

data class VertexUsageMetadata(
    val promptTokenCount: Int = 0,
    val candidatesTokenCount: Int = 0,
    val totalTokenCount: Int = 0,
    val thinkingTokenCount: Int = 0
)

data class VertexStreamChunk(
    val textDelta: String = "",
    val thinkingDelta: String = "",
    val finishReason: String? = null,
    val usageMetadata: VertexUsageMetadata? = null
)

class VertexApiException(
    val httpCode: Int,
    val rawErrorJson: String,
    override val message: String
) : Exception(message)
