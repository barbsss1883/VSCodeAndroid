package com.vscodeandroid.editor.models

data class AppSettings(
    val theme: String = "vs-dark",
    val fontSize: Int = 14,
    val tabSize: Int = 4,
    val wordWrap: Boolean = true,
    val minimap: Boolean = true,
    val lineNumbers: Boolean = true,
    val autoSave: Boolean = false
)
