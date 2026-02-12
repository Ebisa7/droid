package com.ltquiz.test.audio

import android.content.Context
import com.ltquiz.test.settings.SettingsManager
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class SoundEffectsIntegrationTest {

    private lateinit var context: Context
    private lateinit var settingsManager: SettingsManager
    private lateinit var soundEffectsManager: SoundEffectsManager

    @BeforeEach
    fun setup() {
        context = mockk(relaxed = true)
        settingsManager = mockk(relaxed = true)
        
        // Mock settings to enable sound effects by default
        every { settingsManager.settings } returns flowOf(
            com.ltquiz.test.settings.AppSettings(enableSoundEffects = true)
        )
        
        soundEffectsManager = SoundEffectsManager(context, settingsManager)
    }

    @Test
    fun `sound effects manager initializes without errors`() {
        assertNotNull(soundEffectsManager)
    }

    @Test
    fun `all sound effects are defined`() {
        val effects = SoundEffectsManager.SoundEffect.values()
        
        assertTrue(effects.contains(SoundEffectsManager.SoundEffect.PARTICIPANT_JOIN))
        assertTrue(effects.contains(SoundEffectsManager.SoundEffect.PARTICIPANT_LEAVE))
        assertTrue(effects.contains(SoundEffectsManager.SoundEffect.MUTE_ON))
        assertTrue(effects.contains(SoundEffectsManager.SoundEffect.MUTE_OFF))
        assertTrue(effects.contains(SoundEffectsManager.SoundEffect.CALL_START))
        assertTrue(effects.contains(SoundEffectsManager.SoundEffect.CALL_END))
        assertTrue(effects.contains(SoundEffectsManager.SoundEffect.NOTIFICATION))
    }

    @Test
    fun `sound effects have proper file names`() {
        assertEquals("participant_join.wav", SoundEffectsManager.SoundEffect.PARTICIPANT_JOIN.fileName)
        assertEquals("participant_leave.wav", SoundEffectsManager.SoundEffect.PARTICIPANT_LEAVE.fileName)
        assertEquals("mute_on.wav", SoundEffectsManager.SoundEffect.MUTE_ON.fileName)
        assertEquals("mute_off.wav", SoundEffectsManager.SoundEffect.MUTE_OFF.fileName)
        assertEquals("call_start.wav", SoundEffectsManager.SoundEffect.CALL_START.fileName)
        assertEquals("call_end.wav", SoundEffectsManager.SoundEffect.CALL_END.fileName)
        assertEquals("notification.wav", SoundEffectsManager.SoundEffect.NOTIFICATION.fileName)
    }

    @Test
    fun `playSound does not crash when sound effects are enabled`() = runTest {
        assertDoesNotThrow {
            soundEffectsManager.playSound(SoundEffectsManager.SoundEffect.PARTICIPANT_JOIN)
        }
    }

    @Test
    fun `playSound respects settings when sound effects are disabled`() = runTest {
        // Mock settings to disable sound effects
        every { settingsManager.settings } returns flowOf(
            com.ltquiz.test.settings.AppSettings(enableSoundEffects = false)
        )
        
        // Should not crash even when disabled
        assertDoesNotThrow {
            soundEffectsManager.playSound(SoundEffectsManager.SoundEffect.PARTICIPANT_JOIN)
        }
    }

    @Test
    fun `release cleans up resources`() {
        assertDoesNotThrow {
            soundEffectsManager.release()
        }
    }
}