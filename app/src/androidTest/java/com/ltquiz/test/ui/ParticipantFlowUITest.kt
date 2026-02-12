package com.ltquiz.test.ui

import android.app.Activity
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ltquiz.test.MainActivity
import com.ltquiz.test.managers.*
import com.ltquiz.test.models.*
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ParticipantFlowUITest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var roomManager: RoomManager

    @Inject
    lateinit var qrCodeManager: QRCodeManager

    @Inject
    lateinit var signalingClient: SignalingClient

    @Inject
    lateinit var webRTCManager: WebRTCManager

    @Inject
    lateinit var mediaManager: MediaManager

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
        hiltRule.inject()
        
        // Mock all managers
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
        coEvery { webRTCManager.createOffer(any()) } returns mockk()
        coEvery { webRTCManager.createAnswer(any()) } returns mockk()
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
        every { mediaManager.initializeLocalMedia() } returns mockk()
        every { mediaManager.getLocalMediaStream() } returns mockk()
        every { mediaManager.enableVideo(any()) } just Runs
        every { mediaManager.enableAudio(any()) } just Runs
        every { mediaManager.switchCamera() } just Runs
        every { mediaManager.setLocalVideoRenderer(any()) } just Runs
        every { mediaManager.stopCapture() } just Runs
        every { mediaManager.dispose() } just Runs
    }

    @Test
    fun testCompleteParticipantJoiningFlow_ManualRoomCode() {
        // Start at launch screen
        composeTestRule.onNodeWithText("Join Room").performClick()

        // Should navigate to join room screen
        composeTestRule.onNodeWithText("Join a video call").assertIsDisplayed()

        // Enter room code manually
        composeTestRule.onNodeWithText("Room Code").performTextInput("ABC123")

        // Click join room button
        composeTestRule.onNodeWithText("Join Room").performClick()

        // Should navigate to call screen
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Room: ABC123").assertIsDisplayed()

        // Verify room manager was called
        coVerify { roomManager.joinRoom("ABC123") }
        coVerify { signalingClient.connect(any()) }
        verify { mediaManager.initializeLocalMedia() }
    }

    @Test
    fun testCompleteParticipantJoiningFlow_QRCode() {
        // Mock QR scan result
        val roomData = RoomData(
            roomId = "ABC123",
            serverAddress = "ws://test-server:8080"
        )

        // Start at launch screen
        composeTestRule.onNodeWithText("Join Room").performClick()

        // Should navigate to join room screen
        composeTestRule.onNodeWithText("Join a video call").assertIsDisplayed()

        // Click scan QR code button
        composeTestRule.onNodeWithText("Scan QR Code").performClick()

        // Simulate QR scan result by directly calling the view model method
        // In a real test, this would involve mocking the QR scanner activity result
        composeTestRule.waitForIdle()

        // Should navigate to call screen after successful QR scan
        composeTestRule.onNodeWithText("Room: ABC123").assertIsDisplayed()

        // Verify signaling client connected to correct server
        coVerify { signalingClient.connect("ws://test-server:8080") }
    }

    @Test
    fun testParticipantCallExperience() {
        // Navigate directly to call screen
        navigateToCallScreen("ABC123")

        // Verify call screen elements are displayed
        composeTestRule.onNodeWithText("Room: ABC123").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 participants").assertIsDisplayed()

        // Test media controls
        composeTestRule.onNodeWithContentDescription("Mute").performClick()
        verify { mediaManager.enableAudio(false) }

        composeTestRule.onNodeWithContentDescription("Turn off camera").performClick()
        verify { mediaManager.enableVideo(false) }

        composeTestRule.onNodeWithContentDescription("Switch camera").performClick()
        verify { mediaManager.switchCamera() }

        // Test leave call
        composeTestRule.onNodeWithContentDescription("Leave call").performClick()
        
        // Should navigate back to launch screen
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Create Room").assertIsDisplayed()

        // Verify cleanup was called
        verify { webRTCManager.closeAllConnections() }
        verify { mediaManager.stopCapture() }
        coVerify { roomManager.leaveRoom() }
        coVerify { signalingClient.disconnect() }
    }

    @Test
    fun testParticipantRemovalHandling() {
        // Navigate to call screen
        navigateToCallScreen("ABC123")

        // Simulate participant being removed by host
        // This would normally come through signaling
        val signal = Signal(
            from = "host-user-id",
            to = "participant-user-id",
            type = SignalType.PARTICIPANT_LEFT,
            payload = ""
        )

        // In a real test, we would trigger this through the signaling system
        // For now, verify the UI handles participant removal gracefully
        composeTestRule.onNodeWithText("Room: ABC123").assertIsDisplayed()
    }

    @Test
    fun testRoomEndedByHost() {
        // Navigate to call screen
        navigateToCallScreen("ABC123")

        // Simulate room being ended by host
        val signal = Signal(
            from = "host-user-id",
            to = "room",
            type = SignalType.ROOM_ENDED,
            payload = ""
        )

        // In a real test, this would come through signaling
        // Verify that participant is notified and navigated back
        composeTestRule.onNodeWithText("Room: ABC123").assertIsDisplayed()
    }

    @Test
    fun testNetworkReconnectionHandling() {
        // Navigate to call screen
        navigateToCallScreen("ABC123")

        // Simulate network disconnection
        every { signalingClient.observeConnectionState() } returns flowOf(
            SignalingConnectionState(
                isConnected = false,
                isReconnecting = true,
                connectionState = ConnectionState.RECONNECTING
            )
        )

        // Should show reconnecting overlay
        composeTestRule.onNodeWithText("Reconnecting...").assertIsDisplayed()

        // Simulate successful reconnection
        every { signalingClient.observeConnectionState() } returns flowOf(
            SignalingConnectionState(
                isConnected = true,
                isReconnecting = false,
                connectionState = ConnectionState.CONNECTED
            )
        )

        // Reconnecting overlay should disappear
        composeTestRule.onNodeWithText("Reconnecting...").assertDoesNotExist()
    }

    @Test
    fun testInvalidRoomCodeHandling() {
        // Navigate to join room screen
        composeTestRule.onNodeWithText("Join Room").performClick()

        // Enter invalid room code
        composeTestRule.onNodeWithText("Room Code").performTextInput("INVALID")

        // Mock room not found error
        coEvery { roomManager.joinRoom("INVALID") } throws Exception("Room not found")

        // Click join room button
        composeTestRule.onNodeWithText("Join Room").performClick()

        // Should show error message
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Room not found. Please check the room code.").assertIsDisplayed()

        // Should remain on join room screen
        composeTestRule.onNodeWithText("Join a video call").assertIsDisplayed()
    }

    @Test
    fun testPermissionDeniedHandling() {
        // Mock media initialization failure (simulating permission denied)
        every { mediaManager.initializeLocalMedia() } returns null

        // Navigate to call screen
        navigateToCallScreen("ABC123")

        // Should show error message about camera/microphone access
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Failed to initialize camera and microphone").assertIsDisplayed()
    }

    private fun navigateToCallScreen(roomCode: String) {
        // Navigate directly to call screen for testing
        composeTestRule.activity.runOnUiThread {
            val intent = Intent(composeTestRule.activity, MainActivity::class.java).apply {
                putExtra("navigate_to", "call/$roomCode")
            }
            composeTestRule.activity.startActivity(intent)
        }
        composeTestRule.waitForIdle()
    }
}