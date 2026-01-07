package com.example.translationapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.nl.languageid.LanguageIdentification
import java.util.Locale

class RecognitionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recognition)

        supportActionBar?.title = "Detect Language"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val etInput = findViewById<EditText>(R.id.etInputText)
        val btnDetect = findViewById<Button>(R.id.btnDetect)
        val tvResult = findViewById<TextView>(R.id.tvResult)

        // Initialize the Offline Language Identifier
        val languageIdentifier = LanguageIdentification.getClient()

        btnDetect.setOnClickListener {
            val text = etInput.text.toString()

            if (text.isEmpty()) {
                Toast.makeText(this, "Please type something first", Toast.LENGTH_SHORT).show()
            } else {
                tvResult.text = "Analyzing..."

                languageIdentifier.identifyLanguage(text)
                    .addOnSuccessListener { languageCode ->
                        if (languageCode == "und") {
                            tvResult.text = "Cannot identify language"
                        } else {
                            // Convert "es" -> "Spanish"
                            val languageName = Locale(languageCode).displayLanguage
                            tvResult.text = "Detected: $languageName ($languageCode)"
                        }
                    }
                    .addOnFailureListener {
                        tvResult.text = "Error identifying language"
                    }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}