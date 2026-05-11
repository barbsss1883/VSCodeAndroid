package com.vscodeandroid.editor.terminal

import android.util.Log
import android.webkit.WebView
import java.io.InputStream
import java.io.OutputStream
import java.io.File

class TerminalSession(
    private val webView: WebView,
    private val workingDir: String
) {
    private var process: Process? = null
    private var outputStream: OutputStream? = null
    private var readerThread: Thread? = null
    var isRunning = false
        private set

    fun start(shellPath: String = "/system/bin/sh") {
        try {
            val pb = ProcessBuilder(shellPath)
            pb.directory(File(workingDir))
            pb.environment().apply {
                put("TERM", "xterm-256color")
                put("COLORTERM", "truecolor")
                put("HOME", workingDir)
                put("TMPDIR", workingDir)
                put("LANG", "en_US.UTF-8")
                // Add Termux path if available
                val termuxBin = "/data/data/com.termux/files/usr/bin"
                val termuxUsrBin = "/data/data/com.termux/files/usr/bin"
                val currentPath = get("PATH") ?: "/system/bin:/system/xbin"
                put("PATH", "$termuxBin:$termuxUsrBin:$currentPath")
                put("PREFIX", "/data/data/com.termux/files/usr")
            }
            pb.redirectErrorStream(true)
            process = pb.start()
            outputStream = process!!.outputStream
            isRunning = true

            // Start reader thread
            readerThread = Thread {
                val inputStream: InputStream = process!!.inputStream
                val buffer = ByteArray(4096)
                try {
                    while (isRunning) {
                        val n = inputStream.read(buffer)
                        if (n < 0) break
                        if (n > 0) {
                            val text = String(buffer, 0, n, Charsets.UTF_8)
                            val escaped = escapeForJs(text)
                            webView.post {
                                webView.evaluateJavascript("receiveOutput('$escaped');", null)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d("TerminalSession", "Reader thread ended: ${e.message}")
                }
                isRunning = false
                webView.post {
                    webView.evaluateJavascript("receiveOutput('\\r\\n[Process ended]\\r\\n');", null)
                }
            }.apply {
                isDaemon = true
                start()
            }

            // Send initial prompt
            sendInput("echo Welcome to VSCode Android Terminal && echo\n")
        } catch (e: Exception) {
            Log.e("TerminalSession", "Failed to start shell: ${e.message}")
            webView.post {
                webView.evaluateJavascript(
                    "receiveOutput('Failed to start shell: ${e.message}\\r\\n');", null
                )
            }
        }
    }

    fun sendInput(input: String) {
        try {
            outputStream?.write(input.toByteArray(Charsets.UTF_8))
            outputStream?.flush()
        } catch (e: Exception) {
            Log.e("TerminalSession", "Failed to send input: ${e.message}")
        }
    }

    fun resize(cols: Int, rows: Int) {
        // Basic resize notification (full PTY resize needs native code)
        sendInput("stty cols $cols rows $rows 2>/dev/null\n")
    }

    fun kill() {
        isRunning = false
        try {
            sendInput("exit\n")
            process?.destroy()
        } catch (e: Exception) { }
    }

    private fun escapeForJs(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\r", "\\r")
            .replace("\n", "\\n")
            .replace("`", "\\`")
    }
}
