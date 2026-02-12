package com.ltquiz.test.errors

/**
 * Sealed class hierarchy for application errors with user-friendly messages.
 */
sealed class AppError(
    val technicalMessage: String,
    val userMessage: String,
    val isRecoverable: Boolean = true
) : Exception(technicalMessage) {
    
    /**
     * Network-related errors.
     */
    sealed class NetworkError(
        technicalMessage: String,
        userMessage: String = "Connection problem. Please check your network.",
        isRecoverable: Boolean = true
    ) : AppError(technicalMessage, userMessage, isRecoverable) {
        
        class ConnectionTimeout(details: String = "") : NetworkError(
            "Connection timeout: $details",
            "Connection timed out. Please try again."
        )
        
        class ServerUnreachable(details: String = "") : NetworkError(
            "Server unreachable: $details",
            "Unable to connect to server. Please check your internet connection."
        )
        
        class SignalingFailure(details: String = "") : NetworkError(
            "Signaling failure: $details",
            "Connection to call server failed. Please try again."
        )
        
        class PeerConnectionFailure(details: String = "") : NetworkError(
            "Peer connection failure: $details",
            "Unable to connect to other participants. Please check your network."
        )
        
        class NetworkQualityPoor(details: String = "") : NetworkError(
            "Poor network quality: $details",
            "Network quality is poor. Call quality may be affected.",
            isRecoverable = true
        )
    }
    
    /**
     * Permission-related errors.
     */
    sealed class PermissionError(
        technicalMessage: String,
        userMessage: String = "Required permissions not granted.",
        isRecoverable: Boolean = true
    ) : AppError(technicalMessage, userMessage, isRecoverable) {
        
        class CameraPermissionDenied : PermissionError(
            "Camera permission denied",
            "Camera access is required for video calls. Please grant camera permission in settings."
        )
        
        class MicrophonePermissionDenied : PermissionError(
            "Microphone permission denied",
            "Microphone access is required for audio calls. Please grant microphone permission in settings."
        )
        
        class PermissionsPermanentlyDenied : PermissionError(
            "Permissions permanently denied",
            "Required permissions have been permanently denied. Please enable them in app settings.",
            isRecoverable = false
        )
    }
    
    /**
     * Room-related errors.
     */
    sealed class RoomError(
        technicalMessage: String,
        userMessage: String = "Room error occurred.",
        isRecoverable: Boolean = true
    ) : AppError(technicalMessage, userMessage, isRecoverable) {
        
        class RoomNotFound(roomCode: String) : RoomError(
            "Room not found: $roomCode",
            "Room not found. Please check the room code and try again."
        )
        
        class RoomFull(roomCode: String) : RoomError(
            "Room full: $roomCode",
            "This room is full. Please try joining another room."
        )
        
        class RoomEnded(roomCode: String) : RoomError(
            "Room ended: $roomCode",
            "This room has ended.",
            isRecoverable = false
        )
        
        class InvalidRoomCode(roomCode: String) : RoomError(
            "Invalid room code: $roomCode",
            "Invalid room code format. Room codes are 6 characters long."
        )
        
        class RoomCreationFailed(details: String = "") : RoomError(
            "Room creation failed: $details",
            "Failed to create room. Please try again."
        )
        
        class HostDisconnected : RoomError(
            "Host disconnected",
            "The host has left the meeting.",
            isRecoverable = false
        )
    }
    
    /**
     * Media-related errors.
     */
    sealed class MediaError(
        technicalMessage: String,
        userMessage: String = "Media error occurred.",
        isRecoverable: Boolean = true
    ) : AppError(technicalMessage, userMessage, isRecoverable) {
        
        class CameraInitializationFailed(details: String = "") : MediaError(
            "Camera initialization failed: $details",
            "Unable to access camera. Please check if another app is using it."
        )
        
        class MicrophoneInitializationFailed(details: String = "") : MediaError(
            "Microphone initialization failed: $details",
            "Unable to access microphone. Please check if another app is using it."
        )
        
        class CodecNotSupported(codec: String) : MediaError(
            "Codec not supported: $codec",
            "Your device doesn't support the required video format.",
            isRecoverable = false
        )
        
        class MediaStreamFailed(details: String = "") : MediaError(
            "Media stream failed: $details",
            "Video or audio stream failed. Trying to reconnect..."
        )
        
        class CameraSwitchFailed(details: String = "") : MediaError(
            "Camera switch failed: $details",
            "Unable to switch camera. Please try again."
        )
    }
    
    /**
     * QR Code related errors.
     */
    sealed class QRCodeError(
        technicalMessage: String,
        userMessage: String = "QR code error occurred.",
        isRecoverable: Boolean = true
    ) : AppError(technicalMessage, userMessage, isRecoverable) {
        
        class InvalidQRCode(details: String = "") : QRCodeError(
            "Invalid QR code: $details",
            "Invalid QR code. Please scan a valid XSEND Meet QR code."
        )
        
        class QRScannerFailed(details: String = "") : QRCodeError(
            "QR scanner failed: $details",
            "Unable to start QR scanner. Please try again."
        )
        
        class QRGenerationFailed(details: String = "") : QRCodeError(
            "QR generation failed: $details",
            "Unable to generate QR code. Please try again."
        )
    }
    
    /**
     * General application errors.
     */
    sealed class GeneralError(
        technicalMessage: String,
        userMessage: String = "An unexpected error occurred.",
        isRecoverable: Boolean = true
    ) : AppError(technicalMessage, userMessage, isRecoverable) {
        
        class UnknownError(details: String = "") : GeneralError(
            "Unknown error: $details",
            "An unexpected error occurred. Please try again."
        )
        
        class ConfigurationError(details: String = "") : GeneralError(
            "Configuration error: $details",
            "App configuration error. Please restart the app.",
            isRecoverable = false
        )
        
        class InsufficientResources(details: String = "") : GeneralError(
            "Insufficient resources: $details",
            "Device resources are low. Please close other apps and try again."
        )
    }
}