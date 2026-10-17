package com.example.ui.chat

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ChatThreadEntity
import com.example.data.local.entity.FileArtifactEntity
import com.example.ui.artifact.FileArtifactViewerDialog
import com.example.ui.chat.components.FileArtifactCard
import com.example.ui.chat.components.MessageItem
import com.example.ui.chat.components.ThinkingBlock
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseError
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val allThreads by viewModel.allThreads.collectAsStateWithLifecycle()
    val activeThreadId by viewModel.activeThreadId.collectAsStateWithLifecycle()
    val messagesWithFiles by viewModel.currentMessagesWithFiles.collectAsStateWithLifecycle()
    val streamState by viewModel.activeStreamState.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var modelDropdownExpanded by remember { mutableStateOf(false) }
    var thinkingDropdownExpanded by remember { mutableStateOf(false) }

    // Dialog States
    var editingMessage by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var renamingThread by remember { mutableStateOf<ChatThreadEntity?>(null) }
    var activeViewerFile by remember { mutableStateOf<FileArtifactEntity?>(null) }
    var pendingDownloadFile by remember { mutableStateOf<FileArtifactEntity?>(null) }
    var pendingExportMarkdown by remember { mutableStateOf<String?>(null) }

    // Request notification permission for background generation on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // SAF Create Document for File Downloads
    val downloadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null && pendingDownloadFile != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(pendingDownloadFile!!.content.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Soubor '${pendingDownloadFile!!.fileName}' byl uložen", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Chyba při ukládání: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
        pendingDownloadFile = null
    }

    // SAF Create Document for Markdown Export
    val exportMarkdownLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null && pendingExportMarkdown != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(pendingExportMarkdown!!.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Konverzace byla exportována do Markdownu", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Chyba při exportu: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
        pendingExportMarkdown = null
    }

    val listState = rememberLazyListState()

    // Auto-scroll on new messages or stream updates
    LaunchedEffect(messagesWithFiles.size, streamState.textContent) {
        if (messagesWithFiles.isNotEmpty() || streamState.isStreaming) {
            val totalItems = messagesWithFiles.size + if (streamState.isStreaming) 1 else 0
            if (totalItems > 0) {
                listState.animateScrollToItem(totalItems - 1)
            }
        }
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(320.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Vertex AI Studio",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.createNewThread()
                            scope.launch { drawerState.close() }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_thread_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Nová konverzace")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Hledat vlákno...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val filteredThreads = allThreads.filter {
                        searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true)
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(filteredThreads, key = { it.id }) { thread ->
                            val isSelected = thread.id == activeThreadId
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable {
                                        viewModel.selectThread(thread.id)
                                        scope.launch { drawerState.close() }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = thread.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = thread.modelId,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { renamingThread = thread },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Přejmenovat", modifier = Modifier.size(14.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteThread(thread.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Smazat", tint = RoseError, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                onNavigateToSettings()
                            }
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Nastavení & Témata")
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Quick active model dropdown selector
                            Box {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { modelDropdownExpanded = true }
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = settings.activeModelId,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${settings.region} • ${settings.appTheme}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Změnit model")
                                }

                                DropdownMenu(
                                    expanded = modelDropdownExpanded,
                                    onDismissRequest = { modelDropdownExpanded = false }
                                ) {
                                    settings.allAvailableModels.forEach { model ->
                                        DropdownMenuItem(
                                            text = { Text(model) },
                                            onClick = {
                                                viewModel.switchActiveModel(model)
                                                modelDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Interactive Thinking Level Quick Dropdown Selector
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (settings.thinkingLevel == "OFF") MaterialTheme.colorScheme.surfaceVariant else IndigoAccent.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (settings.thinkingLevel == "OFF") MaterialTheme.colorScheme.outlineVariant else IndigoAccent.copy(alpha = 0.8f)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { thinkingDropdownExpanded = true }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Psychology,
                                            contentDescription = null,
                                            tint = if (settings.thinkingLevel == "OFF") MaterialTheme.colorScheme.onSurfaceVariant else IndigoAccent,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = when (settings.thinkingLevel.uppercase()) {
                                                "LOW" -> "Low"
                                                "HIGH", "MEDIUM" -> "High"
                                                else -> "Off"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (settings.thinkingLevel == "OFF") MaterialTheme.colorScheme.onSurfaceVariant else IndigoAccent
                                        )
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = if (settings.thinkingLevel == "OFF") MaterialTheme.colorScheme.onSurfaceVariant else IndigoAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = thinkingDropdownExpanded,
                                    onDismissRequest = { thinkingDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("❌ Vypnuto (Off)") },
                                        onClick = {
                                            viewModel.setThinkingLevel("OFF")
                                            thinkingDropdownExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("⚡ Nízká (Low) - rychlá odezva") },
                                        onClick = {
                                            viewModel.setThinkingLevel("LOW")
                                            thinkingDropdownExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("🧠 Vysoká (High) - hloubkový kód") },
                                        onClick = {
                                            viewModel.setThinkingLevel("HIGH")
                                            thinkingDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("drawer_menu_button")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Otevřít menu konverzací")
                        }
                    },
                    actions = {
                        // Export thread to markdown
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val md = viewModel.exportCurrentThreadToMarkdown()
                                    if (md.isNotBlank()) {
                                        pendingExportMarkdown = md
                                        exportMarkdownLauncher.launch("konverzace.md")
                                    } else {
                                        Toast.makeText(context, "Konverzace je prázdná", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Exportovat do Markdownu")
                        }

                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Nastavení")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .imePadding()
            ) {
                // Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(messagesWithFiles, key = { it.first.id }) { (message, files) ->
                        MessageItem(
                            message = message,
                            files = files,
                            onEditUserMessage = { id, text ->
                                editingMessage = Pair(id, text)
                            },
                            onRegenerate = { id ->
                                viewModel.regenerateModelResponse(id)
                            },
                            onTogglePin = { id, isPinned ->
                                viewModel.toggleMessagePin(id, isPinned)
                            },
                            onContinueGeneration = { id ->
                                viewModel.continueGeneration(id)
                            },
                            onOpenFile = { file ->
                                activeViewerFile = file
                            },
                            onDownloadFile = { file ->
                                pendingDownloadFile = file
                                downloadLauncher.launch(file.fileName)
                            }
                        )
                    }

                    // Live Streaming Card with HUD
                    if (streamState.isStreaming) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                // Live Streaming HUD header
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = if (streamState.isThinkingStage) IndigoAccent else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (streamState.autoContinuing) {
                                            "Auto-Continue: Navazuji psaní kódu..."
                                        } else if (streamState.isThinkingStage) {
                                            "🧠 Model plánuje myšlenkový proces (Thinking)..."
                                        } else {
                                            "⚡ Vertex AI generuje kód..."
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (streamState.isThinkingStage) IndigoAccent else MaterialTheme.colorScheme.primary
                                    )

                                    if (streamState.files.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "${streamState.files.sumOf { it.lineCount }} řádků",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (streamState.isThinkingStage) IndigoAccent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        if (streamState.thinkingContent.isNotBlank()) {
                                            ThinkingBlock(thinking = streamState.thinkingContent)
                                            Spacer(modifier = Modifier.height(10.dp))
                                        }

                                        if (streamState.textContent.isNotBlank()) {
                                            Text(
                                                text = streamState.textContent,
                                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Live updating file cards during stream!
                                        if (streamState.files.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = "Generované soubory (${streamState.files.size}):",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            streamState.files.forEach { extFile ->
                                                val dummyEntity = FileArtifactEntity(
                                                    messageId = 0,
                                                    threadId = 0,
                                                    fileName = extFile.fileName,
                                                    language = extFile.language,
                                                    mimeType = extFile.mimeType,
                                                    content = extFile.content,
                                                    lineCount = extFile.lineCount,
                                                    sizeBytes = extFile.sizeBytes
                                                )
                                                FileArtifactCard(
                                                    file = dummyEntity,
                                                    onOpen = { activeViewerFile = dummyEntity },
                                                    onDownload = {
                                                        pendingDownloadFile = dummyEntity
                                                        downloadLauncher.launch(dummyEntity.fileName)
                                                    }
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Input Area
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = { Text("Napište zadání pro Vertex AI...") },
                                maxLines = 6,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_input_field"),
                                shape = RoundedCornerShape(20.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            if (streamState.isStreaming) {
                                FilledIconButton(
                                    onClick = { viewModel.stopGeneration() },
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = RoseError),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = "Zastavit generování")
                                }
                            } else {
                                FilledIconButton(
                                    onClick = {
                                        val toSend = inputText
                                        inputText = ""
                                        viewModel.sendMessage(toSend)
                                    },
                                    enabled = inputText.isNotBlank(),
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("send_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Odeslat")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    // 1. Edit User Message Dialog
    if (editingMessage != null) {
        var editText by remember { mutableStateOf(editingMessage!!.second) }
        AlertDialog(
            onDismissRequest = { editingMessage = null },
            title = { Text("Upravit zprávu") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val (msgId, _) = editingMessage!!
                        editingMessage = null
                        viewModel.editUserMessage(msgId, editText)
                    }
                ) {
                    Text("Uložit a přegenerovat")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMessage = null }) {
                    Text("Zrušit")
                }
            }
        )
    }

    // 2. Rename Thread Dialog
    if (renamingThread != null) {
        var newTitle by remember { mutableStateOf(renamingThread!!.title) }
        AlertDialog(
            onDismissRequest = { renamingThread = null },
            title = { Text("Přejmenovat konverzaci") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameThread(renamingThread!!.id, newTitle)
                        renamingThread = null
                    }
                ) {
                    Text("Uložit")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingThread = null }) {
                    Text("Zrušit")
                }
            }
        )
    }

    // 3. Fullscreen File Artifact Viewer Dialog
    if (activeViewerFile != null) {
        FileArtifactViewerDialog(
            file = activeViewerFile!!,
            onDismiss = { activeViewerFile = null },
            onDownload = { fileToDownload ->
                pendingDownloadFile = fileToDownload
                downloadLauncher.launch(fileToDownload.fileName)
            }
        )
    }
}
