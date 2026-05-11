package com.vscodeandroid.editor

import android.webkit.JavascriptInterface

class EditorBridge(private val viewModel: EditorViewModel) {
    @JavascriptInterface
    fun onContentChange(tabId: String, content: String) {
        viewModel.updateTabContent(tabId, content)
    }

    @JavascriptInterface
    fun onSave(tabId: String, content: String) {
        viewModel.updateTabContent(tabId, content)
        viewModel.saveCurrentTab()
    }

    @JavascriptInterface
    fun log(message: String) {
        android.util.Log.d("EditorBridge", message)
    }
}
