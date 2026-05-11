package com.vscodeandroid.editor.extensions

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vscodeandroid.editor.R

class ExtensionAdapter(
    private var items: List<Extension> = emptyList(),
    private val onInstall: (Extension) -> Unit,
    private val onUninstall: (Extension) -> Unit,
    private val onApplyTheme: ((Extension) -> Unit)? = null
) : RecyclerView.Adapter<ExtensionAdapter.ExtViewHolder>() {

    class ExtViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: View = view.findViewById(R.id.v_ext_icon)
        val name: TextView = view.findViewById(R.id.tv_ext_name)
        val description: TextView = view.findViewById(R.id.tv_ext_description)
        val author: TextView = view.findViewById(R.id.tv_ext_author)
        val type: TextView = view.findViewById(R.id.tv_ext_type)
        val btnAction: Button = view.findViewById(R.id.btn_ext_action)
        val btnApply: Button = view.findViewById(R.id.btn_ext_apply)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExtViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_extension, parent, false)
        return ExtViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExtViewHolder, position: Int) {
        val ext = items[position]
        holder.name.text = ext.name
        holder.description.text = ext.description
        holder.author.text = "by ${ext.author}"
        holder.type.text = when (ext.type) {
            ExtensionType.THEME -> "Tema"
            ExtensionType.SNIPPETS -> "Snippets"
            ExtensionType.LANGUAGE_SUPPORT -> "Lenguaje"
            ExtensionType.FORMATTER -> "Formateador"
        }

        try {
            holder.icon.setBackgroundColor(Color.parseColor(ext.iconColor))
        } catch (e: Exception) {
            holder.icon.setBackgroundColor(Color.parseColor("#007ACC"))
        }

        if (ext.isInstalled) {
            holder.btnAction.text = "Desinstalar"
            holder.btnAction.setOnClickListener { onUninstall(ext) }
            if (ext.type == ExtensionType.THEME) {
                holder.btnApply.visibility = View.VISIBLE
                holder.btnApply.setOnClickListener { onApplyTheme?.invoke(ext) }
            } else {
                holder.btnApply.visibility = View.GONE
            }
        } else {
            holder.btnAction.text = "Instalar"
            holder.btnAction.setOnClickListener { onInstall(ext) }
            holder.btnApply.visibility = View.GONE
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<Extension>) {
        items = newItems
        notifyDataSetChanged()
    }
}
