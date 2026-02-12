package com.ltquiz.test.managers

import com.ltquiz.test.models.MediaState
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.MediaStream
import org.webrtc.SurfaceViewRenderer

/**
 * Manages camera, microphone, and video rendering.
 */
interface MediaManager {
    /**
     * Current media state (video/audio enabled, camera direction).
     */
    val mediaState: StateFlow<MediaState>
    
    /**
     * Local video renderer for displaying own video.
     */
    val localVideoRenderer: StateFlow<SurfaceViewRenderer?>
    
    /**
     * Initializes local media stream with audio and video tracks.
     */
    fun initializeLocalMedia(): MediaStream?
    
    /**
     * Enables or disables video capture.
     */
    fun enableVideo(enable: Boolean)
    
    /**
     * Enables or disables audio capture.
     */
    fun enableAudio(enable: Boolean)
    
    /**
     * Switches between front and back camera.
     */
    fun switchCamera()
    
    /**
     * Sets the local video renderer for displaying own video.
     */
    fun setLocalVideoRenderer(renderer: SurfaceViewRenderer?)
    
    /**
     * Gets the current local media stream.
     */
    fun getLocalMediaStream(): MediaStream?
    
    /**
     * Stops video capture.
     */
    fun stopCapture()
    
    /**
     * Disposes all media resources.
     */
    fun dispose()
}