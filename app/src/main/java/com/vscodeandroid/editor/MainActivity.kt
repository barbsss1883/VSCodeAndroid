package com.vscodeandroid.editor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
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
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: EditorViewModel by viewModels()
    private lateinit var fileAdapter: FileAdapter
    private lateinit var tabAdapter: TabAdapter
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var editorBridge: EditorBridge
    private var isEditorReady = false

    private val openFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val root = DocumentFile.fromTreeUri(this, it)
            root?.let { docFile -> viewModel.openDocumentTree(docFile) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        editorBridge = EditorBridge(viewModel)
        
        setupDrawer()
        setupFileExplorer()
        setupTabs()
        setupEditor()
        setupObservers()
        setupToolbarActions()

        checkAndRequestStoragePermission()

        viewModel.getLastOpenedPath()?.let { path ->
            val file = File(path)
            if (file.exists()) viewModel.openFolder(file)
        }
    }

    private fun setupEditor() {
        binding.editorWebView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true
            }
            addJavascriptInterface(editorBridge, "AndroidBridge")
            loadUrl("file:///android_asset/monaco/index.html")
            
            webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: android.webkit.View?, url: String?) {
                    super.onPageFinished(view, url)
                    isEditorReady = true
                    viewModel.activeTab.value?.let { updateMonacoContent(it) }
                }
            }
        }
    }

    private fun setupObservers() {
        viewModel.activeTab.observe(this) { tab ->
            if (tab != null && isEditorReady) {
                updateMonacoContent(tab)
                binding.tvEditorPlaceholder.visibility = View.GONE
                binding.editorWebView.visibility = View.VISIBLE
            } else if (tab == null) {
                binding.tvEditorPlaceholder.visibility = View.VISIBLE
                binding.editorWebView.visibility = View.GONE
            }
        }

        viewModel.fileItems.observe(this) { items ->
            fileAdapter.updateItems(items)
        }

        viewModel.tabs.observe(this) { tabs ->
            val activeId = viewModel.activeTab.value?.id ?: ""
            tabAdapter.setTabs(tabs, activeId)
        }
    }

    private fun updateMonacoContent(tab: com.vscodeandroid.editor.models.EditorTab) {
        val escapedContent = escapeJsonString(tab.content)
        val js = "if(window.setEditorContent) { setEditorContent($escapedContent, '${tab.language}', '${tab.id}'); }"
        binding.editorWebView.evaluateJavascript(js, null)
    }

    private fun setupFileExplorer() {
        fileAdapter = FileAdapter(
            onFileClick = { item -> 
                if (item.isDirectory) viewModel.toggleDirectory(item) 
                else viewModel.openFile(item.file)
            },
            onFileLongClick = { /* Implementar menú contextual */ }
        )
        binding.rvFiles.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = fileAdapter
        }
        binding.btnOpenFolder.setOnClickListener { openFolderPicker() }
        binding.btnNewFile.setOnClickListener { showNewFileDialog() }
    }

    private fun showNewFileDialog() {
        val lastPath = viewModel.getLastOpenedPath() ?: return
        val root = File(lastPath)
        
        val input = EditText(this)
        MaterialAlertDialogBuilder(this)
            .setTitle("Nuevo Archivo")
            .setView(input)
            .setPositiveButton("Crear") { _, _ ->
                val name = input.text.toString()
                if (name.isNotEmpty()) {
                    val newFile = FileUtils.createNewFile(root, name)
                    if (newFile != null) {
                        viewModel.refreshFileTree()
                        viewModel.openFile(newFile)
                    } else {
                        Toast.makeText(this, "No se pudo crear el archivo", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun checkAndRequestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    private fun escapeJsonString(s: String): String {
        return "\"" + s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t") + "\""
    }

    private fun setupTabs() {
        tabAdapter = TabAdapter(
            onTabClick = { tab -> viewModel.switchTab(tab) },
            onTabClose = { tab -> viewModel.closeTab(tab) }
        )
        binding.rvTabs.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvTabs.adapter = tabAdapter
    }

    private fun setupDrawer() {
        drawerToggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, 0, 0)
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
    }

    private fun setupToolbarActions() {
        binding.btnSave.setOnClickListener { viewModel.saveCurrentTab() }
        binding.btnTerminal.setOnClickListener { 
            TerminalActivity.launch(this, viewModel.getLastOpenedPath() ?: filesDir.absolutePath) 
        }
    }

    private fun openFolderPicker() {
        openFolderLauncher.launch(null)
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
