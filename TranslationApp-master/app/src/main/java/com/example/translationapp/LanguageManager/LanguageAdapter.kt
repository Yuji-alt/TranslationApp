package com.example.translationapp.LanguageManager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.translationapp.R
import com.example.translationapp.dataClass.LanguageModel

class LanguageAdapter(
    private val languageList: List<LanguageModel>,
    private val onActionClick: (LanguageModel) -> Unit // Callback when button is clicked
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    class LanguageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvLangName)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val btnAction: ImageButton = itemView.findViewById(R.id.btnAction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_language, parent, false)
        return LanguageViewHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        val lang = languageList[position]
        holder.tvName.text = lang.name

        // Logic: If downloaded, show Trash Icon. If not, show Download Icon.
        if (lang.isDownloaded) {
            holder.tvStatus.visibility = View.VISIBLE
            holder.tvStatus.text = "Downloaded"
            holder.btnAction.setImageResource(android.R.drawable.ic_menu_delete) // Use standard delete icon
        } else {
            holder.tvStatus.visibility = View.GONE
            holder.btnAction.setImageResource(R.drawable.ic_download) // Your download icon
        }

        holder.btnAction.setOnClickListener {
            onActionClick(lang)
        }
    }

    override fun getItemCount() = languageList.size
}