package com.vscodeandroid.editor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vscodeandroid.editor.adapters.FileAdapter
import com.vscodeandroid.editor.adapters.TabAdapter
import com.vscodeandroid.editor.databinding.ActivityMainBinding
import com.vscodeandroid.editor.extensions.ExtensionsActivity
import com.vscodeandroid.editor.models.AppSettings
import com.vscodeandroid.editor.models.FileItem
import com.vscodeandroid.editor.terminal.TerminalActivity
import com.vscodeandroid.editor.utils.FileUtils
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: EditorViewModel by viewModels()
    private lateinit var fileAdapter: FileAdapter
    private lateinit var tabAdapter: TabAdapter
    private lateinit var drawerToggle: ActionBarDrawerToggle

    private val openFolderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                val path = getRealPathFromUri(uri)
                if (path != null) viewModel.openFolder(File(path))
            }
        }
    }

    private val extensionsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val newTheme = result.data?.getStringExtra(ExtensionsActivity.EXTRA_NEW_THEME)
            if (newTheme != null) {
                val current = viewModel.settings.value ?: AppSettings()
                viewModel.updateSettings(current.copy(theme = newTheme))
                applyMonacoTheme(newTheme)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupDrawer()
        setupFileExplorer()
        setupTabs()
        setupEditor()
        setupObservers()
        setupToolbarActions()

        val lastPath = viewModel.getLastOpenedPath()
        if (lastPath != null && File(lastPath).exists()) {
            viewModel.openFolder(File(lastPath))
        }
    }

    private fun setupDrawer() {
        drawerToggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.open_drawer, R.string.close_drawer
        )
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
    }

    private fun setupFileExplorer() {
        fileAdapter = FileAdapter(
            onFileClick = { item -> onFileItemClick(item) },
            onFileLongClick = { item -> showFileContextMenu(item) }
        )
        binding.rvFiles.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = fileAdapter
        }

        binding.btnOpenFolder.setOnClickListener { openFolderPicker() }
        binding.btnNewFile.setOnClickListener { showNewFileDialog() }
        binding.btnNewFolder.setOnClickListener { showNewFolderDialog() }
        binding.btnRefresh.setOnClickListener { viewModel.refreshFileTree() }
    }

    private fun setupTabs() {
        tabAdapter = TabAdapter(
            onTabClick = { tab -> viewModel.switchTab(tab) },
            onTabClose = { tab ->
                if (tab.isModified) showUnsavedDialog(tab) else viewModel.closeTab(tab)
            }
        )
        binding.rvTabs.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = tabAdapter
        }
    }

    private fun setupEditor() {
        binding.editorWebView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                loadWithOverviewMode = true
                useWideViewPort = true
            }
            addJavascriptInterface(EditorBridge(viewModel), "AndroidBridge")
            loadUrl("file:///android_asset/monaco/index.html")
        }
    }

    private fun setupObservers() {
        viewModel.fileItems.observe(this) { items ->
            fileAdapter.updateItems(items)
            binding.tvEmptyExplorer.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.tabs.observe(this) { tabs ->
            val activeId = viewModel.activeTab.value?.id ?: ""
            tabAdapter.setTabs(tabs, activeId)
            binding.rvTabs.visibility = if (tabs.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.activeTab.observe(this) { tab ->
            if (tab != null) {
                val settings = viewModel.settings.value
                val js = """
                    setEditorContent(
                        ${escapeJsonString(tab.content)},
                        '${tab.language}',
                        '${tab.id}'
                    );
                """.trimIndent()
                binding.editorWebView.evaluateJavascript(js, null)
                binding.tvEditorPlaceholder.visibility = View.GONE
                binding.editorWebView.visibility = View.VISIBLE
                supportActionBar?.subtitle = tab.file.name
            } else {
                binding.tvEditorPlaceholder.visibility = View.VISIBLE
                binding.editorWebView.visibility = View.GONE
                supportActionBar?.subtitle = null
            }
            val activeId = tab?.id ?: ""
            val tabs = viewModel.tabs.value ?: emptyList()
            tabAdapter.setTabs(tabs, activeId)
        }

        viewModel.settings.observe(this) { settings ->
            val js = "applySettings(${com.google.gson.Gson().toJson(settings)});"
            binding.editorWebView.evaluateJavascript(js, null)
        }
    }

    private fun setupToolbarActions() {
        binding.btnSave.setOnClickListener {
            viewModel.saveCurrentTab()
            binding.editorWebView.evaluateJavascript("getEditorContent();", null)
        }
        binding.btnSearch.setOnClickListener {
            binding.editorWebView.evaluateJavascript("toggleSearch();", null)
        }
        binding.btnSettings.setOnClickListener { showSettingsDialog() }
        binding.btnUndo.setOnClickListener {
            binding.editorWebView.evaluateJavascript("editor.trigger('', 'undo', null);", null)
        }
        binding.btnRedo.setOnClickListener {
            binding.editorWebView.evaluateJavascript("editor.trigger('', 'redo', null);", null)
        }
        binding.btnTerminal.setOnClickListener {
            val projectPath = viewModel.getLastOpenedPath()
                ?: getExternalFilesDir(null)?.absolutePath
                ?: filesDir.absolutePath
            TerminalActivity.launch(this, projectPath)
        }
        binding.btnExtensions.setOnClickListener {
            extensionsLauncher.launch(Intent(this, ExtensionsActivity::class.java))
        }
    }

    private fun applyMonacoTheme(themeId: String) {
        val themeJs = getMonacoThemeJs(themeId)
        binding.editorWebView.evaluateJavascript(themeJs, null)
    }

    private fun getMonacoThemeJs(themeId: String): String = when (themeId) {
        "dracula" -> """
            monaco.editor.defineTheme('dracula', {
                base: 'vs-dark', inherit: true,
                rules: [
                    {token: 'comment', foreground: '6272a4', fontStyle: 'italic'},
                    {token: 'keyword', foreground: 'ff79c6'},
                    {token: 'string', foreground: 'f1fa8c'},
                    {token: 'number', foreground: 'bd93f9'},
                    {token: 'type', foreground: '8be9fd'},
                    {token: 'function', foreground: '50fa7b'},
                    {token: 'variable', foreground: 'f8f8f2'},
                ],
                colors: {
                    'editor.background': '#282a36',
                    'editor.foreground': '#f8f8f2',
                    'editorLineNumber.foreground': '#6272a4',
                    'editor.selectionBackground': '#44475a',
                    'editor.lineHighlightBackground': '#44475a66',
                    'editorCursor.foreground': '#f8f8f0',
                }
            });
            monaco.editor.setTheme('dracula');
        """.trimIndent()
        "monokai" -> """
            monaco.editor.defineTheme('monokai', {
                base: 'vs-dark', inherit: true,
                rules: [
                    {token: 'comment', foreground: '75715e', fontStyle: 'italic'},
                    {token: 'keyword', foreground: 'f92672'},
                    {token: 'string', foreground: 'e6db74'},
                    {token: 'number', foreground: 'ae81ff'},
                    {token: 'type', foreground: '66d9e8'},
                    {token: 'function', foreground: 'a6e22e'},
                ],
                colors: {
                    'editor.background': '#272822',
                    'editor.foreground': '#f8f8f2',
                    'editorLineNumber.foreground': '#75715e',
                    'editor.selectionBackground': '#49483e',
                    'editor.lineHighlightBackground': '#3e3d32',
                }
            });
            monaco.editor.setTheme('monokai');
        """.trimIndent()
        "one-dark-pro" -> """
            monaco.editor.defineTheme('one-dark-pro', {
                base: 'vs-dark', inherit: true,
                rules: [
                    {token: 'comment', foreground: '5c6370', fontStyle: 'italic'},
                    {token: 'keyword', foreground: 'c678dd'},
                    {token: 'string', foreground: '98c379'},
                    {token: 'number', foreground: 'd19a66'},
                    {token: 'type', foreground: 'e5c07b'},
                    {token: 'function', foreground: '61afef'},
                    {token: 'variable', foreground: 'e06c75'},
                ],
                colors: {
                    'editor.background': '#282c34',
                    'editor.foreground': '#abb2bf',
                    'editorLineNumber.foreground': '#4b5263',
                    'editor.selectionBackground': '#3e4451',
                    'editor.lineHighlightBackground': '#2c313c',
                }
            });
            monaco.editor.setTheme('one-dark-pro');
        """.trimIndent()
        "night-owl" -> """
            monaco.editor.defineTheme('night-owl', {
                base: 'vs-dark', inherit: true,
                rules: [
                    {token: 'comment', foreground: '637777', fontStyle: 'italic'},
                    {token: 'keyword', foreground: 'c792ea'},
                    {token: 'string', foreground: 'ecc48d'},
                    {token: 'number', foreground: 'f78c6c'},
                    {token: 'type', foreground: 'ffcb8b'},
                    {token: 'function', foreground: '82aaff'},
                    {token: 'variable', foreground: 'addb67'},
                ],
                colors: {
                    'editor.background': '#011627',
                    'editor.foreground': '#d6deeb',
                    'editorLineNumber.foreground': '#4b6479',
                    'editor.selectionBackground': '#1d3b53',
                    'editor.lineHighlightBackground': '#0d2a3f',
                }
            });
            monaco.editor.setTheme('night-owl');
        """.trimIndent()
        "solarized-dark" -> "monaco.editor.setTheme('vs-dark');"
        "github-dark" -> """
            monaco.editor.defineTheme('github-dark', {
                base: 'vs-dark', inherit: true,
                rules: [
                    {token: 'comment', foreground: '8b949e'},
                    {token: 'keyword', foreground: 'ff7b72'},
                    {token: 'string', foreground: 'a5d6ff'},
                    {token: 'number', foreground: '79c0ff'},
                    {token: 'function', foreground: 'd2a8ff'},
                ],
                colors: {
                    'editor.background': '#0d1117',
                    'editor.foreground': '#c9d1d9',
                    'editorLineNumber.foreground': '#6e7681',
                    'editor.selectionBackground': '#264f78',
                    'editor.lineHighlightBackground': '#161b22',
                }
            });
            monaco.editor.setTheme('github-dark');
        """.trimIndent()
        else -> "monaco.editor.setTheme('$themeId');"
    }

    private fun onFileItemClick(item: FileItem) {
        if (item.isDirectory) {
            viewModel.toggleDirectory(item)
        } else {
            viewModel.openFile(item.file)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun showFileContextMenu(item: FileItem) {
        val options = arrayOf("Rename", "Delete", if (item.isDirectory) "New File Here" else "")
            .filter { it.isNotEmpty() }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(item.name)
            .setItems(options) { _, which ->
                when (options[which]) {
                    "Rename" -> showRenameDialog(item)
                    "Delete" -> showDeleteDialog(item)
                    "New File Here" -> showNewFileDialog(item.file)
                }
            }.show()
    }

    private fun showNewFileDialog(parent: File? = null) {
        val root = parent ?: File(viewModel.getLastOpenedPath() ?: return)
        showInputDialog("New File", "Filename") { name ->
            FileUtils.createNewFile(root, name)
            viewModel.refreshFileTree()
        }
    }

    private fun showNewFolderDialog() {
        val root = File(viewModel.getLastOpenedPath() ?: return)
        showInputDialog("New Folder", "Folder name") { name ->
            FileUtils.createNewFolder(root, name)
            viewModel.refreshFileTree()
        }
    }

    private fun showRenameDialog(item: FileItem) {
        showInputDialog("Rename", "New name", item.name) { newName ->
            val newFile = File(item.file.parent, newName)
            item.file.renameTo(newFile)
            viewModel.refreshFileTree()
        }
    }

    private fun showDeleteDialog(item: FileItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete ${item.name}?")
            .setMessage("This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                FileUtils.deleteFile(item.file)
                viewModel.refreshFileTree()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showUnsavedDialog(tab: com.vscodeandroid.editor.models.EditorTab) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Unsaved changes")
            .setMessage("${tab.file.name} has unsaved changes. Save before closing?")
            .setPositiveButton("Save") { _, _ ->
                viewModel.saveCurrentTab()
                viewModel.closeTab(tab)
            }
            .setNegativeButton("Discard") { _, _ -> viewModel.closeTab(tab) }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun showSettingsDialog() {
        val settings = viewModel.settings.value ?: return
        val themes = arrayOf("vs-dark", "vs", "hc-black")
        val themeIdx = themes.indexOf(settings.theme).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this)
            .setTitle("Settings")
            .setSingleChoiceItems(arrayOf("Dark Theme", "Light Theme", "High Contrast"), themeIdx) { dialog, which ->
                viewModel.updateSettings(settings.copy(theme = themes[which]))
                dialog.dismiss()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showInputDialog(title: String, hint: String, default: String = "", onConfirm: (String) -> Unit) {
        val input = android.widget.EditText(this).apply {
            this.hint = hint
            setText(default)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) onConfirm(text)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        openFolderLauncher.launch(intent)
    }

    private fun getRealPathFromUri(uri: Uri): String? {
        return try {
            if (DocumentsContract.isTreeUri(uri)) {
                val docId = DocumentsContract.getTreeDocumentId(uri)
                val parts = docId.split(":")
                val volumeId = parts.getOrNull(0) ?: return null
                val relativePath = parts.getOrNull(1) ?: ""

                val basePath = when {
                    volumeId.equals("primary", ignoreCase = true) ->
                        Environment.getExternalStorageDirectory().absolutePath
                    volumeId.equals("home", ignoreCase = true) ->
                        Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOCUMENTS
                        ).absolutePath
                    else -> "/storage/$volumeId"
                }

                val resolvedPath = if (relativePath.isNotEmpty()) "$basePath/$relativePath" else basePath
                val file = File(resolvedPath)
                if (file.exists()) resolvedPath else null
            } else {
                uri.path
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun escapeJsonString(s: String): String {
        val escaped = s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) return true
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else super.onBackPressed()
    }
}
