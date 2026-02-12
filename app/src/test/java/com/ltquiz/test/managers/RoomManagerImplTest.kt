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
import org.junit.jupiter.api.assertThrows

class RoomManagerImplTest {
    
    private lateinit var roomManager: RoomManagerImpl
    private lateinit var mockIdentityManager: IdentityManager
    private lateinit var mockSignalingClient: SignalingClient
    
    @BeforeEach
    fun setup() {
        mockIdentityManager = mockk()
        mockSignalingClient = mockk()
        
        every { mockIdentityManager.getUserId() } returns "test-user-id"
        every { mockIdentityManager.getDisplayName() } returns "Test User"
        
        roomManager = RoomManagerImpl(mockIdentityManager, mockSignalingClient)
    }
    
    @Test
    fun `createRoom should generate valid room with 6-character code`() = runTest {
        // When
        val room = roomManager.createRoom()
        
        // Then
        assertNotNull(room)
        assertEquals(6, room.roomCode.length)
        assertTrue(room.roomCode.all { it.isLetterOrDigit() })
        assertEquals("test-user-id", room.hostId)
        assertEquals(RoomStatus.WAITING, room.status)
        assertEquals(1, room.participants.size)
        
        val hostParticipant = room.participants.first()
        assertEquals("test-user-id", hostParticipant.userId)
        assertEquals("Test User", hostParticipant.displayName)
        assertTrue(hostParticipant.isHost)
        assertEquals(ConnectionState.CONNECTED, hostParticipant.connectionState)
    }
    
    @Test
    fun `createRoom should exclude confusing characters from room code`() = runTest {
        // When - Generate multiple room codes to test character exclusion
        val roomCodes = (1..100).map { 
            roomManager.createRoom().roomCode 
        }
        
        // Then - No confusing characters should be present
        val confusingChars = setOf('0', 'O', 'I', '1')
        roomCodes.forEach { code ->
            assertFalse(code.any { it in confusingChars }, "Room code $code contains confusing characters")
        }
    }
    
    @Test
    fun `getCurrentRoom should return created room`() = runTest {
        // Given
        val room = roomManager.createRoom()
        
        // When
        val currentRoom = roomManager.getCurrentRoom()
        
        // Then
        assertEquals(room, currentRoom)
    }
    
    @Test
    fun `getParticipants should return current room participants`() = runTest {
        // Given
        val room = roomManager.createRoom()
        
        // When
        val participants = roomManager.getParticipants()
        
        // Then
        assertEquals(1, participants.size)
        assertEquals(room.participants, participants)
    }
    
    @Test
    fun `joinRoom should create room with participant as non-host`() = runTest {
        // When
        val room = roomManager.joinRoom("ABC123")
        
        // Then
        assertNotNull(room)
        assertEquals("ABC123", room.roomCode)
        assertEquals(1, room.participants.size)
        
        val participant = room.participants.first()
        assertEquals("test-user-id", participant.userId)
        assertEquals("Test User", participant.displayName)
        assertFalse(participant.isHost)
        assertEquals(ConnectionState.CONNECTING, participant.connectionState)
    }
    
    @Test
    fun `joinRoom should throw exception for invalid room code format - too short`() = runTest {
        // When & Then
        assertThrows<IllegalArgumentException> {
            roomManager.joinRoom("INVALID")
        }
    }
    
    @Test
    fun `joinRoom should throw exception for invalid room code format - confusing character`() = runTest {
        // When & Then
        assertThrows<IllegalArgumentException> {
            roomManager.joinRoom("ABC12O") // Contains confusing character 'O'
        }
    }
    
    @Test
    fun `joinRoom should throw exception for invalid room code format - too long`() = runTest {
        // When & Then
        assertThrows<IllegalArgumentException> {
            roomManager.joinRoom("ABC1234") // Too long
        }
    }
    
    @Test
    fun `leaveRoom should clear current room`() = runTest {
        // Given
        roomManager.createRoom()
        
        // When
        roomManager.leaveRoom()
        
        // Then
        assertNull(roomManager.getCurrentRoom())
        assertTrue(roomManager.getParticipants().isEmpty())
    }
    
