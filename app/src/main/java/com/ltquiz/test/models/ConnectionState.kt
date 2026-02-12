package com.ltquiz.test.models

/**
 * Represents the connection state of a participant.
 */
enum class ConnectionState {
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    DISCONNECTED,
    FAILED
}