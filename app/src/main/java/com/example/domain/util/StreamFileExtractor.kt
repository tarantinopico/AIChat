package com.example.domain.util

data class ExtractedFile(
    val index: Int,
    val fileName: String,
    val language: String,
    val mimeType: String,
    val content: String,
    val lineCount: Int,
    val sizeBytes: Long,
    val isComplete: Boolean
)

data class ParseResult(
    val cleanText: String,
    val files: List<ExtractedFile>
)

object StreamFileExtractor {

    private val FENCE_START = "```"

    fun parse(rawText: String): ParseResult {
        if (!rawText.contains(FENCE_START)) {
            return ParseResult(cleanText = rawText.trim(), files = emptyList())
        }

        val cleanTextBuilder = StringBuilder()
        val extractedFiles = mutableListOf<ExtractedFile>()

        var cursor = 0
        var fileIndex = 1

        while (cursor < rawText.length) {
            val startIndex = rawText.indexOf(FENCE_START, cursor)
            if (startIndex == -1) {
                // No more code fences, append rest of text
                cleanTextBuilder.append(rawText.substring(cursor))
                break
            }

            // Append text before the fence
            cleanTextBuilder.append(rawText.substring(cursor, startIndex))

            // Find end of the language header line
            val afterFence = startIndex + FENCE_START.length
            val lineEndIndex = rawText.indexOf('\n', afterFence)

            val rawLang: String
            val codeStart: Int
            if (lineEndIndex == -1) {
                // Fencing line is still being typed at the very end
                rawLang = rawText.substring(afterFence).trim()
                codeStart = rawText.length
            } else {
                rawLang = rawText.substring(afterFence, lineEndIndex).trim()
                codeStart = lineEndIndex + 1
            }

            // Find closing fence ```
            val closingIndex = rawText.indexOf(FENCE_START, codeStart)
            val codeContent: String
            val isComplete: Boolean

            if (closingIndex != -1) {
                codeContent = rawText.substring(codeStart, closingIndex)
                isComplete = true
                // Advance cursor past closing fence and newline if present
                var nextCursor = closingIndex + FENCE_START.length
                if (nextCursor < rawText.length && rawText[nextCursor] == '\n') {
                    nextCursor++
                }
                cursor = nextCursor
            } else {
                // Stream is still in progress inside this code block!
                codeContent = rawText.substring(codeStart)
                isComplete = false
                cursor = rawText.length
            }

            val language = normalizeLanguage(rawLang)
            val fileName = extractOrGenerateFileName(codeContent, language, fileIndex)
            val mimeType = resolveMimeType(language, fileName)

            val lines = if (codeContent.isEmpty()) 0 else codeContent.lines().size
            val bytes = codeContent.toByteArray(Charsets.UTF_8).size.toLong()

            extractedFiles.add(
                ExtractedFile(
                    index = fileIndex,
                    fileName = fileName,
                    language = language,
                    mimeType = mimeType,
                    content = codeContent,
                    lineCount = lines,
                    sizeBytes = bytes,
                    isComplete = isComplete
                )
            )

            fileIndex++
        }

        return ParseResult(
            cleanText = cleanTextBuilder.toString().trim(),
            files = extractedFiles
        )
    }

    private fun normalizeLanguage(raw: String): String {
        val lang = raw.split(" ", ":", "/").firstOrNull()?.trim()?.lowercase().orEmpty()
        return when (lang) {
            "kt", "kotlin" -> "kotlin"
            "html", "htm" -> "html"
            "js", "javascript" -> "javascript"
            "ts", "typescript" -> "typescript"
            "py", "python" -> "python"
            "json" -> "json"
            "svg" -> "svg"
            "css" -> "css"
            "xml" -> "xml"
            "sh", "bash", "shell", "zsh" -> "bash"
            "java" -> "java"
            "cpp", "c++", "cc" -> "cpp"
            "c" -> "c"
            "rs", "rust" -> "rust"
            "go", "golang" -> "go"
            "sql" -> "sql"
            "yaml", "yml" -> "yaml"
            "md", "markdown" -> "markdown"
            else -> if (lang.isNotBlank()) lang else "code"
        }
    }

    private fun extractOrGenerateFileName(content: String, language: String, index: Int): String {
        // Check first 3 lines for explicit file name hints, e.g. // filename: index.html
        val firstLines = content.lineSequence().take(3).toList()
        for (line in firstLines) {
            val trimmed = line.trim()
            val fileNameRegex = """(?:(?:#|//|<!--|/\*)\s*(?:filename|file|name|název souboru)\s*[:=]\s*([a-zA-Z0-9_\-\.]+))""".toRegex(RegexOption.IGNORE_CASE)
            val match = fileNameRegex.find(trimmed)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                if (candidate.isNotBlank() && candidate.contains('.')) {
                    return candidate
                }
            }
        }

        // Generate standard idiomatic file name based on language
        val suffix = if (index > 1) "_$index" else ""
        return when (language) {
            "html" -> if (index == 1) "index.html" else "page$suffix.html"
            "css" -> if (index == 1) "styles.css" else "style$suffix.css"
            "javascript" -> if (index == 1) "script.js" else "module$suffix.js"
            "typescript" -> if (index == 1) "app.ts" else "module$suffix.ts"
            "kotlin" -> if (index == 1) "MainActivity.kt" else "Component$suffix.kt"
            "java" -> if (index == 1) "Main.java" else "Class$suffix.java"
            "python" -> if (index == 1) "app.py" else "script$suffix.py"
            "json" -> if (index == 1) "data.json" else "config$suffix.json"
            "svg" -> if (index == 1) "vector.svg" else "graphic$suffix.svg"
            "xml" -> if (index == 1) "layout.xml" else "data$suffix.xml"
            "bash" -> if (index == 1) "run.sh" else "script$suffix.sh"
            "rust" -> if (index == 1) "main.rs" else "lib$suffix.rs"
            "go" -> if (index == 1) "main.go" else "app$suffix.go"
            "sql" -> if (index == 1) "query.sql" else "schema$suffix.sql"
            "yaml" -> if (index == 1) "config.yaml" else "spec$suffix.yaml"
            "markdown" -> "document$suffix.md"
            else -> "source$suffix.txt"
        }
    }

    fun resolveMimeType(language: String, fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when {
            extension == "html" || language == "html" -> "text/html"
            extension == "svg" || language == "svg" -> "image/svg+xml"
            extension == "json" || language == "json" -> "application/json"
            extension == "css" || language == "css" -> "text/css"
            extension == "js" || language == "javascript" -> "text/javascript"
            extension == "xml" || language == "xml" -> "application/xml"
            extension == "md" || language == "markdown" -> "text/markdown"
            else -> "text/plain"
        }
    }
}
