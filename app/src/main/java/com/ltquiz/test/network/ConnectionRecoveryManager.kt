package com.ltquiz.test.network

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class ConnectionState(
    val isConnected: Boolean = false,
    val isReconnecting: Boolean = false,
    val reconnectAttempts: Int = 0,
    val lastError: String? = null
)

@Singleton
class ConnectionRecoveryManager @Inject constructor() {
    companion object {
        private const val MAX_RECONNECT_ATTEMPTS = 3
        private const val RECONNECT_TIMEOUT_MS = 5000L
        private const val RECONNECT_DELAY_MS = 1000L
    }
    
    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private var reconnectJob: Job? = null
    private var timeoutJob: Job? = null
    
    fun startReconnection(
        onReconnect: suspend () -> Boolean,
        onTimeout: () -> Unit,
        scope: CoroutineScope
    ) {
        if (_connectionState.value.isReconnecting) return
        
        _connectionState.value = _connectionState.value.copy(
            isReconnecting = true,
            reconnectAttempts = 0
        )
        
        // Start timeout timer
        timeoutJob = scope.launch {
            delay(RECONNECT_TIMEOUT_MS)
            if (_connectionState.value.isReconnecting) {
                stopReconnection()
                onTimeout()
            }
        }
        
        // Start reconnection attempts
        reconnectJob = scope.launch {
            var attempts = 0
            
            while (attempts < MAX_RECONNECT_ATTEMPTS && _connectionState.value.isReconnecting) {
                attempts++
                _connectionState.value = _connectionState.value.copy(reconnectAttempts = attempts)
                
                try {
                    val success = onReconnect()
                    if (success) {
                        onReconnectionSuccess()
                        return@launch
                    }
                } catch (e: Exception) {
                    _connectionState.value = _connectionState.value.copy(
                        lastError = e.message
                    )
                }
                
                if (attempts < MAX_RECONNECT_ATTEMPTS) {
                    delay(RECONNECT_DELAY_MS * attempts) // Exponential backoff
                }
            }
            
            // All attempts failed
            if (_connectionState.value.isReconnecting) {
                stopReconnection()
                onTimeout()
            }
        }
    }
    
    fun onReconnectionSuccess() {
        stopReconnection()
        _connectionState.value = ConnectionState(isConnected = true)
    }
    
    fun onConnectionLost(error: String? = null) {
        _connectionState.value = _connectionState.value.copy(
            isConnected = false,
            lastError = error
        )
    }
    
    fun stopReconnection() {
        reconnectJob?.cancel()
        timeoutJob?.cancel()
        reconnectJob = null
        timeoutJob = null
        
        _connectionState.value = _connectionState.value.copy(
            isReconnecting = false
        )
    }
    
    fun reset() {
        stopReconnection()
        _connectionState.value = ConnectionState()
    }
}