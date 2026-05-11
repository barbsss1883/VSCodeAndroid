package com.vscodeandroid.editor.terminal

import android.webkit.JavascriptInterface

class TerminalBridge(private val session: TerminalSession) {
    @JavascriptInterface
    fun sendInput(data: String) {
        session.sendInput(data)
    }

    @JavascriptInterface
    fun onResize(cols: Int, rows: Int) {
        session.resize(cols, rows)
    }

    @JavascriptInterface
    fun log(msg: String) {
        android.util.Log.d("TerminalBridge", msg)
    }
}
