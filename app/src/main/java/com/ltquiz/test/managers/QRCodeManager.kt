package com.ltquiz.test.managers

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LifecycleOwner
import com.ltquiz.test.models.RoomData
import kotlinx.coroutines.flow.Flow

/**
 * Handles QR code generation and scanning.
 */
interface QRCodeManager {
    /**
     * Generates a QR code bitmap containing room data.
     */
    fun generateQRCode(roomData: RoomData): Bitmap
    
    /**
     * Starts QR code scanning with camera and returns flow of detected room data.
     * Requires a LifecycleOwner to manage camera lifecycle.
     */
    suspend fun startQRCodeScanning(lifecycleOwner: LifecycleOwner): Flow<RoomData>
    
    /**
     * Processes a single image for QR code detection.
     * Used for manual image processing.
     */
    suspend fun processImageForQRCode(imageProxy: ImageProxy): RoomData?
    
    /**
     * Stops QR code scanning and releases camera resources.
     */
    fun stopQRCodeScanning()
}