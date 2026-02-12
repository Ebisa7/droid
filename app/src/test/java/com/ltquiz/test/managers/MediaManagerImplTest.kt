package com.ltquiz.test.managers

import android.content.Context
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.webrtc.*

class MediaManagerImplTest {

    private lateinit var context: Context
    private lateinit var mediaManager: MediaManagerImpl
    private lateinit var mockPeerConnectionFactory: PeerConnectionFactory
    private lateinit var mockMediaStream: MediaStream
    private lateinit var mockVideoTrack: VideoTrack
    private lateinit var mockAudioTrack: AudioTrack

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        mockPeerConnectionFactory = mockk(relaxed = true)
        mockMediaStream = mockk(relaxed = true)
        mockVideoTrack = mockk(relaxed = true)
        mockAudioTrack = mockk(relaxed = true)
        
        mockkStatic(PeerConnectionFactory::class)
        every { PeerConnectionFactory.initialize(any()) } just Runs
        every { PeerConnectionFactory.builder() } returns mockk {
            every { setVideoEncoderFactory(any()) } returns this
            every { setVideoDecoderFactory(any()) } returns this
            every { createPeerConnectionFactory() } returns mockPeerConnectionFactory
        }
        
        every { mockPeerConnectionFactory.createLocalMediaStream(any()) } returns mockMediaStream
        every { mockPeerConnectionFactory.createAudioSource(any()) } returns mockk(relaxed = true)
        every { mockPeerConnectionFactory.createAudioTrack(any(), any()) } returns mockAudioTrack
        every { mockPeerConnectionFactory.createVideoSource(any()) } returns mockk(relaxed = true)
        every { mockPeerConnectionFactory.createVideoTrack(any(), any()) } returns mockVideoTrack
        
        every { mockMediaStream.addTrack(any()) } returns true
        
        mediaManager = MediaManagerImpl(context)
    }

    @Test
    fun `initializeLocalMedia should create media stream with audio and video tracks`() {
        mockkConstructor(Camera2Enumerator::class)
        every { anyConstructed<Camera2Enumerator>().deviceNames } returns arrayOf("front_camera")
        every { anyConstructed<Camera2Enumerator>().isFrontFacing("front_camera") } returns true
        every { anyConstructed<Camera2Enumerator>().createCapturer("front_camera", null) } returns mockk(relaxed = true)
        
        mockkStatic(SurfaceTextureHelper::class)
        every { SurfaceTextureHelper.create(any(), any()) } returns mockk(relaxed = true)

        val result = mediaManager.initializeLocalMedia()

        assertNotNull(result)
        assertEquals(mockMediaStream, result)
        verify { mockMediaStream.addTrack(mockAudioTrack) }
        verify { mockMediaStream.addTrack(mockVideoTrack) }
    }

    @Test
    fun `enableVideo should enable or disable video track`() {
        every { mockVideoTrack.setEnabled(any()) } just Runs
        
        // Setup media first
        mediaManager.initializeLocalMedia()
        
        mediaManager.enableVideo(false)
        verify { mockVideoTrack.setEnabled(false) }
        
        mediaManager.enableVideo(true)
        verify { mockVideoTrack.setEnabled(true) }
    }

    @Test
    fun `enableAudio should enable or disable audio track`() {
        every { mockAudioTrack.setEnabled(any()) } just Runs
        
        // Setup media first
        mediaManager.initializeLocalMedia()
        
        mediaManager.enableAudio(false)
        verify { mockAudioTrack.setEnabled(false) }
        
        mediaManager.enableAudio(true)
        verify { mockAudioTrack.setEnabled(true) }
    }

    @Test
    fun `switchCamera should switch between front and back camera`() {
        val mockCapturer = mockk<CameraVideoCapturer>(relaxed = true)
        
        mockkConstructor(Camera2Enumerator::class)
        every { anyConstructed<Camera2Enumerator>().deviceNames } returns arrayOf("front_camera")
        every { anyConstructed<Camera2Enumerator>().isFrontFacing("front_camera") } returns true
        every { anyConstructed<Camera2Enumerator>().createCapturer("front_camera", null) } returns mockCapturer
        
        mockkStatic(SurfaceTextureHelper::class)
        every { SurfaceTextureHelper.create(any(), any()) } returns mockk(relaxed = true)
        
        every { mockCapturer.switchCamera(any()) } answers {
            val handler = firstArg<CameraVideoCapturer.CameraSwitchHandler>()
            handler.onCameraSwitchDone(false) // Switched to back camera
        }

        mediaManager.initializeLocalMedia()
        mediaManager.switchCamera()

        verify { mockCapturer.switchCamera(any()) }
    }

    @Test
    fun `setLocalVideoRenderer should add video sink to renderer`() {
        val mockRenderer = mockk<SurfaceViewRenderer>(relaxed = true)
        every { mockVideoTrack.addSink(any()) } just Runs
        
        mediaManager.initializeLocalMedia()
        mediaManager.setLocalVideoRenderer(mockRenderer)

        verify { mockVideoTrack.addSink(mockRenderer) }
    }

    @Test
    fun `getLocalMediaStream should return current media stream`() {
        mediaManager.initializeLocalMedia()
        
        val result = mediaManager.getLocalMediaStream()
        
        assertEquals(mockMediaStream, result)
    }

    @Test
    fun `dispose should clean up all resources`() {
        val mockCapturer = mockk<CameraVideoCapturer>(relaxed = true)
        
        mockkConstructor(Camera2Enumerator::class)
        every { anyConstructed<Camera2Enumerator>().deviceNames } returns arrayOf("front_camera")
        every { anyConstructed<Camera2Enumerator>().isFrontFacing("front_camera") } returns true
        every { anyConstructed<Camera2Enumerator>().createCapturer("front_camera", null) } returns mockCapturer
        
        mockkStatic(SurfaceTextureHelper::class)
        every { SurfaceTextureHelper.create(any(), any()) } returns mockk(relaxed = true)
        
        every { mockCapturer.stopCapture() } just Runs
        every { mockCapturer.dispose() } just Runs
        every { mockVideoTrack.dispose() } just Runs
        every { mockAudioTrack.dispose() } just Runs
        every { mockMediaStream.dispose() } just Runs

        mediaManager.initializeLocalMedia()
        mediaManager.dispose()

        verify { mockCapturer.stopCapture() }
        verify { mockVideoTrack.dispose() }
        verify { mockAudioTrack.dispose() }
        verify { mockMediaStream.dispose() }
        verify { mockCapturer.dispose() }
    }

    @Test
    fun `mediaState should reflect current media configuration`() = runTest {
        val initialState = mediaManager.mediaState.value
        
        assertFalse(initialState.isVideoEnabled)
        assertFalse(initialState.isAudioEnabled)
        assertTrue(initialState.isFrontCamera)
    }
}