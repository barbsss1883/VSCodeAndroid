package com.vscodeandroid.editor.extensions

data class Extension(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val author: String,
    val type: ExtensionType,
    val isInstalled: Boolean = false,
    val isBuiltIn: Boolean = false,
    val iconColor: String = "#007ACC",
    val themeData: Map<String, Any>? = null,
    val snippets: Map<String, List<Snippet>>? = null
)

enum class ExtensionType {
    THEME, SNIPPETS, LANGUAGE_SUPPORT, FORMATTER
}

data class Snippet(
    val prefix: String,
    val body: List<String>,
    val description: String
)
