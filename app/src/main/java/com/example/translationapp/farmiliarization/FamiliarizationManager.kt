package com.example.translationapp.farmiliarization

import android.content.Context

object FamiliarizationManager {
    private const val PREFS_NAME = "LanguagePoints"

    // Add 1 point to a specific language
    fun addPoint(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentPoints = prefs.getInt(languageCode, 0)
        prefs.edit().putInt(languageCode, currentPoints + 1).apply()
    }

    // Get the points for a language
    fun getPoints(context: Context, languageCode: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(languageCode, 0)
    }

    // Get all languages the user has practiced
    fun getAllPracticedLanguages(context: Context): Map<String, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.all.mapValues { it.value as Int }
    }

    // Determine the "Rank" based on your image
    fun getLevel(points: Int): String {
        return when (points) {
            in 0..249 -> "Novice"
            in 250..999 -> "Functional Beginner (A1)"
            in 1000..1999 -> "Elementary (A2/B1)"
            in 2000..3999 -> "Upper Intermediate (B2)"
            else -> "Advanced/Fluent (C1/C2)"
        }
    }
}