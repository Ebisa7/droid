package com.ltquiz.test.managers

import com.ltquiz.test.models.Signal
import com.ltquiz.test.models.SignalingConnectionState
import kotlinx.coroutines.flow.Flow

/**
 * Manages WebSocket communication with signaling server.
 */
interface SignalingClient {
    /**
     * Connects to the signaling server.
     */
    suspend fun connect(serverUrl: String = "ws://localhost:8080")
    
    /**
     * Disconnects from the signaling server.
     */
    suspend fun disconnect()
    
    /**
     * Sends a signal message to the server.
     */
    suspend fun sendSignal(signal: Signal)
    
    /**
     * Observes incoming signals from the server.
     */
    fun observeSignals(): Flow<Signal>
    
    /**
     * Observes connection state changes.
     */
    fun observeConnectionState(): Flow<SignalingConnectionState>
}