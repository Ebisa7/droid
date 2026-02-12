package com.ltquiz.test.models

import com.ltquiz.test.performance.VideoQuality

/**
 * Represents the current state of media (camera/microphone) with performance metrics.
 */
data class MediaState(
    val isVideoEnabled: Boolean = false,
    val isAudioEnabled: Boolean = false,
    val isFrontCamera: Boolean = true,
    val videoQuality: VideoQuality = VideoQuality.HIGH,
    val isHardwareAccelerationEnabled: Boolean = true,
    val currentFrameRate: Float = 0f,
    val droppedFrameCount: Int = 0
)