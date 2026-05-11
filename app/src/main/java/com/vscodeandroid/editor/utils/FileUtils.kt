package com.vscodeandroid.editor.utils

import android.content.Context
import com.vscodeandroid.editor.models.FileItem
import java.io.File

object FileUtils {
    fun readFile(file: File): String {
        return try {
            file.readText(Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    fun writeFile(file: File, content: String): Boolean {
        return try {
            file.writeText(content, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getFileTree(root: File, depth: Int = 0): List<FileItem> {
        val items = mutableListOf<FileItem>()
        if (!root.exists() || !root.isDirectory) return items

        val children = root.listFiles()?.sortedWith(
            compareBy({ !it.isDirectory }, { it.name.lowercase() })
        ) ?: return items

        for (child in children) {
            if (child.name.startsWith(".")) continue
            val item = FileItem(file = child, depth = depth)
            items.add(item)
            if (child.isDirectory && item.isExpanded) {
                items.addAll(getFileTree(child, depth + 1))
            }
        }
        return items
    }

    fun getLanguageIcon(extension: String): String = when (extension.lowercase()) {
        "js", "jsx" -> "js"
        "ts", "tsx" -> "ts"
        "py" -> "py"
        "kt" -> "kt"
        "java" -> "java"
        "html", "htm" -> "html"
        "css" -> "css"
        "json" -> "json"
        "xml" -> "xml"
        "md" -> "md"
        "sh" -> "sh"
        "c", "cpp" -> "cpp"
        "cs" -> "cs"
        "go" -> "go"
        "rs" -> "rust"
        "php" -> "php"
        "rb" -> "rb"
        else -> "file"
    }

    fun createNewFile(parent: File, name: String): File? {
        return try {
            val file = File(parent, name)
            if (!file.exists()) {
                file.createNewFile()
                file
            } else null
        } catch (e: Exception) { null }
    }

    fun createNewFolder(parent: File, name: String): File? {
        return try {
            val folder = File(parent, name)
            if (!folder.exists()) {
                folder.mkdirs()
                folder
            } else null
        } catch (e: Exception) { null }
    }

    fun deleteFile(file: File): Boolean {
        return try {
            if (file.isDirectory) file.deleteRecursively() else file.delete()
        } catch (e: Exception) { false }
    }
}
