package com.ltquiz.test.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ltquiz.test.managers.RoomManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoomJoiningUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val joinSuccess: Boolean = false
)

@HiltViewModel
class RoomJoiningViewModel @Inject constructor(
    private val roomManager: RoomManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoomJoiningUiState())
    val uiState: StateFlow<RoomJoiningUiState> = _uiState.asStateFlow()

    fun joinRoom(roomCode: String) {
        if (roomCode.length != 6) {
            _uiState.value = _uiState.value.copy(
                error = "Room code must be 6 characters"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val room = roomManager.joinRoom(roomCode)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    joinSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to join room. Please check your connection and try again."
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}