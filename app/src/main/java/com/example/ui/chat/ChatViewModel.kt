package com.example.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.ServiceAccountAuthManager
import com.example.data.local.AppDatabase
import com.example.data.local.dao.ModelTokenStats
import com.example.data.local.dao.OverallTokenStats
import com.example.data.local.datastore.AppSettings
import com.example.data.local.datastore.SettingsDataStore
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatThreadEntity
import com.example.data.local.entity.FileArtifactEntity
import com.example.data.remote.VertexAiApiClient
import com.example.data.remote.dto.VertexApiException
import com.example.data.remote.dto.VertexContent
import com.example.data.remote.dto.VertexContentPart
import com.example.data.remote.dto.VertexGenerationConfig
import com.example.data.remote.dto.VertexSafetySetting
import com.example.data.remote.dto.VertexUsageMetadata
import com.example.domain.util.ExtractedFile
import com.example.domain.util.MarkdownExporter
import com.example.domain.util.StreamFileExtractor
import com.example.service.GenerationForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ActiveStreamState(
    val isStreaming: Boolean = false,
    val streamingThreadId: Long? = null,
    val textContent: String = "",
    val thinkingContent: String = "",
    val files: List<ExtractedFile> = emptyList(),
    val usageMetadata: VertexUsageMetadata? = null,
    val autoContinuing: Boolean = false,
    val isThinkingStage: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val chatDao = database.chatDao()
    val settingsDataStore = SettingsDataStore(application)
    private val authManager = ServiceAccountAuthManager()
    val vertexClient = VertexAiApiClient(authManager)

    val settings: StateFlow<AppSettings> = settingsDataStore.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    val allThreads: StateFlow<List<ChatThreadEntity>> = chatDao.getAllThreads().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _activeThreadId = MutableStateFlow<Long?>(null)
    val activeThreadId: StateFlow<Long?> = _activeThreadId.asStateFlow()

    private val _activeStreamState = MutableStateFlow(ActiveStreamState())
    val activeStreamState: StateFlow<ActiveStreamState> = _activeStreamState.asStateFlow()

    private var activeGenerationJob: Job? = null

    // Messages and their files for the currently active thread
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMessagesWithFiles: StateFlow<List<Pair<ChatMessageEntity, List<FileArtifactEntity>>>> =
        _activeThreadId.flatMapLatest { threadId ->
            if (threadId == null) {
                flowOf(emptyList())
            } else {
                chatDao.getMessagesForThread(threadId).flatMapLatest { messages ->
                    if (messages.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        val messageFilesFlows = messages.map { msg ->
                            chatDao.getFilesForMessage(msg.id)
                        }
                        combine(messageFilesFlows) { filesArray ->
                            messages.mapIndexed { index, message ->
                                message to filesArray[index]
                            }
                        }
                    }
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val modelTokenStats: StateFlow<List<ModelTokenStats>> = chatDao.getTokenStatsByModel().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val overallTokenStats: StateFlow<OverallTokenStats> = chatDao.getOverallTokenStats().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OverallTokenStats(0, 0, 0, 0)
    )

    init {
        // Initialize active thread if available
        viewModelScope.launch {
            allThreads.collect { threads ->
                if (_activeThreadId.value == null && threads.isNotEmpty()) {
                    _activeThreadId.value = threads.first().id
                }
            }
        }
    }

    fun selectThread(threadId: Long) {
        _activeThreadId.value = threadId
    }

    fun createNewThread(title: String = "Nová konverzace", customModelId: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val model = customModelId ?: settings.value.activeModelId
            val newThread = ChatThreadEntity(
                title = title,
                modelId = model
            )
            val newId = chatDao.insertThread(newThread)
            _activeThreadId.value = newId
        }
    }

    fun renameThread(threadId: Long, newTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val thread = chatDao.getThreadById(threadId) ?: return@launch
            chatDao.updateThread(thread.copy(title = newTitle.trim(), updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteThread(threadId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.deleteThreadById(threadId)
            if (_activeThreadId.value == threadId) {
                val remaining = allThreads.value.filter { it.id != threadId }
                _activeThreadId.value = remaining.firstOrNull()?.id
            }
        }
    }

    fun switchActiveModel(modelId: String) {
        viewModelScope.launch {
            settingsDataStore.updateActiveModelId(modelId)
            val currentId = _activeThreadId.value
            if (currentId != null) {
                withContext(Dispatchers.IO) {
                    val thread = chatDao.getThreadById(currentId)
                    if (thread != null) {
                        chatDao.updateThread(thread.copy(modelId = modelId, updatedAt = System.currentTimeMillis()))
                    }
                }
            }
        }
    }

    fun updateAppTheme(theme: String) {
        viewModelScope.launch {
            settingsDataStore.updateAppTheme(theme)
        }
    }

    fun setThinkingLevel(level: String) {
        viewModelScope.launch {
            settingsDataStore.updateThinkingLevel(level)
        }
    }

    fun toggleMessagePin(messageId: Long, currentPin: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.updateMessagePinStatus(messageId, !currentPin)
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.deleteMessageById(messageId)
        }
    }

    fun editUserMessage(messageId: Long, newText: String) {
        val threadId = _activeThreadId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // Delete this user message and all subsequent responses
            chatDao.deleteMessagesFrom(threadId, messageId)
            // Re-send user prompt
            sendMessage(newText)
        }
    }

    fun regenerateModelResponse(messageId: Long) {
        val threadId = _activeThreadId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val messages = chatDao.getMessagesForThreadSync(threadId)
            val targetIndex = messages.indexOfFirst { it.id == messageId }
            if (targetIndex > 0) {
                val prevUserMessage = messages[targetIndex - 1]
                if (prevUserMessage.role == "user") {
                    // Delete from target model message onwards
                    chatDao.deleteMessagesFrom(threadId, messageId)
                    // Trigger generation for previous user message
                    triggerModelGeneration(threadId, continuePrompt = null)
                }
            }
        }
    }

    fun continueGeneration(messageId: Long) {
        val threadId = _activeThreadId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            triggerModelGeneration(
                threadId = threadId,
                continuePrompt = "Pokračuj v psaní kódu přesně od posledního znaku bez úvodního komentáře a bez opakování"
            )
        }
    }

    fun stopGeneration() {
        activeGenerationJob?.cancel()
        activeGenerationJob = null
        GenerationForegroundService.stopService(getApplication())
        finalizeStreaming(isCancelled = true)
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            var threadId = _activeThreadId.value
            if (threadId == null) {
                val title = if (trimmed.length > 35) trimmed.take(35) + "…" else trimmed
                val newThread = ChatThreadEntity(
                    title = title,
                    modelId = settings.value.activeModelId
                )
                threadId = chatDao.insertThread(newThread)
                _activeThreadId.value = threadId
            }

            // Insert user message
            val userMessage = ChatMessageEntity(
                threadId = threadId,
                role = "user",
                cleanTextContent = trimmed,
                modelId = settings.value.activeModelId
            )
            chatDao.insertMessage(userMessage)
            chatDao.touchThread(threadId)

            triggerModelGeneration(threadId, continuePrompt = null)
        }
    }

    private suspend fun triggerModelGeneration(threadId: Long, continuePrompt: String?) {
        val currentSettings = settings.value
        val saJson = currentSettings.serviceAccountJson

        if (saJson.isBlank()) {
            val errorMsg = ChatMessageEntity(
                threadId = threadId,
                role = "model",
                cleanTextContent = "Prosím vložte platný Service Account JSON v Nastavení pro volání Vertex AI.",
                modelId = currentSettings.activeModelId,
                isError = true,
                errorSummary = "Chybí Service Account JSON klíč.",
                rawErrorJson = null
            )
            chatDao.insertMessage(errorMsg)
            return
        }

        // Build conversation context
        val dbMessages = chatDao.getMessagesForThreadSync(threadId)
        val contextLimit = currentSettings.contextLimit

        // Pinned messages are ALWAYS preserved regardless of contextLimit
        val pinnedMessages = dbMessages.filter { it.isPinned }
        val nonPinnedMessages = dbMessages.filter { !it.isPinned }
        val recentNonPinned = nonPinnedMessages.takeLast(contextLimit)

        val finalContextList = (pinnedMessages + recentNonPinned)
            .distinctBy { it.id }
            .sortedBy { it.createdAt }

        val vertexContents = mutableListOf<VertexContent>()
        for (msg in finalContextList) {
            if (msg.isError) continue
            val parts = mutableListOf<VertexContentPart>()

            // Retrieve files attached to this message if any
            val files = chatDao.getFilesForMessageSync(msg.id)
            var fullText = msg.cleanTextContent
            if (files.isNotEmpty()) {
                val fileCodeBlocks = files.joinToString("\n\n") { f ->
                    "// Filename: ${f.fileName}\n```${f.language}\n${f.content}\n```"
                }
                fullText = if (fullText.isNotBlank()) "$fullText\n\n$fileCodeBlocks" else fileCodeBlocks
            }

            parts.add(VertexContentPart(fullText))
            vertexContents.add(VertexContent(role = msg.role, parts = parts))
        }

        if (!continuePrompt.isNullOrBlank()) {
            vertexContents.add(
                VertexContent(
                    role = "user",
                    parts = listOf(VertexContentPart(continuePrompt))
                )
            )
        }

        val config = VertexGenerationConfig(
            temperature = currentSettings.temperature,
            topP = currentSettings.topP,
            topK = currentSettings.topK,
            maxOutputTokens = currentSettings.maxOutputTokens,
            thinkingLevel = currentSettings.thinkingLevel
        )
        val safety = if (currentSettings.isBlockNoneSafety) {
            VertexSafetySetting.createBlockNoneList()
        } else {
            emptyList()
        }

        _activeStreamState.value = ActiveStreamState(
            isStreaming = true,
            streamingThreadId = threadId,
            autoContinuing = continuePrompt != null,
            isThinkingStage = currentSettings.thinkingLevel != "OFF"
        )

        // Start Foreground Service for continuous background processing & notifications
        GenerationForegroundService.startService(getApplication(), currentSettings.activeModelId)

        activeGenerationJob?.cancel()
        activeGenerationJob = viewModelScope.launch(Dispatchers.IO) {
            val rawAccumulatedText = StringBuilder()
            val thinkingAccumulator = StringBuilder()
            var lastFinishReason: String? = null
            var lastUsageMetadata: VertexUsageMetadata? = null

            try {
                if (currentSettings.isStreaming) {
                    vertexClient.streamGenerateContent(
                        serviceAccountJson = saJson,
                        region = currentSettings.region,
                        modelId = currentSettings.activeModelId,
                        contents = vertexContents,
                        systemInstruction = currentSettings.systemInstructions,
                        generationConfig = config,
                        safetySettings = safety
                    ).collect { chunk ->
                        rawAccumulatedText.append(chunk.textDelta)
                        thinkingAccumulator.append(chunk.thinkingDelta)
                        if (chunk.finishReason != null) {
                            lastFinishReason = chunk.finishReason
                        }
                        if (chunk.usageMetadata != null) {
                            lastUsageMetadata = chunk.usageMetadata
                        }

                        val parsed = StreamFileExtractor.parse(rawAccumulatedText.toString())
                        val isStillThinking = thinkingAccumulator.isNotEmpty() && rawAccumulatedText.isEmpty()

                        _activeStreamState.value = _activeStreamState.value.copy(
                            textContent = parsed.cleanText,
                            thinkingContent = thinkingAccumulator.toString(),
                            files = parsed.files,
                            usageMetadata = lastUsageMetadata,
                            isThinkingStage = isStillThinking
                        )

                        // Update Notification Progress
                        val statusText = if (isStillThinking) {
                            "Přemýšlím (Thinking)..."
                        } else if (parsed.files.isNotEmpty()) {
                            "Píši soubor: ${parsed.files.last().fileName}"
                        } else {
                            "Generuji odpověď..."
                        }
                        val lineCount = parsed.files.sumOf { it.lineCount }
                        GenerationForegroundService.updateProgress(
                            getApplication(),
                            statusText,
                            lineCount,
                            parsed.files.size
                        )
                    }
                } else {
                    val result = vertexClient.generateContent(
                        serviceAccountJson = saJson,
                        region = currentSettings.region,
                        modelId = currentSettings.activeModelId,
                        contents = vertexContents,
                        systemInstruction = currentSettings.systemInstructions,
                        generationConfig = config,
                        safetySettings = safety
                    )
                    if (result.isSuccess) {
                        val chunk = result.getOrNull()!!
                        rawAccumulatedText.append(chunk.textDelta)
                        thinkingAccumulator.append(chunk.thinkingDelta)
                        lastFinishReason = chunk.finishReason
                        lastUsageMetadata = chunk.usageMetadata
                    } else {
                        throw result.exceptionOrNull()!!
                    }
                }

                // Finalize and save message + files
                val finalParsed = StreamFileExtractor.parse(rawAccumulatedText.toString())
                val modelMessage = ChatMessageEntity(
                    threadId = threadId,
                    role = "model",
                    cleanTextContent = finalParsed.cleanText,
                    thinkingContent = thinkingAccumulator.toString().takeIf { it.isNotBlank() },
                    promptTokens = lastUsageMetadata?.promptTokenCount ?: 0,
                    candidateTokens = lastUsageMetadata?.candidatesTokenCount ?: 0,
                    thinkingTokens = lastUsageMetadata?.thinkingTokenCount ?: 0,
                    modelId = currentSettings.activeModelId,
                    finishReason = lastFinishReason
                )
                val messageId = chatDao.insertMessage(modelMessage)
                chatDao.touchThread(threadId)

                // Save all extracted files to Room
                for (file in finalParsed.files) {
                    val fileEntity = FileArtifactEntity(
                        messageId = messageId,
                        threadId = threadId,
                        fileName = file.fileName,
                        language = file.language,
                        mimeType = file.mimeType,
                        content = file.content,
                        lineCount = file.lineCount,
                        sizeBytes = file.sizeBytes
                    )
                    chatDao.insertFile(fileEntity)
                }

                _activeStreamState.value = ActiveStreamState(isStreaming = false)

                // Foreground Service Completion Notification
                val totalLines = finalParsed.files.sumOf { it.lineCount }
                GenerationForegroundService.completeService(getApplication(), finalParsed.files.size, totalLines)

                // Handle Auto-Continue if finishReason was MAX_TOKENS
                if (lastFinishReason == "MAX_TOKENS" && currentSettings.autoContinueMode == "AUTO") {
                    triggerModelGeneration(
                        threadId = threadId,
                        continuePrompt = "Pokračuj v psaní kódu přesně od posledního znaku bez úvodního komentáře a bez opakování"
                    )
                }

            } catch (e: Exception) {
                GenerationForegroundService.stopService(getApplication())
                val rawErrorJson = (e as? VertexApiException)?.rawErrorJson
                val errorMsg = ChatMessageEntity(
                    threadId = threadId,
                    role = "model",
                    cleanTextContent = "Během komunikace s Vertex AI došlo k chybě.",
                    modelId = currentSettings.activeModelId,
                    isError = true,
                    errorSummary = e.localizedMessage,
                    rawErrorJson = rawErrorJson
                )
                chatDao.insertMessage(errorMsg)
                _activeStreamState.value = ActiveStreamState(isStreaming = false)
            }
        }
    }

    private fun finalizeStreaming(isCancelled: Boolean) {
        val state = _activeStreamState.value
        val threadId = state.streamingThreadId ?: return

        viewModelScope.launch(Dispatchers.IO) {
            if (state.textContent.isNotBlank() || state.files.isNotEmpty()) {
                val modelMessage = ChatMessageEntity(
                    threadId = threadId,
                    role = "model",
                    cleanTextContent = state.textContent,
                    thinkingContent = state.thinkingContent.takeIf { it.isNotBlank() },
                    promptTokens = state.usageMetadata?.promptTokenCount ?: 0,
                    candidateTokens = state.usageMetadata?.candidatesTokenCount ?: 0,
                    thinkingTokens = state.usageMetadata?.thinkingTokenCount ?: 0,
                    modelId = settings.value.activeModelId,
                    finishReason = if (isCancelled) "CANCELLED" else null
                )
                val messageId = chatDao.insertMessage(modelMessage)
                for (file in state.files) {
                    chatDao.insertFile(
                        FileArtifactEntity(
                            messageId = messageId,
                            threadId = threadId,
                            fileName = file.fileName,
                            language = file.language,
                            mimeType = file.mimeType,
                            content = file.content,
                            lineCount = file.lineCount,
                            sizeBytes = file.sizeBytes
                        )
                    )
                }
            }
            _activeStreamState.value = ActiveStreamState(isStreaming = false)
        }
    }

    suspend fun exportCurrentThreadToMarkdown(): String = withContext(Dispatchers.IO) {
        val threadId = _activeThreadId.value ?: return@withContext ""
        val thread = chatDao.getThreadById(threadId) ?: return@withContext ""
        val messages = chatDao.getMessagesForThreadSync(threadId)
        val messagesWithFiles = messages.map { msg ->
            msg to chatDao.getFilesForMessageSync(msg.id)
        }
        MarkdownExporter.exportThread(thread, messagesWithFiles)
    }

    suspend fun testVertexConnection(): Result<String> {
        val currentSettings = settings.value
        return vertexClient.testConnection(
            serviceAccountJson = currentSettings.serviceAccountJson,
            region = currentSettings.region,
            modelId = currentSettings.activeModelId
        )
    }

    suspend fun refreshModelsFromApi(): Result<List<String>> {
        val currentSettings = settings.value
        val result = vertexClient.fetchAvailableModels(
            serviceAccountJson = currentSettings.serviceAccountJson,
            region = currentSettings.region
        )
        if (result.isSuccess) {
            val list = result.getOrNull()!!
            settingsDataStore.setApiModels(list.toSet())
        }
        return result
    }
}
