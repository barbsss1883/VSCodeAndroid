package com.vscodeandroid.editor.utils

import android.util.Log
import com.vscodeandroid.editor.models.FileItem
import java.io.File

object FileUtils {

    private const val TAG = "FileUtils"
    private const val MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB limit

    fun readFile(file: File): String {
        return try {
            if (!file.exists()) {
                Log.e(TAG, "File does not exist: ${file.absolutePath}")
                return ""
            }
            if (!file.canRead()) {
                Log.e(TAG, "Cannot read file (permission denied): ${file.absolutePath}")
                return ""
            }
            if (file.length() > MAX_FILE_SIZE) {
                Log.w(TAG, "File too large (${file.length()} bytes): ${file.absolutePath}")
                return "// Archivo demasiado grande para mostrar (>${MAX_FILE_SIZE / 1024 / 1024}MB)"
            }
            file.readText(Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading file: ${file.absolutePath}", e)
            ""
        }
    }

    fun writeFile(file: File, content: String): Boolean {
        return try {
            if (!file.canWrite()) {
                Log.e(TAG, "Cannot write file (permission denied): ${file.absolutePath}")
                return false
            }
            file.writeText(content, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error writing file: ${file.absolutePath}", e)
            false
        }
    }

    fun canAccessFile(file: File): Boolean {
        return file.exists() && file.canRead()
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

    fun getLanguageFromExtension(extension: String): String = when (extension.lowercase()) {
        "js", "jsx"     -> "javascript"
        "ts", "tsx"     -> "typescript"
        "py"            -> "python"
        "kt", "kts"     -> "kotlin"
        "java"          -> "java"
        "html", "htm"   -> "html"
        "css", "scss"   -> "css"
        "json"          -> "json"
        "xml"           -> "xml"
        "md"            -> "markdown"
        "sh", "bash"    -> "shell"
        "c"             -> "c"
        "cpp", "cc"     -> "cpp"
        "cs"            -> "csharp"
        "go"            -> "go"
        "rs"            -> "rust"
        "php"           -> "php"
        "rb"            -> "ruby"
        "yaml", "yml"   -> "yaml"
        "toml"          -> "toml"
        "sql"           -> "sql"
        "gradle"        -> "groovy"
        "swift"         -> "swift"
        "dart"          -> "dart"
        else            -> "plaintext"
    }

    fun createNewFile(parent: File, name: String): File? {
        return try {
            val file = File(parent, name)
            if (!file.exists()) { file.createNewFile(); file } else null
        } catch (e: Exception) { null }
    }

    fun createNewFolder(parent: File, name: String): File? {
        return try {
            val folder = File(parent, name)
            if (!folder.exists()) { folder.mkdirs(); folder } else null
        } catch (e: Exception) { null }
    }

    fun deleteFile(file: File): Boolean {
        return try {
            if (file.isDirectory) file.deleteRecursively() else file.delete()
        } catch (e: Exception) { false }
    }
}
