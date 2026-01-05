package com.example.translationapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.translationapp.LanguageManager.LanguageManagerActivity
import com.google.android.material.navigation.NavigationView
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // ... (Your existing variables remain the same) ...
    private lateinit var etInput: EditText
    private lateinit var tvResult: TextView
    private lateinit var tvSourceLang: TextView
    private lateinit var tvTargetLang: TextView
    private lateinit var btnSwitch: ImageButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    private var translator: Translator? = null
    private val modelManager = RemoteModelManager.getInstance()

    private var sourceLangCode = TranslateLanguage.ENGLISH
    private var targetLangCode = TranslateLanguage.TAGALOG

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        etInput = findViewById(R.id.etInputText)
        tvResult = findViewById(R.id.tvTranslatedText)
        tvSourceLang = findViewById(R.id.tvSourceLang)
        tvTargetLang = findViewById(R.id.tvTargetLang)
        btnSwitch = findViewById(R.id.btnSwitchLanguages)
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        val btnMenu = findViewById<ImageButton>(R.id.btnMenu)

        // 1. AUTO-DOWNLOAD FUNCTION
        downloadDefaultLanguages()

        // 2. Initialize Translator
        prepareTranslator()

        etInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val textToTranslate = s.toString()
                if (textToTranslate.isNotEmpty()) {
                    translateText(textToTranslate)
                } else {
                    tvResult.text = ""
                }
            }
        })

        tvSourceLang.setOnClickListener { showLanguageSelectionDialog(isSource = true) }
        tvTargetLang.setOnClickListener { showLanguageSelectionDialog(isSource = false) }

        btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.END) }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_history -> { }

                // --- UPDATED NAVIGATION FOR STEP 5 ---
                R.id.nav_saved -> {
                    val intent = Intent(this, FamiliarizationActivity::class.java)
                    startActivity(intent)
                }
                // -------------------------------------

                // This opens your Language Detector
                R.id.nav_settings -> {
                    val intent = Intent(this, RecognitionActivity::class.java)
                    startActivity(intent)
                }

                R.id.downloaded_language -> {
                    val intent = Intent(this, LanguageManagerActivity::class.java)
                    startActivity(intent)
                }

                // ADD THIS NEW BLOCK FOR THE NEW SETTINGS BUTTON
                R.id.nav_app_settings -> {
                    // Future settings code goes here
                }
            }
            drawerLayout.closeDrawer(GravityCompat.END)
            true
        }

        btnSwitch.setOnClickListener { swapLanguages() }
    }
    private fun downloadDefaultLanguages() {
        val languagesToDownload = listOf(TranslateLanguage.ENGLISH, TranslateLanguage.TAGALOG)
        val conditions = DownloadConditions.Builder()
            .build()

        for (langCode in languagesToDownload) {
            val model = TranslateRemoteModel.Builder(langCode).build()
            modelManager.download(model, conditions)
                .addOnSuccessListener {
                    Log.d("AutoDownload", "Model $langCode downloaded successfully")
                }
                .addOnFailureListener {
                    Log.e("AutoDownload", "Failed to download $langCode")
                }
        }
    }

    private fun showLanguageSelectionDialog(isSource: Boolean) {
        // 1. Ask ML Kit: "What is fully downloaded?"
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener { models ->

                val availableLanguages = ArrayList<Pair<String, String>>()
                // 2. Add ONLY what ML Kit says is ready
                for (model in models) {
                    val code = model.language
                    val name = Locale(code).displayLanguage

                    availableLanguages.add(Pair(name, code))
                }

                // Sort alphabetically
                availableLanguages.sortBy { it.first }

                // Check if list is empty (Optional: Show a message if nothing is downloaded yet)
                if (availableLanguages.isEmpty()) {
                    Toast.makeText(this, "No languages downloaded yet. Please wait.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // 3. Setup the Dialog (Visuals)
                val languageNames = availableLanguages.map { it.first }.toTypedArray()

                val titleView = TextView(this)
                titleView.text = if (isSource) "Select Source Language" else "Select Target Language"
                titleView.textSize = 20f
                titleView.setPadding(60, 60, 60, 30)
                titleView.setTextColor(Color.parseColor("#6200EE"))
                titleView.setTypeface(null, Typeface.BOLD)

                val adapter = object : ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_list_item_1,
                    languageNames
                ) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = super.getView(position, convertView, parent) as TextView
                        view.setTextColor(Color.BLACK)
                        view.textSize = 18f
                        return view
                    }
                }

                AlertDialog.Builder(this)
                    .setCustomTitle(titleView)
                    .setAdapter(adapter) { _, which ->
                        val selectedPair = availableLanguages[which]
                        val selectedName = selectedPair.first
                        val selectedCode = selectedPair.second

                        if (isSource) {
                            sourceLangCode = selectedCode
                            tvSourceLang.text = selectedName
                        } else {
                            targetLangCode = selectedCode
                            tvTargetLang.text = selectedName
                        }

                        tvResult.text = "Loading..."
                        prepareTranslator()
                    }
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching languages", Toast.LENGTH_SHORT).show()
            }
    }
    private fun prepareTranslator() {
        translator?.close()
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLangCode)
            .setTargetLanguage(targetLangCode)
            .build()
        translator = Translation.getClient(options)
        val conditions = DownloadConditions.Builder().requireWifi().build()
        translator?.downloadModelIfNeeded(conditions)
            ?.addOnSuccessListener {
                if (etInput.text.isNotEmpty()) translateText(etInput.text.toString())
            }
    }

    private fun translateText(text: String) {
        translator?.translate(text)
            ?.addOnSuccessListener { resultText ->
                tvResult.text = resultText

                // --- ADDED FOR STEP 2: POINTS SYSTEM ---
                // This adds +1 point to the target language every time a translation finishes
                FamiliarizationManager.addPoint(this, targetLangCode)
                // ---------------------------------------
            }
            ?.addOnFailureListener { tvResult.text = "Error" }
    }

    private fun swapLanguages() {
        val tempCode = sourceLangCode
        sourceLangCode = targetLangCode
        targetLangCode = tempCode
        val tempLabel = tvSourceLang.text
        tvSourceLang.text = tvTargetLang.text
        tvTargetLang.text = tempLabel
        etInput.setText("")
        tvResult.text = ""
        prepareTranslator()
    }

    override fun onDestroy() {
        super.onDestroy()
        translator?.close()
    }
}