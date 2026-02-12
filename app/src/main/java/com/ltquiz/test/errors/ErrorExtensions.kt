package com.ltquiz.test.errors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Extension functions for easier error handling in ViewModels and other components.
 */

/**
 * Launches a coroutine with automatic error handling.
 */
fun ViewModel.launchWithErrorHandling(
    errorHandler: ErrorHandler,
    context: String = "",
    block: suspend CoroutineScope.() -> Unit
) {
    val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        viewModelScope.launch {
            errorHandler.handleError(exception.toAppError(), context)
        }
    }
    
    viewModelScope.launch(exceptionHandler) {
        block()
    }
}

/**
 * Executes a block with error handling and returns the result or null on error.
 */
suspend inline fun <T> ErrorHandler.safeCall(
    context: String = "",
    crossinline block: suspend () -> T
): T? {
    return try {
        block()
    } catch (e: Exception) {
        handleError(e.toAppError(), context)
        null
    }
}

/**
 * Executes a block with error handling and returns a Result.
 */
suspend inline fun <T> ErrorHandler.safeCallResult(
    context: String = "",
    crossinline block: suspend () -> T
): Result<T> {
    return try {
        Result.success(block())
    } catch (e: Exception) {
        val appError = e.toAppError()
        handleError(appError, context)
        Result.failure(appError)
    }
}

/**
 * Shows a success message to the user.
 */
suspend fun ErrorHandler.showSuccess(message: String) {
    showUserMessage(message, isError = false)
}

/**
 * Shows an info message to the user.
 */
suspend fun ErrorHandler.showInfo(message: String) {
    showUserMessage(message, isError = false)
}

/**
 * Validates a condition and throws an appropriate error if false.
 */
inline fun validateOrThrow(condition: Boolean, lazyError: () -> AppError) {
    if (!condition) {
        throw lazyError()
    }
}

/**
 * Validates a room code format.
 */
fun validateRoomCode(roomCode: String): String {
    validateOrThrow(roomCode.length == 6) {
        AppError.RoomError.InvalidRoomCode(roomCode)
    }
    validateOrThrow(roomCode.all { it.isLetterOrDigit() }) {
        AppError.RoomError.InvalidRoomCode(roomCode)
    }
    return roomCode.uppercase()
}

/**
 * Validates network connectivity.
 */
fun validateNetworkConnection(isConnected: Boolean) {
    validateOrThrow(isConnected) {
        AppError.NetworkError.ServerUnreachable("No network connection")
    }
}

/**
 * Validates permissions.
 */
fun validatePermissions(hasCamera: Boolean, hasMicrophone: Boolean) {
    if (!hasCamera) {
        throw AppError.PermissionError.CameraPermissionDenied()
    }
    if (!hasMicrophone) {
        throw AppError.PermissionError.MicrophonePermissionDenied()
    }
}