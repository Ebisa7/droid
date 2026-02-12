package com.ltquiz.test.managers

import android.content.Context
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.webrtc.*

class WebRTCManagerImplTest {

    private lateinit var context: Context
    private lateinit var webRTCManager: WebRTCManagerImpl
    private lateinit var mockPeerConnectionFactory: PeerConnectionFactory
    private lateinit var mockPeerConnection: PeerConnection

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        mockPeerConnectionFactory = mockk(relaxed = true)
        mockPeerConnection = mockk(relaxed = true)
        
        mockkStatic(PeerConnectionFactory::class)
        every { PeerConnectionFactory.initialize(any()) } just Runs
        every { PeerConnectionFactory.builder() } returns mockk {
            every { setVideoEncoderFactory(any()) } returns this
            every { setVideoDecoderFactory(any()) } returns this
            every { createPeerConnectionFactory() } returns mockPeerConnectionFactory
        }
        
        every { mockPeerConnectionFactory.createPeerConnection(any(), any<PeerConnection.Observer>()) } returns mockPeerConnection
        
        webRTCManager = WebRTCManagerImpl(context)
    }

    @Test
    fun `createPeerConnection should create new connection successfully`() {
        val participantId = "participant1"
        val onIceCandidate: (IceCandidate) -> Unit = mockk(relaxed = true)
        val onRemoteStream: (MediaStream) -> Unit = mockk(relaxed = true)

        val result = webRTCManager.createPeerConnection(participantId, onIceCandidate, onRemoteStream)

        assertTrue(result)
        verify { mockPeerConnectionFactory.createPeerConnection(any(), any<PeerConnection.Observer>()) }
    }

    @Test
    fun `createPeerConnection should return false for duplicate participant`() {
        val participantId = "participant1"
        val onIceCandidate: (IceCandidate) -> Unit = mockk(relaxed = true)
        val onRemoteStream: (MediaStream) -> Unit = mockk(relaxed = true)

        webRTCManager.createPeerConnection(participantId, onIceCandidate, onRemoteStream)
        val result = webRTCManager.createPeerConnection(participantId, onIceCandidate, onRemoteStream)

        assertFalse(result)
    }

    @Test
    fun `addIceCandidate should add candidate to existing connection`() {
        val participantId = "participant1"
        val candidate = mockk<IceCandidate>()
        
        every { mockPeerConnection.addIceCandidate(candidate) } returns true
        
        webRTCManager.createPeerConnection(participantId, {}, {})
        val result = webRTCManager.addIceCandidate(participantId, candidate)

        assertTrue(result)
        verify { mockPeerConnection.addIceCandidate(candidate) }
    }

    @Test
    fun `addIceCandidate should return false for non-existent connection`() {
        val participantId = "nonexistent"
        val candidate = mockk<IceCandidate>()

        val result = webRTCManager.addIceCandidate(participantId, candidate)

        assertFalse(result)
    }

    @Test
    fun `createOffer should create and set local description`() = runTest {
        val participantId = "participant1"
        val mockSdp = mockk<SessionDescription>()
        
        every { mockPeerConnection.createOffer(any()) } returns mockSdp
        every { mockPeerConnection.setLocalDescription(mockSdp) } just Runs
        
        webRTCManager.createPeerConnection(participantId, {}, {})
        val result = webRTCManager.createOffer(participantId)

        assertEquals(mockSdp, result)
        verify { mockPeerConnection.createOffer(any()) }
        verify { mockPeerConnection.setLocalDescription(mockSdp) }
    }

    @Test
    fun `createAnswer should create and set local description`() = runTest {
        val participantId = "participant1"
        val mockSdp = mockk<SessionDescription>()
        
        every { mockPeerConnection.createAnswer(any()) } returns mockSdp
        every { mockPeerConnection.setLocalDescription(mockSdp) } just Runs
        
        webRTCManager.createPeerConnection(participantId, {}, {})
        val result = webRTCManager.createAnswer(participantId)

        assertEquals(mockSdp, result)
        verify { mockPeerConnection.createAnswer(any()) }
        verify { mockPeerConnection.setLocalDescription(mockSdp) }
    }

    @Test
    fun `setRemoteDescription should set remote description successfully`() {
        val participantId = "participant1"
        val sessionDescription = mockk<SessionDescription>()
        
        every { mockPeerConnection.setRemoteDescription(sessionDescription) } just Runs
        
        webRTCManager.createPeerConnection(participantId, {}, {})
        val result = webRTCManager.setRemoteDescription(participantId, sessionDescription)

        assertTrue(result)
        verify { mockPeerConnection.setRemoteDescription(sessionDescription) }
    }

    @Test
    fun `addLocalStream should add stream to connection`() {
        val participantId = "participant1"
        val stream = mockk<MediaStream>()
        
        every { mockPeerConnection.addStream(stream) } returns true
        
        webRTCManager.createPeerConnection(participantId, {}, {})
        val result = webRTCManager.addLocalStream(participantId, stream)

        assertTrue(result)
        verify { mockPeerConnection.addStream(stream) }
    }

    @Test
    fun `removePeerConnection should close and remove connection`() {
        val participantId = "participant1"
        
        every { mockPeerConnection.close() } just Runs
        
        webRTCManager.createPeerConnection(participantId, {}, {})
        webRTCManager.removePeerConnection(participantId)

        verify { mockPeerConnection.close() }
    }

    @Test
    fun `closeAllConnections should close all peer connections`() {
        val participantId1 = "participant1"
        val participantId2 = "participant2"
        
        every { mockPeerConnection.close() } just Runs
        
        webRTCManager.createPeerConnection(participantId1, {}, {})
        webRTCManager.createPeerConnection(participantId2, {}, {})
        webRTCManager.closeAllConnections()

        verify(exactly = 2) { mockPeerConnection.close() }
    }
}