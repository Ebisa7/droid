package com.ltquiz.test.managers

import android.content.Context
import android.hardware.camera2.CameraManager
import com.ltquiz.test.models.MediaState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaManagerImpl @Inject constructor(
    private val context: Context
) : MediaManager {

    private val peerConnectionFactory: PeerConnectionFactory by lazy {
        initializePeerConnectionFactory()
    }

    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var localMediaStream: MediaStream? = null

    private val _mediaState = MutableStateFlow(
        MediaState(
            isVideoEnabled = false,
            isAudioEnabled = false,
            isFrontCamera = true
        )
    )
    override val mediaState: StateFlow<MediaState> = _mediaState

    private val _localVideoRenderer = MutableStateFlow<SurfaceViewRenderer?>(null)
    override val localVideoRenderer: StateFlow<SurfaceViewRenderer?> = _localVideoRenderer

    override fun initializeLocalMedia(): MediaStream? {
        try {
            val stream = peerConnectionFactory.createLocalMediaStream("local_stream")
            
            // Initialize audio track
            val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
            localAudioTrack = peerConnectionFactory.createAudioTrack("audio_track", audioSource)
            stream.addTrack(localAudioTrack)

            // Initialize video track
            initializeVideoTrack()?.let { videoTrack ->
                localVideoTrack = videoTrack
                stream.addTrack(videoTrack)
            }

            localMediaStream = stream
            updateMediaState(isVideoEnabled = true, isAudioEnabled = true)
            
            return stream
        } catch (e: Exception) {
            return null
        }
    }

    override fun enableVideo(enable: Boolean) {
        localVideoTrack?.setEnabled(enable)
        if (enable && localVideoTrack == null) {
            // Re-initialize video if it was null
            initializeVideoTrack()?.let { videoTrack ->
                localVideoTrack = videoTrack
                localMediaStream?.addTrack(videoTrack)
            }
        }
        updateMediaState(isVideoEnabled = enable)
    }

    override fun enableAudio(enable: Boolean) {
        localAudioTrack?.setEnabled(enable)
        updateMediaState(isAudioEnabled = enable)
    }

    override fun switchCamera() {
        videoCapturer?.let { capturer ->
            if (capturer is CameraVideoCapturer) {
                capturer.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
                    override fun onCameraSwitchDone(isFrontCamera: Boolean) {
                        updateMediaState(isFrontCamera = isFrontCamera)
                    }

                    override fun onCameraSwitchError(errorDescription: String?) {
                        // Handle camera switch error
                    }
                })
            }
        }
    }

    override fun setLocalVideoRenderer(renderer: SurfaceViewRenderer?) {
        _localVideoRenderer.value = renderer
        renderer?.let { 
            localVideoTrack?.addSink(it)
        }
    }

    override fun getLocalMediaStream(): MediaStream? = localMediaStream

    override fun stopCapture() {
        try {
            videoCapturer?.stopCapture()
        } catch (e: Exception) {
            // Handle stop capture error
        }
    }

    override fun dispose() {
        stopCapture()
        localVideoTrack?.dispose()
        localAudioTrack?.dispose()
        localMediaStream?.dispose()
        videoCapturer?.dispose()
        
        localVideoTrack = null
        localAudioTrack = null
        localMediaStream = null
        videoCapturer = null
        
        updateMediaState(isVideoEnabled = false, isAudioEnabled = false)
    }

    private fun initializeVideoTrack(): VideoTrack? {
        return try {
            val videoCapturer = createCameraVideoCapturer() ?: return null
            this.videoCapturer = videoCapturer

            val videoSource = peerConnectionFactory.createVideoSource(false)
            val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", null)
            
            videoCapturer.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
            videoCapturer.startCapture(1280, 720, 30) // 720p@30fps

            peerConnectionFactory.createVideoTrack("video_track", videoSource)
        } catch (e: Exception) {
            null
        }
    }

    private fun createCameraVideoCapturer(): CameraVideoCapturer? {
        val cameraEnumerator = Camera2Enumerator(context)
        
        // Try front camera first
        for (deviceName in cameraEnumerator.deviceNames) {
            if (cameraEnumerator.isFrontFacing(deviceName)) {
                val capturer = cameraEnumerator.createCapturer(deviceName, null)
                if (capturer != null) {
                    updateMediaState(isFrontCamera = true)
                    return capturer
                }
            }
        }
        
        // Fallback to back camera
        for (deviceName in cameraEnumerator.deviceNames) {
            if (!cameraEnumerator.isFrontFacing(deviceName)) {
                val capturer = cameraEnumerator.createCapturer(deviceName, null)
                if (capturer != null) {
                    updateMediaState(isFrontCamera = false)
                    return capturer
                }
            }
        }
        
        return null
    }

    private fun updateMediaState(
        isVideoEnabled: Boolean? = null,
        isAudioEnabled: Boolean? = null,
        isFrontCamera: Boolean? = null
    ) {
        val currentState = _mediaState.value
        _mediaState.value = currentState.copy(
            isVideoEnabled = isVideoEnabled ?: currentState.isVideoEnabled,
            isAudioEnabled = isAudioEnabled ?: currentState.isAudioEnabled,
            isFrontCamera = isFrontCamera ?: currentState.isFrontCamera
        )
    }

    private fun initializePeerConnectionFactory(): PeerConnectionFactory {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )

        return PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(null, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(null))
            .createPeerConnectionFactory()
    }
}