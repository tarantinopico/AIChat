package com.example.ui.artifact

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.FileArtifactEntity
import com.example.ui.theme.CyanPrimaryDark
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileArtifactViewerDialog(
    file: FileArtifactEntity,
    onDismiss: () -> Unit,
    onDownload: (FileArtifactEntity) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isWebOrSvg = file.language.equals("html", ignoreCase = true) ||
            file.language.equals("svg", ignoreCase = true) ||
            file.fileName.endsWith(".html", ignoreCase = true) ||
            file.fileName.endsWith(".svg", ignoreCase = true)

    var currentTab by remember { mutableStateOf(if (isWebOrSvg) "PREVIEW" else "CODE") }
    val lines = remember(file.content) { file.content.lines() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = file.fileName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            Text(
                                text = "${file.language.uppercase()} • ${file.lineCount} řádků • ${(file.sizeBytes / 1024.0).let { String.format("%.1f", it) }} kB",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Zavřít")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(file.content))
                                Toast.makeText(context, "Obsah souboru byl zkopírován", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Kopírovat kód")
                        }
                        Button(
                            onClick = { onDownload(file) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                            Text("Stáhnout")
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
            ) {
                // Tab switcher for HTML/SVG
                if (isWebOrSvg) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = currentTab == "PREVIEW",
                            onClick = { currentTab = "PREVIEW" },
                            label = { Text("Živý náhled (WebView)") },
                            leadingIcon = {
                                Icon(Icons.Default.Visibility, contentDescription = null)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        FilterChip(
                            selected = currentTab == "CODE",
                            onClick = { currentTab = "CODE" },
                            label = { Text("Zdrojový kód") },
                            leadingIcon = {
                                Icon(Icons.Default.Code, contentDescription = null)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                // Main viewer content
                if (isWebOrSvg && currentTab == "PREVIEW") {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                                    webViewClient = WebViewClient()

                                    val finalHtml = if (file.fileName.endsWith(".svg", ignoreCase = true) || file.language == "svg") {
                                        "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'><style>body{margin:0;display:flex;justify-content:center;align-items:center;min-height:100vh;background:#0f172a;}svg{max-width:90vw;max-height:90vh;}</style></head><body>${file.content}</body></html>"
                                    } else {
                                        file.content
                                    }
                                    loadDataWithBaseURL("https://localhost/", finalHtml, "text/html", "UTF-8", null)
                                }
                            },
                            update = { webView ->
                                val finalHtml = if (file.fileName.endsWith(".svg", ignoreCase = true) || file.language == "svg") {
                                    "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'><style>body{margin:0;display:flex;justify-content:center;align-items:center;min-height:100vh;background:#0f172a;}svg{max-width:90vw;max-height:90vh;}</style></head><body>${file.content}</body></html>"
                                } else {
                                    file.content
                                }
                                webView.loadDataWithBaseURL("https://localhost/", finalHtml, "text/html", "UTF-8", null)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    // Virtualized LazyColumn Code Viewer with line numbers
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Slate950)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(lines) { index, lineText ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (index % 2 == 0) Slate950 else Slate900)
                                        .padding(vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Line Number Gutter
                                    Text(
                                        text = "${index + 1}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = Slate700,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier
                                            .width(54.dp)
                                            .padding(end = 12.dp)
                                    )

                                    // Vertical Gutter Separator
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .fillMaxHeight()
                                            .background(Slate800)
                                    )

                                    // Line Code Content
                                    Text(
                                        text = lineText.ifEmpty { " " },
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        color = if (lineText.trim().startsWith("//") || lineText.trim().startsWith("#")) Color(0xFF64748B) else Color(0xFFF1F5F9),
                                        modifier = Modifier
                                            .horizontalScroll(rememberScrollState())
                                            .padding(start = 12.dp, end = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
