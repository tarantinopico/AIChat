package com.example.ui.chat

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.HorizontalDivider
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
import com.example.ui.models.ModelManagerDialog
import com.example.ui.thinking.ThinkingLevelManagerDialog
import com.example.ui.theme.CyanPrimaryDark
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseError
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
    var showModelManagerDialog by remember { mutableStateOf(false) }
    var showThinkingManagerDialog by remember { mutableStateOf(false) }
    var showQuickParamKeyDialog by remember { mutableStateOf(false) }

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

                    if (filteredThreads.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "Zatím žádné konverzace" else "Žádné výsledky hledání",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
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
                            val activeModel = settings.activeModelConfig
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
                                            text = activeModel.displayName,
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
                                    settings.models.forEach { model ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = if (model.supportsThinking) "🧠 " else "⚡ ",
                                                        fontSize = 12.sp
                                                    )
                                                    Text(
                                                        text = model.displayName,
                                                        fontWeight = if (model.id == settings.activeModelId) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            },
                                            onClick = {
                                                viewModel.switchActiveModel(model.id)
                                                modelDropdownExpanded = false
                                            }
                                        )
                                    }
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Spravovat modely (CRUD & Test)...", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        },
                                        onClick = {
                                            modelDropdownExpanded = false
                                            showModelManagerDialog = true
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Interactive Thinking Level Quick Dropdown Selector (if feature is enabled)
                            if (settings.enableThinkingFeature) {
                                val currentThinking = settings.activeThinkingLevel
                                val supportsThinking = activeModel.supportsThinking

                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (!supportsThinking || currentThinking.isOff) {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        } else {
                                            IndigoAccent.copy(alpha = 0.2f)
                                        },
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (!supportsThinking || currentThinking.isOff) {
                                                MaterialTheme.colorScheme.outlineVariant
                                            } else {
                                                IndigoAccent.copy(alpha = 0.8f)
                                            }
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
                                                tint = if (!supportsThinking || currentThinking.isOff) MaterialTheme.colorScheme.onSurfaceVariant else IndigoAccent,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = if (!supportsThinking) "Bez thinking" else currentThinking.label.take(14),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (!supportsThinking || currentThinking.isOff) MaterialTheme.colorScheme.onSurfaceVariant else IndigoAccent,
                                                maxLines = 1
                                            )
                                            Icon(
                                                Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = if (!supportsThinking || currentThinking.isOff) MaterialTheme.colorScheme.onSurfaceVariant else IndigoAccent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = thinkingDropdownExpanded,
                                        onDismissRequest = { thinkingDropdownExpanded = false }
                                    ) {
                                        if (!supportsThinking) {
                                            DropdownMenuItem(
                                                text = { Text("⚠️ Tento model nepodporuje uvažování", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                onClick = { }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("⚡ Zapnout uvažování pro ${activeModel.displayName}") },
                                                onClick = {
                                                    viewModel.setModelSupportsThinking(activeModel.id, true)
                                                    thinkingDropdownExpanded = false
                                                }
                                            )
                                            HorizontalDivider()
                                        }

                                        settings.thinkingLevels.forEach { level ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = level.label,
                                                        fontWeight = if (level.id == settings.activeThinkingLevelId) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.setThinkingLevel(level.id)
                                                    thinkingDropdownExpanded = false
                                                }
                                            )
                                        }

                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp), tint = IndigoAccent)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Column {
                                                        Text("Vlastní název argumentu...", fontWeight = FontWeight.Bold, color = IndigoAccent)
                                                        Text("Nyní: \"${activeModel.thinkingParamKey}\"", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            },
                                            onClick = {
                                                thinkingDropdownExpanded = false
                                                showQuickParamKeyDialog = true
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp), tint = IndigoAccent)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Spravovat úrovně (CRUD)...", fontWeight = FontWeight.Bold, color = IndigoAccent)
                                                }
                                            },
                                            onClick = {
                                                thinkingDropdownExpanded = false
                                                showThinkingManagerDialog = true
                                            }
                                        )
                                    }
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
                // Empty state or message list
                if (messagesWithFiles.isEmpty() && !streamState.isStreaming) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(IndigoAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = IndigoAccent,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Vertex AI Studio",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val activeModel = settings.activeModelConfig
                            Text(
                                text = "Aktivní: ${activeModel.displayName} • ${if (activeModel.supportsThinking && settings.enableThinkingFeature) "Thinking zapnuto" else "Standardní režim"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "Rychlé začátky pro generování kódu a souborů:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            val prompts = listOf(
                                "📁 Vytvoř Kotlin Foreground Service pro běh na pozadí",
                                "⚡ Napiš kompletní Jetpack Compose obrazovku s animacemi",
                                "🛠️ Navrhni bezpečný REST API klient s testy a error handlingem"
                            )

                            prompts.forEach { p ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .padding(vertical = 4.dp)
                                        .clickable { inputText = p }
                                ) {
                                    Text(
                                        text = p,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
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
                                },
                                onFixThinkingError = { msgId, threadId, modelId ->
                                    viewModel.fixThinkingErrorAndRetry(msgId, threadId, modelId)
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
                                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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

                                            if (streamState.files.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text = "Píši soubory v reálném čase:",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                streamState.files.forEach { file ->
                                                    val tempEntity = FileArtifactEntity(
                                                        messageId = streamState.targetMessageId ?: 0,
                                                        threadId = streamState.streamingThreadId ?: 0,
                                                        fileName = file.fileName,
                                                        language = file.language,
                                                        mimeType = file.mimeType,
                                                        content = file.content,
                                                        lineCount = file.lineCount,
                                                        sizeBytes = file.sizeBytes,
                                                        isComplete = file.isComplete
                                                    )
                                                    FileArtifactCard(
                                                        file = tempEntity,
                                                        onOpen = { activeViewerFile = tempEntity },
                                                        onDownload = {
                                                            pendingDownloadFile = tempEntity
                                                            downloadLauncher.launch(file.fileName)
                                                        },
                                                        onContinue = null
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
                }

                // Bottom Input Area & Controls
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        // Quick status bar: Model, Active Thinking, Token info
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "${settings.activeModelConfig.displayName} • ${settings.region}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (settings.enableThinkingFeature && settings.activeModelConfig.supportsThinking) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "🧠 ${settings.activeThinkingLevel.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = IndigoAccent
                                    )
                                }
                            }

                            if (streamState.isStreaming) {
                                Button(
                                    onClick = { viewModel.stopGeneration() },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Zastavit", fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Text Field and Send Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = { Text("Zadejte instrukci, prompt nebo požadavek na kód...") },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                                    .testTag("chat_input_field"),
                                maxLines = 6,
                                shape = RoundedCornerShape(20.dp)
                            )

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

    // --- DIALOGS ---

    // 1. Model Manager Dialog (CRUD)
    if (showModelManagerDialog) {
        ModelManagerDialog(
            viewModel = viewModel,
            onDismiss = { showModelManagerDialog = false }
        )
    }

    // 2. Thinking Level Manager Dialog (CRUD)
    if (showThinkingManagerDialog) {
        ThinkingLevelManagerDialog(
            viewModel = viewModel,
            onDismiss = { showThinkingManagerDialog = false }
        )
    }

    // 2b. Quick Argument Param Key Dialog
    if (showQuickParamKeyDialog) {
        val currentModel = settings.activeModelConfig
        var quickArgKey by remember(currentModel.id, currentModel.thinkingParamKey) {
            mutableStateOf(currentModel.thinkingParamKey)
        }
        var quickContainerKey by remember(currentModel.id, currentModel.thinkingContainerKey) {
            mutableStateOf(currentModel.thinkingContainerKey)
        }

        AlertDialog(
            onDismissRequest = { showQuickParamKeyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = IndigoAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Název argumentu pro thinking")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Nastavte název parametru v JSON payloadu pro model ${currentModel.displayName}:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = quickArgKey,
                        onValueChange = { quickArgKey = it },
                        label = { Text("Vlastní název argumentu:") },
                        placeholder = { Text("např. premysleni_pro_api, thinkingLevel...") },
                        supportingText = { Text("Zadejte libovolný název argumentu pro tento model.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Doporučené standardní názvy:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Presets using FlowRow so none get squished
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "thinkingLevel" to "Gemini 2.5",
                            "thinking_level" to "Vertex AI",
                            "thinkingBudget" to "Token Budget"
                        ).forEach { (presetKey, hint) ->
                            val isChosen = quickArgKey == presetKey
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isChosen) IndigoAccent else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (isChosen) IndigoAccent else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.clickable { quickArgKey = presetKey }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    if (isChosen) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = "$presetKey ($hint)",
                                        fontSize = 11.sp,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isChosen) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = quickContainerKey,
                        onValueChange = { quickContainerKey = it },
                        label = { Text("Obalovací JSON objekt (Container):") },
                        placeholder = { Text("výchozí: thinkingConfig") },
                        supportingText = { Text("Ponechte thinkingConfig nebo vymažte pro přímý zápis.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateActiveModelThinkingParamKey(quickArgKey, quickContainerKey)
                        showQuickParamKeyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent)
                ) {
                    Text("Uložit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickParamKeyDialog = false }) {
                    Text("Zrušit")
                }
            }
        )
    }

    // 3. Edit User Message Dialog
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

    // 4. Rename Thread Dialog
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

    // 5. Fullscreen File Artifact Viewer Dialog
    val liveViewerFile = remember(activeViewerFile, messagesWithFiles, streamState.files) {
        val active = activeViewerFile ?: return@remember null
        if (streamState.isStreaming) {
            val streamingMatch = streamState.files.find { it.fileName == active.fileName }
            if (streamingMatch != null) {
                return@remember active.copy(
                    content = streamingMatch.content,
                    lineCount = streamingMatch.lineCount,
                    sizeBytes = streamingMatch.sizeBytes,
                    isComplete = streamingMatch.isComplete
                )
            }
        }
        for ((_, files) in messagesWithFiles) {
            val dbMatch = files.find { it.id == active.id || (active.id == 0L && it.fileName == active.fileName) }
            if (dbMatch != null) return@remember dbMatch
        }
        active
    }

    if (liveViewerFile != null) {
        FileArtifactViewerDialog(
            file = liveViewerFile,
            onDismiss = { activeViewerFile = null },
            onDownload = { fileToDownload ->
                pendingDownloadFile = fileToDownload
                downloadLauncher.launch(fileToDownload.fileName)
            },
            onContinueGeneration = { msgId ->
                val targetId = if (msgId != 0L) msgId else liveViewerFile.messageId
                if (targetId != 0L) {
                    viewModel.continueGeneration(targetId)
                }
            }
        )
    }
}
