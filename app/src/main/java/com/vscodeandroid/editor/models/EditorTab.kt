package com.vscodeandroid.editor.models

import java.io.File

data class EditorTab(
    val id: String = java.util.UUID.randomUUID().toString(),
    val file: File,
    var content: String = "",
    var isModified: Boolean = false,
    val language: String = detectLanguage(file.extension)
) {
    companion object {
        fun detectLanguage(extension: String): String = when (extension.lowercase()) {
            "js", "jsx" -> "javascript"
            "ts", "tsx" -> "typescript"
            "py" -> "python"
            "kt", "kts" -> "kotlin"
            "java" -> "java"
            "html", "htm" -> "html"
            "css" -> "css"
            "scss", "sass" -> "scss"
            "json" -> "json"
            "xml" -> "xml"
            "md", "markdown" -> "markdown"
            "sh", "bash" -> "shell"
            "c" -> "c"
            "cpp", "cc", "cxx" -> "cpp"
            "cs" -> "csharp"
            "go" -> "go"
            "rs" -> "rust"
            "php" -> "php"
            "rb" -> "ruby"
            "swift" -> "swift"
            "yaml", "yml" -> "yaml"
            "sql" -> "sql"
            "r" -> "r"
            "dart" -> "dart"
            else -> "plaintext"
        }
    }
}
