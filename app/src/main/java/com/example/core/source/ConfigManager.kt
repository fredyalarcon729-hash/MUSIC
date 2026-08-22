package com.example.core.source

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.core.model.VisualizerStyle

/**
 * Manages application configuration and persistent settings using SharedPreferences.
 */
class ConfigManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("fusion_music_config", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_YOUTUBE_API_KEY = "youtube_api_key"
        private const val KEY_RAPID_API_KEY = "rapid_api_key"
        private const val KEY_GOOGLE_CLIENT_ID = "google_client_id"
        private const val KEY_THEME_MODE = "theme_mode" // "system", "light", "dark"
        private const val KEY_CUSTOM_ACCENT_COLOR = "custom_accent_color"
        private const val KEY_VISUALIZER_STYLE = "visualizer_style"
        private const val KEY_FILTER_VOICE_NOTES = "filter_voice_notes"
        private const val KEY_FILTER_DUPLICATES = "filter_duplicates"
        private const val KEY_SEARCH_HISTORY = "search_history"
        private const val MAX_HISTORY_ITEMS = 10
    }

    /**
     * Get the stored theme mode.
     */
    fun getThemeMode(): String {
        return prefs.getString(KEY_THEME_MODE, "dark") ?: "dark"
    }

    /**
     * Save the theme mode.
     */
    fun saveThemeMode(mode: String) {
        prefs.edit { putString(KEY_THEME_MODE, mode) }
    }

    /**
     * Get the stored custom accent color (Long hex).
     * If 0, it means use dynamic color from artwork.
     */
    fun getCustomAccentColor(): Long {
        return prefs.getLong(KEY_CUSTOM_ACCENT_COLOR, 0L)
    }

    /**
     * Save the custom accent color.
     */
    fun saveCustomAccentColor(color: Long) {
        prefs.edit { putLong(KEY_CUSTOM_ACCENT_COLOR, color) }
    }

    /**
     * Get the stored visualizer style.
     */
    fun getVisualizerStyle(): VisualizerStyle {
        val name = prefs.getString(KEY_VISUALIZER_STYLE, VisualizerStyle.BARS.name)
        return try { VisualizerStyle.valueOf(name ?: VisualizerStyle.BARS.name) } catch (e: Exception) { VisualizerStyle.BARS }
    }

    /**
     * Save the visualizer style.
     */
    fun saveVisualizerStyle(style: VisualizerStyle) {
        prefs.edit { putString(KEY_VISUALIZER_STYLE, style.name) }
    }

    /**
     * Check if voice notes and chat audio should be filtered out.
     */
    fun isFilterVoiceNotesEnabled(): Boolean {
        return prefs.getBoolean(KEY_FILTER_VOICE_NOTES, true)
    }

    /**
     * Save whether to filter voice notes.
     */
    fun setFilterVoiceNotesEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_FILTER_VOICE_NOTES, enabled) }
    }

    /**
     * Check if duplicate songs should be filtered out.
     */
    fun isFilterDuplicatesEnabled(): Boolean {
        return prefs.getBoolean(KEY_FILTER_DUPLICATES, true)
    }

    /**
     * Save whether to filter duplicate songs.
     */
    fun setFilterDuplicatesEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_FILTER_DUPLICATES, enabled) }
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
     * Get the stored RapidAPI key.
     */
    fun getRapidApiKey(): String? {
        return prefs.getString(KEY_RAPID_API_KEY, null)
    }

    /**
     * Save the RapidAPI key.
     */
    fun saveRapidApiKey(apiKey: String?) {
        prefs.edit { putString(KEY_RAPID_API_KEY, apiKey) }
    }

    /**
     * Get the stored Google Client ID.
     */
    fun getGoogleClientId(): String? {
        return prefs.getString(KEY_GOOGLE_CLIENT_ID, null)
    }

    /**
     * Save the Google Client ID.
     */
    fun saveGoogleClientId(clientId: String?) {
        prefs.edit { putString(KEY_GOOGLE_CLIENT_ID, clientId) }
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
