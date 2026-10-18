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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
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
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.example.ui.models.ModelManagerDialog
import com.example.ui.thinking.ThinkingLevelManagerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.chat.ChatViewModel
import com.example.ui.chat.components.ErrorDiagnosticCard
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.CyberpunkNeonCyan
import com.example.ui.theme.CyberpunkNeonPink
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.ObsidianSilver
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var showModelManagerDialog by remember { mutableStateOf(false) }
    var showThinkingManagerDialog by remember { mutableStateOf(false) }

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

            // --- SECTION 1: Barevná témata (Color Themes) ---
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Barevná témata",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Vyberte si styl prostředí podle vašich preferencí a displeje:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val themes = listOf(
                        AppThemeMode.SLATE to Pair(Color(0xFF0F172A), Color(0xFF38BDF8)),
                        AppThemeMode.OBSIDIAN to Pair(Color(0xFF000000), ObsidianSilver),
                        AppThemeMode.CYBERPUNK to Pair(Color(0xFF0A0612), CyberpunkNeonCyan),
                        AppThemeMode.EMERALD to Pair(Color(0xFF04140D), EmeraldMint)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        themes.forEach { (themeMode, colors) ->
                            val isSelected = settings.appTheme.equals(themeMode.name, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.updateAppTheme(themeMode.name) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Theme color preview circles
                                    Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(colors.first)
                                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(colors.second)
                                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = themeMode.displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = themeMode.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Aktivní",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- SECTION 2: Úroveň Thinking (Uvažování modelu) ---
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = IndigoAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Úroveň Thinking (Uvažování modelu)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Master Switch to completely disable/enable thinking
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Povolit volbu Thinking v aplikaci",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Vypněte pro úplné skrytí výběru uvažování z chatu i generování.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.enableThinkingFeature,
                                onCheckedChange = { viewModel.updateEnableThinkingFeature(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = IndigoAccent, checkedTrackColor = IndigoAccent.copy(alpha = 0.5f))
                            )
                        }
                    }

                    if (settings.enableThinkingFeature) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // --- KARTA PRO PŘÍMÉ NASTAVENÍ VLASTNÍHO ARGUMENTU ---
                        val activeModel = settings.activeModelConfig
                        var customArgText by remember(activeModel.id, activeModel.thinkingParamKey) {
                            mutableStateOf(activeModel.thinkingParamKey)
                        }
                        var customContainerText by remember(activeModel.id, activeModel.thinkingContainerKey) {
                            mutableStateOf(activeModel.thinkingContainerKey)
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Tune, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Název argumentu v JSON (pro model ${activeModel.displayName})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = IndigoAccent
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = customArgText,
                                    onValueChange = {
                                        customArgText = it
                                        viewModel.updateActiveModelThinkingParamKey(it)
                                    },
                                    label = { Text("Vlastní název argumentu pro API:") },
                                    placeholder = { Text("např. premysleni_pro_api, my_custom_param") },
                                    supportingText = { Text("Zadejte libovolný název argumentu pro model.") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Doporučené standardní názvy pro Vertex AI / Gemini:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                // Responsive FlowRow for presets so NONE get squished or clipped
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val presets = listOf(
                                        "thinkingLevel" to "Gemini 2.5",
                                        "thinking_level" to "Vertex AI",
                                        "thinkingBudget" to "Token Budget"
                                    )
                                    presets.forEach { (presetKey, hint) ->
                                        val isChosen = customArgText == presetKey
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isChosen) IndigoAccent else MaterialTheme.colorScheme.surfaceVariant,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isChosen) IndigoAccent else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            modifier = Modifier.clickable {
                                                customArgText = presetKey
                                                viewModel.updateActiveModelThinkingParamKey(presetKey)
                                                Toast.makeText(context, "Nastaven klíč: $presetKey", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                if (isChosen) {
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(13.dp)
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

                                Spacer(modifier = Modifier.height(4.dp))

                                OutlinedTextField(
                                    value = customContainerText,
                                    onValueChange = {
                                        customContainerText = it
                                        viewModel.updateActiveModelThinkingContainerKey(it)
                                    },
                                    label = { Text("Obalovací JSON objekt (Container):") },
                                    placeholder = { Text("výchozí: thinkingConfig (nebo nechte prázdné pro kořen)") },
                                    supportingText = { Text("Ponechte 'thinkingConfig', nebo vymažte pro přímý zápis do payloadu.") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Live JSON Preview
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E1E2E),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val currentVal = settings.activeThinkingLevel.value.ifBlank { "HIGH" }
                                    val previewJson = if (settings.activeThinkingLevel.isOff) {
                                        "// Uvažování je pro aktivní úroveň vypnuto (žádný thinking objekt se neposílá)"
                                    } else if (customContainerText.isBlank()) {
                                        "\"generationConfig\": {\n  \"${customArgText.ifBlank { "thinkingLevel" }}\": \"$currentVal\"\n}"
                                    } else {
                                        "\"generationConfig\": {\n  \"$customContainerText\": {\n    \"${customArgText.ifBlank { "thinkingLevel" }}\": \"$currentVal\"\n  }\n}"
                                    }
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Živý náhled JSON payloadu:",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFA6ADC8)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = previewJson,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFFA6E3A1)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Aktivní úroveň: ${settings.activeThinkingLevel.label}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = IndigoAccent
                            )

                            Button(
                                onClick = { showThinkingManagerDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                                modifier = Modifier.height(34.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Správce úrovní (CRUD)", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            settings.thinkingLevels.forEach { level ->
                                val isSelected = level.id == settings.activeThinkingLevelId
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) IndigoAccent else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setThinkingLevel(level.id) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = level.label,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            if (level.description.isNotBlank()) {
                                                Text(
                                                    text = level.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "Aktivní",
                                                tint = IndigoAccent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- SECTION 3: Service Account JSON ---
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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

            // --- SECTION 4: Region & Správa Modelů ---
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Region & Správa Modelů",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Button(
                            onClick = { showModelManagerDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.height(34.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Správce modelů (CRUD)", fontSize = 12.sp)
                        }
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
                            value = "${settings.activeModelConfig.displayName} (${settings.activeModelId})",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Model") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = modelExpanded,
                            onDismissRequest = { modelExpanded = false }
                        ) {
                            settings.models.forEach { m ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(if (m.supportsThinking) "🧠 " else "⚡ ")
                                            Text(m.displayName, fontWeight = if (m.id == settings.activeModelId) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    },
                                    onClick = {
                                        viewModel.switchActiveModel(m.id)
                                        modelExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    val activeModel = settings.activeModelConfig
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (activeModel.supportsThinking) Icons.Default.Psychology else Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = if (activeModel.supportsThinking) IndigoAccent else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Režim uvažování: " + if (activeModel.supportsThinking) "Podporováno (${activeModel.thinkingParamKey})" else "Standardní (bez uvažování)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeModel.supportsThinking) IndigoAccent else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Max tokenů: ${activeModel.maxOutputTokens} • Teplota: ${activeModel.temperature}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // --- SECTION 5: System Instructions & Generation Parameters ---
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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

            // --- SECTION 6: Toggles (Streaming, Safety, Auto-Continue) ---
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatické pokračování (Auto-Continue)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "Při dosažení limitu délky tokenů (MAX_TOKENS) automaticky naváže v psaní přímo v artefaktu",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.autoContinueMode == "AUTO",
                            onCheckedChange = { isAuto ->
                                scope.launch {
                                    viewModel.settingsDataStore.updateAutoContinueSettings(
                                        autoContinueMode = if (isAuto) "AUTO" else "MANUAL",
                                        maxAutoContinueSteps = settings.maxAutoContinueSteps
                                    )
                                }
                            }
                        )
                    }

                    AnimatedVisibility(visible = settings.autoContinueMode == "AUTO") {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                text = "Maximální počet automatických navázání:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(1, 2, 3, 5).forEach { steps ->
                                    FilterChip(
                                        selected = settings.maxAutoContinueSteps == steps,
                                        onClick = {
                                            scope.launch {
                                                viewModel.settingsDataStore.updateAutoContinueSettings(
                                                    autoContinueMode = "AUTO",
                                                    maxAutoContinueSteps = steps
                                                )
                                            }
                                        },
                                        label = {
                                            Text(if (steps == 3) "3× (doporučeno)" else "$steps×", fontSize = 11.sp)
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (settings.autoContinueMode == "AUTO") {
                            "⚡ Auto-Continue je AKTIVNÍ: model automaticky dopíše kód do stejného souboru až ${settings.maxAutoContinueSteps}× za sebou."
                        } else {
                            "👆 Manuální režim: v chatu a v prohlížeči artefaktu se při limitu tokenů zobrazí tlačítko 'Pokračovat v kódu'."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // --- SECTION 7: $300 Free Trial Credit Dashboard ---
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (progressRatio > 0.8f) RoseError else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
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
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
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

    if (showModelManagerDialog) {
        ModelManagerDialog(
            viewModel = viewModel,
            onDismiss = { showModelManagerDialog = false }
        )
    }

    if (showThinkingManagerDialog) {
        ThinkingLevelManagerDialog(
            viewModel = viewModel,
            onDismiss = { showThinkingManagerDialog = false }
        )
    }
}
