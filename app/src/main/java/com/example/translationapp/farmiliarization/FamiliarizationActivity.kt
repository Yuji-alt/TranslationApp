package com.example.translationapp.farmiliarization

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar // Import this
import com.example.translationapp.R
import java.util.Locale

class FamiliarizationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_familiarization)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.title = "Proficiency Levels"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val container = findViewById<LinearLayout>(R.id.statsContainer)
        val allData = FamiliarizationManager.getAllPracticedLanguages(this)

        if (allData.isEmpty()) {
            val emptyMsg = TextView(this)
            emptyMsg.text = "No translations yet. Start translating to earn points!"
            emptyMsg.textSize = 16f
            emptyMsg.setPadding(0, 50, 0, 0) // Adjusted padding
            container.addView(emptyMsg)
        } else {
            for ((langCode, points) in allData) {
                addLanguageCard(container, langCode, points)
            }
        }
    }
    private fun addLanguageCard(container: LinearLayout, code: String, points: Int) {
        val langName = Locale(code).displayLanguage
        val level = FamiliarizationManager.getLevel(points)

        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(40, 40, 40, 40)
        card.setBackgroundColor(Color.WHITE)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, 30)
        card.layoutParams = params

        val nameView = TextView(this)
        nameView.text = "$langName ($code)"
        nameView.textSize = 20f
        nameView.setTypeface(null, Typeface.BOLD)
        nameView.setTextColor(Color.BLACK)

        val statsView = TextView(this)
        statsView.text = "Score: $points"
        statsView.textSize = 16f

        val levelView = TextView(this)
        levelView.text = "Rank: $level"
        levelView.textSize = 16f
        levelView.setTextColor(Color.parseColor("#6200EE"))
        levelView.setTypeface(null, Typeface.BOLD)

        card.addView(nameView)
        card.addView(statsView)
        card.addView(levelView)
        container.addView(card)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}