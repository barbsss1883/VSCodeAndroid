package com.vscodeandroid.editor

import android.app.Activity
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
import android.content.Context;
import java.io.IOException;
import androidx.documentfile.provider.DocumentFile
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: EditorViewModel by viewModels()
    private lateinit var fileAdapter: FileAdapter
    private lateinit var tabAdapter: TabAdapter
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var editorBridge: EditorBridge

    companion object {
        private const val REQUEST_MANAGE_STORAGE = 200
    }

    private val openFolderLauncher =
    registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->

        if (uri != null) {

            try {

                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                val root =
                    DocumentFile.fromTreeUri(this, uri)

                if (root != null) {
                    viewModel.openDocumentTree(root)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

                val path = getRealPathFromUri(uri)
                if (path != null && File(path).canRead()) {
                    viewModel.openFolder(File(path))
                } else {
                    Toast.makeText(
                        this,
                        "Para acceso completo, otorga permiso de almacenamiento en Ajustes → Apps → VSCode Android",
                        Toast.LENGTH_LONG
                    ).show()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivityForResult(intent, REQUEST_MANAGE_STORAGE)
                    }
                }
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
        editorBridge = EditorBridge(viewModel)
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

        // Request storage permission on first launch
        checkAndRequestStoragePermission()

        // CORRECCIÓN: Manejar apertura de archivos desde otras apps
        handleIncomingIntent(intent)
    }

    // CORRECCIÓN: Manejar intents cuando la app ya está abierta
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    /**
     * CORRECCIÓN: Procesa ACTION_VIEW / ACTION_EDIT recibidos desde otras apps.
     * Soporta URIs de tipo content:// y file://.
     */
    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW && intent?.action != Intent.ACTION_EDIT) return
        val uri = intent.data ?: return

        val path = getRealPathFromUri(uri)
        if (path != null) {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                viewModel.openFolder(file.parentFile ?: file)
                viewModel.openFile(file)
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                Toast.makeText(
                    this,
                    "Sin permiso para leer el archivo. Ve a Ajustes → Apps → VSCode Android → Permisos → Archivos y medios → Permitir administrar todos los archivos",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            // Fallback para URIs content:// sin ruta real (ej. desde Drive, email, etc.)
            try {
                val inputStream = contentResolver.openInputStream(uri)
                    ?: run {
                        Toast.makeText(this, "No se pudo leer el archivo compartido", Toast.LENGTH_SHORT).show()
                        return
                    }
                val fileName = DocumentFile.fromSingleUri(this, uri)?.name ?: "archivo_temp.txt"
                val tmpFile = File(cacheDir, fileName)
                tmpFile.outputStream().use { out -> inputStream.copyTo(out) }
                viewModel.openFolder(cacheDir)
                viewModel.openFile(tmpFile)
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } catch (e: Exception) {
                Toast.makeText(this, "No se pudo abrir: ${e.message}", Toast.LENGTH_LONG).show()
            }
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
                if (tab.isModified) showUnsavedDialog(tab) else {
                    editorBridge.cancelPending(tab.id)
                    viewModel.closeTab(tab)
                }
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
            addJavascriptInterface(editorBridge, "AndroidBridge")
            loadUrl("file:///android_asset/monaco/index.html")
        }
    }

    private fun setupObservers() {
        viewModel.fileItems.observe(this) { items ->
            fileAdapter.updateItems(items)
            val isEmpty = items.isEmpty()
            binding.tvEmptyExplorer.visibility = if (isEmpty) View.VISIBLE else View.GONE
            val lastPath = viewModel.getLastOpenedPath()
            if (lastPath != null && !isEmpty) {
                binding.tvFolderName.text = File(lastPath).name.uppercase()
                binding.tvFolderName.visibility = View.VISIBLE
                binding.tvStatusBranch.text = File(lastPath).name
            } else {
                binding.tvFolderName.visibility = View.GONE
                binding.tvStatusBranch.text = "Sin carpeta"
            }
        }

        viewModel.tabs.observe(this) { tabs ->
            val activeId = viewModel.activeTab.value?.id ?: ""
            tabAdapter.setTabs(tabs, activeId)
            val hasTabs = tabs.isNotEmpty()
            binding.rvTabs.visibility = if (hasTabs) View.VISIBLE else View.GONE
            binding.vTabsBorder.visibility = if (hasTabs) View.VISIBLE else View.GONE
        }

        viewModel.activeTab.observe(this) { tab ->
            if (tab != null) {
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
                binding.tvStatusLang.text = tab.language.uppercase()
                binding.tvStatusEncoding.text = "UTF-8"
            } else {
                binding.tvEditorPlaceholder.visibility = View.VISIBLE
                binding.editorWebView.visibility = View.GONE
                supportActionBar?.subtitle = null
                binding.tvStatusLang.text = ""
            }
            val activeId = tab?.id ?: ""
            val tabs = viewModel.tabs.value ?: emptyList()
            tabAdapter.setTabs(tabs, activeId)
        }

        viewModel.settings.observe(this) { settings ->
            val js = "applySettings(${com.google.gson.Gson().toJson(settings)});"
            binding.editorWebView.evaluateJavascript(js, null)
        }

        // CORRECCIÓN: Observar errores del ViewModel y mostrarlos como Toast
        viewModel.errorEvent.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
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

    private fun checkAndRequestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Permiso necesario")
                .setMessage("VSCode Android necesita acceso completo al almacenamiento.\n\nEn la siguiente pantalla, activa \"Permitir administrar todos los archivos\".")
                .setPositiveButton("Configurar ahora") { _, _ ->
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivityForResult(intent, REQUEST_MANAGE_STORAGE)
                }
                // CORRECCIÓN: Botón negativo ahora advierte sobre la consecuencia
                .setNegativeButton("Ahora no") { _, _ ->
                    Toast.makeText(
                        this,
                        "Sin permiso de almacenamiento la app no podrá abrir archivos.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun openFolderPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                showFolderPickerDialog()
            } else {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Permiso de almacenamiento")
                    .setMessage("Para acceder a tus archivos, VSCode Android necesita permiso de administrador de almacenamiento.\n\nEn la siguiente pantalla, activa \"Permitir acceso a todos los archivos\".")
                    .setPositiveButton("Continuar") { _, _ ->
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivityForResult(intent, REQUEST_MANAGE_STORAGE)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        } else {
            showFolderPickerDialog()
        }
    }

    private fun showFolderPickerDialog() {
        val storage = Environment.getExternalStorageDirectory()
        val home = storage.absolutePath

        val quickPaths = mutableListOf<Pair<String, String>>()
        quickPaths.add(Pair("📁  Almacenamiento interno", home))

        val termuxShared = File("/data/data/com.termux/files/home/storage/shared")
        if (termuxShared.exists() && termuxShared.canRead()) {
            quickPaths.add(Pair("🐧  Termux ~/storage/shared", termuxShared.absolutePath))
        }
        val termuxHome = File(home, "Termux")
        if (termuxHome.exists() && termuxHome.canRead()) {
            quickPaths.add(Pair("🐧  Termux Home", termuxHome.absolutePath))
        }

        listOf("Projects", "projects", "Dev", "dev", "Code", "code", "repos", "Repos",
               "Bitacora57", "bitacora57", "VSCodeAndroid").forEach { name ->
            val f = File(home, name)
            if (f.exists() && f.isDirectory) quickPaths.add(Pair("📂  $name", f.absolutePath))
        }

        File(home, "Download").takeIf { it.exists() }?.let { quickPaths.add(Pair("⬇️  Descargas", it.absolutePath)) }
        File(home, "Documents").takeIf { it.exists() }?.let { quickPaths.add(Pair("📄  Documentos", it.absolutePath)) }

        quickPaths.add(Pair("✏️  Escribir ruta manualmente...", "__manual__"))
        quickPaths.add(Pair("🗂️  Selector del sistema...", "__saf__"))

        val labels = quickPaths.map { it.first }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Abrir carpeta")
            .setItems(labels) { _, which ->
                val (_, path) = quickPaths[which]
                when (path) {
                    "__manual__" -> showManualPathDialog()
                    "__saf__" -> {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                        openFolderLauncher.launch(intent)
                    }
                    else -> {
                        val folder = File(path)
                        if (folder.exists()) {
                            if (folder.canRead()) {
                                viewModel.openFolder(folder)
                                binding.drawerLayout.closeDrawer(GravityCompat.START)
                            } else {
                                Toast.makeText(this, "Sin acceso. Otorga permiso de almacenamiento primero.", Toast.LENGTH_LONG).show()
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                        data = Uri.parse("package:$packageName")
                                    }
                                    startActivityForResult(intent, REQUEST_MANAGE_STORAGE)
                                }
                            }
                        } else {
                            Toast.makeText(this, "La carpeta no existe: $path", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .show()
    }

    private fun showManualPathDialog() {
        val input = EditText(this).apply {
            hint = "/storage/emulated/0/MiProyecto"
            setText(viewModel.getLastOpenedPath() ?: "/storage/emulated/0/")
            setSelectAllOnFocus(true)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Escribir ruta")
            .setView(input)
            .setPositiveButton("Abrir") { _, _ ->
                val path = input.text.toString().trim()
                val folder = File(path)
                if (folder.exists() && folder.isDirectory) {
                    viewModel.openFolder(folder)
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    Toast.makeText(this, "Ruta no válida o no existe", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MANAGE_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                Toast.makeText(this, "✓ Permiso otorgado. Selecciona una carpeta.", Toast.LENGTH_SHORT).show()
                showFolderPickerDialog()
            } else {
                Toast.makeText(this, "Permiso no otorgado. Usa el selector del sistema o escribe la ruta.", Toast.LENGTH_LONG).show()
                showFolderPickerDialog()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            && Environment.isExternalStorageManager()
            && viewModel.getLastOpenedPath() == null
            && (viewModel.fileItems.value?.isEmpty() == true)) {
            binding.tvStatusBranch.text = "✓ Listo — abre una carpeta"
        }
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
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath
                    else -> "/storage/$volumeId"
                }
                val resolvedPath = if (relativePath.isNotEmpty()) "$basePath/$relativePath" else basePath
                val file = File(resolvedPath)
                if (file.exists()) resolvedPath else null
            } else if (uri.scheme == "file") {
                // CORRECCIÓN: manejar URIs file:// directamente
                uri.path
            } else if (uri.scheme == "content") {
                // CORRECCIÓN: intentar obtener la ruta real desde content URI
                val docId = try { DocumentsContract.getDocumentId(uri) } catch (e: Exception) { null }
                if (docId != null) {
                    val parts = docId.split(":")
                    val volumeId = parts.getOrNull(0) ?: return null
                    val relativePath = parts.getOrNull(1) ?: return null
                    val basePath = when {
                        volumeId.equals("primary", ignoreCase = true) ->
                            Environment.getExternalStorageDirectory().absolutePath
                        else -> "/storage/$volumeId"
                    }
                    val resolvedPath = "$basePath/$relativePath"
                    val file = File(resolvedPath)
                    if (file.exists() && file.canRead()) resolvedPath else null
                } else {
                    uri.path
                }
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

    override fun onDestroy() {
        super.onDestroy()
        editorBridge.cancelAll()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else super.onBackPressed()
    }


    // [AID_START: file_write]
    private fun writeToFile(context: Context, fileName: String, data: String) {
        try {
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { fos ->
                fos.write(data.toByteArray())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    // [AID_END: file_write]
}
