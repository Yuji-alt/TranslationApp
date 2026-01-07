package com.example.translationapp.dataClass

data class LanguageModel(
    val code: String,       // e.g., "en", "tl"
    val name: String,       // e.g., "English", "Tagalog"
    var isDownloaded: Boolean = false, // Status
    var isDownloading: Boolean = false
)