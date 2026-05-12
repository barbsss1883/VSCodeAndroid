package com.vscodeandroid.editor.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
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
        val modifiedDot: View = view.findViewById(R.id.v_file_modified)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context

        holder.name.text = item.name
        holder.modifiedDot.visibility = View.GONE

        // Indentation
        val indentPx = (item.depth * 14 * ctx.resources.displayMetrics.density).toInt()
        holder.indent.layoutParams.width = indentPx
        holder.indent.requestLayout()

        if (item.isDirectory) {
            holder.icon.setImageResource(
                if (item.isExpanded) R.drawable.ic_folder_open else R.drawable.ic_folder
            )
            holder.icon.setColorFilter(
                ContextCompat.getColor(ctx, R.color.icon_folder)
            )
            holder.arrow.visibility = View.VISIBLE
            holder.arrow.rotation = if (item.isExpanded) 90f else 0f
            holder.arrow.setColorFilter(ContextCompat.getColor(ctx, R.color.explorer_icon))
        } else {
            val (iconRes, colorRes) = getFileIconAndColor(item.extension)
            holder.icon.setImageResource(iconRes)
            holder.icon.setColorFilter(ContextCompat.getColor(ctx, colorRes))
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

    private fun getFileIconAndColor(extension: String): Pair<Int, Int> = when (extension.lowercase()) {
        "js", "jsx"     -> Pair(R.drawable.ic_file_js,      R.color.icon_js)
        "ts", "tsx"     -> Pair(R.drawable.ic_file_ts,      R.color.icon_ts)
        "py"            -> Pair(R.drawable.ic_file_py,      R.color.icon_py)
        "kt", "kts"     -> Pair(R.drawable.ic_file_kt,      R.color.icon_kt)
        "java"          -> Pair(R.drawable.ic_file_java,    R.color.icon_java)
        "html", "htm"   -> Pair(R.drawable.ic_file_html,    R.color.icon_html)
        "css", "scss"   -> Pair(R.drawable.ic_file_css,     R.color.icon_css)
        "json"          -> Pair(R.drawable.ic_file_json,    R.color.icon_json)
        "xml"           -> Pair(R.drawable.ic_file_xml,     R.color.icon_xml)
        "md"            -> Pair(R.drawable.ic_file_md,      R.color.icon_md)
        "sh", "bash"    -> Pair(R.drawable.ic_file_sh,      R.color.icon_sh)
        else            -> Pair(R.drawable.ic_file_default, R.color.icon_default)
    }
}
