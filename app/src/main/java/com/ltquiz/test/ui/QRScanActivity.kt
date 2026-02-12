package com.ltquiz.test.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.ltquiz.test.managers.QRCodeManager
import com.ltquiz.test.models.RoomData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activity for scanning QR codes to join rooms.
 */
@AndroidEntryPoint
class QRScanActivity : ComponentActivity() {
    
    @Inject
    lateinit var qrCodeManager: QRCodeManager
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startScanning()
        } else {
            showPermissionDeniedMessage()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            QRScanScreen(
                onPermissionRequest = { checkCameraPermission() },
                onQRCodeDetected = { roomData -> handleQRCodeDetected(roomData) },
                onBackPressed = { finish() }
            )
        }
    }
    
    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startScanning()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun startScanning() {
        lifecycleScope.launch {
            qrCodeManager.startQRCodeScanning(this@QRScanActivity)
                .catch { exception ->
                    showErrorMessage("Scanning failed: ${exception.message}")
                }
                .collect { roomData ->
                    handleQRCodeDetected(roomData)
                }
        }
    }
    
    private fun handleQRCodeDetected(roomData: RoomData) {
        // Stop scanning once we detect a valid QR code
        qrCodeManager.stopQRCodeScanning()
        
        // Return the room data to the calling activity
        val resultIntent = android.content.Intent().apply {
            putExtra("roomId", roomData.roomId)
            putExtra("serverAddress", roomData.serverAddress)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
    
    private fun showPermissionDeniedMessage() {
        Toast.makeText(
            this,
            "Camera permission is required to scan QR codes",
            Toast.LENGTH_LONG
        ).show()
        finish()
    }
    
    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        qrCodeManager.stopQRCodeScanning()
    }
}

@Composable
fun QRScanScreen(
    onPermissionRequest: () -> Unit,
    onQRCodeDetected: (RoomData) -> Unit,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    LaunchedEffect(Unit) {
        onPermissionRequest()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackPressed) {
                Text("Cancel")
            }
            
            Text(
                text = "Scan QR Code",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.width(48.dp)) // Balance the cancel button
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Camera preview
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            AndroidView(
                factory = { context ->
                    PreviewView(context).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Instructions
        Text(
            text = "Point your camera at a QR code to join a room",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Make sure the QR code is clearly visible and well-lit",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}