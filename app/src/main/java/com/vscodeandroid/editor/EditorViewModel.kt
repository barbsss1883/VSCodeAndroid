package com.vscodeandroid.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.vscodeandroid.editor.models.AppSettings
import com.vscodeandroid.editor.models.EditorTab
import com.vscodeandroid.editor.models.FileItem
import com.vscodeandroid.editor.utils.FileUtils
import com.vscodeandroid.editor.utils.PreferencesManager
import java.io.File

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesManager(application)

    private val _tabs = MutableLiveData<List<EditorTab>>(emptyList())
    val tabs: LiveData<List<EditorTab>> = _tabs

    private val _activeTab = MutableLiveData<EditorTab?>()
    val activeTab: LiveData<EditorTab?> = _activeTab

    private val _settings = MutableLiveData(prefs.loadSettings())
    val settings: LiveData<AppSettings> = _settings

    private val _fileItems = MutableLiveData<List<FileItem>>(emptyList())
    val fileItems: LiveData<List<FileItem>> = _fileItems

    private var rootDir: File? = null
    private val fileItemsCache = mutableListOf<FileItem>()

    fun openFolder(dir: File) {
        rootDir = dir
        prefs.saveLastOpenedPath(dir.absolutePath)
        refreshFileTree()
    }

    fun refreshFileTree() {
        val root = rootDir ?: return
        val items = buildFileTree(root, fileItemsCache.associate { it.file.path to it.isExpanded })
        fileItemsCache.clear()
        fileItemsCache.addAll(items)
        _fileItems.value = items.toList()
    }

    private fun buildFileTree(dir: File, expandedState: Map<String, Boolean>, depth: Int = 0): List<FileItem> {
        val result = mutableListOf<FileItem>()
        val children = dir.listFiles()?.sortedWith(
            compareBy({ !it.isDirectory }, { it.name.lowercase() })
        ) ?: return result

        for (child in children) {
            if (child.name.startsWith(".")) continue
            val isExpanded = expandedState[child.path] ?: false
            val item = FileItem(file = child, depth = depth, isExpanded = isExpanded)
            result.add(item)
            if (child.isDirectory && isExpanded) {
                result.addAll(buildFileTree(child, expandedState, depth + 1))
            }
        }
        return result
    }

    fun toggleDirectory(item: FileItem) {
        val idx = fileItemsCache.indexOfFirst { it.file.path == item.file.path }
        if (idx >= 0) {
            fileItemsCache[idx] = fileItemsCache[idx].copy(isExpanded = !fileItemsCache[idx].isExpanded)
            val expandedState = fileItemsCache.associate { it.file.path to it.isExpanded }
            val root = rootDir ?: return
            val newItems = buildFileTree(root, expandedState)
            fileItemsCache.clear()
            fileItemsCache.addAll(newItems)
            _fileItems.value = newItems.toList()
        }
    }

    fun openFile(file: File) {
        val currentTabs = _tabs.value?.toMutableList() ?: mutableListOf()
        val existing = currentTabs.find { it.file.path == file.path }
        if (existing != null) {
            _activeTab.value = existing
            return
        }
        val content = FileUtils.readFile(file)
        val tab = EditorTab(file = file, content = content)
        currentTabs.add(tab)
        _tabs.value = currentTabs
        _activeTab.value = tab
    }

    fun updateTabContent(tabId: String, content: String) {
        val tabs = _tabs.value?.toMutableList() ?: return
        val idx = tabs.indexOfFirst { it.id == tabId }
        if (idx >= 0) {
            tabs[idx] = tabs[idx].copy(content = content, isModified = true)
            _tabs.value = tabs
            if (_activeTab.value?.id == tabId) _activeTab.value = tabs[idx]
        }
    }

    fun saveCurrentTab() {
        val tab = _activeTab.value ?: return
        FileUtils.writeFile(tab.file, tab.content)
        val tabs = _tabs.value?.toMutableList() ?: return
        val idx = tabs.indexOfFirst { it.id == tab.id }
        if (idx >= 0) {
            tabs[idx] = tabs[idx].copy(isModified = false)
            _tabs.value = tabs
            _activeTab.value = tabs[idx]
        }
    }

    fun closeTab(tab: EditorTab) {
        val tabs = _tabs.value?.toMutableList() ?: return
        val idx = tabs.indexOfFirst { it.id == tab.id }
        if (idx < 0) return
        tabs.removeAt(idx)
        _tabs.value = tabs
        if (_activeTab.value?.id == tab.id) {
            _activeTab.value = when {
                tabs.isEmpty() -> null
                idx > 0 -> tabs[idx - 1]
                else -> tabs[0]
            }
        }
    }

    fun switchTab(tab: EditorTab) {
        _activeTab.value = tab
    }

    fun updateSettings(settings: AppSettings) {
        _settings.value = settings
        prefs.saveSettings(settings)
    }

    fun getLastOpenedPath() = prefs.loadLastOpenedPath()
}
