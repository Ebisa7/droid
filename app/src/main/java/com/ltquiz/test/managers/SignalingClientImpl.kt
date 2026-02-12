package com.ltquiz.test.managers

import com.ltquiz.test.models.Signal
import com.ltquiz.test.models.SignalType
import com.ltquiz.test.models.ConnectionState
import com.ltquiz.test.models.SignalingConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SignalingClient using OkHttp WebSocket.
 */
@Singleton
class SignalingClientImpl @Inject constructor() : SignalingClient {
    
    private val okHttpClient = OkHttpClient()
    private var webSocket: WebSocket? = null
    private var serverUrl: String? = null
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _connectionState = MutableStateFlow(
        SignalingConnectionState(
            isConnected = false,
            isReconnecting = false,
            connectionState = ConnectionState.DISCONNECTED
        )
    )
    private val connectionState: StateFlow<SignalingConnectionState> = _connectionState.asStateFlow()
    
    private val _signalChannel = Channel<Signal>(Channel.UNLIMITED)
    
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val reconnectDelayMs = 2000L
    
    override suspend fun connect(serverUrl: String) {
        this.serverUrl = serverUrl
        _connectionState.value = _connectionState.value.copy(
            isConnected = false,
            isReconnecting = false,
            connectionState = ConnectionState.CONNECTING
        )
        
        val request = Request.Builder()
            .url(serverUrl)
            .build()
            
        webSocket = okHttpClient.newWebSocket(request, createWebSocketListener())
    }
    
    override suspend fun disconnect() {
        _connectionState.value = _connectionState.value.copy(
            isConnected = false,
            isReconnecting = false,
            connectionState = ConnectionState.DISCONNECTED
        )
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        reconnectAttempts = 0
    }
    
    override suspend fun sendSignal(signal: Signal) {
        val json = JSONObject().apply {
            put("from", signal.from)
            put("to", signal.to)
            put("type", signal.type.name)
            put("payload", signal.payload)
        }
        
        webSocket?.send(json.toString())
    }
    
    override fun observeSignals(): Flow<Signal> {
        return _signalChannel.receiveAsFlow()
    }
    
    override fun observeConnectionState(): Flow<SignalingConnectionState> {
        return connectionState
    }
    
    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = _connectionState.value.copy(
                    isConnected = true,
                    isReconnecting = false,
                    connectionState = ConnectionState.CONNECTED
                )
                reconnectAttempts = 0
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val signal = Signal(
                        from = json.getString("from"),
                        to = json.getString("to"),
                        type = SignalType.valueOf(json.getString("type")),
                        payload = json.getString("payload")
                    )
                    
                    scope.launch {
                        _signalChannel.send(signal)
                    }
                } catch (e: Exception) {
                    // Log error but don't crash
                    e.printStackTrace()
                }
            }
            
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // Handle binary messages if needed
                onMessage(webSocket, bytes.utf8())
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = _connectionState.value.copy(
                    isConnected = false,
                    isReconnecting = false,
                    connectionState = ConnectionState.DISCONNECTED
                )
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = _connectionState.value.copy(
                    isConnected = false,
                    isReconnecting = false,
                    connectionState = ConnectionState.DISCONNECTED
                )
                
                // Attempt reconnection if not intentionally closed
                if (code != 1000 && reconnectAttempts < maxReconnectAttempts) {
                    attemptReconnection()
                }
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = _connectionState.value.copy(
                    isConnected = false,
                    isReconnecting = false,
                    connectionState = ConnectionState.FAILED
                )
                
                // Attempt reconnection on failure
                if (reconnectAttempts < maxReconnectAttempts) {
                    attemptReconnection()
                } else {
                    _connectionState.value = _connectionState.value.copy(
                        isConnected = false,
                        isReconnecting = false,
                        connectionState = ConnectionState.DISCONNECTED
                    )
                }
            }
        }
    }
    
    private fun attemptReconnection() {
        if (serverUrl == null) return
        
        reconnectAttempts++
        _connectionState.value = _connectionState.value.copy(
            isConnected = false,
            isReconnecting = true,
            connectionState = ConnectionState.RECONNECTING
        )
        
        scope.launch {
            delay(reconnectDelayMs * reconnectAttempts)
            
            if (_connectionState.value.isReconnecting) {
                try {
                    connect(serverUrl!!)
                } catch (e: Exception) {
                    if (reconnectAttempts >= maxReconnectAttempts) {
                        _connectionState.value = _connectionState.value.copy(
                            isConnected = false,
                            isReconnecting = false,
                            connectionState = ConnectionState.FAILED
                        )
                    }
                }
            }
        }
    }
}