package com.ltquiz.test.ui.animations

import androidx.compose.animation.core.tween
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class AnimationUtilsTest {

    @Test
    fun `animation durations are within expected ranges`() {
        assertTrue(AnimationUtils.FAST_ANIMATION < AnimationUtils.MEDIUM_ANIMATION)
        assertTrue(AnimationUtils.MEDIUM_ANIMATION < AnimationUtils.SLOW_ANIMATION)
        
        assertEquals(200, AnimationUtils.FAST_ANIMATION)
        assertEquals(300, AnimationUtils.MEDIUM_ANIMATION)
        assertEquals(500, AnimationUtils.SLOW_ANIMATION)
    }

    @Test
    fun `easing curves are properly defined`() {
        assertNotNull(AnimationUtils.FastOutSlowInEasing)
        assertNotNull(AnimationUtils.LinearOutSlowInEasing)
        assertNotNull(AnimationUtils.FastOutLinearInEasing)
    }

    @Test
    fun `transition animations are created without errors`() {
        // Test that animation functions don't throw exceptions
        assertDoesNotThrow {
            AnimationUtils.slideInFromRight()
            AnimationUtils.slideOutToLeft()
            AnimationUtils.slideInFromLeft()
            AnimationUtils.slideOutToRight()
            AnimationUtils.fadeInTransition()
            AnimationUtils.fadeOutTransition()
            AnimationUtils.scaleInTransition()
            AnimationUtils.scaleOutTransition()
        }
    }
}