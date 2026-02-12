package com.ltquiz.test.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.ltquiz.test.settings.SettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundEffectsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager
) {
    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<SoundEffect, Int>()
    
    enum class SoundEffect(val fileName: String) {
        PARTICIPANT_JOIN("participant_join.wav"),
        PARTICIPANT_LEAVE("participant_leave.wav"),
        MUTE_ON("mute_on.wav"),
        MUTE_OFF("mute_off.wav"),
        CALL_START("call_start.wav"),
        CALL_END("call_end.wav"),
        NOTIFICATION("notification.wav")
    }
    
    init {
        initializeSoundPool()
    }
    
    private fun initializeSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()
        
        // Load sound effects (in a real app, you'd have actual sound files)
        // For now, we'll use system sounds or create simple tones
        loadSoundEffects()
    }
    
    private fun loadSoundEffects() {
        // In a real implementation, you would load actual sound files from assets
        // For this demo, we'll simulate loading sounds
        SoundEffect.values().forEach { effect ->
            // soundIds[effect] = soundPool?.load(context.assets.openFd(effect.fileName), 1) ?: 0
            soundIds[effect] = 1 // Placeholder
        }
    }
    
    suspend fun playSound(effect: SoundEffect) {
        if (!settingsManager.settings.first().enableSoundEffects) return
        
        soundPool?.let { pool ->
            soundIds[effect]?.let { soundId ->
                pool.play(soundId, 0.5f, 0.5f, 1, 0, 1.0f)
            }
        }
    }
    
    fun release() {
        soundPool?.release()
        soundPool = null
        soundIds.clear()
    }
}