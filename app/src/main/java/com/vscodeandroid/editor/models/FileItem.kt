package com.vscodeandroid.editor.models

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

data class FileItem(
    val name: String,
    val uri: Uri? = null,
    val isDirectory: Boolean = false,
    val depth: Int = 0,
    var isExpanded: Boolean = false,
    val extension: String = "",
    val documentFile: DocumentFile? = null,
    val localFile: File? = null
)