    @Test
    fun `addParticipant should add participant to current room`() = runTest {
        // Given
        roomManager.createRoom()
        val newParticipant = Participant(
            userId = "participant-2",
            displayName = "Participant 2",
            isHost = false,
            isVideoEnabled = false,
            isAudioEnabled = false,
            connectionState = ConnectionState.CONNECTING
        )
        
        // When
        roomManager.addParticipant(newParticipant)
        
        // Then
        val participants = roomManager.getParticipants()
        assertEquals(2, participants.size)
        assertTrue(participants.contains(newParticipant))
    }
    
    @Test
    fun `removeParticipant should remove participant from current room`() = runTest {
        // Given
        roomManager.createRoom()
        val newParticipant = Participant(
            userId = "participant-2",
            displayName = "Participant 2",
            isHost = false,
            isVideoEnabled = false,
            isAudioEnabled = false,
            connectionState = ConnectionState.CONNECTING
        )
        roomManager.addParticipant(newParticipant)
        
        // When
        roomManager.removeParticipant("participant-2")
        
        // Then
        val participants = roomManager.getParticipants()
        assertEquals(1, participants.size)
        assertFalse(participants.any { it.userId == "participant-2" })
    }
    
    @Test
    fun `updateParticipant should modify participant state`() = runTest {
        // Given
        roomManager.createRoom()
        val participantId = "test-user-id"
        
        // When
        roomManager.updateParticipant(participantId) { participant ->
            participant.copy(isVideoEnabled = true, isAudioEnabled = true)
        }
        
        // Then
        val participants = roomManager.getParticipants()
        val updatedParticipant = participants.first { it.userId == participantId }
        assertTrue(updatedParticipant.isVideoEnabled)
        assertTrue(updatedParticipant.isAudioEnabled)
    }
    
    @Test
    fun `observeParticipants should emit participant list changes`() = runTest {
        // Given
        val participantsFlow = roomManager.observeParticipants()
        roomManager.createRoom()
        
        // When
        val newParticipant = Participant(
            userId = "participant-2",
            displayName = "Participant 2",
            isHost = false,
            isVideoEnabled = false,
            isAudioEnabled = false,
            connectionState = ConnectionState.CONNECTING
        )
        roomManager.addParticipant(newParticipant)
        
        // Then
        assertEquals(2, participantsFlow.value.size)
    }
    
    @Test
    fun `room code generation should be consistent format`() = runTest {
        // When - Generate multiple rooms
        val rooms = (1..50).map { roomManager.createRoom() }
        
        // Then - All room codes should be 6 characters and alphanumeric
        rooms.forEach { room ->
            assertEquals(6, room.roomCode.length)
            assertTrue(room.roomCode.all { it.isLetterOrDigit() })
            assertTrue(room.roomCode.all { it.isUpperCase() || it.isDigit() })
        }
    }
    
