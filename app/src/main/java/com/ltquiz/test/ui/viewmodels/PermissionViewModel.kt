package com.ltquiz.test.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ltquiz.test.permissions.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PermissionUiState(
    val hasCameraPermission: Boolean = false,
    val hasMicrophonePermission: Boolean = false,
    val hasAllRequiredPermissions: Boolean = false,
    val requiredPermissions: List<String> = PermissionManager.REQUIRED_PERMISSIONS.toList(),
    val showDeniedMessage: Boolean = false,
    val permissionRequestCount: Int = 0
)

@HiltViewModel
class PermissionViewModel @Inject constructor(
    private val permissionManager: PermissionManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PermissionUiState())
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()
    
    init {
        observePermissionState()
    }
    
    private fun observePermissionState() {
        viewModelScope.launch {
            permissionManager.permissionState.collect { permissionState ->
                _uiState.value = _uiState.value.copy(
                    hasCameraPermission = permissionState.hasCameraPermission,
                    hasMicrophonePermission = permissionState.hasMicrophonePermission,
                    hasAllRequiredPermissions = permissionState.hasAllRequiredPermissions
                )
            }
        }
    }
    
    fun onPermissionsResult(permissions: Map<String, Boolean>) {
        permissionManager.updatePermissionState()
        
        val currentCount = _uiState.value.permissionRequestCount + 1
        val hasAnyDenied = permissions.values.any { !it }
        
        _uiState.value = _uiState.value.copy(
            permissionRequestCount = currentCount,
            showDeniedMessage = hasAnyDenied && currentCount > 0
        )
    }
    
    fun getPermissionTitle(permission: String): String {
        return permissionManager.getPermissionTitle(permission)
    }
    
    fun getPermissionExplanation(permission: String): String {
        return permissionManager.getPermissionExplanation(permission)
    }
    
    fun checkPermissions() {
        permissionManager.updatePermissionState()
    }
}