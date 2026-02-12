package com.ltquiz.test.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ltquiz.test.ui.animations.AnimationUtils
import com.ltquiz.test.ui.components.AnimatedButton
import com.ltquiz.test.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var deviceName by remember { mutableStateOf(uiState.deviceName) }
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(uiState.deviceName) {
        deviceName = uiState.deviceName
    }
    
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        AnimatedVisibility(
            visible = isVisible,
            enter = AnimationUtils.fadeInTransition()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Device Settings Section with staggered animation
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(400, delayMillis = 100)
                    ) + fadeIn(animationSpec = tween(400, delayMillis = 100))
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Device Settings",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedTextField(
                                value = deviceName,
                                onValueChange = { deviceName = it },
                                label = { Text("Device Name") },
                                placeholder = { Text("Enter your device name") },
                                modifier = Modifier.fillMaxWidth(),
                                supportingText = {
                                    Text("This name will be shown to other participants")
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            AnimatedButton(
                                onClick = { viewModel.updateDeviceName(deviceName) },
                                modifier = Modifier.align(Alignment.End),
                                enabled = deviceName != uiState.deviceName && deviceName.isNotBlank()
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Default Call Settings Section with staggered animation
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(400, delayMillis = 200)
                    ) + fadeIn(animationSpec = tween(400, delayMillis = 200))
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Default Call Settings",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Video On by Default
                            AnimatedSettingRow(
                                title = "Video On by Default",
                                description = "Start calls with video enabled",
                                checked = uiState.defaultVideoEnabled,
                                onCheckedChange = { viewModel.updateDefaultVideoEnabled(it) }
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Audio On by Default
                            AnimatedSettingRow(
                                title = "Audio On by Default",
                                description = "Start calls with microphone enabled",
                                checked = uiState.defaultAudioEnabled,
                                onCheckedChange = { viewModel.updateDefaultAudioEnabled(it) }
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Sound Effects
                            AnimatedSettingRow(
                                title = "Sound Effects",
                                description = "Play sounds for join, leave, and mute actions",
                                checked = uiState.enableSoundEffects,
                                onCheckedChange = { viewModel.updateEnableSoundEffects(it) }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // About Section with staggered animation
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(400, delayMillis = 300)
                    ) + fadeIn(animationSpec = tween(400, delayMillis = 300))
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "About",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "App Version",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "1.0.0",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Device ID",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = uiState.deviceId.take(8) + "...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "XSEND Meet - Instant video calls, zero confusion",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun AnimatedSettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Animated switch with color transition
        val switchColors = SwitchDefaults.colors()
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = switchColors
        )
    }
}