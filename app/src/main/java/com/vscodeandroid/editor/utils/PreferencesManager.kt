package com.vscodeandroid.editor.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.vscodeandroid.editor.models.AppSettings

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("vscode_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveSettings(settings: AppSettings) {
        prefs.edit().putString("settings", gson.toJson(settings)).apply()
    }

    fun loadSettings(): AppSettings {
        val json = prefs.getString("settings", null)
        return if (json != null) gson.fromJson(json, AppSettings::class.java) else AppSettings()
    }

    fun saveLastOpenedPath(path: String) {
        prefs.edit().putString("last_path", path).apply()
    }

    fun loadLastOpenedPath(): String? = prefs.getString("last_path", null)
}
