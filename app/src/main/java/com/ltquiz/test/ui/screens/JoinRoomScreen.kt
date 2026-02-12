package com.ltquiz.test.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ltquiz.test.ui.QRScanActivity
import com.ltquiz.test.ui.viewmodels.JoinRoomViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinRoomScreen(
    onNavigateBack: () -> Unit,
    onJoinSuccess: (String) -> Unit,
    viewModel: JoinRoomViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var roomCode by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    // QR Scanner launcher
    val qrScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val roomId = result.data?.getStringExtra("roomId")
            val serverAddress = result.data?.getStringExtra("serverAddress")
            viewModel.handleQRScanResult(roomId, serverAddress)
        }
    }
    
    // Handle QR scan trigger
    LaunchedEffect(uiState.isQRScanActive) {
        if (uiState.isQRScanActive) {
            val intent = Intent(context, QRScanActivity::class.java)
            qrScannerLauncher.launch(intent)
        }
    }
    
    // Handle successful join
    LaunchedEffect(uiState.joinSuccess, uiState.roomCode) {
        if (uiState.joinSuccess) {
            val roomCode = uiState.roomCode
            if (roomCode != null) {
                onJoinSuccess(roomCode)
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Join Room") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header
            Text(
                text = "Join a video call",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Scan a QR code or enter the room code manually",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // QR Scan Button
            Button(
                onClick = { viewModel.startQRScan() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Scan QR Code",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Divider with "OR"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "OR",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Manual Entry Section
            Text(
                text = "Enter Room Code",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = roomCode,
                onValueChange = { 
                    roomCode = it.uppercase().take(6) // Limit to 6 characters and uppercase
                },
                label = { Text("Room Code") },
                placeholder = { Text("A7K9XQ") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters
                ),
                isError = uiState.error != null,
                supportingText = {
                    val error = uiState.error
                    if (error != null) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { viewModel.joinRoom(roomCode) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = roomCode.length == 6 && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Join Room",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Help Text
            Text(
                text = "Room codes are 6 characters long and case-insensitive",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}