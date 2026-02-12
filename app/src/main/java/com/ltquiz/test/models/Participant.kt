package com.ltquiz.test.models

/**
 * Represents a participant in a video call room.
 */
data class Participant(
    val userId: String,
    val displayName: String,
    val isHost: Boolean,
    val isVideoEnabled: Boolean,
    val isAudioEnabled: Boolean,
    val connectionState: ConnectionState
)