package com.vscodeandroid.editor

import android.app.Application
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.vscodeandroid.editor.models.AppSettings
import com.vscodeandroid.editor.models.EditorTab
import com.vscodeandroid.editor.models.FileItem
import com.vscodeandroid.editor.utils.FileUtils
import com.vscodeandroid.editor.utils.PreferencesManager
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "EditorViewModel"

    private val prefs = PreferencesManager(application)

    private val _tabs =
        MutableLiveData<List<EditorTab>>(emptyList())

    val tabs: LiveData<List<EditorTab>> = _tabs

    private val _activeTab =
        MutableLiveData<EditorTab?>(null)

    val activeTab: LiveData<EditorTab?> = _activeTab

    private val _settings =
        MutableLiveData(prefs.loadSettings())

    val settings: LiveData<AppSettings> = _settings

    private val _fileItems =
        MutableLiveData<List<FileItem>>(emptyList())

    val fileItems: LiveData<List<FileItem>> =
        _fileItems

    private val _errorEvent =
        MutableLiveData<String?>()

    val errorEvent: LiveData<String?> =
        _errorEvent

    private var rootDir: File? = null

    private val fileItemsCache =
        mutableListOf<FileItem>()

    /*
     LOCAL FILESYSTEM
    */

    fun openFolder(dir: File) {

        if (!dir.exists()) {

            _errorEvent.value =
                "La carpeta no existe: ${dir.absolutePath}"

            return
        }

        if (!dir.canRead()) {

            _errorEvent.value =
                "Sin permiso para leer la carpeta"

            return
        }

        rootDir = dir

        prefs.saveLastOpenedPath(dir.absolutePath)

        refreshFileTree()

        Log.d(TAG, "Opened folder: ${dir.absolutePath}")
    }

    fun refreshFileTree() {

        val root = rootDir ?: return

        viewModelScope.launch(Dispatchers.IO) {

            val items = buildFileTree(
                root,
                fileItemsCache.associate {

                    (it.localFile?.path
                        ?: it.uri.toString()) to it.isExpanded
                }
            )

            withContext(Dispatchers.Main) {

                fileItemsCache.clear()
                fileItemsCache.addAll(items)

                _fileItems.value = items.toList()
            }
        }
    }

    private fun buildFileTree(
        dir: File,
        expandedState: Map<String, Boolean>,
        depth: Int = 0
    ): List<FileItem> {

        val result = mutableListOf<FileItem>()

        val children = try {

            dir.listFiles()?.sortedWith(
                compareBy(
                    { !it.isDirectory },
                    { it.name.lowercase() }
                )
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Cannot list files in ${dir.absolutePath}",
                e
            )

            null
        } ?: return result

        for (child in children) {

            if (child.name.startsWith(".")) {
                continue
            }

            val isExpanded =
                expandedState[child.path] ?: false

            val item = FileItem(
                name = child.name,
                localFile = child,
                uri = null,
                isDirectory = child.isDirectory,
                depth = depth,
                isExpanded = isExpanded,
                extension = child.extension.lowercase()
            )

            result.add(item)

            if (child.isDirectory && isExpanded) {

                result.addAll(
                    buildFileTree(
                        child,
                        expandedState,
                        depth + 1
                    )
                )
            }
        }

        return result
    }

    /*
     SAF / DOCUMENT TREE
    */

    fun openDocumentTree(root: DocumentFile) {

        val items = root.listFiles()
            .sortedWith(
                compareBy<DocumentFile> {
                    !it.isDirectory
                }.thenBy {
                    it.name?.lowercase() ?: ""
                }
            )
            .map {

                FileItem(
                    name = it.name ?: "unknown",
                    uri = it.uri,
                    isDirectory = it.isDirectory,
                    depth = 0,
                    extension = it.name
                        ?.substringAfterLast('.', "")
                        ?.lowercase()
                        ?: "",
                    documentFile = it
                )
            }

        fileItemsCache.clear()
        fileItemsCache.addAll(items)

        _fileItems.value = items
    }

    fun toggleDirectory(item: FileItem) {

        val key =
            item.localFile?.path
                ?: item.uri.toString()

        val idx =
            fileItemsCache.indexOfFirst {

                val otherKey =
                    it.localFile?.path
                        ?: it.uri.toString()

                otherKey == key
            }

        if (idx < 0) return

        fileItemsCache[idx] =
            fileItemsCache[idx].copy(
                isExpanded =
                    !fileItemsCache[idx].isExpanded
            )

        _fileItems.value =
            fileItemsCache.toList()
    }

    /*
     OPEN FILES
    */

    fun openFile(file: File) {

        if (!file.exists()) {

            _errorEvent.value =
                "Archivo no encontrado: ${file.name}"

            return
        }

        if (!file.canRead()) {

            _errorEvent.value =
                "Sin permiso para leer \"${file.name}\""

            return
        }

        val currentTabs =
            _tabs.value?.toMutableList()
                ?: mutableListOf()

        val existing =
            currentTabs.find {
                it.file.path == file.path
            }

        if (existing != null) {

            _activeTab.value = existing

            return
        }

        viewModelScope.launch(Dispatchers.IO) {

            val content =
                FileUtils.readFile(file)

            val extension =
                file.extension

            val language =
                FileUtils.getLanguageFromExtension(
                    extension
                )

            val tab = EditorTab(
                file = file,
                content = content,
                language = language
            )

            withContext(Dispatchers.Main) {

                val tabs =
                    _tabs.value?.toMutableList()
                        ?: mutableListOf()

                tabs.add(tab)

                _tabs.value = tabs

                _activeTab.value = tab

                Log.d(
                    TAG,
                    "Opened file: ${file.absolutePath}"
                )
            }
        }
    }

    /*
     TABS
    */

    fun updateTabContent(
        tabId: String,
        content: String
    ) {

        val tabs =
            _tabs.value?.toMutableList()
                ?: return

        val idx =
            tabs.indexOfFirst { it.id == tabId }

        if (idx >= 0) {

            tabs[idx] =
                tabs[idx].copy(
                    content = content,
                    isModified = true
                )

            _tabs.value = tabs

            if (_activeTab.value?.id == tabId) {

                _activeTab.value = tabs[idx]
            }
        }
    }

    fun saveCurrentTab() {

        val tab = _activeTab.value ?: return

        viewModelScope.launch(Dispatchers.IO) {

            val success =
                FileUtils.writeFile(
                    tab.file,
                    tab.content
                )

            withContext(Dispatchers.Main) {

                if (success) {

                    val tabs =
                        _tabs.value?.toMutableList()
                            ?: return@withContext

                    val idx =
                        tabs.indexOfFirst {
                            it.id == tab.id
                        }

                    if (idx >= 0) {

                        tabs[idx] =
                            tabs[idx].copy(
                                isModified = false
                            )

                        _tabs.value = tabs

                        _activeTab.value = tabs[idx]
                    }

                } else {

                    _errorEvent.value =
                        "No se pudo guardar \"${tab.file.name}\""
                }
            }
        }
    }

    fun closeTab(tab: EditorTab) {

        val tabs =
            _tabs.value?.toMutableList()
                ?: return

        val idx =
            tabs.indexOfFirst {
                it.id == tab.id
            }

        if (idx < 0) return

        tabs.removeAt(idx)

        _tabs.value = tabs

        if (_activeTab.value?.id == tab.id) {

            _activeTab.value =
                when {

                    tabs.isEmpty() -> null

                    idx > 0 ->
                        tabs[idx - 1]

                    else ->
                        tabs[0]
                }
        }
    }

    fun switchTab(tab: EditorTab) {
        _activeTab.value = tab
    }

    /*
     SETTINGS
    */

    fun updateSettings(settings: AppSettings) {

        _settings.value = settings

        prefs.saveSettings(settings)
    }

    fun getLastOpenedPath(): String? {
        return prefs.loadLastOpenedPath()
    }

    /*
     ERRORS
    */

    fun clearError() {
        _errorEvent.value = null
    }
}