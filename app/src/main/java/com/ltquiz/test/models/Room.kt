package com.ltquiz.test.models

/**
 * Represents a video call room.
 */
data class Room(
    val roomId: String,
    val roomCode: String,
    val hostId: String,
    val participants: MutableList<Participant>,
    val status: RoomStatus
)