package com.ltquiz.test.models

/**
 * Represents a signaling message between clients.
 */
data class Signal(
    val from: String,
    val to: String,
    val type: SignalType,
    val payload: String
)

/**
 * Types of signaling messages.
 */
enum class SignalType {
    OFFER, 
    ANSWER, 
    ICE_CANDIDATE, 
    JOIN_ROOM, 
    LEAVE_ROOM, 
    PARTICIPANT_UPDATE,
    PARTICIPANT_JOINED,
    PARTICIPANT_LEFT,
    ROOM_ENDED
}