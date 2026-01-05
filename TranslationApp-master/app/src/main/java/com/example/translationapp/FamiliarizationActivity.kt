package com.example.translationapp

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class FamiliarizationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_familiarization)

        // Set the title of the top bar
        supportActionBar?.title = "Proficiency Levels"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Find the empty container in our layout where we will add the cards
        val container = findViewById<LinearLayout>(R.id.statsContainer)

        // Get the data from our helper class
        val allData = FamiliarizationManager.getAllPracticedLanguages(this)

        if (allData.isEmpty()) {
            val emptyMsg = TextView(this)
            emptyMsg.text = "No translations yet. Start translating to earn points!"
            emptyMsg.textSize = 16f
            emptyMsg.setPadding(20, 50, 20, 0)
            container.addView(emptyMsg)
        } else {
            // Loop through every language you have points for and create a card
            for ((langCode, points) in allData) {
                addLanguageCard(container, langCode, points)
            }
        }
    }

    private fun addLanguageCard(container: LinearLayout, code: String, points: Int) {
        val langName = Locale(code).displayLanguage
        val level = FamiliarizationManager.getLevel(points)

        // 1. Create the main card container
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(40, 40, 40, 40)
        card.setBackgroundColor(Color.WHITE)

        // Add spacing between cards
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, 30)
        card.layoutParams = params

        // 2. Text for Language Name (e.g., "Spanish")
        val nameView = TextView(this)
        nameView.text = "$langName ($code)"
        nameView.textSize = 20f
        nameView.setTypeface(null, Typeface.BOLD)
        nameView.setTextColor(Color.BLACK)

        // 3. Text for Score (e.g., "Score: 5 / 10000")
        val statsView = TextView(this)
        statsView.text = "Score: $points"
        statsView.textSize = 16f

        // 4. Text for Rank (e.g., "Rank: Novice")
        val levelView = TextView(this)
        levelView.text = "Rank: $level"
        levelView.textSize = 16f
        levelView.setTextColor(Color.parseColor("#6200EE")) // Purple color
        levelView.setTypeface(null, Typeface.BOLD)

        // Add all text views to the card, then add the card to the screen
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