package com.example.ui.thinking

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.model.ThinkingLevelConfig
import com.example.ui.chat.ChatViewModel
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseError
import kotlinx.coroutines.launch

@Composable
fun ThinkingLevelManagerDialog(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = viewModel.settings.value

    var editingLevel by remember { mutableStateOf<ThinkingLevelConfig?>(null) }

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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Správce Thinking Levelů (CRUD)",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Vlastní úrovně, hodnoty i názvy argumentů (např. premysleni_pro_api)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Zavřít")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Master Switch to Enable/Disable Thinking Completely
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
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
                                text = "Povolit funkci Thinking v aplikaci",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Pokud vypnete, uvažování bude zcela deaktivováno a odznáček v chatu skryt.",
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

                Spacer(modifier = Modifier.height(14.dp))

                // Add button & title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Dostupné úrovně (${settings.thinkingLevels.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Button(
                        onClick = {
                            editingLevel = ThinkingLevelConfig(
                                id = "CUSTOM_${System.currentTimeMillis() % 1000}",
                                label = "Vlastní úroveň",
                                description = "Vlastní konfigurace uvažování",
                                value = "HIGH",
                                customParamKeyOverride = "",
                                isOff = false,
                                isBudgetNumber = false
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Přidat úroveň", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (settings.thinkingLevels.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Žádné definované úrovně", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Vytvořte si vlastní úroveň uvažování nebo obnovte výchozí.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(onClick = {
                                scope.launch { viewModel.settingsDataStore.resetThinkingLevelsToDefault() }
                            }) {
                                Text("Obnovit výchozí úrovně")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(settings.thinkingLevels, key = { it.id }) { level ->
                            val isActive = level.id == settings.activeThinkingLevelId
                            ThinkingLevelCardItem(
                                level = level,
                                defaultModelKey = settings.activeModelConfig.thinkingParamKey,
                                isActive = isActive,
                                onSelect = { viewModel.setThinkingLevel(level.id) },
                                onEdit = { editingLevel = level },
                                onDelete = { viewModel.deleteThinkingLevel(level.id) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        scope.launch {
                            viewModel.settingsDataStore.resetThinkingLevelsToDefault()
                            Toast.makeText(context, "Úrovně uvažování obnoveny", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Obnovit výchozí", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }

                    Button(onClick = onDismiss) {
                        Text("Zavřít")
                    }
                }
            }
        }
    }

    if (editingLevel != null) {
        ThinkingLevelEditDialog(
            initialLevel = editingLevel!!,
            activeModelParamKey = settings.activeModelConfig.thinkingParamKey,
            onSave = { updated ->
                viewModel.saveThinkingLevel(updated)
                editingLevel = null
                Toast.makeText(context, "Úroveň '${updated.label}' uložena", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { editingLevel = null }
        )
    }
}

@Composable
fun ThinkingLevelCardItem(
    level: ThinkingLevelConfig,
    defaultModelKey: String,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) IndigoAccent else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = level.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (isActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = IndigoAccent
                        ) {
                            Text(
                                text = "AKTIVNÍ",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                if (level.description.isNotBlank()) {
                    Text(
                        text = level.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                val effectiveKey = level.customParamKeyOverride.ifBlank { defaultModelKey }
                Text(
                    text = if (level.isOff) {
                        "Payload: Vynecháno (OFF)"
                    } else {
                        "Klíč: \"$effectiveKey\" • Hodnota: \"${level.value}\"" + if (level.isBudgetNumber) " (číslo)" else " (text)"
                    },
                    fontSize = 11.sp,
                    color = if (level.isOff) MaterialTheme.colorScheme.onSurfaceVariant else IndigoAccent
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isActive) {
                    TextButton(onClick = onSelect) {
                        Text("Zvolit", fontSize = 12.sp)
                    }
                }

                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Upravit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }

                if (level.id != "OFF") {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Smazat", tint = RoseError, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThinkingLevelEditDialog(
    initialLevel: ThinkingLevelConfig,
    activeModelParamKey: String,
    onSave: (ThinkingLevelConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var id by remember { mutableStateOf(initialLevel.id) }
    var label by remember { mutableStateOf(initialLevel.label) }
    var description by remember { mutableStateOf(initialLevel.description) }
    var value by remember { mutableStateOf(initialLevel.value) }
    var customParamKeyOverride by remember { mutableStateOf(initialLevel.customParamKeyOverride) }
    var customContainerKeyOverride by remember { mutableStateOf(initialLevel.customContainerKeyOverride) }
    var isOff by remember { mutableStateOf(initialLevel.isOff) }
    var isBudgetNumber by remember { mutableStateOf(initialLevel.isBudgetNumber) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialLevel.id.isBlank()) "Přidat úroveň uvažování" else "Upravit úroveň uvažování") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = id,
                    onValueChange = { id = it },
                    label = { Text("ID úrovně (identifikátor)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Zobrazovaný název (např. 🧠 Vysoká)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Popis (volitelný)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Is Off switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vypnout uvažování (isOff)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Při volbě této úrovně se thinkingConfig vůbec neposílá.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isOff, onCheckedChange = { isOff = it })
                }

                if (!isOff) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("Hodnota odesílaná do API") },
                        placeholder = { Text("např. LOW, HIGH, nebo 4096") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Customizable Argument Key (e.g. premysleni_pro_api)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Vlastní název klíče argumentu:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("Výchozí pro aktivní model je: $activeModelParamKey", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))

                            // FlowRow layout so chips wrap cleanly and never get squished or truncated
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "premysleni_pro_api" to "Vlastní",
                                    "thinkingLevel" to "Gemini 2.5",
                                    "thinking_level" to "Vertex",
                                    "thinkingBudget" to "Token Budget"
                                ).forEach { (presetKey, _) ->
                                    val isChosen = customParamKeyOverride == presetKey
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isChosen) IndigoAccent else MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isChosen) IndigoAccent else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier.clickable { customParamKeyOverride = presetKey }
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
                                                    modifier = Modifier.size(12.dp)
                                                )
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
                                value = customParamKeyOverride,
                                onValueChange = { customParamKeyOverride = it },
                                label = { Text("Pole pro vlastní název argumentu:") },
                                placeholder = { Text("např. premysleni_pro_api (prázdné = výchozí z modelu)") },
                                supportingText = { Text("Zadejte libovolný název nebo zvolte čip výše.") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = customContainerKeyOverride,
                                onValueChange = { customContainerKeyOverride = it },
                                label = { Text("Obalovací objekt JSON (Container override):") },
                                placeholder = { Text("ponechte prázdné pro výchozí z modelu") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Posílat jako číslo (budget integer)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Zapněte, pokud model vyžaduje thinkingBudget jako celé číslo namísto řetězce.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = isBudgetNumber, onCheckedChange = { isBudgetNumber = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    initialLevel.copy(
                        id = id.trim(),
                        label = label.trim().ifBlank { id.trim() },
                        description = description.trim(),
                        value = if (isOff) "" else value.trim(),
                        customParamKeyOverride = if (isOff) "" else customParamKeyOverride.trim(),
                        customContainerKeyOverride = if (isOff) "" else customContainerKeyOverride.trim(),
                        isOff = isOff,
                        isBudgetNumber = if (isOff) false else isBudgetNumber
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
