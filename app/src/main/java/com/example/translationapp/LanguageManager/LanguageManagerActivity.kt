package com.example.translationapp.LanguageManager

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.translationapp.R
import com.example.translationapp.dataClass.LanguageModel
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import java.util.Locale

class LanguageManagerActivity : AppCompatActivity() {

    private lateinit var rvLanguages: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var searchView: SearchView
    private lateinit var adapter: LanguageAdapter
    private val languageList = ArrayList<LanguageModel>()

    // ML Kit Model Manager
    private val modelManager = RemoteModelManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_manager)

        rvLanguages = findViewById(R.id.rvLanguages)
        progressBar = findViewById(R.id.progressBar)
        searchView = findViewById(R.id.searchView)

        rvLanguages.layoutManager = LinearLayoutManager(this)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        // Load data
        setupSearch()
        loadLanguages()
    }
    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filter(newText)
                return true
            }
        })
    }
    private fun filter(text: String?) {
        val query = text?.lowercase(Locale.getDefault()) ?: ""

        val filteredList = if (query.isEmpty()) {
            languageList // If empty, show full list
        } else {
            // Filter the Master List
            languageList.filter {
                it.name.lowercase(Locale.getDefault()).contains(query)
            }
        }
        if (::adapter.isInitialized) {
            adapter.updateList(filteredList)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed() // This function closes the current activity and goes back
        return true
    }
    private fun loadLanguages() {
        progressBar.visibility = View.VISIBLE
        languageList.clear()

        // 1. Get all languages supported by ML Kit
        val allLanguageCodes = TranslateLanguage.getAllLanguages()

        // 2. Check each one: Is it downloaded?
        // Note: This loop assumes we check one by one.
        // For a smoother UI, we check downloaded models first.

        modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener { downloadedModels ->
                // Create a Set of downloaded codes for fast lookup (e.g., {"en", "tl"})
                val downloadedCodes = downloadedModels.map { it.language }.toSet()

                for (code in allLanguageCodes) {
                    // Convert code "tl" to name "Filipino" using built-in Locale class
                    val name = Locale(code).displayLanguage

                    val isDownloaded = downloadedCodes.contains(code)
                    languageList.add(LanguageModel(code, name, isDownloaded))
                }

                // Sort list alphabetically
                languageList.sortBy { it.name }

                // Setup Adapter
                adapter = LanguageAdapter(languageList) { selectedLang ->
                    if (selectedLang.isDownloaded) {
                        deleteLanguage(selectedLang)
                    } else {
                        downloadLanguage(selectedLang)
                    }
                }
                rvLanguages.adapter = adapter
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error checking languages", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
    }


    private fun downloadLanguage(lang: LanguageModel) {

        // 1. Update UI State
        lang.isDownloading = true
        adapter.notifyDataSetChanged() // Refresh visible list safely

        val model = TranslateRemoteModel.Builder(lang.code).build()
        val conditions = DownloadConditions.Builder().requireWifi().build()

        modelManager.download(model, conditions)
            .addOnSuccessListener {
                Toast.makeText(this, "${lang.name} Downloaded!", Toast.LENGTH_SHORT).show()
                refreshList() // This reloads the master list
            }
            .addOnFailureListener {
                lang.isDownloading = false
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Download Failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteLanguage(lang: LanguageModel) {
        val model = TranslateRemoteModel.Builder(lang.code).build()

        modelManager.deleteDownloadedModel(model)
            .addOnSuccessListener {
                Toast.makeText(this, "${lang.name} Deleted", Toast.LENGTH_SHORT).show()
                refreshList()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Delete Failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun refreshList() {
        // Simple way: just reload everything to get fresh status
        loadLanguages()
    }
}