    @Test
    fun `leaveRoom when host leaves should end room`() = runTest {
        // Given - Host creates room
        val room = roomManager.createRoom()
        val hostId = room.hostId
        
        // Add another participant
        val participant = Participant(
            userId = "participant-2",
            displayName = "Participant 2",
            isHost = false,
            isVideoEnabled = false,
            isAudioEnabled = false,
            connectionState = ConnectionState.CONNECTED
        )
        roomManager.addParticipant(participant)
        
        // When - Host leaves
        roomManager.leaveRoom()
        
        // Then - Room should be ended
        assertNull(roomManager.getCurrentRoom())
        assertTrue(roomManager.getParticipants().isEmpty())
    }
}  
  
    @Test
    fun `room code should only contain allowed characters`() = runTest {
        // Given - The allowed character set
        val allowedChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        
        // When - Generate multiple room codes
        val roomCodes = (1..50).map { roomManager.createRoom().roomCode }
        
        // Then - All characters should be from allowed set
        roomCodes.forEach { code ->
            assertTrue(code.all { it in allowedChars }, "Room code $code contains disallowed characters")
        }
    }
    
    @Test
    fun `real-time participant updates should work correctly`() = runTest {
        // Given
        val participantsFlow = roomManager.observeParticipants()
        roomManager.createRoom()
        
        // When - Add multiple participants
        val participant1 = Participant(
            userId = "participant-1",
            displayName = "Participant 1",
            isHost = false,
            isVideoEnabled = false,
            isAudioEnabled = false,
            connectionState = ConnectionState.CONNECTING
        )
        
        val participant2 = Participant(
            userId = "participant-2", 
            displayName = "Participant 2",
            isHost = false,
            isVideoEnabled = true,
            isAudioEnabled = true,
            connectionState = ConnectionState.CONNECTED
        )
        
        roomManager.addParticipant(participant1)
        assertEquals(2, participantsFlow.value.size)
        
        roomManager.addParticipant(participant2)
        assertEquals(3, participantsFlow.value.size)
        
        // When - Remove a participant
        roomManager.removeParticipant("participant-1")
        
        // Then - Participant list should be updated
        assertEquals(2, participantsFlow.value.size)
        assertFalse(participantsFlow.value.any { it.userId == "participant-1" })
        assertTrue(participantsFlow.value.any { it.userId == "participant-2" })
    }
    
    @Test
    fun `room creation should satisfy requirement 2_1`() = runTest {
        // Requirement 2.1: WHEN a user taps "Create Room" THEN the system SHALL connect to the signaling server and create a new room
        
        // When
        val room = roomManager.createRoom()
        
        // Then
        assertNotNull(room)
        assertNotNull(room.roomId)
        assertTrue(room.roomId.isNotEmpty())
        assertEquals(RoomStatus.WAITING, room.status)
    }
    
    @Test
    fun `room creation should satisfy requirement 2_2`() = runTest {
        // Requirement 2.2: WHEN a room is created THEN the server SHALL return a roomId and host confirmation
        
        // When
        val room = roomManager.createRoom()
        
        // Then
        assertNotNull(room.roomId)
        assertEquals("test-user-id", room.hostId)
        assertTrue(room.participants.any { it.isHost && it.userId == "test-user-id" })
    }
    
    @Test
    fun `room creation should satisfy requirement 2_5`() = runTest {
        // Requirement 2.5: WHEN a room code is generated THEN the system SHALL use 6 uppercase alphanumeric characters
        
        // When
        val room = roomManager.createRoom()
        
        // Then
        assertEquals(6, room.roomCode.length)
        assertTrue(room.roomCode.all { it.isUpperCase() || it.isDigit() })
        assertTrue(room.roomCode.all { it.isLetterOrDigit() })
    }
    
    @Test
    fun `participant management should satisfy requirement 8_4`() = runTest {
        // Requirement 8.4: WHEN managing participants THEN the system SHALL update the participant list in real-time for all users
        
        // Given
        roomManager.createRoom()
        val participantsFlow = roomManager.observeParticipants()
        
        // When - Add participant
        val newParticipant = Participant(
            userId = "new-participant",
            displayName = "New Participant",
            isHost = false,
            isVideoEnabled = false,
            isAudioEnabled = false,
            connectionState = ConnectionState.CONNECTING
        )
        roomManager.addParticipant(newParticipant)
        
        // Then - Real-time update should occur
        assertEquals(2, participantsFlow.value.size)
        assertTrue(participantsFlow.value.contains(newParticipant))
        
        // When - Update participant
        roomManager.updateParticipant("new-participant") { participant ->
            participant.copy(connectionState = ConnectionState.CONNECTED, isVideoEnabled = true)
        }
        
        // Then - Real-time update should occur
        val updatedParticipant = participantsFlow.value.first { it.userId == "new-participant" }
        assertEquals(ConnectionState.CONNECTED, updatedParticipant.connectionState)
        assertTrue(updatedParticipant.isVideoEnabled)
    }