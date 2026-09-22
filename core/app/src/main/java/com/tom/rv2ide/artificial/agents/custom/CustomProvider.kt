/*
 *  This file is part of AndroidCodeStudio.
 *
 *  AndroidCodeStudio is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidCodeStudio is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidCodeStudio.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.rv2ide.artificial.agents.custom

import android.content.Context
import com.tom.rv2ide.artificial.agents.AIAgent
import com.tom.rv2ide.artificial.agents.AIAgentRegistry
import com.tom.rv2ide.artificial.agents.Agents
import com.tom.rv2ide.artificial.agents.ModificationAttempt
import com.tom.rv2ide.artificial.rules.WritingRules
import com.tom.rv2ide.artificial.project.awareness.ProjectTreeResult
import com.tom.rv2ide.artificial.file.AIFileWriter
import com.tom.rv2ide.artificial.file.FileWriteResult
import com.tom.rv2ide.artificial.exceptions.RateLimitException
import com.tom.rv2ide.artificial.exceptions.QuotaExceededException
import com.tom.rv2ide.artificial.exceptions.InvalidApiKeyException
import com.tom.rv2ide.app.BaseApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.json.JSONArray

/**
 * A generic OpenAI-compatible provider that works with any backend exposing
 * a /v1/chat/completions endpoint (e.g. LM Studio, Ollama, Groq, Together.ai,
 * Mistral, OpenRouter, Perplexity, etc.).
 *
 * Configure via app settings:
 *  - Base URL  -> "custom_provider_base_url"   (e.g. https://api.openrouter.ai)
 *  - API Key   -> "custom_provider_api_key"    (empty string if no auth needed)
 *  - Model     -> "custom_provider_model_name" (e.g. mistralai/mixtral-8x7b)
 */
class CustomProvider : AIAgent {

    private var apiKey: String? = null
    private var baseUrl: String? = null
    private var modelName: String? = null

    private val writingRules = WritingRules.Instructions()
    private var projectTreeResult: ProjectTreeResult? = null
    private var fileWriter: AIFileWriter? = null
    private val conversationHistory = mutableListOf<ConversationMessage>()
    private val modificationHistory = mutableListOf<ModificationAttempt>()
    private var currentAttemptCount = 0
    private val maxRetryAttempts = 3
    private var agents: Agents? = null

    override val providerId = "custom"
    override val providerName = "Custom (OpenAI-compatible)"

    companion object {
        fun registerAgent() {
            AIAgentRegistry.register("custom", object : AIAgentRegistry.AgentFactory {
                override fun create(context: Context): AIAgent = CustomProvider()

                override fun hasValidApiKey(): Boolean {
                    val prefs = BaseApplication.getBaseInstance().prefManager
                    val url = prefs.getString("custom_provider_base_url", null)
                    val model = prefs.getString("custom_provider_model_name", null)
                    // URL and model are required; API key is optional (some servers need no auth)
                    return !url.isNullOrBlank() && !model.isNullOrBlank()
                }

                override fun getApiKey(): String? {
                    val prefs = BaseApplication.getBaseInstance().prefManager
                    return prefs.getString("custom_provider_api_key", "")
                }
            })
        }
    }

    // AIAgent lifecycle

    override fun initialize(apiKey: String, context: Context) {
        val prefs = BaseApplication.getBaseInstance().prefManager
        this.baseUrl = prefs.getString("custom_provider_base_url", null)?.trimEnd('/')
        this.modelName = prefs.getString("custom_provider_model_name", null)
        this.apiKey = prefs.getString("custom_provider_api_key", "")
            .takeIf { !it.isNullOrBlank() }
        agents = Agents(context)

        android.util.Log.d("CustomProvider", "Initialized: url=$baseUrl, model=$modelName")

        if (baseUrl.isNullOrBlank() || modelName.isNullOrBlank()) {
            throw IllegalStateException(
                "Custom provider not configured. Please set Base URL and Model Name in settings."
            )
        }
    }

    override fun reinitializeWithNewModel(apiKey: String, context: Context) {
        initialize(apiKey, context)
    }

    override fun setContext(context: Context) {
        fileWriter = AIFileWriter(context)
    }

    override fun setProjectData(projectTreeResult: ProjectTreeResult) {
        this.projectTreeResult = projectTreeResult
    }

