package com.example.translationapp.LanguageManager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.translationapp.R
import com.example.translationapp.dataClass.LanguageModel

class LanguageAdapter(
    private var languageList: List<LanguageModel>,
    private val onActionClick: (LanguageModel) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    class LanguageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvLangName)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val btnAction: ImageButton = itemView.findViewById(R.id.btnAction)
        val pbDownloading: ProgressBar = itemView.findViewById(R.id.pbDownloading) // <--- New View
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_language, parent, false)
        return LanguageViewHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        val lang = languageList[position]
        holder.tvName.text = lang.name

        // PRIORITY CHECK: Is it currently downloading?
        if (lang.isDownloading) {
            holder.pbDownloading.visibility = View.VISIBLE // Show Bar
            holder.btnAction.visibility = View.INVISIBLE   // Hide Button
            holder.tvStatus.visibility = View.VISIBLE
            holder.tvStatus.text = "Downloading..."        // Update text
            return // Skip the rest of the logic
        } else {
            holder.pbDownloading.visibility = View.GONE
            holder.btnAction.visibility = View.VISIBLE
        }

        // Standard Logic (Downloaded vs Not Downloaded)
        if (lang.isDownloaded) {
            holder.tvStatus.visibility = View.VISIBLE
            holder.tvStatus.text = "Downloaded"
            holder.btnAction.setImageResource(android.R.drawable.ic_menu_delete)
        } else {
            holder.tvStatus.visibility = View.GONE
            holder.btnAction.setImageResource(R.drawable.ic_download) // Ensure this drawable exists
        }

        holder.btnAction.setOnClickListener {
            onActionClick(lang)
        }
    }

    override fun getItemCount() = languageList.size
    fun updateList(newList: List<LanguageModel>) {
        languageList = newList
        notifyDataSetChanged()
    }
}