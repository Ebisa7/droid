package com.ltquiz.test.managers

import kotlinx.coroutines.flow.StateFlow
import org.webrtc.*

/**
 * Manages peer connections and media streams for WebRTC.
 */
interface WebRTCManager {
    /**
     * Connection states for all participants.
     */
    val connectionStates: StateFlow<Map<String, PeerConnection.PeerConnectionState>>
    
    /**
     * Creates a peer connection for a specific participant.
     */
    fun createPeerConnection(
        participantId: String,
        onIceCandidate: (IceCandidate) -> Unit,
        onRemoteStream: (MediaStream) -> Unit
    ): Boolean
    
    /**
     * Adds an ICE candidate for a participant's peer connection.
     */
    fun addIceCandidate(participantId: String, candidate: IceCandidate): Boolean
    
    /**
     * Creates an offer for establishing connection with a participant.
     */
    suspend fun createOffer(participantId: String): SessionDescription?
    
    /**
     * Creates an answer in response to an offer from a participant.
     */
    suspend fun createAnswer(participantId: String): SessionDescription?
    
    /**
     * Sets the remote description for a participant's peer connection.
     */
    fun setRemoteDescription(participantId: String, sessionDescription: SessionDescription): Boolean
    
    /**
     * Adds local media stream to a participant's peer connection.
     */
    fun addLocalStream(participantId: String, stream: MediaStream): Boolean
    
    /**
     * Removes and closes a peer connection for a participant.
     */
    fun removePeerConnection(participantId: String)
    
    /**
     * Checks if a peer connection exists for a participant.
     */
    fun hasPeerConnection(participantId: String): Boolean
    
    /**
     * Closes all peer connections.
     */
    fun closeAllConnections()
}