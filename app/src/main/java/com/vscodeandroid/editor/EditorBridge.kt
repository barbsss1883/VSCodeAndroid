package com.vscodeandroid.editor

import android.webkit.JavascriptInterface
import android.os.Handler
import android.os.Looper

class EditorBridge(private val viewModel: EditorViewModel) {

    private val handler = Handler(Looper.getMainLooper())
    private val debounceDelay = 400L // ms — espera 400ms después del último keystroke
    private val pendingUpdates = mutableMapOf<String, Runnable>()

    @JavascriptInterface
    fun onContentChange(tabId: String, content: String) {
        // Cancela el update anterior para este tab si aún no se ejecutó
        pendingUpdates[tabId]?.let { handler.removeCallbacks(it) }

        // Programa un nuevo update después del delay
        val runnable = Runnable {
            viewModel.updateTabContent(tabId, content)
            pendingUpdates.remove(tabId)
        }
        pendingUpdates[tabId] = runnable
        handler.postDelayed(runnable, debounceDelay)
    }

    @JavascriptInterface
    fun onSave(tabId: String, content: String) {
        // Al guardar, cancela cualquier debounce pendiente y aplica inmediatamente
        pendingUpdates[tabId]?.let { handler.removeCallbacks(it) }
        pendingUpdates.remove(tabId)

        handler.post {
            viewModel.updateTabContent(tabId, content)
            viewModel.saveCurrentTab()
        }
    }

    @JavascriptInterface
    fun log(message: String) {
        android.util.Log.d("EditorBridge", message)
    }

    // Llamar esto cuando se cierra un tab para limpiar memoria
    fun cancelPending(tabId: String) {
        pendingUpdates[tabId]?.let { handler.removeCallbacks(it) }
        pendingUpdates.remove(tabId)
    }

    fun cancelAll() {
        pendingUpdates.values.forEach { handler.removeCallbacks(it) }
        pendingUpdates.clear()
    }
}
