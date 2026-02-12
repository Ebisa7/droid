package com.ltquiz.test.managers

import com.ltquiz.test.models.Room
import com.ltquiz.test.models.Participant
import com.ltquiz.test.models.RoomStatus
import com.ltquiz.test.models.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.random.Random

/**
 * Implementation of RoomManager that handles room creation, joining, and participant management.
 */
class RoomManagerImpl(
    private val identityManager: IdentityManager,
    private val signalingClient: SignalingClient
) : RoomManager {
    
    private val _currentRoom = MutableStateFlow<Room?>(null)
    override val currentRoom: StateFlow<Room?> = _currentRoom.asStateFlow()
    
    private val _participants = MutableStateFlow<List<Participant>>(emptyList())
    
    // Characters to use for room codes (excluding confusing ones like 0, O, I, 1, etc.)
    private val roomCodeChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    
    override suspend fun createRoom(): Room {
        val roomId = UUID.randomUUID().toString()
        val roomCode = generateRoomCode()
        val hostId = identityManager.getUserId()
        
        val hostParticipant = Participant(
            userId = hostId,
            displayName = identityManager.getDisplayName(),
            isHost = true,
            isVideoEnabled = false,
            isAudioEnabled = false,
            connectionState = ConnectionState.CONNECTED
        )
        
        val room = Room(
            roomId = roomId,
            roomCode = roomCode,
            hostId = hostId,
            participants = mutableListOf(hostParticipant),
            status = RoomStatus.WAITING
        )
        
        _currentRoom.value = room
        _participants.value = room.participants.toList()
        
        return room
    }
    
    override suspend fun joinRoom(roomCode: String): Room {
        // Validate room code format
        if (!isValidRoomCode(roomCode)) {
            throw IllegalArgumentException("Invalid room code format")
        }
        
        val userId = identityManager.getUserId()
        val displayName = identityManager.getDisplayName()
        
        // For now, simulate room joining - in real implementation this would
        // communicate with the signaling server
        val roomId = UUID.randomUUID().toString()
        
        val participant = Participant(
            userId = userId,
            displayName = displayName,
            isHost = false,
            isVideoEnabled = false,
            isAudioEnabled = false,
            connectionState = ConnectionState.CONNECTING
        )
        
        val room = Room(
            roomId = roomId,
            roomCode = roomCode,
            hostId = "host-id", // Would come from server
            participants = mutableListOf(participant),
            status = RoomStatus.WAITING
        )
        
        _currentRoom.value = room
        _participants.value = room.participants.toList()
        
        return room
    }
    
    override suspend fun leaveRoom() {
        _currentRoom.value?.let { room ->
            val userId = identityManager.getUserId()
            room.participants.removeAll { it.userId == userId }
            _participants.value = room.participants.toList()
            
            if (room.participants.isEmpty() || room.hostId == userId) {
                // If host leaves or room is empty, end the room
                _currentRoom.value = null
                _participants.value = emptyList()
            }
        }
        _currentRoom.value = null
    }
    
    override fun getCurrentRoom(): Room? {
        return _currentRoom.value
    }
    
    override fun getParticipants(): List<Participant> {
        return _participants.value
    }
    
    /**
     * Observes participant list changes.
     */
    override fun observeParticipants(): StateFlow<List<Participant>> {
        return _participants.asStateFlow()
    }
    
    /**
     * Adds a participant to the current room.
     */
    fun addParticipant(participant: Participant) {
        _currentRoom.value?.let { room ->
            room.participants.add(participant)
            _participants.value = room.participants.toList()
        }
    }
    
    /**
     * Removes a participant from the current room.
     */
    fun removeParticipant(userId: String) {
        _currentRoom.value?.let { room ->
            room.participants.removeAll { it.userId == userId }
            _participants.value = room.participants.toList()
        }
    }
    
    /**
     * Updates a participant's state in the current room.
     */
    fun updateParticipant(userId: String, updater: (Participant) -> Participant) {
        _currentRoom.value?.let { room ->
            val index = room.participants.indexOfFirst { it.userId == userId }
            if (index != -1) {
                room.participants[index] = updater(room.participants[index])
                _participants.value = room.participants.toList()
            }
        }
    }
    
    /**
     * Generates a 6-character room code excluding confusing characters.
     */
    private fun generateRoomCode(): String {
        return (1..6)
            .map { roomCodeChars[Random.nextInt(roomCodeChars.length)] }
            .joinToString("")
    }
    
    /**
     * Validates if a room code has the correct format.
     */
    private fun isValidRoomCode(roomCode: String): Boolean {
        return roomCode.length == 6 && 
               roomCode.all { it in roomCodeChars }
    }
}