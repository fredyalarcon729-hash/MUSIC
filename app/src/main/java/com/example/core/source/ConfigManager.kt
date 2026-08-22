package com.example.core.source

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.core.model.Song
import com.example.core.model.VisualizerStyle
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Manages application configuration and persistent settings using SharedPreferences.
 */
class ConfigManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("fusion_music_config", Context.MODE_PRIVATE)
    
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    companion object {
        private const val KEY_YOUTUBE_API_KEY = "youtube_api_key"
        private const val KEY_RAPID_API_KEY = "rapid_api_key"
        private const val KEY_GOOGLE_CLIENT_ID = "google_client_id"
        private const val KEY_THEME_MODE = "theme_mode" // "system", "light", "dark"
        private const val KEY_CUSTOM_ACCENT_COLOR = "custom_accent_color"
        private const val KEY_VISUALIZER_STYLE = "visualizer_style"
        private const val KEY_FILTER_VOICE_NOTES = "filter_voice_notes"
        private const val KEY_FILTER_DUPLICATES = "filter_duplicates"
        private const val KEY_AMBIENT_AURA_ENABLED = "ambient_aura_enabled"
        private const val KEY_AMBIENT_AURA_STYLE = "ambient_aura_style"
        private const val KEY_AMBIENT_AURA_INTENSITY = "ambient_aura_intensity"
        private const val KEY_AMBIENT_AURA_WEIGHT = "ambient_aura_weight"
        private const val KEY_SEARCH_HISTORY = "search_history"
        
        private const val KEY_LAST_SONG_JSON = "last_song_json"
        private const val KEY_LAST_QUEUE_JSON = "last_queue_json"
        private const val KEY_LAST_QUEUE_INDEX = "last_queue_index"
        private const val KEY_LAST_POSITION = "last_position"
        
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
     * Check if Ambient Aura effect is enabled.
     */
    fun isAmbientAuraEnabled(): Boolean {
        return prefs.getBoolean(KEY_AMBIENT_AURA_ENABLED, true)
    }

    /**
     * Save Ambient Aura enabled state.
     */
    fun setAmbientAuraEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AMBIENT_AURA_ENABLED, enabled) }
    }

    fun getAmbientAuraStyle(): com.example.core.model.AmbientAuraStyle {
        val name = prefs.getString(KEY_AMBIENT_AURA_STYLE, com.example.core.model.AmbientAuraStyle.MINIMAL_EDGE.name)
        return try { com.example.core.model.AmbientAuraStyle.valueOf(name ?: com.example.core.model.AmbientAuraStyle.MINIMAL_EDGE.name) } 
        catch (e: Exception) { com.example.core.model.AmbientAuraStyle.MINIMAL_EDGE }
    }

    fun saveAmbientAuraStyle(style: com.example.core.model.AmbientAuraStyle) {
        prefs.edit { putString(KEY_AMBIENT_AURA_STYLE, style.name) }
    }

    fun getAmbientAuraIntensity(): Float = prefs.getFloat(KEY_AMBIENT_AURA_INTENSITY, 0.4f)
    fun saveAmbientAuraIntensity(intensity: Float) = prefs.edit { putFloat(KEY_AMBIENT_AURA_INTENSITY, intensity) }

    fun getAmbientAuraWeight(): Float = prefs.getFloat(KEY_AMBIENT_AURA_WEIGHT, 0.8f)
    fun saveAmbientAuraWeight(weight: Float) = prefs.edit { putFloat(KEY_AMBIENT_AURA_WEIGHT, weight) }

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

    // Playback Persistence
    fun saveLastPlaybackState(song: Song?, queue: List<Song>, index: Int, position: Long) {
        prefs.edit {
            if (song != null) {
                val songJson = moshi.adapter(Song::class.java).toJson(song)
                putString(KEY_LAST_SONG_JSON, songJson)
            } else {
                remove(KEY_LAST_SONG_JSON)
            }

            if (queue.isNotEmpty()) {
                val listType = Types.newParameterizedType(List::class.java, Song::class.java)
                val queueJson = moshi.adapter<List<Song>>(listType).toJson(queue)
                putString(KEY_LAST_QUEUE_JSON, queueJson)
            } else {
                remove(KEY_LAST_QUEUE_JSON)
            }

            putInt(KEY_LAST_QUEUE_INDEX, index)
            putLong(KEY_LAST_POSITION, position)
        }
    }

    fun getLastSong(): Song? {
        val json = prefs.getString(KEY_LAST_SONG_JSON, null) ?: return null
        return try { moshi.adapter(Song::class.java).fromJson(json) } catch (e: Exception) { null }
    }

    fun getLastQueue(): List<Song> {
        val json = prefs.getString(KEY_LAST_QUEUE_JSON, null) ?: return emptyList()
        val listType = Types.newParameterizedType(List::class.java, Song::class.java)
        return try { moshi.adapter<List<Song>>(listType).fromJson(json) ?: emptyList() } catch (e: Exception) { emptyList() }
    }

    fun getLastQueueIndex(): Int = prefs.getInt(KEY_LAST_QUEUE_INDEX, -1)
    fun getLastPosition(): Long = prefs.getLong(KEY_LAST_POSITION, 0L)
}
