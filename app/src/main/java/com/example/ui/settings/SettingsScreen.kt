package com.example.ui.settings

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.auth.ServiceAccountAuthManager
import com.example.ui.chat.ChatViewModel
import com.example.ui.chat.components.ErrorDiagnosticCard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyanPrimaryDark
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val modelStats by viewModel.modelTokenStats.collectAsStateWithLifecycle()
    val overallStats by viewModel.overallTokenStats.collectAsStateWithLifecycle()

    var saJsonText by remember(settings.serviceAccountJson) { mutableStateOf(settings.serviceAccountJson) }
    var newCustomModelName by remember { mutableStateOf("") }
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionTestResult by remember { mutableStateOf<String?>(null) }
    var connectionTestError by remember { mutableStateOf<Pair<String, String?>?>(null) }
    var isRefreshingModels by remember { mutableStateOf(false) }

    // File picker for .json service account key
    val jsonFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val content = stream.bufferedReader().readText()
                    saJsonText = content
                    scope.launch {
                        viewModel.settingsDataStore.updateServiceAccountJson(content)
                    }
                    Toast.makeText(context, "Soubor klíče byl úspěšně načten", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Chyba při čtení souboru: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val availableRegions = listOf("us-central1", "europe-west1", "global", "us-east4", "asia-northeast1")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nastavení Vertex AI", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // --- SECTION 1: Service Account JSON ---
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Autentizace (Service Account JSON)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Vložte JSON klíč vašeho Google Cloud Service Accountu s rolí 'Vertex AI User'. Token se podepisuje čistě v aplikaci přes RSA-SHA256.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = saJsonText,
                        onValueChange = {
                            saJsonText = it
                            scope.launch { viewModel.settingsDataStore.updateServiceAccountJson(it) }
                        },
                        label = { Text("Service Account JSON") },
                        placeholder = { Text("{\n  \"type\": \"service_account\",\n  \"project_id\": \"...\"\n}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipText = clipboardManager.getText()?.text.orEmpty()
                                if (clipText.isNotBlank()) {
                                    saJsonText = clipText
                                    scope.launch { viewModel.settingsDataStore.updateServiceAccountJson(clipText) }
                                    Toast.makeText(context, "Vloženo ze schránky", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Vložit text", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { jsonFilePickerLauncher.launch("application/json") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Načíst soubor", fontSize = 12.sp)
                        }
                    }

                    // Parsed credentials indicator
                    val parsedCreds = remember(saJsonText) {
                        try {
                            if (saJsonText.isNotBlank()) {
                                val json = JSONObject(saJsonText)
                                Pair(json.optString("project_id"), json.optString("client_email"))
                            } else null
                        } catch (_: Exception) {
                            null
                        }
                    }

                    if (parsedCreds != null && parsedCreds.first.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Projekt: ${parsedCreds.first}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Text("Klient: ${parsedCreds.second}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            isTestingConnection = true
                            connectionTestResult = null
                            connectionTestError = null
                            scope.launch {
                                val result = viewModel.testVertexConnection()
                                isTestingConnection = false
                                if (result.isSuccess) {
                                    connectionTestResult = result.getOrNull()
                                } else {
                                    val err = result.exceptionOrNull()!!
                                    val raw = (err as? com.example.data.remote.dto.VertexApiException)?.rawErrorJson
                                    connectionTestError = Pair(err.localizedMessage ?: "Neznámá chyba", raw)
                                }
                            }
                        },
                        enabled = !isTestingConnection && saJsonText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testuji spojení...")
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Otestovat spojení s Vertex AI")
                        }
                    }

                    if (connectionTestResult != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = connectionTestResult!!,
                            color = EmeraldSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (connectionTestError != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        ErrorDiagnosticCard(
                            errorSummary = connectionTestError!!.first,
                            rawErrorJson = connectionTestError!!.second
                        )
                    }
                }
            }

            // --- SECTION 2: Region & Model Management ---
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Region & Modely",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Google Cloud Region:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))

                    var regionExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = regionExpanded,
                        onExpandedChange = { regionExpanded = !regionExpanded }
                    ) {
                        OutlinedTextField(
                            value = settings.region,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Region") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = regionExpanded,
                            onDismissRequest = { regionExpanded = false }
                        ) {
                            availableRegions.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r) },
                                    onClick = {
                                        scope.launch { viewModel.settingsDataStore.updateRegion(r) }
                                        regionExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Aktivní model:", style = MaterialTheme.typography.labelMedium)
                        OutlinedButton(
                            onClick = {
                                isRefreshingModels = true
                                scope.launch {
                                    val res = viewModel.refreshModelsFromApi()
                                    isRefreshingModels = false
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Modely načteny z API", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Použit katalog modelů", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.height(34.dp)
                        ) {
                            if (isRefreshingModels) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Načíst z API", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    var modelExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = modelExpanded,
                        onExpandedChange = { modelExpanded = !modelExpanded }
                    ) {
                        OutlinedTextField(
                            value = settings.activeModelId,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Model ID") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = modelExpanded,
                            onDismissRequest = { modelExpanded = false }
                        ) {
                            settings.allAvailableModels.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m) },
                                    onClick = {
                                        viewModel.switchActiveModel(m)
                                        modelExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Přidat Custom Model ID:", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCustomModelName,
                            onValueChange = { newCustomModelName = it },
                            placeholder = { Text("např. gemini-4-argon") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newCustomModelName.isNotBlank()) {
                                    scope.launch {
                                        viewModel.settingsDataStore.addCustomModel(newCustomModelName)
                                        viewModel.switchActiveModel(newCustomModelName.trim())
                                        newCustomModelName = ""
                                    }
                                }
                            },
                            enabled = newCustomModelName.isNotBlank()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("Přidat")
                        }
                    }

                    // Custom models list with remove
                    if (settings.customModels.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Vlastní modely:", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        settings.customModels.forEach { customModel ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(customModel, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                                IconButton(
                                    onClick = { scope.launch { viewModel.settingsDataStore.removeCustomModel(customModel) } },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Smazat", tint = RoseError, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // --- SECTION 3: System Instructions & Generation Parameters ---
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Konfigurace generování & Kontext",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("System Instructions (Jedno přímé pole):", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = settings.systemInstructions,
                        onValueChange = { scope.launch { viewModel.settingsDataStore.updateSystemInstructions(it) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Temperature: ${String.format("%.2f", settings.temperature)}", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = settings.temperature,
                        onValueChange = { newVal ->
                            scope.launch {
                                viewModel.settingsDataStore.updateParameters(
                                    temperature = newVal,
                                    topP = settings.topP,
                                    topK = settings.topK,
                                    maxOutputTokens = settings.maxOutputTokens,
                                    contextLimit = settings.contextLimit
                                )
                            }
                        },
                        valueRange = 0.0f..2.0f
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Top-P: ${String.format("%.2f", settings.topP)}", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = settings.topP,
                        onValueChange = { newVal ->
                            scope.launch {
                                viewModel.settingsDataStore.updateParameters(
                                    temperature = settings.temperature,
                                    topP = newVal,
                                    topK = settings.topK,
                                    maxOutputTokens = settings.maxOutputTokens,
                                    contextLimit = settings.contextLimit
                                )
                            }
                        },
                        valueRange = 0.0f..1.0f
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Max Output Tokens: ${settings.maxOutputTokens}", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = settings.maxOutputTokens.toFloat(),
                        onValueChange = { newVal ->
                            scope.launch {
                                viewModel.settingsDataStore.updateParameters(
                                    temperature = settings.temperature,
                                    topP = settings.topP,
                                    topK = settings.topK,
                                    maxOutputTokens = newVal.toInt(),
                                    contextLimit = settings.contextLimit
                                )
                            }
                        },
                        valueRange = 512f..65536f,
                        steps = 7
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Kontextový limit (max zpráv v historii): ${settings.contextLimit}", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = settings.contextLimit.toFloat(),
                        onValueChange = { newVal ->
                            scope.launch {
                                viewModel.settingsDataStore.updateParameters(
                                    temperature = settings.temperature,
                                    topP = settings.topP,
                                    topK = settings.topK,
                                    maxOutputTokens = settings.maxOutputTokens,
                                    contextLimit = newVal.toInt()
                                )
                            }
                        },
                        valueRange = 5f..50f,
                        steps = 8
                    )
                }
            }

            // --- SECTION 4: Toggles (Streaming, Safety, Auto-Continue) ---
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Přepínače chování",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Streamování odpovědí (SSE)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(":streamGenerateContent?alt=sse", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.isStreaming,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    viewModel.settingsDataStore.updateToggles(
                                        isStreaming = checked,
                                        isBlockNoneSafety = settings.isBlockNoneSafety,
                                        autoContinueMode = settings.autoContinueMode
                                    )
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SafetySettings (BLOCK_NONE)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Vypne filtry blokování pro kód a vývoj", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.isBlockNoneSafety,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    viewModel.settingsDataStore.updateToggles(
                                        isStreaming = settings.isStreaming,
                                        isBlockNoneSafety = checked,
                                        autoContinueMode = settings.autoContinueMode
                                    )
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Režim dlouhého kódu (Auto-Continue):", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = settings.autoContinueMode == "AUTO",
                            onClick = {
                                scope.launch {
                                    viewModel.settingsDataStore.updateToggles(
                                        isStreaming = settings.isStreaming,
                                        isBlockNoneSafety = settings.isBlockNoneSafety,
                                        autoContinueMode = "AUTO"
                                    )
                                }
                            },
                            label = { Text("Automaticky navázat", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = settings.autoContinueMode == "MANUAL",
                            onClick = {
                                scope.launch {
                                    viewModel.settingsDataStore.updateToggles(
                                        isStreaming = settings.isStreaming,
                                        isBlockNoneSafety = settings.isBlockNoneSafety,
                                        autoContinueMode = "MANUAL"
                                    )
                                }
                            },
                            label = { Text("Tlačítko pokračovat", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // --- SECTION 5: $300 Free Trial Credit Dashboard ---
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Paid, contentDescription = null, tint = EmeraldSuccess)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Spotřeba a 300$ Free Trial kredit",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val promptM = overallStats.totalPromptTokens / 1_000_000.0
                    val candM = overallStats.totalCandidateTokens / 1_000_000.0
                    val estimatedCost = (promptM * settings.costPerMillionInputTokens) + (candM * settings.costPerMillionOutputTokens)
                    val remainingCredit = max(0.0, settings.initialCreditUsd - estimatedCost)
                    val progressRatio = (estimatedCost / settings.initialCreditUsd).toFloat().coerceIn(0f, 1f)

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Zbývající kredit:", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "$${String.format("%.2f", remainingCredit)} / $${String.format("%.2f", settings.initialCreditUsd)}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (remainingCredit > 50.0) EmeraldSuccess else RoseError
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progressRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = if (progressRatio > 0.8f) RoseError else CyanPrimary,
                                trackColor = Slate800
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Celkem vyčerpáno: $${String.format("%.4f", estimatedCost)} USD",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Celkem tokenů: ${overallStats.totalPromptTokens + overallStats.totalCandidateTokens} (Vstup: ${overallStats.totalPromptTokens}, Výstup: ${overallStats.totalCandidateTokens}, Thinking: ${overallStats.totalThinkingTokens})",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )

                    if (modelStats.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Rozpad spotřeby podle modelů:", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        modelStats.forEach { stat ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(stat.modelId, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("P: ${stat.totalPromptTokens} | C: ${stat.totalCandidateTokens}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
