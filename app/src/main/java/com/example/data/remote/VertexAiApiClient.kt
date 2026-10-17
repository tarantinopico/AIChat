package com.example.data.remote

import com.example.data.auth.ServiceAccountAuthManager
import com.example.data.remote.dto.VertexApiException
import com.example.data.remote.dto.VertexContent
import com.example.data.remote.dto.VertexGenerationConfig
import com.example.data.remote.dto.VertexSafetySetting
import com.example.data.remote.dto.VertexStreamChunk
import com.example.data.remote.dto.VertexUsageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class VertexAiApiClient(
    private val authManager: ServiceAccountAuthManager,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    private fun getBaseUrl(region: String): String {
        return if (region.equals("global", ignoreCase = true)) {
            "https://aiplatform.googleapis.com"
        } else {
            "https://${region.lowercase()}-aiplatform.googleapis.com"
        }
    }

    private fun buildRequestBodyJson(
        contents: List<VertexContent>,
        systemInstruction: String?,
        generationConfig: VertexGenerationConfig,
        safetySettings: List<VertexSafetySetting>
    ): String {
        val root = JSONObject()

        val contentsArray = JSONArray()
        for (content in contents) {
            contentsArray.put(content.toJson())
        }
        root.put("contents", contentsArray)

        if (!systemInstruction.isNullOrBlank()) {
            val sysInstructionObj = JSONObject()
            val sysParts = JSONArray()
            sysParts.put(JSONObject().apply { put("text", systemInstruction) })
            sysInstructionObj.put("parts", sysParts)
            root.put("systemInstruction", sysInstructionObj)
        }

        root.put("generationConfig", generationConfig.toJson())

        if (safetySettings.isNotEmpty()) {
            val safetyArray = JSONArray()
            for (setting in safetySettings) {
                safetyArray.put(setting.toJson())
            }
            root.put("safetySettings", safetyArray)
        }

        return root.toString()
    }

    private fun parseErrorResponse(code: Int, rawBody: String): VertexApiException {
        val humanMessage = try {
            val json = JSONObject(rawBody)
            val errorObj = json.optJSONObject("error")
            if (errorObj != null) {
                val msg = errorObj.optString("message", "")
                val status = errorObj.optString("status", "")
                "Vertex AI API chyba ($status / HTTP $code): $msg"
            } else {
                "HTTP $code: $rawBody"
            }
        } catch (_: Exception) {
            "HTTP $code: $rawBody"
        }
        return VertexApiException(httpCode = code, rawErrorJson = rawBody, message = humanMessage)
    }

    fun streamGenerateContent(
        serviceAccountJson: String,
        region: String,
        modelId: String,
        contents: List<VertexContent>,
        systemInstruction: String?,
        generationConfig: VertexGenerationConfig,
        safetySettings: List<VertexSafetySetting>
    ): Flow<VertexStreamChunk> = flow {
        val tokenResult = authManager.getAccessToken(serviceAccountJson)
        if (tokenResult.isFailure) {
            val err = tokenResult.exceptionOrNull()!!
            throw VertexApiException(
                httpCode = 401,
                rawErrorJson = JSONObject().apply {
                    put("error", "OAuth2 Authentication Failed")
                    put("details", err.localizedMessage)
                }.toString(),
                message = "Chyba autentizace: ${err.localizedMessage}"
            )
        }
        val accessToken = tokenResult.getOrNull()!!
        val projectId = authManager.getProjectId().orEmpty()

        val baseUrl = getBaseUrl(region)
        val url = "$baseUrl/v1/projects/$projectId/locations/$region/publishers/google/models/$modelId:streamGenerateContent?alt=sse"

        val bodyJson = buildRequestBodyJson(contents, systemInstruction, generationConfig, safetySettings)
        val requestBody = bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string().orEmpty()
            response.close()
            throw parseErrorResponse(response.code, errorBody)
        }

        val responseBody = response.body
            ?: throw VertexApiException(response.code, "{}", "Prázdné tělo odpovědi z Vertex AI.")

        val reader = BufferedReader(InputStreamReader(responseBody.byteStream(), Charsets.UTF_8))
        try {
            var line: String? = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.startsWith("data:")) {
                    val dataJson = trimmed.removePrefix("data:").trim()
                    if (dataJson.isNotBlank() && dataJson != "[DONE]") {
                        val chunk = parseChunkJson(dataJson)
                        if (chunk != null) {
                            emit(chunk)
                        }
                    }
                }
                line = reader.readLine()
            }
        } finally {
            reader.close()
            response.close()
        }
    }.flowOn(Dispatchers.IO)

    private fun parseChunkJson(jsonStr: String): VertexStreamChunk? {
        return try {
            val root = JSONObject(jsonStr)
            val candidates = root.optJSONArray("candidates")
            var textBuilder = StringBuilder()
            var thinkingBuilder = StringBuilder()
            var finishReason: String? = null

            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                finishReason = firstCandidate.optString("finishReason").takeIf { it.isNotBlank() }

                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        val text = part.optString("text")
                        val isThought = part.optBoolean("thought", false)
                        if (isThought) {
                            thinkingBuilder.append(text)
                        } else {
                            textBuilder.append(text)
                        }
                    }
                }
            }

            var usage: VertexUsageMetadata? = null
            val usageObj = root.optJSONObject("usageMetadata")
            if (usageObj != null) {
                usage = VertexUsageMetadata(
                    promptTokenCount = usageObj.optInt("promptTokenCount", 0),
                    candidatesTokenCount = usageObj.optInt("candidatesTokenCount", 0),
                    totalTokenCount = usageObj.optInt("totalTokenCount", 0),
                    thinkingTokenCount = usageObj.optInt("thinkingTokenCount", 0)
                )
            }

            VertexStreamChunk(
                textDelta = textBuilder.toString(),
                thinkingDelta = thinkingBuilder.toString(),
                finishReason = finishReason,
                usageMetadata = usage
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun generateContent(
        serviceAccountJson: String,
        region: String,
        modelId: String,
        contents: List<VertexContent>,
        systemInstruction: String?,
        generationConfig: VertexGenerationConfig,
        safetySettings: List<VertexSafetySetting>
    ): Result<VertexStreamChunk> = withContext(Dispatchers.IO) {
        val tokenResult = authManager.getAccessToken(serviceAccountJson)
        if (tokenResult.isFailure) {
            return@withContext Result.failure(tokenResult.exceptionOrNull()!!)
        }
        val accessToken = tokenResult.getOrNull()!!
        val projectId = authManager.getProjectId().orEmpty()

        val baseUrl = getBaseUrl(region)
        val url = "$baseUrl/v1/projects/$projectId/locations/$region/publishers/google/models/$modelId:generateContent"

        val bodyJson = buildRequestBodyJson(contents, systemInstruction, generationConfig, safetySettings)
        val requestBody = bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .post(requestBody)
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val rawBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return@withContext Result.failure(parseErrorResponse(response.code, rawBody))
            }
            val chunk = parseChunkJson(rawBody)
                ?: return@withContext Result.failure(Exception("Nepodařilo se naparsovat odpověď z Vertex AI."))
            Result.success(chunk)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAvailableModels(
        serviceAccountJson: String,
        region: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        val tokenResult = authManager.getAccessToken(serviceAccountJson)
        if (tokenResult.isFailure) {
            return@withContext Result.failure(tokenResult.exceptionOrNull()!!)
        }
        val accessToken = tokenResult.getOrNull()!!
        val projectId = authManager.getProjectId().orEmpty()

        val baseUrl = getBaseUrl(region)
        val url = "$baseUrl/v1/projects/$projectId/locations/$region/publishers/google/models"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val rawBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val json = JSONObject(rawBody)
                val modelsArray = json.optJSONArray("models")
                val foundModels = mutableListOf<String>()
                if (modelsArray != null) {
                    for (i in 0 until modelsArray.length()) {
                        val modelObj = modelsArray.getJSONObject(i)
                        val name = modelObj.optString("name")
                        val cleanName = name.substringAfterLast("/")
                        if (cleanName.isNotBlank()) {
                            foundModels.add(cleanName)
                        }
                    }
                }
                if (foundModels.isNotEmpty()) {
                    return@withContext Result.success(foundModels.distinct().sorted())
                }
            }
            // Fallback catalog of known official Gemini models on Vertex AI
            val standardModels = listOf(
                "gemini-2.5-pro",
                "gemini-2.5-flash",
                "gemini-2.0-flash",
                "gemini-2.0-flash-lite",
                "gemini-1.5-pro",
                "gemini-1.5-flash",
                "gemini-4-argon"
            )
            Result.success(standardModels)
        } catch (e: Exception) {
            val standardModels = listOf(
                "gemini-2.5-pro",
                "gemini-2.5-flash",
                "gemini-2.0-flash",
                "gemini-1.5-pro",
                "gemini-1.5-flash"
            )
            Result.success(standardModels)
        }
    }

    suspend fun testConnection(serviceAccountJson: String, region: String, modelId: String): Result<String> = withContext(Dispatchers.IO) {
        val tokenResult = authManager.getAccessToken(serviceAccountJson, forceRefresh = true)
        if (tokenResult.isFailure) {
            return@withContext Result.failure(tokenResult.exceptionOrNull()!!)
        }
        val projectId = authManager.getProjectId().orEmpty()

        val contents = listOf(
            VertexContent(
                role = "user",
                parts = listOf(com.example.data.remote.dto.VertexContentPart("Ping! Napiš 'OK'"))
            )
        )
        val config = VertexGenerationConfig(temperature = 0.1f, maxOutputTokens = 10)
        val result = generateContent(
            serviceAccountJson = serviceAccountJson,
            region = region,
            modelId = modelId,
            contents = contents,
            systemInstruction = null,
            generationConfig = config,
            safetySettings = emptyList()
        )

        if (result.isSuccess) {
            val chunk = result.getOrNull()!!
            Result.success("Spojení s Vertex AI v regionu '$region' (Projekt: $projectId, Model: $modelId) je úspěšné! Odpověď: ${chunk.textDelta.trim()}")
        } else {
            Result.failure(result.exceptionOrNull()!!)
        }
    }
}
