package com.ltquiz.test.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AppSettings(
    val defaultVideoEnabled: Boolean = true,
    val defaultAudioEnabled: Boolean = true,
    val videoQuality: VideoQuality = VideoQuality.HD,
    val autoJoinWithVideo: Boolean = true,
    val autoJoinWithAudio: Boolean = true,
    val showNetworkIndicator: Boolean = true,
    val enableSoundEffects: Boolean = true
)

enum class VideoQuality(val displayName: String, val width: Int, val height: Int, val fps: Int) {
    SD("Standard (480p)", 640, 480, 30),
    HD("High (720p)", 1280, 720, 30),
    FHD("Full HD (1080p)", 1920, 1080, 30)
}

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "xsend_meet_settings"
        private const val KEY_DEFAULT_VIDEO_ENABLED = "default_video_enabled"
        private const val KEY_DEFAULT_AUDIO_ENABLED = "default_audio_enabled"
        private const val KEY_VIDEO_QUALITY = "video_quality"
        private const val KEY_AUTO_JOIN_VIDEO = "auto_join_video"
        private const val KEY_AUTO_JOIN_AUDIO = "auto_join_audio"
        private const val KEY_SHOW_NETWORK_INDICATOR = "show_network_indicator"
        private const val KEY_ENABLE_SOUND_EFFECTS = "enable_sound_effects"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    
    private fun loadSettings(): AppSettings {
        return AppSettings(
            defaultVideoEnabled = prefs.getBoolean(KEY_DEFAULT_VIDEO_ENABLED, true),
            defaultAudioEnabled = prefs.getBoolean(KEY_DEFAULT_AUDIO_ENABLED, true),
            videoQuality = VideoQuality.valueOf(
                prefs.getString(KEY_VIDEO_QUALITY, VideoQuality.HD.name) ?: VideoQuality.HD.name
            ),
            autoJoinWithVideo = prefs.getBoolean(KEY_AUTO_JOIN_VIDEO, true),
            autoJoinWithAudio = prefs.getBoolean(KEY_AUTO_JOIN_AUDIO, true),
            showNetworkIndicator = prefs.getBoolean(KEY_SHOW_NETWORK_INDICATOR, true),
            enableSoundEffects = prefs.getBoolean(KEY_ENABLE_SOUND_EFFECTS, true)
        )
    }
    
    fun updateDefaultVideoEnabled(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_DEFAULT_VIDEO_ENABLED, enabled)
        }
        _settings.value = _settings.value.copy(defaultVideoEnabled = enabled)
    }
    
    fun updateDefaultAudioEnabled(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_DEFAULT_AUDIO_ENABLED, enabled)
        }
        _settings.value = _settings.value.copy(defaultAudioEnabled = enabled)
    }
    
    fun updateVideoQuality(quality: VideoQuality) {
        prefs.edit {
            putString(KEY_VIDEO_QUALITY, quality.name)
        }
        _settings.value = _settings.value.copy(videoQuality = quality)
    }
    
    fun updateAutoJoinWithVideo(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_AUTO_JOIN_VIDEO, enabled)
        }
        _settings.value = _settings.value.copy(autoJoinWithVideo = enabled)
    }
    
    fun updateAutoJoinWithAudio(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_AUTO_JOIN_AUDIO, enabled)
        }
        _settings.value = _settings.value.copy(autoJoinWithAudio = enabled)
    }
    
    fun updateShowNetworkIndicator(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_SHOW_NETWORK_INDICATOR, enabled)
        }
        _settings.value = _settings.value.copy(showNetworkIndicator = enabled)
    }
    
    fun updateEnableSoundEffects(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_ENABLE_SOUND_EFFECTS, enabled)
        }
        _settings.value = _settings.value.copy(enableSoundEffects = enabled)
    }
    
    fun resetToDefaults() {
        prefs.edit {
            clear()
        }
        _settings.value = AppSettings()
    }
    
    // Convenience methods for getting current values
    fun isDefaultVideoEnabled(): Boolean = _settings.value.defaultVideoEnabled
    fun isDefaultAudioEnabled(): Boolean = _settings.value.defaultAudioEnabled
    fun getCurrentVideoQuality(): VideoQuality = _settings.value.videoQuality
    fun shouldAutoJoinWithVideo(): Boolean = _settings.value.autoJoinWithVideo
    fun shouldAutoJoinWithAudio(): Boolean = _settings.value.autoJoinWithAudio
    fun shouldShowNetworkIndicator(): Boolean = _settings.value.showNetworkIndicator
    fun areSoundEffectsEnabled(): Boolean = _settings.value.enableSoundEffects
}