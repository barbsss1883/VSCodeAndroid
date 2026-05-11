package com.vscodeandroid.editor.extensions

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ExtensionManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("extensions", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val builtInExtensions = listOf(
        Extension(
            id = "dracula-theme",
            name = "Dracula Theme",
            description = "El popular tema oscuro con colores vibrantes",
            version = "1.0.0",
            author = "Dracula Theme",
            type = ExtensionType.THEME,
            isBuiltIn = true,
            iconColor = "#BD93F9"
        ),
        Extension(
            id = "one-dark-pro",
            name = "One Dark Pro",
            description = "Tema oscuro inspirado en Atom One Dark",
            version = "1.0.0",
            author = "binaryify",
            type = ExtensionType.THEME,
            isBuiltIn = true,
            iconColor = "#61AFEF"
        ),
        Extension(
            id = "monokai-pro",
            name = "Monokai Pro",
            description = "El clásico tema Monokai con colores cálidos",
            version = "1.0.0",
            author = "Monokai",
            type = ExtensionType.THEME,
            isBuiltIn = true,
            iconColor = "#F92672"
        ),
        Extension(
            id = "solarized-dark",
            name = "Solarized Dark",
            description = "Paleta de colores de precisión calibrada para pantalla",
            version = "1.0.0",
            author = "Ethan Schoonover",
            type = ExtensionType.THEME,
            isBuiltIn = true,
            iconColor = "#268BD2"
        ),
        Extension(
            id = "github-dark",
            name = "GitHub Dark",
            description = "Tema oscuro de GitHub para código",
            version = "1.0.0",
            author = "GitHub",
            type = ExtensionType.THEME,
            isBuiltIn = true,
            iconColor = "#6E40C9"
        ),
        Extension(
            id = "night-owl",
            name = "Night Owl",
            description = "Tema hecho para noctámbulos",
            version = "1.0.0",
            author = "Sarah Drasner",
            type = ExtensionType.THEME,
            isBuiltIn = true,
            iconColor = "#011627"
        ),
        Extension(
            id = "snippets-javascript",
            name = "JavaScript Snippets",
            description = "Snippets útiles para JavaScript/TypeScript",
            version = "1.0.0",
            author = "VSCode Android",
            type = ExtensionType.SNIPPETS,
            isBuiltIn = true,
            iconColor = "#F0DB4F",
            snippets = mapOf("javascript" to listOf(
                Snippet("log", listOf("console.log(\${1:value});"), "console.log"),
                Snippet("fn", listOf("function \${1:name}(\${2:params}) {", "\t\${3:// body}", "}"), "function"),
                Snippet("afn", listOf("const \${1:name} = (\${2:params}) => {", "\t\${3:// body}", "};"), "arrow function"),
                Snippet("imp", listOf("import \${1:name} from '\${2:module}';"), "import"),
                Snippet("for", listOf("for (let \${1:i} = 0; \${1:i} < \${2:arr}.length; \${1:i}++) {", "\t\${3:// body}", "}"), "for loop"),
                Snippet("forEach", listOf("\${1:arr}.forEach((\${2:item}) => {", "\t\${3:// body}", "});"), "forEach"),
                Snippet("fetch", listOf("const res = await fetch('\${1:url}');", "const data = await res.json();"), "fetch")
            ))
        ),
        Extension(
            id = "snippets-python",
            name = "Python Snippets",
            description = "Snippets esenciales para Python",
            version = "1.0.0",
            author = "VSCode Android",
            type = ExtensionType.SNIPPETS,
            isBuiltIn = true,
            iconColor = "#3572A5",
            snippets = mapOf("python" to listOf(
                Snippet("def", listOf("def \${1:name}(\${2:params}):", "\t\${3:pass}"), "function"),
                Snippet("class", listOf("class \${1:Name}:", "\tdef __init__(self\${2:, args}):", "\t\t\${3:pass}"), "class"),
                Snippet("for", listOf("for \${1:item} in \${2:iterable}:", "\t\${3:pass}"), "for loop"),
                Snippet("if", listOf("if \${1:condition}:", "\t\${2:pass}"), "if statement"),
                Snippet("print", listOf("print(\${1:value})"), "print"),
                Snippet("import", listOf("import \${1:module}"), "import"),
                Snippet("from", listOf("from \${1:module} import \${2:name}"), "from import"),
                Snippet("list", listOf("[\${1:item} for \${2:item} in \${3:iterable}]"), "list comprehension")
            ))
        ),
        Extension(
            id = "snippets-kotlin",
            name = "Kotlin Snippets",
            description = "Snippets para desarrollo Android con Kotlin",
            version = "1.0.0",
            author = "VSCode Android",
            type = ExtensionType.SNIPPETS,
            isBuiltIn = true,
            iconColor = "#7F52FF",
            snippets = mapOf("kotlin" to listOf(
                Snippet("fun", listOf("fun \${1:name}(\${2:params}): \${3:Unit} {", "\t\${4:// body}", "}"), "function"),
                Snippet("class", listOf("class \${1:Name}(\${2:params}) {", "\t\${3:// body}", "}"), "class"),
                Snippet("data", listOf("data class \${1:Name}(", "\tval \${2:property}: \${3:Type}", ")"), "data class"),
                Snippet("for", listOf("for (\${1:item} in \${2:collection}) {", "\t\${3:// body}", "}"), "for loop"),
                Snippet("log", listOf("Log.d(\"\${1:TAG}\", \"\${2:message}\")"), "Log.d"),
                Snippet("coroutine", listOf("viewModelScope.launch {", "\t\${1:// body}", "}"), "coroutine"),
                Snippet("livedata", listOf("private val _\${1:name} = MutableLiveData<\${2:Type}>()", "val \${1:name}: LiveData<\${2:Type}> = _\${1:name}"), "LiveData")
            ))
        )
    )

    fun getAllExtensions(): List<Extension> {
        val installedIds = getInstalledIds()
        return builtInExtensions.map { it.copy(isInstalled = installedIds.contains(it.id)) }
    }

    fun getInstalledExtensions(): List<Extension> {
        val installedIds = getInstalledIds()
        return builtInExtensions.filter { installedIds.contains(it.id) }
    }

    fun install(extensionId: String) {
        val ids = getInstalledIds().toMutableSet()
        ids.add(extensionId)
        saveInstalledIds(ids)
    }

    fun uninstall(extensionId: String) {
        val ids = getInstalledIds().toMutableSet()
        ids.remove(extensionId)
        saveInstalledIds(ids)
    }

    fun isInstalled(extensionId: String): Boolean {
        return getInstalledIds().contains(extensionId)
    }

    fun getActiveTheme(): String {
        return prefs.getString("active_theme", "vs-dark") ?: "vs-dark"
    }

    fun setActiveTheme(themeId: String) {
        prefs.edit().putString("active_theme", themeId).apply()
    }

    fun getThemeMonacoId(extensionId: String): String = when (extensionId) {
        "dracula-theme" -> "dracula"
        "one-dark-pro" -> "one-dark-pro"
        "monokai-pro" -> "monokai"
        "solarized-dark" -> "solarized-dark"
        "github-dark" -> "github-dark"
        "night-owl" -> "night-owl"
        else -> "vs-dark"
    }

    private fun getInstalledIds(): Set<String> {
        val json = prefs.getString("installed_ids", null) ?: return emptySet()
        val type = object : TypeToken<Set<String>>() {}.type
        return try { gson.fromJson(json, type) } catch (e: Exception) { emptySet() }
    }

    private fun saveInstalledIds(ids: Set<String>) {
        prefs.edit().putString("installed_ids", gson.toJson(ids)).apply()
    }
}
