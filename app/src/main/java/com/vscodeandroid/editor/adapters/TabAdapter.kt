package com.vscodeandroid.editor.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.vscodeandroid.editor.R
import com.vscodeandroid.editor.models.EditorTab

class TabAdapter(
    private var tabs: MutableList<EditorTab> = mutableListOf(),
    private var activeTabId: String = "",
    private val onTabClick: (EditorTab) -> Unit,
    private val onTabClose: (EditorTab) -> Unit
) : RecyclerView.Adapter<TabAdapter.TabViewHolder>() {

    class TabViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_tab_name)
        val close: ImageButton = view.findViewById(R.id.btn_tab_close)
        val dot: View = view.findViewById(R.id.v_modified_dot)
        val activeIndicator: View = view.findViewById(R.id.v_tab_active_indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tab, parent, false)
        return TabViewHolder(view)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        val tab = tabs[position]
        holder.name.text = tab.file.name
        holder.dot.visibility = if (tab.isModified) View.VISIBLE else View.GONE

        val isActive = tab.id == activeTabId
        val bgColor = if (isActive) R.color.tab_active_bg else R.color.tab_inactive_bg
        val textColor = if (isActive) R.color.tab_active_text else R.color.tab_inactive_text

        holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, bgColor))
        holder.name.setTextColor(ContextCompat.getColor(holder.itemView.context, textColor))
        holder.activeIndicator.visibility = if (isActive) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onTabClick(tab) }
        holder.close.setOnClickListener { onTabClose(tab) }
    }

    override fun getItemCount() = tabs.size

    fun setTabs(newTabs: List<EditorTab>, activeId: String) {
        tabs.clear()
        tabs.addAll(newTabs)
        activeTabId = activeId
        notifyDataSetChanged()
    }
}
