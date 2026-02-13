package com.ltquiz.test.managers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.ltquiz.test.models.RoomData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Implementation of QRCodeManager for generating and scanning QR codes.
 */
@Singleton
class QRCodeManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : QRCodeManager {
    
    private val qrCodeWriter = QRCodeWriter()
    private val json = Json { ignoreUnknownKeys = true }
    private val barcodeScanner = BarcodeScanning.getClient()
    private var cameraProvider: ProcessCameraProvider? = null
    
    override fun generateQRCode(roomData: RoomData): Bitmap {
        return try {
            // Serialize room data to JSON
            val qrContent = json.encodeToString(roomData)
            
            // Configure QR code generation
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            
            // Generate QR code matrix
            val bitMatrix = qrCodeWriter.encode(
                qrContent,
                BarcodeFormat.QR_CODE,
                QR_CODE_SIZE,
                QR_CODE_SIZE,
                hints
            )
            
            // Convert matrix to bitmap
            val bitmap = Bitmap.createBitmap(
                QR_CODE_SIZE,
                QR_CODE_SIZE,
                Bitmap.Config.RGB_565
            )
            
            for (x in 0 until QR_CODE_SIZE) {
                for (y in 0 until QR_CODE_SIZE) {
                    bitmap.setPixel(
                        x, 
                        y, 
                        if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                    )
                }
            }
            
            bitmap
        } catch (e: Exception) {
            throw QRCodeGenerationException("Failed to generate QR code: ${e.message}", e)
        }
    }
    
    override suspend fun startQRCodeScanning(lifecycleOwner: LifecycleOwner): Flow<RoomData> = callbackFlow {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
            processImageProxy(imageProxy) { roomData ->
                trySend(roomData)
            }
        }
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                close(QRCodeScanningException("Failed to start camera: ${e.message}", e))
            }
        }, ContextCompat.getMainExecutor(context))
        
        awaitClose {
            stopQRCodeScanning()
        }
    }
    
    override suspend fun processImageForQRCode(imageProxy: ImageProxy): RoomData? {
        return suspendCancellableCoroutine { continuation ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                
                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        // Find the first valid QR code
                        val roomData = barcodes.firstNotNullOfOrNull { barcode ->
                            if (barcode.format == Barcode.FORMAT_QR_CODE) {
                                barcode.rawValue?.let { qrContent ->
                                    try {
                                        json.decodeFromString<RoomData>(qrContent)
                                    } catch (e: Exception) {
                                        null // Invalid QR code format, continue to next barcode
                                    }
                                }
                            } else null
                        }
                        continuation.resume(roomData)
                    }
                    .addOnFailureListener { exception ->
                        continuation.resume(null)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
                continuation.resume(null)
            }
        }
    }
    
    override fun stopQRCodeScanning() {
        cameraProvider?.unbindAll()
        cameraProvider = null
    }
    
    private fun processImageProxy(imageProxy: ImageProxy, onQRCodeFound: (RoomData) -> Unit) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    // Find the first valid QR code
                    val validRoomData = barcodes.firstNotNullOfOrNull { barcode ->
                        if (barcode.format == Barcode.FORMAT_QR_CODE) {
                            barcode.rawValue?.let { qrContent ->
                                try {
                                    json.decodeFromString<RoomData>(qrContent)
                                } catch (e: Exception) {
                                    null // Invalid QR code format, ignore
                                }
                            }
                        } else null
                    }
                    
                    validRoomData?.let { roomData ->
                        onQRCodeFound(roomData)
                    }
                }
                .addOnFailureListener {
                    // Scanning failed, ignore and continue
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
    
    companion object {
        private const val QR_CODE_SIZE = 512
    }
}

/**
 * Exception thrown when QR code generation fails.
 */
class QRCodeGenerationException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when QR code scanning fails.
 */
class QRCodeScanningException(message: String, cause: Throwable? = null) : Exception(message, cause)