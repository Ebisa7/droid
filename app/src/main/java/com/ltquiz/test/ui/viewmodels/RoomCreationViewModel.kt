package com.ltquiz.test.ui.viewmodels

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ltquiz.test.errors.ErrorHandler
import com.ltquiz.test.errors.launchWithErrorHandling
import com.ltquiz.test.errors.safeCall
import com.ltquiz.test.errors.showSuccess
import com.ltquiz.test.managers.QRCodeManager
import com.ltquiz.test.managers.RoomManager
import com.ltquiz.test.managers.SignalingClient
import com.ltquiz.test.managers.WebRTCManager
import com.ltquiz.test.managers.MediaManager
import com.ltquiz.test.models.Participant
import com.ltquiz.test.models.Room
import com.ltquiz.test.models.Signal
import com.ltquiz.test.models.SignalType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoomCreationUiState(
    val isLoading: Boolean = false,
    val room: Room? = null,
    val participants: List<Participant> = emptyList(),
    val qrCodeBitmap: Bitmap? = null,
    val error: String? = null,
    val isReadyForCall: Boolean = false,
    val shouldAutoStartCall: Boolean = false
)

@HiltViewModel
class RoomCreationViewModel @Inject constructor(
    private val roomManager: RoomManager,
    private val qrCodeManager: QRCodeManager,
    private val signalingClient: SignalingClient,
    private val webRTCManager: WebRTCManager,
    private val mediaManager: MediaManager,
    private val errorHandler: ErrorHandler
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(RoomCreationUiState())
    val uiState: StateFlow<RoomCreationUiState> = _uiState.asStateFlow()
    
    init {
        // Observe signaling messages for participant updates
        observeSignalingMessages()
    }
    
    fun createRoom() {
        launchWithErrorHandling(errorHandler, "RoomCreation") {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // Connect to signaling server first
            signalingClient.connect("ws://localhost:8080")
            
            val room = roomManager.createRoom()
            val qrCodeBitmap = qrCodeManager.generateQRCode(
                com.ltquiz.test.models.RoomData(
                    roomId = room.roomId,
                    serverAddress = "ws://localhost:8080"
                )
            )
            
            // Initialize WebRTC for the host
            initializeWebRTCForHost()
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                room = room,
                participants = room.participants,
                qrCodeBitmap = qrCodeBitmap,
                isReadyForCall = true
            )
            
            errorHandler.showSuccess("Room created successfully!")
            
            // Start observing participants and check for auto-start conditions
            observeParticipants()
            checkAutoStartConditions()
        }
    }
    
    fun cancelRoom() {
        launchWithErrorHandling(errorHandler, "RoomCancellation") {
            // Clean up WebRTC connections
            webRTCManager.closeAllConnections()
            
            // Disconnect signaling
            signalingClient.disconnect()
            
            // Leave room
            roomManager.leaveRoom()
            
            errorHandler.showSuccess("Room cancelled")
        }
    }
    
    fun prepareForCallTransition() {
        launchWithErrorHandling(errorHandler, "CallPreparation") {
            // Initialize local media for the call
            val localStream = mediaManager.initializeLocalMedia()
            if (localStream == null) {
                _uiState.value = _uiState.value.copy(error = "Failed to initialize camera and microphone")
                return@launchWithErrorHandling
            }
            
            // Add local stream to all existing peer connections
            val participants = _uiState.value.participants.filter { !it.isHost }
            participants.forEach { participant ->
                webRTCManager.addLocalStream(participant.userId, localStream)
            }
            
            _uiState.value = _uiState.value.copy(isReadyForCall = true)
        }
    }
    
    private fun initializeWebRTCForHost() {
        // Host doesn't need peer connections until participants join
        // This will be handled when participants actually join
    }
    
    private fun observeParticipants() {
        viewModelScope.launch {
            roomManager.observeParticipants().collect { participants ->
                _uiState.value = _uiState.value.copy(participants = participants)
                
                // Check if we should auto-start the call
                checkAutoStartConditions()
            }
        }
    }
    
    private fun observeSignalingMessages() {
        viewModelScope.launch {
            signalingClient.observeSignals().collect { signal ->
                handleSignalingMessage(signal)
            }
        }
    }
    
    private fun handleSignalingMessage(signal: Signal) {
        launchWithErrorHandling(errorHandler, "SignalingMessage") {
            when (signal.type) {
                SignalType.JOIN_ROOM -> {
                    // New participant joined
                    handleParticipantJoined(signal)
                }
                SignalType.LEAVE_ROOM -> {
                    // Participant left
                    handleParticipantLeft(signal)
                }
                SignalType.OFFER -> {
                    // Handle WebRTC offer from participant
                    handleWebRTCOffer(signal)
                }
                SignalType.ANSWER -> {
                    // Handle WebRTC answer from participant
                    handleWebRTCAnswer(signal)
                }
                SignalType.ICE_CANDIDATE -> {
                    // Handle ICE candidate from participant
                    handleIceCandidate(signal)
                }
                else -> {
                    // Handle other signal types
                }
            }
        }
    }
    
    private fun handleParticipantJoined(signal: Signal) {
        val participantId = signal.from
        
        // Create peer connection for new participant
        val success = webRTCManager.createPeerConnection(
            participantId = participantId,
            onIceCandidate = { candidate ->
                // Send ICE candidate to participant
                viewModelScope.launch {
                    signalingClient.sendSignal(
                        Signal(
                            from = roomManager.getCurrentRoom()?.hostId ?: "",
                            to = participantId,
                            type = SignalType.ICE_CANDIDATE,
                            payload = candidate.toString() // Serialize candidate
                        )
                    )
                }
            },
            onRemoteStream = { stream ->
                // Handle remote stream from participant
                // This will be used in the call screen
            }
        )
        
        if (success) {
            // Create offer for the new participant
            viewModelScope.launch {
                val offer = webRTCManager.createOffer(participantId)
                offer?.let {
                    signalingClient.sendSignal(
                        Signal(
                            from = roomManager.getCurrentRoom()?.hostId ?: "",
                            to = participantId,
                            type = SignalType.OFFER,
                            payload = it.description
                        )
                    )
                }
            }
        }
    }
    
    private fun handleParticipantLeft(signal: Signal) {
        val participantId = signal.from
        
        // Remove peer connection
        webRTCManager.removePeerConnection(participantId)
        
        // The participant removal will be handled by the room manager
        // through the signaling system and participant updates
    }
    
    private fun handleWebRTCOffer(signal: Signal) {
        // This shouldn't happen for host, but handle gracefully
    }
    
    private fun handleWebRTCAnswer(signal: Signal) {
        val participantId = signal.from
        val answerDescription = org.webrtc.SessionDescription(
            org.webrtc.SessionDescription.Type.ANSWER,
            signal.payload
        )
        
        webRTCManager.setRemoteDescription(participantId, answerDescription)
    }
    
    private fun handleIceCandidate(signal: Signal) {
        val participantId = signal.from
        // Parse ICE candidate from payload
        // This is simplified - in real implementation you'd properly deserialize
        try {
            val candidate = org.webrtc.IceCandidate("", 0, signal.payload)
            webRTCManager.addIceCandidate(participantId, candidate)
        } catch (e: Exception) {
            // Handle parsing error
        }
    }
    
    private fun checkAutoStartConditions() {
        val currentState = _uiState.value
        val room = currentState.room ?: return
        
        // Auto-start call when:
        // 1. Host has camera enabled (simulated by having participants > 1)
        // 2. At least one participant has joined
        val hasParticipants = currentState.participants.size > 1 // More than just host
        val hostHasCamera = mediaManager.mediaState.value.isVideoEnabled
        
        if (hasParticipants && currentState.isReadyForCall && !currentState.shouldAutoStartCall) {
            _uiState.value = currentState.copy(shouldAutoStartCall = true)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up resources
        viewModelScope.launch {
            try {
                webRTCManager.closeAllConnections()
                signalingClient.disconnect()
            } catch (e: Exception) {
                // Handle cleanup errors
            }
        }
    }
}