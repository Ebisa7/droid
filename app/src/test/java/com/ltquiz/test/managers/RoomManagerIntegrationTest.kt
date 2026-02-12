package com.ltquiz.test.managers

import com.ltquiz.test.models.ConnectionState
import com.ltquiz.test.models.Participant
import com.ltquiz.test.models.RoomStatus
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Integration tests for RoomManager to verify end-to-end room management functionality.
 */
class RoomManagerIntegrationTest {
    
    private lateinit var hostRoomManager: RoomManagerImpl
    private lateinit var participantRoomManager: RoomManagerImpl
    private lateinit var mockHostIdentityManager: IdentityManager
    private lateinit var mockParticipantIdentityManager: IdentityManager
    private lateinit var mockSignalingClient: SignalingClient
    
    @BeforeEach
    fun setup() {
        mockHostIdentityManager = mockk()
        mockParticipantIdentityManager = mockk()
        mockSignalingClient = mockk()
        
        every { mockHostIdentityManager.getUserId() } returns "host-user-id"
        every { mockHostIdentityManager.getDisplayName() } returns "Host User"
        
        every { mockParticipantIdentityManager.getUserId() } returns "participant-user-id"
        every { mockParticipantIdentityManager.getDisplayName() } returns "Participant User"
        
        hostRoomManager = RoomManagerImpl(mockHostIdentityManager, mockSignalingClient)
        participantRoomManager = RoomManagerImpl(mockParticipantIdentityManager, mockSignalingClient)
    }
    
    @Test
    fun `complete room lifecycle - create, join, manage participants, leave`() = runTest {
        // Host creates room
        val hostRoom = hostRoomManager.createRoom()
        
        // Verify room creation
        assertNotNull(hostRoom)
        assertEquals(RoomStatus.WAITING, hostRoom.status)
        assertEquals("host-user-id", hostRoom.hostId)
        assertEquals(1, hostRoom.participants.size)
        assertTrue(hostRoom.participants.first().isHost)
        
        // Participant joins room using room code
        val participantRoom = participantRoomManager.joinRoom(hostRoom.roomCode)
        
        // Verify participant joined
        assertNotNull(participantRoom)
        assertEquals(hostRoom.roomCode, participantRoom.roomCode)
        assertEquals(1, participantRoom.participants.size)
        assertFalse(participantRoom.participants.first().isHost)
        
        // Simulate adding participant to host's room (in real app, this would be done via signaling)
        val participantInHostRoom = Participant(
            userId = "participant-user-id",
            displayName = "Participant User",
            isHost = false,
            isVideoEnabled = false,
            isAudioEnabled = false,
            connectionState = ConnectionState.CONNECTING
        )
        hostRoomManager.addParticipant(participantInHostRoom)
        
        // Verify participant was added to host's room
        assertEquals(2, hostRoomManager.getParticipants().size)
        assertTrue(hostRoomManager.getParticipants().any { !it.isHost })
        
        // Update participant connection state
        hostRoomManager.updateParticipant("participant-user-id") { participant ->
            participant.copy(connectionState = ConnectionState.CONNECTED, isVideoEnabled = true)
        }
        
        // Verify participant state was updated
        val updatedParticipant = hostRoomManager.getParticipants().first { it.userId == "participant-user-id" }
        assertEquals(ConnectionState.CONNECTED, updatedParticipant.connectionState)
        assertTrue(updatedParticipant.isVideoEnabled)
        
        // Participant leaves
        participantRoomManager.leaveRoom()
        assertNull(participantRoomManager.getCurrentRoom())
        
        // Remove participant from host's room
        hostRoomManager.removeParticipant("participant-user-id")
        assertEquals(1, hostRoomManager.getParticipants().size)
        
        // Host leaves (ends room)
        hostRoomManager.leaveRoom()
        assertNull(hostRoomManager.getCurrentRoom())
        assertTrue(hostRoomManager.getParticipants().isEmpty())
    }
    
    @Test
    fun `room code validation works correctly`() = runTest {
        // Create room with valid code
        val room = hostRoomManager.createRoom()
        val validCode = room.roomCode
        
        // Valid codes should work
        assertDoesNotThrow {
            participantRoomManager.joinRoom(validCode)
        }
        
        // Invalid codes should throw exceptions
        assertThrows(IllegalArgumentException::class.java) {
            runTest { participantRoomManager.joinRoom("SHORT") }
        }
        
        assertThrows(IllegalArgumentException::class.java) {
            runTest { participantRoomManager.joinRoom("TOOLONG1") }
        }
        
        assertThrows(IllegalArgumentException::class.java) {
            runTest { participantRoomManager.joinRoom("ABC12O") } // Contains 'O'
        }
    }
    
    @Test
    fun `real-time participant updates work correctly`() = runTest {
        // Create room and observe participants
        hostRoomManager.createRoom()
        val participantsFlow = hostRoomManager.observeParticipants()
        
        // Initial state: just the host
        assertEquals(1, participantsFlow.value.size)
        
        // Add participants and verify real-time updates
        val participant1 = Participant(
            userId = "participant-1",
            displayName = "Participant 1",
            isHost = false,
            isVideoEnabled = false,
            isAudioEnabled = false,
            connectionState = ConnectionState.CONNECTING
        )
        
        hostRoomManager.addParticipant(participant1)
        assertEquals(2, participantsFlow.value.size)
        
        val participant2 = Participant(
            userId = "participant-2",
            displayName = "Participant 2", 
            isHost = false,
            isVideoEnabled = true,
            isAudioEnabled = true,
            connectionState = ConnectionState.CONNECTED
        )
        
        hostRoomManager.addParticipant(participant2)
        assertEquals(3, participantsFlow.value.size)
        
        // Update participant and verify real-time update
        hostRoomManager.updateParticipant("participant-1") { participant ->
            participant.copy(connectionState = ConnectionState.CONNECTED)
        }
        
        val updatedParticipant = participantsFlow.value.first { it.userId == "participant-1" }
        assertEquals(ConnectionState.CONNECTED, updatedParticipant.connectionState)
        
        // Remove participant and verify real-time update
        hostRoomManager.removeParticipant("participant-1")
        assertEquals(2, participantsFlow.value.size)
        assertFalse(participantsFlow.value.any { it.userId == "participant-1" })
    }
}