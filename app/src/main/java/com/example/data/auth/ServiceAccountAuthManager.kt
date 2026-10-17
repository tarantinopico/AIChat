package com.example.data.auth

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.concurrent.TimeUnit

data class ServiceAccountCredentials(
    val projectId: String,
    val clientEmail: String,
    val privateKeyPem: String,
    val tokenUri: String = "https://oauth2.googleapis.com/token"
)

class ServiceAccountAuthManager(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    @Volatile
    private var cachedToken: String? = null
    @Volatile
    private var tokenExpiryEpochMs: Long = 0L
    @Volatile
    private var currentProjectId: String? = null

    fun parseCredentials(jsonContent: String): Result<ServiceAccountCredentials> {
        return try {
            val json = JSONObject(jsonContent.trim())
            val projectId = json.optString("project_id")
            val clientEmail = json.optString("client_email")
            val privateKey = json.optString("private_key")
            val tokenUri = json.optString("token_uri", "https://oauth2.googleapis.com/token")

            if (projectId.isBlank()) {
                return Result.failure(IllegalArgumentException("Service Account JSON neobsahuje platný 'project_id'."))
            }
            if (clientEmail.isBlank()) {
                return Result.failure(IllegalArgumentException("Service Account JSON neobsahuje platný 'client_email'."))
            }
            if (privateKey.isBlank()) {
                return Result.failure(IllegalArgumentException("Service Account JSON neobsahuje platný 'private_key'."))
            }

            Result.success(
                ServiceAccountCredentials(
                    projectId = projectId,
                    clientEmail = clientEmail,
                    privateKeyPem = privateKey,
                    tokenUri = if (tokenUri.isNotBlank()) tokenUri else "https://oauth2.googleapis.com/token"
                )
            )
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Chyba při parsování Service Account JSON: ${e.localizedMessage}", e))
        }
    }

    private fun parsePrivateKey(pemKey: String): PrivateKey {
        val cleanPem = pemKey
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
            .replace("\\n", "")

        val keyBytes = Base64.decode(cleanPem, Base64.DEFAULT)
        val spec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePrivate(spec)
    }

    private fun createSignedJwt(credentials: ServiceAccountCredentials): String {
        val privateKey = parsePrivateKey(credentials.privateKeyPem)

        val headerJson = JSONObject().apply {
            put("alg", "RS256")
            put("typ", "JWT")
        }.toString()

        val nowSeconds = System.currentTimeMillis() / 1000L
        val expSeconds = nowSeconds + 3600L

        val claimsJson = JSONObject().apply {
            put("iss", credentials.clientEmail)
            put("sub", credentials.clientEmail)
            put("aud", credentials.tokenUri)
            put("iat", nowSeconds)
            put("exp", expSeconds)
            put("scope", "https://www.googleapis.com/auth/cloud-platform")
        }.toString()

        val encodedHeader = Base64.encodeToString(
            headerJson.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        val encodedClaims = Base64.encodeToString(
            claimsJson.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )

        val dataToSign = "$encodedHeader.$encodedClaims"

        val signature = Signature.getInstance("SHA256withRSA").apply {
            initSign(privateKey)
            update(dataToSign.toByteArray(Charsets.UTF_8))
        }
        val signatureBytes = signature.sign()
        val encodedSignature = Base64.encodeToString(
            signatureBytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )

        return "$dataToSign.$encodedSignature"
    }

    suspend fun getAccessToken(jsonContent: String, forceRefresh: Boolean = false): Result<String> =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            // Reuse cached token if valid for more than 5 minutes
            if (!forceRefresh && cachedToken != null && now < (tokenExpiryEpochMs - 300_000L)) {
                return@withContext Result.success(cachedToken!!)
            }

            val credsResult = parseCredentials(jsonContent)
            if (credsResult.isFailure) {
                return@withContext Result.failure(credsResult.exceptionOrNull()!!)
            }
            val credentials = credsResult.getOrNull()!!
            currentProjectId = credentials.projectId

            try {
                val jwtAssertion = createSignedJwt(credentials)
                val formBody = FormBody.Builder()
                    .add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                    .add("assertion", jwtAssertion)
                    .build()

                val request = Request.Builder()
                    .url(credentials.tokenUri)
                    .post(formBody)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("OAuth2 Token Exchange selhal (HTTP ${response.code}): $responseBody")
                    )
                }

                val jsonResponse = JSONObject(responseBody)
                val accessToken = jsonResponse.optString("access_token")
                val expiresInSeconds = jsonResponse.optLong("expires_in", 3600L)

                if (accessToken.isNullOrBlank()) {
                    return@withContext Result.failure(
                        Exception("Odpověď neobsahuje access_token: $responseBody")
                    )
                }

                cachedToken = accessToken
                tokenExpiryEpochMs = System.currentTimeMillis() + (expiresInSeconds * 1000L)

                Result.success(accessToken)
            } catch (e: Exception) {
                Result.failure(Exception("Chyba při autentizaci se Service Account: ${e.localizedMessage}", e))
            }
        }

    fun invalidateCache() {
        cachedToken = null
        tokenExpiryEpochMs = 0L
    }

    fun getProjectId(): String? = currentProjectId
}
