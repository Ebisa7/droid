package com.ltquiz.test.ui.viewmodels

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ltquiz.test.managers.QRCodeManager
import com.ltquiz.test.managers.RoomManager
import com.ltquiz.test.managers.SignalingClient
import com.ltquiz.test.models.RoomData
import com.ltquiz.test.ui.QRScanActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JoinRoomUiState(
    val isLoading: Boolean = false,
    val joinSuccess: Boolean = false,
    val error: String? = null,
    val isQRScanActive: Boolean = false,
    val roomCode: String? = null
)

@HiltViewModel
class JoinRoomViewModel @Inject constructor(
    private val roomManager: RoomManager,
    private val qrCodeManager: QRCodeManager,
    private val signalingClient: SignalingClient
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(JoinRoomUiState())
    val uiState: StateFlow<JoinRoomUiState> = _uiState.asStateFlow()
    
    fun joinRoom(roomCode: String) {
        if (roomCode.length != 6) {
            _uiState.value = _uiState.value.copy(error = "Room code must be 6 characters")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Connect to signaling server first
                signalingClient.connect("ws://localhost:8080")
                
                // Join the room
                val room = roomManager.joinRoom(roomCode)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    joinSuccess = true,
                    roomCode = roomCode
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Room not found. Please check the room code."
                )
            }
        }
    }
    
    fun joinRoomFromQR(roomData: RoomData) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Connect to the signaling server from QR data
                signalingClient.connect(roomData.serverAddress)
                
                // Join the room using the room ID from QR
                val room = roomManager.joinRoom(roomData.roomId)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    joinSuccess = true,
                    roomCode = roomData.roomId
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to join room from QR code."
                )
            }
        }
    }
    
    fun startQRScan() {
        _uiState.value = _uiState.value.copy(isQRScanActive = true)
    }
    
    fun handleQRScanResult(roomId: String?, serverAddress: String?) {
        _uiState.value = _uiState.value.copy(isQRScanActive = false)
        
        if (roomId != null && serverAddress != null) {
            val roomData = RoomData(roomId = roomId, serverAddress = serverAddress)
            joinRoomFromQR(roomData)
        } else {
            _uiState.value = _uiState.value.copy(
                error = "Invalid QR code. Please try again."
            )
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}