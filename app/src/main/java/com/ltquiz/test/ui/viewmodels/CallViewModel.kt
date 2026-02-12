package com.ltquiz.test.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ltquiz.test.audio.SoundEffectsManager
import com.ltquiz.test.managers.*
import com.ltquiz.test.models.Participant
import com.ltquiz.test.models.Signal
import com.ltquiz.test.models.SignalType
import com.ltquiz.test.models.SignalingConnectionState
import com.ltquiz.test.network.NetworkQualityMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject

data class CallUiState(
    val participants: List<Participant> = emptyList(),
    val isVideoEnabled: Boolean = true,
    val isAudioEnabled: Boolean = true,
    val isFrontCamera: Boolean = true,
    val isConnected: Boolean = true,
    val networkQuality: String = "Good",
    val isReconnecting: Boolean = false,
    val localVideoRenderer: SurfaceViewRenderer? = null,
    val error: String? = null,
    val showNetworkBanner: Boolean = false
)

@HiltViewModel
class CallViewModel @Inject constructor(
    private val roomManager: RoomManager,
    private val webRTCManager: WebRTCManager,
    private val mediaManager: MediaManager,
    private val signalingClient: SignalingClient,
    private val networkQualityMonitor: NetworkQualityMonitor,
    private val soundEffectsManager: SoundEffectsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    init {
        observeRoomUpdates()
        observeMediaState()
        observeConnectionState()
        observeNetworkQuality()
    }

    fun initializeCall(roomCode: String) {
        viewModelScope.launch {
            try {
                // Start network monitoring
                networkQualityMonitor.startMonitoring()
                
                // Check if we already have a room (coming from room creation)
                val existingRoom = roomManager.getCurrentRoom()
                if (existingRoom?.roomCode == roomCode) {
                    // We're transitioning from room creation, room is already set up
                    // Just initialize media if not already done
                    val localStream = mediaManager.getLocalMediaStream() 
                        ?: mediaManager.initializeLocalMedia()
                    
                    if (localStream == null) {
                        updateError("Failed to initialize camera and microphone")
                        return@launch
                    }
                    
                    // Start listening for signaling messages to establish peer connections
                    observeSignalingMessages()
                } else {
                    // We're joining an existing room (participant flow)
                    // Initialize media first
                    val localStream = mediaManager.initializeLocalMedia()
                    if (localStream == null) {
                        updateError("Failed to initialize camera and microphone")
                        return@launch
                    }

                    // Room should already be joined by JoinRoomViewModel
                    // Start listening for signaling messages to establish peer connections
                    observeSignalingMessages()
                    
                    // Notify other participants that we've joined
                    notifyParticipantJoined()
                }

            } catch (e: Exception) {
                updateError("Failed to initialize call: ${e.message}")
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
    
    private suspend fun handleSignalingMessage(signal: Signal) {
        when (signal.type) {
            SignalType.PARTICIPANT_JOINED -> {
                // A new participant joined, create peer connection
                createPeerConnectionForParticipant(signal.from)
            }
            SignalType.OFFER -> {
                // Received offer, create answer
                handleOffer(signal.from, signal.payload)
            }
            SignalType.ANSWER -> {
                // Received answer, set remote description
                handleAnswer(signal.from, signal.payload)
            }
            SignalType.ICE_CANDIDATE -> {
                // Received ICE candidate
                handleIceCandidate(signal.from, signal.payload)
            }
            SignalType.PARTICIPANT_LEFT -> {
                // Participant left, remove peer connection
                handleParticipantLeft(signal.from)
            }
            SignalType.ROOM_ENDED -> {
                // Host ended the room
                handleRoomEnded()
            }
            else -> {
                // Handle other signal types
            }
        }
    }
    
    private suspend fun createPeerConnectionForParticipant(participantId: String) {
        val localStream = mediaManager.getLocalMediaStream() ?: return
        
        val success = webRTCManager.createPeerConnection(
            participantId = participantId,
            onIceCandidate = { candidate ->
                viewModelScope.launch {
                    sendIceCandidate(participantId, candidate)
                }
            },
            onRemoteStream = { remoteStream ->
                // Handle remote stream - update UI to show participant video
                handleRemoteStream(participantId, remoteStream)
            }
        )
        
        if (success) {
            // Add local stream to peer connection
            webRTCManager.addLocalStream(participantId, localStream)
            
            // Create and send offer
            val offer = webRTCManager.createOffer(participantId)
            offer?.let {
                sendOffer(participantId, it)
            }
        }
    }
    
    private suspend fun handleOffer(participantId: String, offerSdp: String) {
        val localStream = mediaManager.getLocalMediaStream() ?: return
        
        // Create peer connection if it doesn't exist
        if (!webRTCManager.hasPeerConnection(participantId)) {
            val success = webRTCManager.createPeerConnection(
                participantId = participantId,
                onIceCandidate = { candidate ->
                    viewModelScope.launch {
                        sendIceCandidate(participantId, candidate)
                    }
                },
                onRemoteStream = { remoteStream ->
                    handleRemoteStream(participantId, remoteStream)
                }
            )
            
            if (success) {
                webRTCManager.addLocalStream(participantId, localStream)
            }
        }
        
        // Set remote description and create answer
        val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        webRTCManager.setRemoteDescription(participantId, sessionDescription)
        
        val answer = webRTCManager.createAnswer(participantId)
        answer?.let {
            sendAnswer(participantId, it)
        }
    }
    
    private suspend fun handleAnswer(participantId: String, answerSdp: String) {
        val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
        webRTCManager.setRemoteDescription(participantId, sessionDescription)
    }
    
    private suspend fun handleIceCandidate(participantId: String, candidateJson: String) {
        try {
            // Parse ICE candidate from JSON
            val json = org.json.JSONObject(candidateJson)
            val candidate = IceCandidate(
                json.getString("sdpMid"),
                json.getInt("sdpMLineIndex"),
                json.getString("candidate")
            )
            webRTCManager.addIceCandidate(participantId, candidate)
        } catch (e: Exception) {
            // Handle parsing error
        }
    }
    
    private fun handleParticipantLeft(participantId: String) {
        webRTCManager.removePeerConnection(participantId)
        // Update UI to remove participant
    }
    
    private fun handleRoomEnded() {
        // Show "Host ended the meeting" message and navigate back
        updateError("Host ended the meeting")
        leaveCall()
    }
    
    private fun handleRemoteStream(participantId: String, remoteStream: MediaStream) {
        // Update UI to show remote participant's video
        // This would involve updating the participants list with video renderer
    }
    
    private suspend fun notifyParticipantJoined() {
        val currentRoom = roomManager.getCurrentRoom() ?: return
        val signal = Signal(
            from = currentRoom.participants.find { !it.isHost }?.userId ?: return,
            to = "room",
            type = SignalType.PARTICIPANT_JOINED,
            payload = ""
        )
        signalingClient.sendSignal(signal)
    }
    
    private suspend fun sendOffer(participantId: String, offer: SessionDescription) {
        val signal = Signal(
            from = roomManager.getCurrentRoom()?.participants?.find { !it.isHost }?.userId ?: return,
            to = participantId,
            type = SignalType.OFFER,
            payload = offer.description
        )
        signalingClient.sendSignal(signal)
    }
    
    private suspend fun sendAnswer(participantId: String, answer: SessionDescription) {
        val signal = Signal(
            from = roomManager.getCurrentRoom()?.participants?.find { !it.isHost }?.userId ?: return,
            to = participantId,
            type = SignalType.ANSWER,
            payload = answer.description
        )
        signalingClient.sendSignal(signal)
    }
    
    private suspend fun sendIceCandidate(participantId: String, candidate: IceCandidate) {
        val candidateJson = org.json.JSONObject().apply {
            put("candidate", candidate.sdp)
            put("sdpMid", candidate.sdpMid)
            put("sdpMLineIndex", candidate.sdpMLineIndex)
        }.toString()
        
        val signal = Signal(
            from = roomManager.getCurrentRoom()?.participants?.find { !it.isHost }?.userId ?: return,
            to = participantId,
            type = SignalType.ICE_CANDIDATE,
            payload = candidateJson
        )
        signalingClient.sendSignal(signal)
    }

    fun toggleVideo() {
        val currentState = _uiState.value
        mediaManager.enableVideo(!currentState.isVideoEnabled)
    }

    fun toggleAudio() {
        val currentState = _uiState.value
        mediaManager.enableAudio(!currentState.isAudioEnabled)
    }

    fun switchCamera() {
        mediaManager.switchCamera()
    }

    fun leaveCall() {
        viewModelScope.launch {
            try {
                // Stop network monitoring
                networkQualityMonitor.stopMonitoring()
                
                // Close all peer connections
                webRTCManager.closeAllConnections()
                
                // Stop media capture
                mediaManager.stopCapture()
                
                // Leave room
                roomManager.leaveRoom()
                
                // Disconnect signaling
                signalingClient.disconnect()
                
            } catch (e: Exception) {
                // Log error but don't prevent leaving
            }
        }
    }

    fun setLocalVideoRenderer(renderer: SurfaceViewRenderer) {
        mediaManager.setLocalVideoRenderer(renderer)
        _uiState.value = _uiState.value.copy(localVideoRenderer = renderer)
    }
    
    fun dismissNetworkBanner() {
        _uiState.value = _uiState.value.copy(showNetworkBanner = false)
    }
    
    fun playSound(effect: SoundEffectsManager.SoundEffect) {
        viewModelScope.launch {
            soundEffectsManager.playSound(effect)
        }
    }

    private fun observeRoomUpdates() {
        viewModelScope.launch {
            roomManager.currentRoom.collect { room ->
                room?.let {
                    _uiState.value = _uiState.value.copy(
                        participants = it.participants
                    )
                }
            }
        }
    }

    private fun observeMediaState() {
        viewModelScope.launch {
            mediaManager.mediaState.collect { mediaState ->
                _uiState.value = _uiState.value.copy(
                    isVideoEnabled = mediaState.isVideoEnabled,
                    isAudioEnabled = mediaState.isAudioEnabled,
                    isFrontCamera = mediaState.isFrontCamera
                )
            }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            signalingClient.observeConnectionState().collect { connectionState ->
                _uiState.value = _uiState.value.copy(
                    isConnected = connectionState.isConnected,
                    isReconnecting = connectionState.isReconnecting
                )
            }
        }
        
        viewModelScope.launch {
            webRTCManager.connectionStates.collect { connectionStates ->
                // Update network quality based on connection states
                val networkQuality = calculateNetworkQuality(connectionStates)
                _uiState.value = _uiState.value.copy(
                    networkQuality = networkQuality
                )
            }
        }
    }
    
    private fun observeNetworkQuality() {
        viewModelScope.launch {
            networkQualityMonitor.networkQuality.collect { quality ->
                val displayName = quality.level.displayName
                val shouldShowBanner = quality.level.ordinal >= 3 // POOR or DISCONNECTED
                
                _uiState.value = _uiState.value.copy(
                    networkQuality = if (quality.isLocalNetwork) "$displayName (LAN)" else displayName,
                    isConnected = quality.isConnected,
                    showNetworkBanner = shouldShowBanner
                )
                
                // Automatically adjust video quality based on network
                if (networkQualityMonitor.shouldReduceVideoQuality()) {
                    adjustVideoQualityForNetwork()
                }
            }
        }
    }
    
    private fun adjustVideoQualityForNetwork() {
        viewModelScope.launch {
            try {
                val recommendedQuality = networkQualityMonitor.getRecommendedVideoQuality()
                // TODO: Apply video quality settings to MediaManager
                // This would involve updating the MediaManager to support quality adjustment
            } catch (e: Exception) {
                // Handle quality adjustment error
            }
        }
    }

    private fun calculateNetworkQuality(connectionStates: Map<String, org.webrtc.PeerConnection.PeerConnectionState>): String {
        if (connectionStates.isEmpty()) return "Good"
        
        val connectedCount = connectionStates.values.count { 
            it == org.webrtc.PeerConnection.PeerConnectionState.CONNECTED 
        }
        val totalCount = connectionStates.size
        
        return when {
            connectedCount == totalCount -> "Good"
            connectedCount > totalCount / 2 -> "Fair"
            else -> "Poor"
        }
    }

    private fun updateError(message: String) {
        _uiState.value = _uiState.value.copy(error = message)
    }

    override fun onCleared() {
        super.onCleared()
        networkQualityMonitor.stopMonitoring()
        mediaManager.dispose()
    }
}