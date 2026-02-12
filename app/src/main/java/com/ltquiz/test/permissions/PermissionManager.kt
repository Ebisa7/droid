package com.ltquiz.test.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class PermissionState(
    val hasCameraPermission: Boolean = false,
    val hasMicrophonePermission: Boolean = false,
    val hasAllRequiredPermissions: Boolean = false,
    val deniedPermissions: List<String> = emptyList(),
    val shouldShowRationale: Map<String, Boolean> = emptyMap()
)

@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CAMERA_PERMISSION = Manifest.permission.CAMERA
        const val MICROPHONE_PERMISSION = Manifest.permission.RECORD_AUDIO
        
        val REQUIRED_PERMISSIONS = arrayOf(
            CAMERA_PERMISSION,
            MICROPHONE_PERMISSION
        )
    }
    
    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()
    
    init {
        updatePermissionState()
    }
    
    fun updatePermissionState() {
        val hasCameraPermission = hasPermission(CAMERA_PERMISSION)
        val hasMicrophonePermission = hasPermission(MICROPHONE_PERMISSION)
        val hasAllRequired = hasCameraPermission && hasMicrophonePermission
        
        val deniedPermissions = REQUIRED_PERMISSIONS.filter { !hasPermission(it) }
        
        _permissionState.value = PermissionState(
            hasCameraPermission = hasCameraPermission,
            hasMicrophonePermission = hasMicrophonePermission,
            hasAllRequiredPermissions = hasAllRequired,
            deniedPermissions = deniedPermissions
        )
    }
    
    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun hasAllRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { hasPermission(it) }
    }
    
    fun getMissingPermissions(): List<String> {
        return REQUIRED_PERMISSIONS.filter { !hasPermission(it) }
    }
    
    fun getPermissionExplanation(permission: String): String {
        return when (permission) {
            CAMERA_PERMISSION -> "Camera access is required to share your video in calls"
            MICROPHONE_PERMISSION -> "Microphone access is required to share your audio in calls"
            else -> "This permission is required for the app to function properly"
        }
    }
    
    fun getPermissionTitle(permission: String): String {
        return when (permission) {
            CAMERA_PERMISSION -> "Camera Permission"
            MICROPHONE_PERMISSION -> "Microphone Permission"
            else -> "Permission Required"
        }
    }
}