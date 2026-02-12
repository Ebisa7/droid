package com.ltquiz.test.errors

import android.content.Context
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for handling application errors.
 */
interface ErrorHandler {
    /**
     * Flow of user-friendly error messages to display.
     */
    val errorMessages: SharedFlow<ErrorMessage>
    
    /**
     * Handles an application error.
     */
    suspend fun handleError(error: AppError, context: String = "")
    
    /**
     * Shows a user-friendly message.
     */
    suspend fun showUserMessage(message: String, isError: Boolean = false)
    
    /**
     * Logs a technical error for debugging.
     */
    fun logTechnicalError(error: Throwable, context: String = "")
    
    /**
     * Handles graceful degradation scenarios.
     */
    suspend fun handleGracefulDegradation(error: AppError): Boolean
}

/**
 * Data class for error messages to display to users.
 */
data class ErrorMessage(
    val message: String,
    val isError: Boolean = true,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val duration: Long = 4000L
)

/**
 * Implementation of ErrorHandler.
 */
@Singleton
class ErrorHandlerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ErrorHandler {
    
    companion object {
        private const val TAG = "ErrorHandler"
    }
    
    private val _errorMessages = MutableSharedFlow<ErrorMessage>()
    override val errorMessages: SharedFlow<ErrorMessage> = _errorMessages.asSharedFlow()
    
    override suspend fun handleError(error: AppError, context: String) {
        // Log technical details
        logTechnicalError(error, context)
        
        // Try graceful degradation first
        val handled = handleGracefulDegradation(error)
        if (handled) return
        
        // Show user-friendly message
        val actionLabel = if (error.isRecoverable) "Retry" else null
        
        _errorMessages.emit(
            ErrorMessage(
                message = error.userMessage,
                isError = true,
                actionLabel = actionLabel,
                onAction = if (error.isRecoverable) {
                    { /* Retry logic would be handled by the calling component */ }
                } else null
            )
        )
    }
    
    override suspend fun showUserMessage(message: String, isError: Boolean) {
        _errorMessages.emit(
            ErrorMessage(
                message = message,
                isError = isError,
                duration = if (isError) 4000L else 2000L
            )
        )
    }
    
    override fun logTechnicalError(error: Throwable, context: String) {
        val contextInfo = if (context.isNotEmpty()) "[$context] " else ""
        Log.e(TAG, "${contextInfo}${error.message}", error)
        
        // In production, you might want to send this to a crash reporting service
        // like Firebase Crashlytics or Sentry
    }
    
    override suspend fun handleGracefulDegradation(error: AppError): Boolean {
        return when (error) {
            is AppError.MediaError.CameraInitializationFailed -> {
                // Degrade to audio-only mode
                showUserMessage("Camera unavailable. Continuing with audio only.", isError = false)
                true
            }
            
            is AppError.MediaError.MicrophoneInitializationFailed -> {
                // Degrade to video-only mode
                showUserMessage("Microphone unavailable. Continuing with video only.", isError = false)
                true
            }
            
            is AppError.NetworkError.NetworkQualityPoor -> {
                // Automatically reduce video quality
                showUserMessage("Poor network detected. Reducing video quality.", isError = false)
                true
            }
            
            is AppError.MediaError.CodecNotSupported -> {
                // Try fallback codec or audio-only
                showUserMessage("Video format not supported. Switching to audio only.", isError = false)
                true
            }
            
            else -> false // No graceful degradation available
        }
    }
}

/**
 * Extension function to convert common exceptions to AppError.
 */
fun Throwable.toAppError(): AppError {
    return when (this) {
        is AppError -> this
        
        is java.net.SocketTimeoutException -> AppError.NetworkError.ConnectionTimeout(
            this.message ?: "Socket timeout"
        )
        
        is java.net.UnknownHostException -> AppError.NetworkError.ServerUnreachable(
            this.message ?: "Unknown host"
        )
        
        is java.net.ConnectException -> AppError.NetworkError.ServerUnreachable(
            this.message ?: "Connection refused"
        )
        
        is SecurityException -> {
            when {
                message?.contains("camera", ignoreCase = true) == true -> 
                    AppError.PermissionError.CameraPermissionDenied()
                message?.contains("microphone", ignoreCase = true) == true || 
                message?.contains("record_audio", ignoreCase = true) == true -> 
                    AppError.PermissionError.MicrophonePermissionDenied()
                else -> AppError.PermissionError.PermissionsPermanentlyDenied()
            }
        }
        
        is IllegalArgumentException -> AppError.GeneralError.ConfigurationError(
            this.message ?: "Invalid argument"
        )
        
        is OutOfMemoryError -> AppError.GeneralError.InsufficientResources(
            "Out of memory"
        )
        
        else -> AppError.GeneralError.UnknownError(
            this.message ?: "Unknown exception: ${this.javaClass.simpleName}"
        )
    }
}