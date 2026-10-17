package com.example.ui.models

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.model.ModelConfig
import com.example.ui.chat.ChatViewModel
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseError
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagerDialog(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = viewModel.settings.value

    var searchQuery by remember { mutableStateOf("") }
    var editingModel by remember { mutableStateOf<ModelConfig?>(null) }
    var testingModelId by remember { mutableStateOf<String?>(null) }
    var testResultDialog by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxHeight()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Správce modelů (Model CRUD)",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Konfigurace, parametry a podpora uvažování (thinking) pro každý model",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Zavřít")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Bar: Search & Add button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Hledat model...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            editingModel = ModelConfig(
                                id = "custom-gemini-model",
                                displayName = "Nový model",
                                supportsThinking = true,
                                thinkingParamKey = "thinkingLevel",
                                isCustom = true
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Přidat model", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val filteredModels = settings.models.filter {
                    searchQuery.isBlank() ||
                            it.id.contains(searchQuery, ignoreCase = true) ||
                            it.displayName.contains(searchQuery, ignoreCase = true)
                }

                if (filteredModels.isEmpty()) {
                    // Empty State
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Nebyly nalezeny žádné modely",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Zkuste změnit hledaný výraz nebo klikněte na 'Přidat model'.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredModels, key = { it.id }) { model ->
                            val isActive = model.id == settings.activeModelId
                            ModelCardItem(
                                model = model,
                                isActive = isActive,
                                isTesting = testingModelId == model.id,
                                onSelectAsActive = { viewModel.switchActiveModel(model.id) },
                                onToggleThinking = { enabled ->
                                    viewModel.setModelSupportsThinking(model.id, enabled)
                                },
                                onTest = {
                                    scope.launch {
                                        testingModelId = model.id
                                        val res = viewModel.testModel(model)
                                        testingModelId = null
                                        if (res.isSuccess) {
                                            testResultDialog = "✅ Úspěch!\n${res.getOrNull()}"
                                        } else {
                                            testResultDialog = "❌ Chyba spojení:\n${res.exceptionOrNull()?.localizedMessage}"
                                        }
                                    }
                                },
                                onEdit = { editingModel = model },
                                onDelete = { viewModel.deleteModel(model.id) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        scope.launch {
                            viewModel.settingsDataStore.resetModelsToDefault()
                            Toast.makeText(context, "Modely obnoveny do výchozího stavu", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Obnovit výchozí modely", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }

                    Button(onClick = onDismiss) {
                        Text("Hotovo")
                    }
                }
            }
        }
    }

    // Model Edit Dialog
    if (editingModel != null) {
        ModelEditDialog(
            initialModel = editingModel!!,
            onSave = { updated ->
                viewModel.saveModel(updated)
                editingModel = null
                Toast.makeText(context, "Model '${updated.displayName}' byl uložen", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { editingModel = null }
        )
    }

    // Test Result Alert Dialog
    if (testResultDialog != null) {
        AlertDialog(
            onDismissRequest = { testResultDialog = null },
            title = { Text("Výsledek testu spojení") },
            text = { Text(testResultDialog.orEmpty()) },
            confirmButton = {
                Button(onClick = { testResultDialog = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ModelCardItem(
    model: ModelConfig,
    isActive: Boolean,
    isTesting: Boolean,
    onSelectAsActive: () -> Unit,
    onToggleThinking: (Boolean) -> Unit,
    onTest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = model.displayName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "AKTIVNÍ",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = model.id,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Actions: Test, Edit, Delete
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = onTest, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.NetworkCheck, contentDescription = "Testovat spojení", tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                        }
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Upravit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }

                    if (model.isCustom) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Smazat", tint = RoseError, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            if (model.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = model.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges & Thinking toggle row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Thinking status pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (model.supportsThinking) IndigoAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (model.supportsThinking) IndigoAccent else Color.Transparent)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = null,
                                tint = if (model.supportsThinking) IndigoAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (model.supportsThinking) "Thinking (${model.thinkingParamKey})" else "Standard (bez uvažování)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (model.supportsThinking) IndigoAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Max: ${model.maxOutputTokens} tok",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Uvažování:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = model.supportsThinking,
                        onCheckedChange = onToggleThinking,
                        colors = SwitchDefaults.colors(checkedThumbColor = IndigoAccent, checkedTrackColor = IndigoAccent.copy(alpha = 0.5f))
                    )
                }
            }

            if (!isActive) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSelectAsActive,
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 2.dp)
                ) {
                    Text("Zvolit jako aktivní model", fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ModelEditDialog(
    initialModel: ModelConfig,
    onSave: (ModelConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var modelId by remember { mutableStateOf(initialModel.id) }
    var displayName by remember { mutableStateOf(initialModel.displayName) }
    var description by remember { mutableStateOf(initialModel.description) }
    var supportsThinking by remember { mutableStateOf(initialModel.supportsThinking) }
    var thinkingParamKey by remember { mutableStateOf(initialModel.thinkingParamKey) }
    var thinkingContainerKey by remember { mutableStateOf(initialModel.thinkingContainerKey) }
    var maxOutputTokensText by remember { mutableStateOf(initialModel.maxOutputTokens.toString()) }
    var temperatureText by remember { mutableStateOf(initialModel.temperature.toString()) }

    var paramKeyDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialModel.isCustom && initialModel.id.isBlank()) "Přidat nový model" else "Upravit model")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = modelId,
                    onValueChange = { modelId = it },
                    label = { Text("Model ID (Vertex AI API název)") },
                    placeholder = { Text("např. gemini-2.5-pro") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Zobrazovaný název") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Popis (volitelné)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Thinking Support Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Podporuje Thinking (Uvažování)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Pokud je vypnuto, thinkingConfig se do API nikdy nepošle.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = supportsThinking,
                        onCheckedChange = { supportsThinking = it }
                    )
                }

                // Configurable Argument Key for thinking!
                if (supportsThinking) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Název argumentu v JSON payloadu:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("Vyberte z přednastavených nebo zadejte vlastní do pole níže:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))

                            // FlowRow layout so chips wrap cleanly on multiple lines and never get squished
                            androidx.compose.foundation.layout.FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "premysleni_pro_api" to "Vlastní argument",
                                    "thinkingLevel" to "Gemini 2.5 (camelCase)",
                                    "thinking_level" to "Vertex (snake_case)",
                                    "thinkingBudget" to "Token Budget (číslo)"
                                ).forEach { (presetKey, hint) ->
                                    val isChosen = thinkingParamKey == presetKey
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isChosen) IndigoAccent else MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isChosen) IndigoAccent else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier.clickable { thinkingParamKey = presetKey }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            if (isChosen) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                            Text(
                                                text = presetKey,
                                                fontSize = 11.sp,
                                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isChosen) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = thinkingParamKey,
                                onValueChange = { thinkingParamKey = it },
                                label = { Text("Pole pro vlastní argument:") },
                                placeholder = { Text("např. premysleni_pro_api nebo cokoliv") },
                                supportingText = { Text("Zde můžete zadat jakýkoliv libovolný název argumentu.") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = thinkingContainerKey,
                                onValueChange = { thinkingContainerKey = it },
                                label = { Text("Obalovací objekt JSON (Container):") },
                                placeholder = { Text("výchozí: thinkingConfig (prázdné = kořen)") },
                                supportingText = { Text("Prázdné = zapíše argument přímo do generationConfig.") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = maxOutputTokensText,
                        onValueChange = { maxOutputTokensText = it },
                        label = { Text("Max Tokenů") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = temperatureText,
                        onValueChange = { temperatureText = it },
                        label = { Text("Temperature") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val maxTokens = maxOutputTokensText.toIntOrNull() ?: 8192
                val temp = temperatureText.toFloatOrNull() ?: 0.7f
                onSave(
                    initialModel.copy(
                        id = modelId.trim(),
                        displayName = displayName.trim().ifBlank { modelId.trim() },
                        description = description.trim(),
                        supportsThinking = supportsThinking,
                        thinkingParamKey = thinkingParamKey.trim().ifBlank { "thinkingLevel" },
                        thinkingContainerKey = thinkingContainerKey.trim(),
                        maxOutputTokens = maxTokens,
                        temperature = temp
                    )
                )
            }) {
                Text("Uložit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit")
            }
        }
    )
}
