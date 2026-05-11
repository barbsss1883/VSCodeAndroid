package com.vscodeandroid.editor.terminal

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vscodeandroid.editor.R
import java.io.File

class TerminalActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var tvTermuxStatus: TextView
    private lateinit var btnTermuxOpen: ImageButton
    private var session: TerminalSession? = null
    private var projectPath: String = ""
    private var termuxInstalled = false

    companion object {
        const val EXTRA_PROJECT_PATH = "project_path"
        fun launch(activity: Activity, projectPath: String) {
            activity.startActivity(Intent(activity, TerminalActivity::class.java).apply {
                putExtra(EXTRA_PROJECT_PATH, projectPath)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terminal)

        projectPath = intent.getStringExtra(EXTRA_PROJECT_PATH)
            ?: getExternalFilesDir(null)?.absolutePath
            ?: filesDir.absolutePath

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.terminal_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Terminal"
            subtitle = projectPath.substringAfterLast("/")
            setDisplayHomeAsUpEnabled(true)
        }

        webView = findViewById(R.id.terminal_web_view)
        tvTermuxStatus = findViewById(R.id.tv_termux_status)
        btnTermuxOpen = findViewById(R.id.btn_open_termux)

        termuxInstalled = TermuxIntegration.isTermuxInstalled(this)
        setupTermuxBanner()
        setupWebView()
        setupButtons()
    }

    private fun setupTermuxBanner() {
        if (termuxInstalled) {
            tvTermuxStatus.text = "✓ Termux detectado — npm, python, git disponibles"
            tvTermuxStatus.setBackgroundColor(0xFF1A3A1A.toInt())
            btnTermuxOpen.visibility = View.VISIBLE
        } else {
            tvTermuxStatus.text = "⚠ Instala Termux para npm, python, git, pkg"
            tvTermuxStatus.setBackgroundColor(0xFF3A2A1A.toInt())
            btnTermuxOpen.visibility = View.VISIBLE
        }
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
        }
        webView.setBackgroundColor(0xFF1E1E1E.toInt())
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                startSession()
            }
        }

        val session = TerminalSession(webView, projectPath)
        this.session = session
        webView.addJavascriptInterface(TerminalBridge(session), "AndroidTerminal")
        webView.loadUrl("file:///android_asset/terminal/index.html")
    }

    private fun startSession() {
        val shellPath = if (termuxInstalled) {
            val termuxBash = File("/data/data/com.termux/files/usr/bin/bash")
            if (termuxBash.exists()) termuxBash.absolutePath else "/system/bin/sh"
        } else {
            "/system/bin/sh"
        }
        session?.start(shellPath)
    }

    private fun setupButtons() {
        btnTermuxOpen.setOnClickListener {
            if (termuxInstalled) {
                TermuxIntegration.openTermuxAtPath(this, projectPath)
            } else {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Instalar Termux")
                    .setMessage(
                        "Termux proporciona un entorno Linux completo con:\n\n" +
                        "• pkg install nodejs  (npm, node)\n" +
                        "• pkg install python  (python3, pip)\n" +
                        "• pkg install git     (git)\n" +
                        "• pkg install clang   (C/C++)\n\n" +
                        "Recomendado instalar desde F-Droid (no Play Store)."
                    )
                    .setPositiveButton("Abrir F-Droid") { _, _ ->
                        TermuxIntegration.openTermuxInstallPage(this)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }

        findViewById<ImageButton>(R.id.btn_clear_terminal).setOnClickListener {
            webView.evaluateJavascript("term.clear();", null)
        }

        findViewById<ImageButton>(R.id.btn_new_session).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Nueva sesión")
                .setMessage("¿Reiniciar la terminal?")
                .setPositiveButton("Sí") { _, _ ->
                    session?.kill()
                    startSession()
                }
                .setNegativeButton("No", null)
                .show()
        }

        // Special key buttons
        findViewById<android.widget.Button>(R.id.btn_key_tab).setOnClickListener {
            session?.sendInput("\t")
        }
        findViewById<android.widget.Button>(R.id.btn_key_ctrl_c).setOnClickListener {
            session?.sendInput("")
        }
        findViewById<android.widget.Button>(R.id.btn_key_ctrl_d).setOnClickListener {
            session?.sendInput("")
        }
        findViewById<android.widget.Button>(R.id.btn_key_esc).setOnClickListener {
            session?.sendInput("")
        }
        findViewById<android.widget.Button>(R.id.btn_key_up).setOnClickListener {
            session?.sendInput("[A")
        }
        findViewById<android.widget.Button>(R.id.btn_key_down).setOnClickListener {
            session?.sendInput("[B")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        session?.kill()
    }
}
