package com.vscodeandroid.editor.extensions

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.vscodeandroid.editor.R

class ExtensionsActivity : AppCompatActivity() {
    private lateinit var extensionManager: ExtensionManager
    private lateinit var adapter: ExtensionAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var tabLayout: TabLayout
    private var showingInstalled = false

    companion object {
        const val RESULT_THEME_CHANGED = "theme_changed"
        const val EXTRA_NEW_THEME = "new_theme"

        fun launch(activity: Activity) {
            activity.startActivityForResult(
                Intent(activity, ExtensionsActivity::class.java),
                1001
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_extensions)

        extensionManager = ExtensionManager(this)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.extensions_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        tabLayout = findViewById(R.id.tab_layout_extensions)
        recyclerView = findViewById(R.id.rv_extensions)

        adapter = ExtensionAdapter(
            onInstall = { ext ->
                extensionManager.install(ext.id)
                Toast.makeText(this, "${ext.name} instalada", Toast.LENGTH_SHORT).show()
                refreshList()
            },
            onUninstall = { ext ->
                extensionManager.uninstall(ext.id)
                Toast.makeText(this, "${ext.name} desinstalada", Toast.LENGTH_SHORT).show()
                refreshList()
            },
            onApplyTheme = { ext ->
                val monacoThemeId = extensionManager.getThemeMonacoId(ext.id)
                extensionManager.setActiveTheme(monacoThemeId)
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_NEW_THEME, monacoThemeId)
                    putExtra(RESULT_THEME_CHANGED, true)
                }
                setResult(RESULT_OK, resultIntent)
                Toast.makeText(this, "Tema ${ext.name} aplicado", Toast.LENGTH_SHORT).show()
                finish()
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ExtensionsActivity)
            adapter = this@ExtensionsActivity.adapter
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                showingInstalled = tab.position == 1
                refreshList()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        refreshList()
    }

    private fun refreshList() {
        val items = if (showingInstalled) {
            extensionManager.getInstalledExtensions()
        } else {
            extensionManager.getAllExtensions()
        }
        adapter.updateItems(items)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
