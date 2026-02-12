package com.ltquiz.test.integration

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.ltquiz.test.errors.ErrorHandler
import com.ltquiz.test.managers.*
import com.ltquiz.test.models.*
import com.ltquiz.test.ui.viewmodels.CallViewModel
import com.ltquiz.test.ui.viewmodels.RoomCreationViewModel
import com.ltquiz.test.network.NetworkQualityMonitor
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class HostFlowIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()

    // Mock dependencies
    private val roomManager = mockk<RoomManager>()
    private val qrCodeManager = mockk<QRCodeManager>()
    private val signalingClient = mockk<SignalingClient>()
    private val webRTCManager = mockk<WebRTCManager>()
    private val mediaManager = mockk<MediaManager>()
    private val networkQualityMonitor = mockk<NetworkQualityMonitor>()
    private val errorHandler = mockk<ErrorHandler>()

    // Test data
    private val testRoom = Room(
        roomId = "test-room-id",
        roomCode = "ABC123",
        hostId = "host-user-id",
        participants = mutableListOf(
            Participant(
                userId = "host-user-id",
                displayName = "Host User",
                isHost = true,
                isVideoEnabled = false,
                isAudioEnabled = false,
                connectionState = ConnectionState.CONNECTED
            )
        ),
        status = RoomStatus.WAITING
    )

    private val testParticipant = Participant(
        userId = "participant-1",
        displayName = "Test Participant",
        isHost = false,
        isVideoEnabled = true,
        isAudioEnabled = true,
        connectionState = ConnectionState.CONNECTED
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        // Setup common mocks
        every { errorHandler.showSuccess(any()) } just Runs
        every { roomManager.currentRoom } returns MutableStateFlow(testRoom)
        every { roomManager.observeParticipants() } returns MutableStateFlow(testRoom.participants)
        every { signalingClient.observeSignals() } returns flowOf()
        every { signalingClient.observeConnectionState() } returns flowOf(
            SignalingConnectionState(
                isConnected = true,
                isReconnecting = false,
                connectionState = ConnectionState.CONNECTED
            )
        )
        every { mediaManager.mediaState } returns MutableStateFlow(
            MediaState(
                isVideoEnabled = true,
                isAudioEnabled = true,
                isFrontCamera = true
            )
        )
        every { webRTCManager.connectionStates } returns MutableStateFlow(emptyMap())
        every { networkQualityMonitor.networkQuality } returns flowOf(
            com.ltquiz.test.network.NetworkQuality(
                level = com.ltquiz.test.network.NetworkLevel.GOOD,
                isConnected = true,
                connectionType = com.ltquiz.test.network.ConnectionType.WIFI
            )
        )
    }

    @After
    fun tearDown() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `complete host flow - room creation to call end`() = runBlockingTest {
        // Setup mocks for room creation
        coEvery { signalingClient.connect(any()) } just Runs
        coEvery { roomManager.createRoom() } returns testRoom
        every { qrCodeManager.generateQRCode(any()) } returns mockk()
        every { webRTCManager.createPeerConnection(any(), any(), any()) } returns true
        coEvery { webRTCManager.createOffer(any()) } returns SessionDescription(
            SessionDescription.Type.OFFER, "test-offer"
        )
        coEvery { signalingClient.sendSignal(any()) } just Runs
        every { mediaManager.initializeLocalMedia() } returns mockk<MediaStream>()
        every { mediaManager.getLocalMediaStream() } returns mockk<MediaStream>()
        every { webRTCManager.addLocalStream(any(), any()) } returns true
        every { networkQualityMonitor.startMonitoring() } just Runs
        every { networkQualityMonitor.stopMonitoring() } just Runs
        every { mediaManager.stopCapture() } just Runs
        every { webRTCManager.closeAllConnections() } just Runs
        coEvery { roomManager.leaveRoom() } just Runs
        coEvery { signalingClient.disconnect() } just Runs
        every { mediaManager.dispose() } just Runs

        // Step 1: Create room
        val roomCreationViewModel = RoomCreationViewModel(
            roomManager, qrCodeManager, signalingClient, webRTCManager, mediaManager, errorHandler
        )

        roomCreationViewModel.createRoom()
        testDispatcher.advanceUntilIdle()

        // Verify room creation
        val roomCreationState = roomCreationViewModel.uiState.value
        assertNotNull(roomCreationState.room)
        assertEquals("ABC123", roomCreationState.room.roomCode)
        assertTrue(roomCreationState.isReadyForCall)

        // Step 2: Simulate participant joining
        val updatedRoom = testRoom.copy(
            participants = mutableListOf(testRoom.participants[0], testParticipant)
        )
        every { roomManager.currentRoom } returns MutableStateFlow(updatedRoom)
        every { roomManager.observeParticipants() } returns MutableStateFlow(updatedRoom.participants)

        // Simulate signaling message for participant join
        val joinSignal = Signal(
            from = testParticipant.userId,
            to = testRoom.hostId,
            type = SignalType.JOIN_ROOM,
            payload = ""
        )

        // Step 3: Prepare for call transition
        roomCreationViewModel.prepareForCallTransition()
        testDispatcher.advanceUntilIdle()

        // Verify WebRTC setup
        verify { webRTCManager.createPeerConnection(testParticipant.userId, any(), any()) }
        verify { webRTCManager.addLocalStream(testParticipant.userId, any()) }

        // Step 4: Transition to call
        val callViewModel = CallViewModel(
            roomManager, webRTCManager, mediaManager, signalingClient, networkQualityMonitor
        )

        callViewModel.initializeCall("ABC123")
        testDispatcher.advanceUntilIdle()

        // Verify call initialization
        val callState = callViewModel.uiState.value
        assertEquals(2, callState.participants.size) // Host + 1 participant
        assertTrue(callState.isConnected)
        assertTrue(callState.isVideoEnabled)
        assertTrue(callState.isAudioEnabled)

        // Step 5: Test call controls
        callViewModel.toggleVideo()
        callViewModel.toggleAudio()
        callViewModel.switchCamera()

        verify { mediaManager.enableVideo(false) }
        verify { mediaManager.enableAudio(false) }
        verify { mediaManager.switchCamera() }

        // Step 6: End call
        callViewModel.leaveCall()
        testDispatcher.advanceUntilIdle()

        // Verify cleanup
        verify { networkQualityMonitor.stopMonitoring() }
        verify { webRTCManager.closeAllConnections() }
        verify { mediaManager.stopCapture() }
        verify { roomManager.leaveRoom() }
        verify { signalingClient.disconnect() }
    }

    @Test
    fun `host flow - auto start call when participant joins`() = runBlockingTest {
        // Setup mocks
        coEvery { signalingClient.connect(any()) } just Runs
        coEvery { roomManager.createRoom() } returns testRoom
        every { qrCodeManager.generateQRCode(any()) } returns mockk()
        every { webRTCManager.createPeerConnection(any(), any(), any()) } returns true
        coEvery { webRTCManager.createOffer(any()) } returns SessionDescription(
            SessionDescription.Type.OFFER, "test-offer"
        )
        coEvery { signalingClient.sendSignal(any()) } just Runs
        every { mediaManager.initializeLocalMedia() } returns mockk<MediaStream>()
        every { webRTCManager.addLocalStream(any(), any()) } returns true

        val roomCreationViewModel = RoomCreationViewModel(
            roomManager, qrCodeManager, signalingClient, webRTCManager, mediaManager, errorHandler
        )

        // Create room
        roomCreationViewModel.createRoom()
        testDispatcher.advanceUntilIdle()

        // Initially should not auto-start
        val initialState = roomCreationViewModel.uiState.value
        assertTrue(initialState.isReadyForCall)
        assertEquals(false, initialState.shouldAutoStartCall)

        // Simulate participant joining
        val updatedRoom = testRoom.copy(
            participants = mutableListOf(testRoom.participants[0], testParticipant)
        )
        every { roomManager.observeParticipants() } returns MutableStateFlow(updatedRoom.participants)

        // Trigger participant update
        testDispatcher.advanceUntilIdle()

        // Should now trigger auto-start
        val updatedState = roomCreationViewModel.uiState.value
        assertTrue(updatedState.shouldAutoStartCall)
    }

    @Test
    fun `host flow - handle participant WebRTC signaling`() = runBlockingTest {
        // Setup mocks
        coEvery { signalingClient.connect(any()) } just Runs
        coEvery { roomManager.createRoom() } returns testRoom
        every { qrCodeManager.generateQRCode(any()) } returns mockk()
        every { webRTCManager.createPeerConnection(any(), any(), any()) } returns true
        coEvery { webRTCManager.createOffer(any()) } returns SessionDescription(
            SessionDescription.Type.OFFER, "test-offer"
        )
        coEvery { signalingClient.sendSignal(any()) } just Runs
        every { mediaManager.initializeLocalMedia() } returns mockk<MediaStream>()
        every { webRTCManager.addLocalStream(any(), any()) } returns true
        every { webRTCManager.setRemoteDescription(any(), any()) } returns true
        every { webRTCManager.addIceCandidate(any(), any()) } returns true

        val roomCreationViewModel = RoomCreationViewModel(
            roomManager, qrCodeManager, signalingClient, webRTCManager, mediaManager, errorHandler
        )

        roomCreationViewModel.createRoom()
        testDispatcher.advanceUntilIdle()

        // Simulate WebRTC answer from participant
        val answerSignal = Signal(
            from = testParticipant.userId,
            to = testRoom.hostId,
            type = SignalType.ANSWER,
            payload = "test-answer-sdp"
        )

        // Simulate ICE candidate from participant
        val iceSignal = Signal(
            from = testParticipant.userId,
            to = testRoom.hostId,
            type = SignalType.ICE_CANDIDATE,
            payload = "test-ice-candidate"
        )

        // Verify WebRTC signaling is handled
        verify { webRTCManager.setRemoteDescription(testParticipant.userId, any()) }
        verify { webRTCManager.addIceCandidate(testParticipant.userId, any()) }
    }

    @Test
    fun `host flow - error handling during room creation`() = runBlockingTest {
        // Setup error scenario
        coEvery { signalingClient.connect(any()) } throws Exception("Connection failed")
        every { errorHandler.handleError(any()) } just Runs

        val roomCreationViewModel = RoomCreationViewModel(
            roomManager, qrCodeManager, signalingClient, webRTCManager, mediaManager, errorHandler
        )

        roomCreationViewModel.createRoom()
        testDispatcher.advanceUntilIdle()

        // Verify error handling
        verify { errorHandler.handleError(any()) }
    }

    @Test
    fun `host flow - cleanup on room cancellation`() = runBlockingTest {
        // Setup mocks
        coEvery { signalingClient.connect(any()) } just Runs
        coEvery { roomManager.createRoom() } returns testRoom
        every { qrCodeManager.generateQRCode(any()) } returns mockk()
        every { webRTCManager.closeAllConnections() } just Runs
        coEvery { signalingClient.disconnect() } just Runs
        coEvery { roomManager.leaveRoom() } just Runs

        val roomCreationViewModel = RoomCreationViewModel(
            roomManager, qrCodeManager, signalingClient, webRTCManager, mediaManager, errorHandler
        )

        roomCreationViewModel.createRoom()
        testDispatcher.advanceUntilIdle()

        // Cancel room
        roomCreationViewModel.cancelRoom()
        testDispatcher.advanceUntilIdle()

        // Verify cleanup
        verify { webRTCManager.closeAllConnections() }
        verify { signalingClient.disconnect() }
        verify { roomManager.leaveRoom() }
    }
}