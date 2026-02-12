package com.ltquiz.test.managers

import com.ltquiz.test.models.Room
import com.ltquiz.test.models.Participant
import kotlinx.coroutines.flow.StateFlow

/**
 * Handles room creation, joining, and participant management.
 */
interface RoomManager {
    /**
     * Current room state.
     */
    val currentRoom: StateFlow<Room?>
    
    /**
     * Creates a new room and returns the room details.
     */
    suspend fun createRoom(): Room
    
    /**
     * Joins an existing room using the room code.
     */
    suspend fun joinRoom(roomCode: String): Room
    
    /**
     * Leaves the current room.
     */
    suspend fun leaveRoom()
    
    /**
     * Gets the current room if joined.
     */
    fun getCurrentRoom(): Room?
    
    /**
     * Gets the list of participants in the current room.
     */
    fun getParticipants(): List<Participant>
    
    /**
     * Observes participant list changes.
     */
    fun observeParticipants(): StateFlow<List<Participant>>
}