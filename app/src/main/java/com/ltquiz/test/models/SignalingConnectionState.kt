package com.ltquiz.test.models

/**
 * Represents the connection state of the signaling client.
 */
data class SignalingConnectionState(
    val isConnected: Boolean = false,
    val isReconnecting: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED
)