package com.vscodeandroid.editor.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vscodeandroid.editor.R
import com.vscodeandroid.editor.models.FileItem

class FileAdapter(
    private var items: MutableList<FileItem> = mutableListOf(),
    private val onFileClick: (FileItem) -> Unit,
    private val onFileLongClick: (FileItem) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.iv_file_icon)
        val name: TextView = view.findViewById(R.id.tv_file_name)
        val arrow: ImageView = view.findViewById(R.id.iv_arrow)
        val indent: View = view.findViewById(R.id.v_indent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.name

        val indentPx = (item.depth * 16 * holder.itemView.context.resources.displayMetrics.density).toInt()
        holder.indent.layoutParams.width = indentPx
        holder.indent.requestLayout()

        if (item.isDirectory) {
            holder.icon.setImageResource(if (item.isExpanded) R.drawable.ic_folder_open else R.drawable.ic_folder)
            holder.arrow.visibility = View.VISIBLE
            holder.arrow.rotation = if (item.isExpanded) 90f else 0f
        } else {
            holder.icon.setImageResource(getFileIcon(item.extension))
            holder.arrow.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onFileClick(item) }
        holder.itemView.setOnLongClickListener { onFileLongClick(item); true }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<FileItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    private fun getFileIcon(extension: String): Int = when (extension.lowercase()) {
        "js", "jsx" -> R.drawable.ic_file_js
        "ts", "tsx" -> R.drawable.ic_file_ts
        "py" -> R.drawable.ic_file_py
        "kt", "kts" -> R.drawable.ic_file_kt
        "java" -> R.drawable.ic_file_java
        "html", "htm" -> R.drawable.ic_file_html
        "css", "scss" -> R.drawable.ic_file_css
        "json" -> R.drawable.ic_file_json
        "xml" -> R.drawable.ic_file_xml
        "md" -> R.drawable.ic_file_md
        "sh" -> R.drawable.ic_file_sh
        else -> R.drawable.ic_file_default
    }
}
