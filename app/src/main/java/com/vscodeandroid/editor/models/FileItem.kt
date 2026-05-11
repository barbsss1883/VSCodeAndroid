package com.vscodeandroid.editor.models

import java.io.File

data class FileItem(
    val file: File,
    val name: String = file.name,
    val isDirectory: Boolean = file.isDirectory,
    val depth: Int = 0,
    var isExpanded: Boolean = false,
    val extension: String = file.extension.lowercase()
)
