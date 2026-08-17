package com.example.core.source

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Manages application configuration and persistent settings using SharedPreferences.
 */
class ConfigManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("fusion_music_config", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_YOUTUBE_API_KEY = "youtube_api_key"
        private const val KEY_SEARCH_HISTORY = "search_history"
        private const val MAX_HISTORY_ITEMS = 10
    }

    /**
     * Get the stored YouTube API key.
     */
    fun getYouTubeApiKey(): String? {
        return prefs.getString(KEY_YOUTUBE_API_KEY, null)
    }

    /**
     * Save the YouTube API key.
     */
    fun saveYouTubeApiKey(apiKey: String?) {
        prefs.edit { putString(KEY_YOUTUBE_API_KEY, apiKey) }
    }

    /**
     * Get the recent search history.
     */
    fun getSearchHistory(): List<String> {
        val historyString = prefs.getString(KEY_SEARCH_HISTORY, "") ?: ""
        return if (historyString.isBlank()) emptyList() else historyString.split("|")
    }

    /**
     * Add a query to search history.
     */
    fun addSearchQuery(query: String) {
        val currentHistory = getSearchHistory().toMutableList()
        currentHistory.remove(query)
        currentHistory.add(0, query)
        val newHistory = currentHistory.take(MAX_HISTORY_ITEMS).joinToString("|")
        prefs.edit { putString(KEY_SEARCH_HISTORY, newHistory) }
    }

    /**
     * Clear the search history.
     */
    fun clearSearchHistory() {
        prefs.edit { remove(KEY_SEARCH_HISTORY) }
    }
}
