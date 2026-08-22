package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FusionApplication
import com.example.core.model.MusicSource
import com.example.core.model.VisualizerStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FusionApplication
    private val repository = app.musicRepository
    private val playerManager = app.playerManager
    private val servicesManager = app.servicesManager
    private val configManager = app.configManager

    private val _youtubeApiKey = MutableStateFlow(configManager.getYouTubeApiKey() ?: "")
    val youtubeApiKey: StateFlow<String> = _youtubeApiKey.asStateFlow()

    private val _rapidApiKey = MutableStateFlow(configManager.getRapidApiKey() ?: "")
    val rapidApiKey: StateFlow<String> = _rapidApiKey.asStateFlow()

    private val _googleClientId = MutableStateFlow(configManager.getGoogleClientId() ?: "")
    val googleClientId: StateFlow<String> = _googleClientId.asStateFlow()

    private val _themeMode = MutableStateFlow(configManager.getThemeMode())
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _customAccentColor = MutableStateFlow(configManager.getCustomAccentColor())
    val customAccentColor: StateFlow<Long> = _customAccentColor.asStateFlow()

    private val _visualizerStyle = MutableStateFlow(configManager.getVisualizerStyle())
    val visualizerStyle: StateFlow<VisualizerStyle> = _visualizerStyle.asStateFlow()

    private val _filterVoiceNotes = MutableStateFlow(configManager.isFilterVoiceNotesEnabled())
    val filterVoiceNotes: StateFlow<Boolean> = _filterVoiceNotes.asStateFlow()

    private val _filterDuplicates = MutableStateFlow(configManager.isFilterDuplicatesEnabled())
    val filterDuplicates: StateFlow<Boolean> = _filterDuplicates.asStateFlow()

    private val _isAmbientAuraEnabled = MutableStateFlow(configManager.isAmbientAuraEnabled())
    val isAmbientAuraEnabled: StateFlow<Boolean> = _isAmbientAuraEnabled.asStateFlow()

    private val _ambientAuraStyle = MutableStateFlow(configManager.getAmbientAuraStyle())
    val ambientAuraStyle = _ambientAuraStyle.asStateFlow()

    private val _ambientAuraIntensity = MutableStateFlow(configManager.getAmbientAuraIntensity())
    val ambientAuraIntensity = _ambientAuraIntensity.asStateFlow()

    private val _ambientAuraWeight = MutableStateFlow(configManager.getAmbientAuraWeight())
    val ambientAuraWeight = _ambientAuraWeight.asStateFlow()

    val servicesState = servicesManager.servicesState

    fun updateYouTubeApiKey(apiKey: String) {
        viewModelScope.launch {
            configManager.saveYouTubeApiKey(apiKey)
            _youtubeApiKey.value = apiKey
            repository.youtubeProvider.resolver.updateApiKey(apiKey)
        }
    }

    fun updateRapidApiKey(apiKey: String) {
        viewModelScope.launch {
            configManager.saveRapidApiKey(apiKey)
            _rapidApiKey.value = apiKey
            repository.youtubeProvider.resolver.updateRapidApiKey(apiKey)
        }
    }

    fun updateGoogleClientId(clientId: String) {
        viewModelScope.launch {
            val trimmed = clientId.trim()
            configManager.saveGoogleClientId(trimmed)
            _googleClientId.value = trimmed
        }
    }

    fun updateThemeMode(mode: String) {
        configManager.saveThemeMode(mode)
        _themeMode.value = mode
    }

    fun updateCustomAccentColor(color: Long) {
        configManager.saveCustomAccentColor(color)
        _customAccentColor.value = color
    }

    fun updateVisualizerStyle(style: VisualizerStyle) {
        configManager.saveVisualizerStyle(style)
        _visualizerStyle.value = style
        playerManager.setVisualizerStyle(style)
    }

    fun setFilterVoiceNotesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            configManager.setFilterVoiceNotesEnabled(enabled)
            _filterVoiceNotes.value = enabled
            repository.rescanLocalMusic()
        }
    }

    fun setFilterDuplicatesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            configManager.setFilterDuplicatesEnabled(enabled)
            _filterDuplicates.value = enabled
            repository.rescanLocalMusic()
        }
    }

    fun setAmbientAuraEnabled(enabled: Boolean) {
        configManager.setAmbientAuraEnabled(enabled)
        _isAmbientAuraEnabled.value = enabled
        playerManager.setAmbientAuraEnabled(enabled)
    }

    fun updateAmbientAuraStyle(style: com.example.core.model.AmbientAuraStyle) {
        configManager.saveAmbientAuraStyle(style)
        _ambientAuraStyle.value = style
        playerManager.setAmbientAuraStyle(style)
    }

    fun updateAmbientAuraIntensity(intensity: Float) {
        configManager.saveAmbientAuraIntensity(intensity)
        _ambientAuraIntensity.value = intensity
        playerManager.setAmbientAuraIntensity(intensity)
    }

    fun updateAmbientAuraWeight(weight: Float) {
        configManager.saveAmbientAuraWeight(weight)
        _ambientAuraWeight.value = weight
        playerManager.setAmbientAuraWeight(weight)
    }

    fun toggleExternalService(source: MusicSource, enabled: Boolean) {
        viewModelScope.launch {
            servicesManager.toggleService(source, enabled)
        }
    }
}