    override fun clearConversation() {
        conversationHistory.clear()
        modificationHistory.clear()
        currentAttemptCount = 0
    }

    override fun isInitialized(): Boolean =
        !baseUrl.isNullOrBlank() && !modelName.isNullOrBlank()

    // Modification history

    override fun recordModification(
        filePath: String,
        oldContent: String?,
        newContent: String,
        success: Boolean
    ) {
        modificationHistory.add(
            ModificationAttempt(
                timestamp = System.currentTimeMillis(),
                filePath = filePath,
                previousContent = oldContent,
                newContent = newContent,
                attemptNumber = currentAttemptCount,
                success = success
            )
        )
    }

    override fun undoLastModification(): Boolean {
        if (modificationHistory.isEmpty()) return false
        val lastMod = modificationHistory.lastOrNull { it.success } ?: return false
        return if (lastMod.previousContent != null) {
            val result = writeFile(lastMod.filePath, lastMod.previousContent)
            if (result is FileWriteResult.Success) {
                modificationHistory.removeAt(modificationHistory.lastIndexOf(lastMod))
                true
            } else false
        } else {
            try {
                File(lastMod.filePath).delete()
                modificationHistory.removeAt(modificationHistory.lastIndexOf(lastMod))
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun getModificationHistory(): List<ModificationAttempt> =
        modificationHistory.toList()

    // Retry / attempt tracking

    override fun resetAttemptCount() { currentAttemptCount = 0 }
    override fun incrementAttemptCount() { currentAttemptCount++ }
    override fun getCurrentAttemptCount(): Int = currentAttemptCount
    override fun canRetry(): Boolean = currentAttemptCount < maxRetryAttempts

    // File I/O

    override fun writeFile(filePath: String, content: String): FileWriteResult {
        val writer = fileWriter ?: return FileWriteResult.Error("File writer not initialized")
        return writer.writeFile(filePath, content, createBackup = true)
    }

    // Code generation

    override suspend fun generateCode(
        prompt: String,
        context: String?,
        language: String,
        projectStructure: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = baseUrl
                ?: return@withContext Result.failure(
                    IllegalStateException("Custom provider base URL not configured")
                )
            val model = modelName
                ?: return@withContext Result.failure(
                    IllegalStateException("Custom provider model name not configured")
                )

            val fileContents = readRelevantFiles()

            val fullPrompt = buildString {
                append("=== PROJECT STRUCTURE (THESE ARE THE EXACT PATHS YOU MUST USE) ===\n")
                if (projectTreeResult != null) {
                    append(projectTreeResult!!.tree)
                    append("\n\n")
                    append("CRITICAL: Use ONLY the paths shown above.\n\n")
                }
                if (fileContents.isNotEmpty()) {
                    append("=== CURRENT FILES CONTENT ===\n")
                    fileContents.forEach { (path, content) ->
                        append("FILE: $path\n")
                        append("CONTENT:\n$content\n\n")
                    }
                }
                if (context != null) {
                    append("=== ADDITIONAL CONTEXT ===\n$context\n\n")
                }
                if (conversationHistory.isNotEmpty()) {
                    append("=== CONVERSATION HISTORY ===\n")
                    conversationHistory.forEach { msg ->
                        append("${msg.role.uppercase()}: ${msg.content}\n\n")
                    }
                }
                if (currentAttemptCount > 0) {
                    append("=== RETRY ATTEMPT $currentAttemptCount/$maxRetryAttempts ===\n")
                    append("Previous attempts did not satisfy the user. Please provide a different solution.\n\n")
                }
                append("=== USER REQUEST ===\n$prompt")
            }

            val response = callApi(url, model, fullPrompt)

            if (response.isBlank()) {
                return@withContext Result.failure(Exception("Empty response from Custom provider"))
            }

            conversationHistory.add(ConversationMessage("user", prompt))
            conversationHistory.add(ConversationMessage("assistant", response))
            if (conversationHistory.size > 20) {
                conversationHistory.removeAt(0)
                conversationHistory.removeAt(0)
            }

            Result.success(response)
        } catch (e: RateLimitException) {
            Result.failure(e)
        } catch (e: QuotaExceededException) {
            Result.failure(e)
        } catch (e: InvalidApiKeyException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Build error handling

    override suspend fun handleBuildError(errors: List<String>): Result<List<String>> {
        val prompt = "The build failed with the following errors:\n" +
            errors.joinToString("\n") +
            "\nPlease fix these errors."
        return generateCode(prompt, null, "kotlin", null).map { listOf("Applied fixes") }
    }

    override suspend fun buildAndFixLoop(maxAttempts: Int): Result<java.io.File> =
        Result.failure(UnsupportedOperationException("Not yet implemented"))

    // Internal HTTP call

    /**
     * Calls the OpenAI-compatible /v1/chat/completions endpoint.
     * Compatible with any backend that follows the OpenAI chat completions spec.
     */
    private fun callApi(baseUrl: String, model: String, prompt: String): String {
        android.util.Log.d("CustomProvider", "Calling $baseUrl/v1/chat/completions with model=$model")

        val connection = URL("$baseUrl/v1/chat/completions").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            apiKey?.let { key ->
                connection.setRequestProperty("Authorization", "Bearer $key")
            }
            connection.doOutput = true
            connection.connectTimeout = 60_000
            connection.readTimeout = 120_000

            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", writingRules.useThis())
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }

            val requestBody = JSONObject().apply {
                put("model", model)
                put("messages", messages)
                put("temperature", 0.7)
                put("max_tokens", 4096)
                put("stream", false)
            }

            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            android.util.Log.d("CustomProvider", "Response code: $responseCode")

            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                android.util.Log.e("CustomProvider", "Error: $errorBody")

                val (errorType, errorMessage) = try {
                    val err = JSONObject(errorBody).optJSONObject("error")
                    Pair(
                        err?.optString("type") ?: "",
                        err?.optString("message") ?: errorBody
                    )
                } catch (ex: Exception) {
                    Pair("", errorBody)
                }

                when {
                    responseCode == 429 || errorType.contains("rate_limit") ->
                        throw RateLimitException("Custom provider rate limit exceeded: $errorMessage")
                    errorType.contains("insufficient_quota") || errorMessage.contains("quota") ->
                        throw QuotaExceededException("Custom provider quota exceeded: $errorMessage")
                    responseCode == 401 || errorType.contains("invalid_api_key") ->
                        throw InvalidApiKeyException("Custom provider authentication failed: $errorMessage")
                    else ->
                        throw Exception("Custom provider API error ($responseCode): $errorMessage")
                }
            }

            val responseBody = connection.inputStream.bufferedReader().readText()
            android.util.Log.d("CustomProvider", "Success, response length: ${responseBody.length}")

            val json = JSONObject(responseBody)
            val choices = json.getJSONArray("choices")
            if (choices.length() > 0) {
                return choices.getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            }
            throw Exception("No choices returned from Custom provider")
        } catch (e: RateLimitException) {
            throw e
        } catch (e: QuotaExceededException) {
            throw e
        } catch (e: InvalidApiKeyException) {
            throw e
        } catch (e: java.net.SocketTimeoutException) {
            throw Exception("Custom provider request timed out: ${e.message}")
        } catch (e: java.net.UnknownHostException) {
            throw Exception("Cannot reach custom provider host: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("CustomProvider", "Exception", e)
            throw e
        } finally {
            connection.disconnect()
        }
    }

    // File reading helpers

    private fun readRelevantFiles(): Map<String, String> {
        val filesContent = mutableMapOf<String, String>()
        val tree = projectTreeResult?.tree ?: return filesContent
        tree.lines().filter { it.isNotBlank() }.forEach { filePath ->
            val trimmedPath = filePath.trim()
            val file = File(trimmedPath)
            if (file.isFile &&
                (trimmedPath.endsWith(".kt") ||
                    trimmedPath.endsWith(".java") ||
                    trimmedPath.endsWith(".xml") ||
                    trimmedPath.endsWith(".gradle") ||
                    trimmedPath.endsWith(".gradle.kts")) &&
                !trimmedPath.contains("/build/") &&
                !trimmedPath.contains("/.gradle/")) {
                try {
                    filesContent[trimmedPath] = file.readText()
                } catch (e: Exception) {
                    // skip unreadable files
                }
            }
        }
        return filesContent
    }
}

// Local data classes
private data class ConversationMessage(val role: String, val content: String)
