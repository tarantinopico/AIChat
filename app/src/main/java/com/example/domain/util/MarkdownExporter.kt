package com.example.domain.util

import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatThreadEntity
import com.example.data.local.entity.FileArtifactEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MarkdownExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun exportThread(
        thread: ChatThreadEntity,
        messagesWithFiles: List<Pair<ChatMessageEntity, List<FileArtifactEntity>>>
    ): String {
        val sb = StringBuilder()
        sb.append("# ${thread.title}\n\n")
        sb.append("- **Model:** `${thread.modelId}`\n")
        sb.append("- **Vytvořeno:** ${dateFormat.format(Date(thread.createdAt))}\n")
        sb.append("- **Aktualizováno:** ${dateFormat.format(Date(thread.updatedAt))}\n\n")
        sb.append("---\n\n")

        for ((message, files) in messagesWithFiles) {
            val roleName = if (message.role == "user") "🧑 Uživatel" else "🤖 Model (${message.modelId})"
            sb.append("### $roleName\n")
            sb.append("*${dateFormat.format(Date(message.createdAt))}*")

            if (message.isPinned) {
                sb.append(" • 📌 *Připnuto v kontextu*")
            }

            if (message.role == "model" && (message.promptTokens > 0 || message.candidateTokens > 0)) {
                sb.append(" • *Vstup: ${message.promptTokens} | Výstup: ${message.candidateTokens}")
                if (message.thinkingTokens > 0) {
                    sb.append(" | Thinking: ${message.thinkingTokens}")
                }
                sb.append(" tokenů*")
            }
            sb.append("\n\n")

            if (!message.thinkingContent.isNullOrBlank()) {
                sb.append("<details><summary>🧠 Zobrazit myšlenkový proces (Thinking)</summary>\n\n")
                sb.append(message.thinkingContent)
                sb.append("\n\n</details>\n\n")
            }

            if (message.cleanTextContent.isNotBlank()) {
                sb.append(message.cleanTextContent)
                sb.append("\n\n")
            }

            if (files.isNotEmpty()) {
                sb.append("#### 📁 Vygenerované soubory (${files.size}):\n\n")
                for (file in files) {
                    sb.append("##### 📄 `${file.fileName}` (${file.lineCount} řádků, ${file.sizeBytes / 1024} kB)\n\n")
                    sb.append("```${file.language}\n")
                    sb.append(file.content)
                    sb.append("\n```\n\n")
                }
            }

            if (message.isError) {
                sb.append("> ⚠️ **Chyba:** ${message.errorSummary.orEmpty()}\n")
                if (!message.rawErrorJson.isNullOrBlank()) {
                    sb.append(">\n> ```json\n")
                    message.rawErrorJson.lines().forEach { line ->
                        sb.append("> $line\n")
                    }
                    sb.append("> ```\n\n")
                }
            }

            sb.append("---\n\n")
        }

        return sb.toString()
    }
}
