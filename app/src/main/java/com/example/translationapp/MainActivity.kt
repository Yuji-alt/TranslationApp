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

        // ... (Your existing findViewByIds remain the same) ...
        etInput = findViewById(R.id.etInputText)
        tvResult = findViewById(R.id.tvTranslatedText)
        tvSourceLang = findViewById(R.id.tvSourceLang)
        tvTargetLang = findViewById(R.id.tvTargetLang)
        btnSwitch = findViewById(R.id.btnSwitchLanguages)
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        val btnMenu = findViewById<ImageButton>(R.id.btnMenu)

        // 1. CALL THE NEW AUTO-DOWNLOAD FUNCTION HERE
        downloadDefaultLanguages()

        // 2. Initialize Translator
        prepareTranslator()

        // ... (Rest of your listeners remain the same) ...
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
                R.id.nav_saved -> { }
                R.id.nav_settings -> { }
                R.id.downloaded_language -> {
                    val intent = Intent(this, LanguageManagerActivity::class.java)
                    startActivity(intent)
                }
            }
            drawerLayout.closeDrawer(GravityCompat.END)
            true
        }

        btnSwitch.setOnClickListener { swapLanguages() }
    }

    // --- NEW FUNCTION: Downloads English and Tagalog automatically ---
    private fun downloadDefaultLanguages() {
        val languagesToDownload = listOf(TranslateLanguage.ENGLISH, TranslateLanguage.TAGALOG)

        // You can remove .requireWifi() if you want to allow mobile data
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        for (langCode in languagesToDownload) {
            val model = TranslateRemoteModel.Builder(langCode).build()

            modelManager.download(model, conditions)
                .addOnSuccessListener {
                    // Log success (Invisible to user, but nice for debugging)
                    Log.d("AutoDownload", "Model $langCode downloaded successfully")
                }
                .addOnFailureListener {
                    // Log failure (Maybe no internet)
                    Log.e("AutoDownload", "Failed to download $langCode")
                }
        }
    }

    // ... (Keep your showLanguageSelectionDialog, prepareTranslator, translateText, swapLanguages, onDestroy as is) ...
    // Note: In showLanguageSelectionDialog, you can keep your "manual add" code for Tagalog/English
    // just in case the download hasn't finished yet when they click the dropdown.

    private fun showLanguageSelectionDialog(isSource: Boolean) {
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener { models ->
                val availableLanguages = ArrayList<Pair<String, String>>()

                // IMPORTANT: Always add English and Tagalog manually so they appear in the list
                // even if they are still downloading in the background.
                availableLanguages.add(Pair("English", TranslateLanguage.ENGLISH))
                availableLanguages.add(Pair("Tagalog", TranslateLanguage.TAGALOG))

                for (model in models) {
                    val code = model.language
                    val name = Locale(code).displayLanguage

                    // Prevent duplicates
                    if (code != TranslateLanguage.ENGLISH && code != TranslateLanguage.TAGALOG) {
                        availableLanguages.add(Pair(name, code))
                    }
                }

                availableLanguages.sortBy { it.first }

                // ... (The rest of your Dialog code stays exactly the same) ...
                val languageNames = availableLanguages.map { it.first }.toTypedArray()

                val titleView = TextView(this)
                titleView.text = if (isSource) "Select Source Language" else "Select Target Language"
                titleView.textSize = 20f
                titleView.setPadding(60, 60, 60, 30)
                titleView.setTextColor(Color.parseColor("#6200EE")) // Changed R.color.purple to Color.parseColor for safety
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
    }

    // ... (Keep prepareTranslator, translateText, swapLanguages, onDestroy as is) ...
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
            ?.addOnSuccessListener { tvResult.text = it }
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