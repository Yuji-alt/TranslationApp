package com.example.translationapp

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import java.util.Locale

class RecognitionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recognition)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Detect Language"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val etInput = findViewById<EditText>(R.id.etInputText)
        val btnDetect = findViewById<Button>(R.id.btnDetect)
        val tvResult = findViewById<TextView>(R.id.tvResult)

        // Very low threshold to catch single words
        val options = LanguageIdentificationOptions.Builder()
            .setConfidenceThreshold(0.25f)   // good balance for short/long text
            .build()

        val languageIdentifier = LanguageIdentification.getClient(options)
        val modelManager = RemoteModelManager.getInstance()

        btnDetect.setOnClickListener {
            val text = etInput.text.toString().trim()

            if (text.isEmpty()) {
                Toast.makeText(this, "Please type something first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            tvResult.text = "Analyzing..."
            tvResult.setTextColor(Color.BLACK)

            modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
                .addOnSuccessListener { downloadedModels ->

                    val downloadedCodes = downloadedModels.map { it.language }.toSet()

                    languageIdentifier.identifyPossibleLanguages(text)
                        .addOnSuccessListener { candidates ->

                            if (candidates.isEmpty()) {
                                tvResult.text = "Cannot identify language."
                                return@addOnSuccessListener
                            }

                            // ---- Normalize + map ----
                            val mapped = candidates.map { result ->
                                var tag = result.languageTag
                                val conf = result.confidence

                                // Filipino / Tagalog handling
                                if (tag == "ceb" || tag == "ilo") tag = "tl"
                                if (tag == "fil") tag = "tl"

                                tag to conf
                            }

                            // ---- keep only supported translator languages ----
                            val supported = mapped.filter { (tag, _) ->
                                TranslateLanguage.getAllLanguages().contains(tag)
                            }

                            if (supported.isEmpty()) {
                                tvResult.text = "Detected language is not supported."
                                return@addOnSuccessListener
                            }

                            // ---- choose best by confidence first ----
                            val best = supported.maxByOrNull { it.second }!!

                            var selectedCode = best.first

                            // ---- if offline available, prefer that instead ----
                            supported.forEach { (code, _) ->
                                if (downloadedCodes.contains(code)) {
                                    selectedCode = code
                                    return@forEach
                                }
                            }

                            val languageName = Locale(selectedCode).displayLanguage
                            val isDownloaded = downloadedCodes.contains(selectedCode)

                            if (isDownloaded) {
                                tvResult.text =
                                    "Detected: $languageName ($selectedCode)\n✅ Available Offline"
                                tvResult.setTextColor(Color.parseColor("#4CAF50"))
                            } else {
                                tvResult.text =
                                    "Detected: $languageName ($selectedCode)\n⚠ Model not downloaded"
                                tvResult.setTextColor(Color.parseColor("#FF9800"))
                            }
                        }
                        .addOnFailureListener {
                            tvResult.text = "Error: Could not identify language."
                            tvResult.setTextColor(Color.RED)
                        }
                }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}