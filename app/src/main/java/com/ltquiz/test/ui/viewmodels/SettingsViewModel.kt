package com.ltquiz.test.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ltquiz.test.managers.IdentityManager
import com.ltquiz.test.settings.SettingsManager
import com.ltquiz.test.settings.VideoQuality
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val deviceName: String = "",
    val deviceId: String = "",
    val defaultVideoEnabled: Boolean = true,
    val defaultAudioEnabled: Boolean = true,
    val videoQuality: VideoQuality = VideoQuality.HD,
    val autoJoinWithVideo: Boolean = true,
    val autoJoinWithAudio: Boolean = true,
    val showNetworkIndicator: Boolean = true,
    val enableSoundEffects: Boolean = true,
    val isLoading: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val identityManager: IdentityManager,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsManager.settings.collect { appSettings ->
                val displayName = identityManager.getDisplayName()
                val userId = identityManager.getUserId()
                _uiState.value = _uiState.value.copy(
                    deviceName = displayName,
                    deviceId = userId,
                    defaultVideoEnabled = appSettings.defaultVideoEnabled,
                    defaultAudioEnabled = appSettings.defaultAudioEnabled,
                    videoQuality = appSettings.videoQuality,
                    autoJoinWithVideo = appSettings.autoJoinWithVideo,
                    autoJoinWithAudio = appSettings.autoJoinWithAudio,
                    showNetworkIndicator = appSettings.showNetworkIndicator,
                    enableSoundEffects = appSettings.enableSoundEffects
                )
            }
        }
    }

    fun updateDeviceName(newName: String) {
        if (newName.isBlank()) return
        
        viewModelScope.launch {
            identityManager.setDisplayName(newName)
            _uiState.value = _uiState.value.copy(deviceName = newName)
        }
    }

    fun updateDefaultVideoEnabled(enabled: Boolean) {
        settingsManager.updateDefaultVideoEnabled(enabled)
    }

    fun updateDefaultAudioEnabled(enabled: Boolean) {
        settingsManager.updateDefaultAudioEnabled(enabled)
    }
    
    fun updateVideoQuality(quality: VideoQuality) {
        settingsManager.updateVideoQuality(quality)
    }
    
    fun updateAutoJoinWithVideo(enabled: Boolean) {
        settingsManager.updateAutoJoinWithVideo(enabled)
    }
    
    fun updateAutoJoinWithAudio(enabled: Boolean) {
        settingsManager.updateAutoJoinWithAudio(enabled)
    }
    
    fun updateShowNetworkIndicator(enabled: Boolean) {
        settingsManager.updateShowNetworkIndicator(enabled)
    }
    
    fun updateEnableSoundEffects(enabled: Boolean) {
        settingsManager.updateEnableSoundEffects(enabled)
    }
    
    fun resetToDefaults() {
        settingsManager.resetToDefaults()
        // Device name is not reset as it's part of identity
    }
}