package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ChatMessage
import com.example.data.model.LanguageMode
import com.example.data.model.MessageSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLiveClient {

    companion object {
        private const val TAG = "GeminiLiveClient"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
        // Model prescribed by gemini-api skill for real-time conversational tasks
        const val MODEL_LIVE_AUDIO = "gemini-2.5-flash-native-audio-preview-12-2025"
        const val MODEL_FALLBACK_FLASH = "gemini-3.5-flash"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY.trim()
    }

    fun isApiKeyConfigured(): Boolean {
        val key = getApiKey()
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY"
    }

    suspend fun generateVoiceResponse(
        userInput: String,
        history: List<ChatMessage>,
        languageMode: LanguageMode,
        requestAudio: Boolean = false
    ): GeminiResponseResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeminiResponseResult.Error(
                "Gemini API key is not configured. Please add GEMINI_API_KEY in the AI Studio Secrets panel."
            )
        }

        // Try primary real-time voice model first, fallback to gemini-3.5-flash if needed
        val primaryResult = executeRequest(MODEL_LIVE_AUDIO, userInput, history, languageMode, requestAudio, apiKey)
        if (primaryResult is GeminiResponseResult.Success) {
            return@withContext primaryResult
        }

        Log.w(TAG, "Primary model failed, attempting fallback model: $MODEL_FALLBACK_FLASH")
        return@withContext executeRequest(MODEL_FALLBACK_FLASH, userInput, history, languageMode, false, apiKey)
    }

    private fun executeRequest(
        model: String,
        userInput: String,
        history: List<ChatMessage>,
        languageMode: LanguageMode,
        requestAudio: Boolean,
        apiKey: String
    ): GeminiResponseResult {
        try {
            val url = "$BASE_URL$model:generateContent?key=$apiKey"

            val systemPrompt = buildString {
                append("You are Maya AI, an intelligent, empathetic, and culturally aware voice assistant. ")
                append("You fluently speak and understand Urdu (اردو), English, and Roman Urdu (e.g., 'Assalam-o-Alaikum, aap kaise hain?'). ")
                append("Your standard greeting is: 'Assalam-o-Alaikum! I am Maya AI. How can I help you today?'. ")
                append("Active Language Mode instruction: ${languageMode.promptInstruction} ")
                append("Guidelines for spoken voice responses: Keep replies concise, expressive, polite, and natural for listening. ")
                append("Avoid long tables, markdown headers, or hard-to-speak symbols. Provide warm, direct, and helpful answers.")
            }

            val requestJson = JSONObject()

            // System instruction
            val systemInstruction = JSONObject()
            val sysParts = JSONArray()
            sysParts.put(JSONObject().put("text", systemPrompt))
            systemInstruction.put("parts", sysParts)
            requestJson.put("systemInstruction", systemInstruction)

            // Conversation history (limit to last 10 messages for fast latency)
            val contentsArray = JSONArray()
            val recentHistory = history.takeLast(10)
            for (msg in recentHistory) {
                val role = if (msg.sender == MessageSender.USER) "user" else "model"
                val turnObj = JSONObject()
                turnObj.put("role", role)
                val parts = JSONArray()
                parts.put(JSONObject().put("text", msg.text))
                turnObj.put("parts", parts)
                contentsArray.put(turnObj)
            }

            // Current user turn
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            val currentParts = JSONArray()
            currentParts.put(JSONObject().put("text", userInput))
            currentTurn.put("parts", currentParts)
            contentsArray.put(currentTurn)

            requestJson.put("contents", contentsArray)

            // Generation config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            genConfig.put("topP", 0.95)

            if (requestAudio) {
                val modalities = JSONArray()
                modalities.put("AUDIO")
                modalities.put("TEXT")
                genConfig.put("responseModalities", modalities)

                val speechConfig = JSONObject()
                val voiceConfig = JSONObject()
                val prebuiltVoiceConfig = JSONObject()
                // Natural warm voice suitable for Urdu & English
                prebuiltVoiceConfig.put("voiceName", "Kore")
                voiceConfig.put("prebuiltVoiceConfig", prebuiltVoiceConfig)
                speechConfig.put("voiceConfig", voiceConfig)
                genConfig.put("speechConfig", speechConfig)
            }

            requestJson.put("generationConfig", genConfig)

            val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error code ${response.code}: $responseBody")
                return GeminiResponseResult.Error("API returned ${response.code}: ${parseErrorMessage(responseBody)}")
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return GeminiResponseResult.Error("Maya could not generate a response. Please try again.")
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            var responseText = ""
            var audioBase64: String? = null
            var audioMimeType: String? = null

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("text")) {
                        responseText += part.getString("text")
                    }
                    if (part.has("inlineData")) {
                        val inlineData = part.getJSONObject("inlineData")
                        audioBase64 = inlineData.optString("data")
                        audioMimeType = inlineData.optString("mimeType")
                    }
                }
            }

            if (responseText.isEmpty() && audioBase64 == null) {
                responseText = "Assalam-o-Alaikum! I heard you, but could not formulate a response. Please say that again."
            }

            return GeminiResponseResult.Success(
                text = responseText.trim(),
                audioBase64 = audioBase64,
                audioMimeType = audioMimeType,
                modelUsed = model
            )

        } catch (e: Exception) {
            Log.e(TAG, "Request failed", e)
            return GeminiResponseResult.Error("Connection error: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun parseErrorMessage(errorJson: String): String {
        return try {
            val json = JSONObject(errorJson)
            val error = json.optJSONObject("error")
            error?.optString("message") ?: errorJson.take(120)
        } catch (_: Exception) {
            errorJson.take(120)
        }
    }
}

sealed class GeminiResponseResult {
    data class Success(
        val text: String,
        val audioBase64: String?,
        val audioMimeType: String?,
        val modelUsed: String
    ) : GeminiResponseResult()

    data class Error(val message: String) : GeminiResponseResult()
}
