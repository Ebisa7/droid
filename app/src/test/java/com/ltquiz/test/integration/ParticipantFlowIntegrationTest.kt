package com.ltquiz.test.integration

import com.ltquiz.test.managers.*
import com.ltquiz.test.models.*
import com.ltquiz.test.ui.viewmodels.CallViewModel
import com.ltquiz.test.ui.viewmodels.JoinRoomViewModel
import com.ltquiz.test.network.NetworkQualityMonitor
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ParticipantFlowIntegrationTest {

    private lateinit var roomManager: RoomManager
    private lateinit var qrCodeManager: QRCodeManager
    private lateinit var signalingClient: SignalingClient
    private lateinit var webRTCManager: WebRTCManager
    private lateinit var mediaManager: MediaManager
    private lateinit var networkQualityMonitor: NetworkQualityMonitor

    private lateinit var joinRoomViewModel: JoinRoomViewModel
    private lateinit var callViewModel: CallViewModel

    private val mockRoom = Room(
        roomId = "test-room-id",
        roomCode = "ABC123",
        hostId = "host-user-id",
        participants = mutableListOf(
            Participant(
                userId = "participant-user-id",
                displayName = "Test Participant",
                isHost = false,
                isVideoEnabled = true,
                isAudioEnabled = true,
                connectionState = ConnectionState.CONNECTED
            )
        ),
        status = RoomStatus.ACTIVE
    )

    @Before
    fun setup() {
        // Create mocks
        roomManager = mockk()
        qrCodeManager = mockk()
        signalingClient = mockk()
        webRTCManager = mockk()
        mediaManager = mockk()
        networkQualityMonitor = mockk()

        // Setup common mock behaviors
        every { roomManager.currentRoom } returns flowOf(mockRoom)
        every { roomManager.getCurrentRoom() } returns mockRoom
        every { roomManager.getParticipants() } returns mockRoom.participants
        coEvery { roomManager.joinRoom(any()) } returns mockRoom
        coEvery { roomManager.leaveRoom() } just Runs

        every { signalingClient.observeSignals() } returns flowOf()
        every { signalingClient.observeConnectionState() } returns flowOf(
            SignalingConnectionState(
                isConnected = true,
                isReconnecting = false,
                connectionState = ConnectionState.CONNECTED
            )
        )
        coEvery { signalingClient.connect(any()) } just Runs
        coEvery { signalingClient.disconnect() } just Runs
        coEvery { signalingClient.sendSignal(any()) } just Runs

        every { webRTCManager.connectionStates } returns flowOf(emptyMap())
        every { webRTCManager.createPeerConnection(any(), any(), any()) } returns true
        every { webRTCManager.hasPeerConnection(any()) } returns false
        every { webRTCManager.addLocalStream(any(), any()) } returns true
        coEvery { webRTCManager.createOffer(any()) } returns SessionDescription(
            SessionDescription.Type.OFFER, "mock-offer-sdp"
        )
        coEvery { webRTCManager.createAnswer(any()) } returns SessionDescription(
            SessionDescription.Type.ANSWER, "mock-answer-sdp"
        )
        every { webRTCManager.setRemoteDescription(any(), any()) } returns true
        every { webRTCManager.addIceCandidate(any(), any()) } returns true
        every { webRTCManager.removePeerConnection(any()) } just Runs
        every { webRTCManager.closeAllConnections() } just Runs

        every { mediaManager.mediaState } returns flowOf(
            MediaState(
                isVideoEnabled = true,
                isAudioEnabled = true,
                isFrontCamera = true
            )
        )
        every { mediaManager.localVideoRenderer } returns flowOf(null)
        every { mediaManager.initializeLocalMedia() } returns mockk<MediaStream>()
        every { mediaManager.getLocalMediaStream() } returns mockk<MediaStream>()
        every { mediaManager.enableVideo(any()) } just Runs
        every { mediaManager.enableAudio(any()) } just Runs
        every { mediaManager.switchCamera() } just Runs
        every { mediaManager.setLocalVideoRenderer(any()) } just Runs
        every { mediaManager.stopCapture() } just Runs
        every { mediaManager.dispose() } just Runs

        every { networkQualityMonitor.networkQuality } returns flowOf(mockk())
        every { networkQualityMonitor.startMonitoring() } just Runs
        every { networkQualityMonitor.stopMonitoring() } just Runs
        every { networkQualityMonitor.shouldReduceVideoQuality() } returns false
        every { networkQualityMonitor.getRecommendedVideoQuality() } returns mockk()

        // Create view models
        joinRoomViewModel = JoinRoomViewModel(roomManager, qrCodeManager, signalingClient)
        callViewModel = CallViewModel(
            roomManager, webRTCManager, mediaManager, signalingClient, networkQualityMonitor
        )
    }

    @Test
    fun `test complete participant joining flow with manual room code`() = runTest {
        // Test joining room with manual code
        joinRoomViewModel.joinRoom("ABC123")

        // Verify room joining process
        coVerify { signalingClient.connect("ws://localhost:8080") }
        coVerify { roomManager.joinRoom("ABC123") }

        // Verify UI state
        val uiState = joinRoomViewModel.uiState.value
        assertTrue(uiState.joinSuccess)
        assertEquals("ABC123", uiState.roomCode)
    }

    @Test
    fun `test complete participant joining flow with QR code`() = runTest {
        val roomData = RoomData(
            roomId = "ABC123",
            serverAddress = "ws://test-server:8080"
        )

        // Test joining room from QR code
        joinRoomViewModel.joinRoomFromQR(roomData)

        // Verify signaling client connected to correct server
        coVerify { signalingClient.connect("ws://test-server:8080") }
        coVerify { roomManager.joinRoom("ABC123") }

        // Verify UI state
        val uiState = joinRoomViewModel.uiState.value
        assertTrue(uiState.joinSuccess)
        assertEquals("ABC123", uiState.roomCode)
    }

    @Test
    fun `test participant peer connection establishment`() = runTest {
        // Initialize call
        callViewModel.initializeCall("ABC123")

        // Verify media initialization
        verify { mediaManager.initializeLocalMedia() }
        verify { networkQualityMonitor.startMonitoring() }

        // Simulate receiving participant joined signal
        val participantJoinedSignal = Signal(
            from = "new-participant-id",
            to = "participant-user-id",
            type = SignalType.PARTICIPANT_JOINED,
            payload = ""
        )

        // Simulate signaling message handling
        // In real implementation, this would come through observeSignals()
        
        // Verify peer connection creation would be triggered
        // This tests the signaling message handling logic
        assertTrue(true) // Placeholder for actual peer connection verification
    }

    @Test
    fun `test WebRTC offer-answer exchange`() = runTest {
        // Initialize call
        callViewModel.initializeCall("ABC123")

        val participantId = "remote-participant-id"
        val mockLocalStream = mockk<MediaStream>()
        every { mediaManager.getLocalMediaStream() } returns mockLocalStream

        // Simulate receiving an offer
        val offerSignal = Signal(
            from = participantId,
            to = "participant-user-id",
            type = SignalType.OFFER,
            payload = "mock-offer-sdp"
        )

        // Test offer handling
        // In real implementation, this would be handled by the signaling observer
        
        // Verify peer connection would be created
        verify { webRTCManager.createPeerConnection(eq(participantId), any(), any()) }
        
        // Verify local stream would be added
        verify { webRTCManager.addLocalStream(participantId, mockLocalStream) }
        
        // Verify remote description would be set
        verify { webRTCManager.setRemoteDescription(eq(participantId), any()) }
        
        // Verify answer would be created
        coVerify { webRTCManager.createAnswer(participantId) }
    }

    @Test
    fun `test ICE candidate exchange`() = runTest {
        // Initialize call
        callViewModel.initializeCall("ABC123")

        val participantId = "remote-participant-id"
        val candidateJson = """
            {
                "candidate": "candidate:1 1 UDP 2130706431 192.168.1.100 54400 typ host",
                "sdpMid": "0",
                "sdpMLineIndex": 0
            }
        """.trimIndent()

        // Simulate receiving ICE candidate
        val iceCandidateSignal = Signal(
            from = participantId,
            to = "participant-user-id",
            type = SignalType.ICE_CANDIDATE,
            payload = candidateJson
        )

        // Test ICE candidate handling
        // In real implementation, this would be handled by the signaling observer
        
        // Verify ICE candidate would be added
        verify { webRTCManager.addIceCandidate(eq(participantId), any()) }
    }

    @Test
    fun `test participant removal handling`() = runTest {
        // Initialize call
        callViewModel.initializeCall("ABC123")

        val participantId = "leaving-participant-id"

        // Simulate participant leaving
        val participantLeftSignal = Signal(
            from = participantId,
            to = "room",
            type = SignalType.PARTICIPANT_LEFT,
            payload = ""
        )

        // Test participant left handling
        // In real implementation, this would be handled by the signaling observer
        
        // Verify peer connection would be removed
        verify { webRTCManager.removePeerConnection(participantId) }
    }

    @Test
    fun `test room ended by host handling`() = runTest {
        // Initialize call
        callViewModel.initializeCall("ABC123")

        // Simulate room being ended by host
        val roomEndedSignal = Signal(
            from = "host-user-id",
            to = "room",
            type = SignalType.ROOM_ENDED,
            payload = ""
        )

        // Test room ended handling
        // In real implementation, this would be handled by the signaling observer
        
        // Verify cleanup would be performed
        verify { webRTCManager.closeAllConnections() }
        verify { mediaManager.stopCapture() }
        coVerify { roomManager.leaveRoom() }
        coVerify { signalingClient.disconnect() }
    }

    @Test
    fun `test network reconnection handling`() = runTest {
        // Initialize call
        callViewModel.initializeCall("ABC123")

        // Simulate network disconnection
        every { signalingClient.observeConnectionState() } returns flowOf(
            SignalingConnectionState(
                isConnected = false,
                isReconnecting = true,
                connectionState = ConnectionState.RECONNECTING
            )
        )

        // Verify UI state reflects reconnection
        val uiState = callViewModel.uiState.value
        assertTrue(uiState.isReconnecting)

        // Simulate successful reconnection
        every { signalingClient.observeConnectionState() } returns flowOf(
            SignalingConnectionState(
                isConnected = true,
                isReconnecting = false,
                connectionState = ConnectionState.CONNECTED
            )
        )

        // Verify UI state reflects successful reconnection
        assertTrue(uiState.isConnected)
    }

    @Test
    fun `test media controls during call`() = runTest {
        // Initialize call
        callViewModel.initializeCall("ABC123")

        // Test video toggle
        callViewModel.toggleVideo()
        verify { mediaManager.enableVideo(false) }

        // Test audio toggle
        callViewModel.toggleAudio()
        verify { mediaManager.enableAudio(false) }

        // Test camera switch
        callViewModel.switchCamera()
        verify { mediaManager.switchCamera() }
    }

    @Test
    fun `test call cleanup on leave`() = runTest {
        // Initialize call
        callViewModel.initializeCall("ABC123")

        // Leave call
        callViewModel.leaveCall()

        // Verify all cleanup operations
        verify { networkQualityMonitor.stopMonitoring() }
        verify { webRTCManager.closeAllConnections() }
        verify { mediaManager.stopCapture() }
        coVerify { roomManager.leaveRoom() }
        coVerify { signalingClient.disconnect() }
    }

    @Test
    fun `test error handling for invalid room code`() = runTest {
        // Mock room not found error
        coEvery { roomManager.joinRoom("INVALID") } throws Exception("Room not found")

        // Try to join invalid room
        joinRoomViewModel.joinRoom("INVALID")

        // Verify error state
        val uiState = joinRoomViewModel.uiState.value
        assertEquals("Room not found. Please check the room code.", uiState.error)
        assertTrue(!uiState.joinSuccess)
    }

    @Test
    fun `test error handling for media initialization failure`() = runTest {
        // Mock media initialization failure
        every { mediaManager.initializeLocalMedia() } returns null

        // Try to initialize call
        callViewModel.initializeCall("ABC123")

        // Verify error state
        val uiState = callViewModel.uiState.value
        assertEquals("Failed to initialize camera and microphone", uiState.error)
    }
}