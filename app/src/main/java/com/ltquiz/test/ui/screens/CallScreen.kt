package com.ltquiz.test.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.ltquiz.test.audio.SoundEffectsManager
import com.ltquiz.test.models.Participant
import com.ltquiz.test.ui.animations.AnimationUtils
import com.ltquiz.test.ui.components.*
import com.ltquiz.test.ui.viewmodels.CallViewModel
import kotlinx.coroutines.delay
import org.webrtc.SurfaceViewRenderer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallScreen(
    roomCode: String,
    onLeaveCall: () -> Unit,
    viewModel: CallViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var participantNotifications by remember { mutableStateOf<List<String>>(emptyList()) }
    
    LaunchedEffect(roomCode) {
        viewModel.initializeCall(roomCode)
    }
    
    // Handle participant join/leave sound effects and notifications
    LaunchedEffect(uiState.participants.size) {
        // Play sound effects for participant changes
        if (uiState.participants.isNotEmpty()) {
            viewModel.playSound(SoundEffectsManager.SoundEffect.PARTICIPANT_JOIN)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Main content with entrance animation
        AnimatedVisibility(
            visible = true,
            enter = AnimationUtils.scaleInTransition()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top bar with slide-in animation
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(500, delayMillis = 200)
                    )
                ) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "Room: $roomCode",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                AnimatedContent(
                                    targetState = uiState.participants.size,
                                    transitionSpec = {
                                        slideInVertically { it } + fadeIn() togetherWith
                                                slideOutVertically { -it } + fadeOut()
                                    },
                                    label = "participant_count"
                                ) { count ->
                                    Text(
                                        text = "$count participants",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        actions = {
                            // Animated network indicator
                            NetworkQualityAnimation(
                                networkQuality = uiState.networkQuality,
                                isConnected = uiState.isConnected
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Black.copy(alpha = 0.7f),
                            titleContentColor = Color.White,
                            actionIconContentColor = Color.White
                        )
                    )
                }
                
                // Video grid with fade-in animation
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.Black)
                ) {
                    AnimatedVideoGrid(
                        participants = uiState.participants,
                        localVideoRenderer = uiState.localVideoRenderer
                    )
                }
                
                // Bottom controls with slide-up animation
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(500, delayMillis = 400)
                    )
                ) {
                    AnimatedBottomControls(
                        isVideoEnabled = uiState.isVideoEnabled,
                        isAudioEnabled = uiState.isAudioEnabled,
                        isFrontCamera = uiState.isFrontCamera,
                        onToggleVideo = {
                            viewModel.toggleVideo()
                            viewModel.playSound(SoundEffectsManager.SoundEffect.NOTIFICATION)
                        },
                        onToggleAudio = {
                            viewModel.toggleAudio()
                            viewModel.playSound(
                                if (uiState.isAudioEnabled) SoundEffectsManager.SoundEffect.MUTE_ON
                                else SoundEffectsManager.SoundEffect.MUTE_OFF
                            )
                        },
                        onSwitchCamera = {
                            viewModel.switchCamera()
                            viewModel.playSound(SoundEffectsManager.SoundEffect.NOTIFICATION)
                        },
                        onLeaveCall = {
                            viewModel.playSound(SoundEffectsManager.SoundEffect.CALL_END)
                            viewModel.leaveCall()
                            onLeaveCall()
                        }
                    )
                }
            }
        }
        
        // Connection status overlay with improved animation
        AnimatedVisibility(
            visible = uiState.isReconnecting,
            enter = AnimationUtils.scaleInTransition(),
            exit = AnimationUtils.scaleOutTransition()
        ) {
            ReconnectingOverlay(
                onCancel = {
                    viewModel.leaveCall()
                    onLeaveCall()
                }
            )
        }
        
        // Network quality banner
        AnimatedVisibility(
            visible = uiState.showNetworkBanner,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                NetworkQualityBanner(
                    networkQuality = com.ltquiz.test.network.NetworkQuality(
                        level = com.ltquiz.test.network.NetworkLevel.POOR,
                        isConnected = uiState.isConnected,
                        connectionType = com.ltquiz.test.network.ConnectionType.UNKNOWN
                    ),
                    onDismiss = { viewModel.dismissNetworkBanner() }
                )
            }
        }
        
        // Participant notifications overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        ) {
            participantNotifications.forEach { participantName ->
                key(participantName) {
                    ParticipantJoinAnimation(
                        participantName = participantName,
                        onAnimationComplete = {
                            participantNotifications = participantNotifications - participantName
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedVideoGrid(
    participants: List<Participant>,
    localVideoRenderer: SurfaceViewRenderer?
) {
    val totalParticipants = participants.size + if (localVideoRenderer != null) 1 else 0
    
    AnimatedContent(
        targetState = totalParticipants,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
        },
        label = "video_grid_layout"
    ) { participantCount ->
        when (participantCount) {
            0 -> {
                // No participants with pulsing animation
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PulsingDotsIndicator()
                        Text(
                            text = "Waiting for participants...",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
            1 -> {
                // Single participant (full screen) with scale animation
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(animationSpec = tween(400)) + fadeIn()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        localVideoRenderer?.let { renderer ->
                            AndroidView(
                                factory = { context -> renderer },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
            2 -> {
                // Two participants (split screen) with slide animations
                Column(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            localVideoRenderer?.let { renderer ->
                                AndroidView(
                                    factory = { context -> renderer },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                    
                    AnimatedVisibility(
                        visible = participants.isNotEmpty(),
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            if (participants.isNotEmpty()) {
                                AnimatedVideoTile(
                                    participant = participants[0],
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                // Multiple participants (grid layout) with staggered animations
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Local video tile with entrance animation
                    item {
                        AnimatedVisibility(
                            visible = true,
                            enter = scaleIn(
                                animationSpec = tween(300, delayMillis = 0)
                            ) + fadeIn(animationSpec = tween(300))
                        ) {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                localVideoRenderer?.let { renderer ->
                                    AndroidView(
                                        factory = { context -> 
                                            renderer.apply {
                                                // Initialize the renderer with the context if needed
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                
                                // Local user indicator with slide animation
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInHorizontally(
                                        initialOffsetX = { -it },
                                        animationSpec = tween(300, delayMillis = 200)
                                    ) + fadeIn()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(8.dp)
                                            .background(
                                                Color.Black.copy(alpha = 0.6f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "You",
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Remote participant tiles with staggered animations
                    items(participants) { participant ->
                        val index = participants.indexOf(participant)
                        AnimatedVisibility(
                            visible = true,
                            enter = scaleIn(
                                animationSpec = tween(300, delayMillis = (index + 1) * 100)
                            ) + fadeIn(animationSpec = tween(300, delayMillis = (index + 1) * 100))
                        ) {
                            AnimatedVideoTile(
                                participant = participant,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedVideoTile(
    participant: Participant,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(participant.userId) {
        delay(100)
        isVisible = true
    }
    
    Box(modifier = modifier.background(Color.Gray)) {
        // Video renderer would go here
        // For now, showing placeholder with animation
        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn() + fadeIn()
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = participant.displayName.take(2).uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
        
        // Participant info overlay with slide animation
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(300, delayMillis = 200)
            ) + fadeIn()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = participant.displayName,
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    // Animated mute indicator
                    AnimatedVisibility(
                        visible = !participant.isAudioEnabled,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        Icon(
                            imageVector = Icons.Default.MicOff,
                            contentDescription = "Muted",
                            tint = Color.Red,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedBottomControls(
    isVideoEnabled: Boolean,
    isAudioEnabled: Boolean,
    isFrontCamera: Boolean,
    onToggleVideo: () -> Unit,
    onToggleAudio: () -> Unit,
    onSwitchCamera: () -> Unit,
    onLeaveCall: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.8f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Microphone toggle with animation
            MuteToggleAnimation(
                isMuted = !isAudioEnabled,
                icon = if (isAudioEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = if (isAudioEnabled) "Mute" else "Unmute",
                onClick = onToggleAudio
            )
            
            // Camera toggle with animation
            MuteToggleAnimation(
                isMuted = !isVideoEnabled,
                icon = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                contentDescription = if (isVideoEnabled) "Turn off camera" else "Turn on camera",
                onClick = onToggleVideo
            )
            
            // Camera switch with animation
            AnimatedFloatingActionButton(
                onClick = onSwitchCamera,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(
                    imageVector = Icons.Default.FlipCameraAndroid,
                    contentDescription = "Switch camera"
                )
            }
            
            // Leave call with animation
            AnimatedFloatingActionButton(
                onClick = onLeaveCall,
                containerColor = Color.Red,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Leave call"
                )
            }
        }
    }
}

@Composable
private fun VideoGrid(
    participants: List<Participant>,
    localVideoRenderer: SurfaceViewRenderer?
) {
    val totalParticipants = participants.size + if (localVideoRenderer != null) 1 else 0
    
    when (totalParticipants) {
        0 -> {
            // No participants
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Waiting for participants...",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        1 -> {
            // Single participant (full screen)
            Box(modifier = Modifier.fillMaxSize()) {
                localVideoRenderer?.let { renderer ->
                    AndroidView(
                        factory = { context -> renderer },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        2 -> {
            // Two participants (split screen)
            Column(modifier = Modifier.fillMaxSize()) {
                // Local video (top half)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    localVideoRenderer?.let { renderer ->
                        AndroidView(
                            factory = { context -> renderer },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                
                // Remote video (bottom half)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (participants.isNotEmpty()) {
                        VideoTile(
                            participant = participants[0],
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
        else -> {
            // Multiple participants (grid layout)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Local video tile
                item {
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        localVideoRenderer?.let { renderer ->
                            AndroidView(
                                factory = { context -> renderer },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        
                        // Local user indicator
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.6f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "You",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                // Remote participant tiles
                items(participants) { participant ->
                    VideoTile(
                        participant = participant,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoTile(
    participant: Participant,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(Color.Gray)) {
        // Video renderer would go here
        // For now, showing placeholder
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = participant.displayName.take(2).uppercase(),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
        // Participant info overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .background(
                    Color.Black.copy(alpha = 0.6f),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = participant.displayName,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
                
                if (!participant.isAudioEnabled) {
                    Icon(
                        imageVector = Icons.Default.MicOff,
                        contentDescription = "Muted",
                        tint = Color.Red,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomControls(
    isVideoEnabled: Boolean,
    isAudioEnabled: Boolean,
    isFrontCamera: Boolean,
    onToggleVideo: () -> Unit,
    onToggleAudio: () -> Unit,
    onSwitchCamera: () -> Unit,
    onLeaveCall: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.8f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Microphone toggle
            FloatingActionButton(
                onClick = onToggleAudio,
                containerColor = if (isAudioEnabled) {
                    MaterialTheme.colorScheme.surface
                } else {
                    Color.Red
                },
                contentColor = if (isAudioEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    Color.White
                }
            ) {
                Icon(
                    imageVector = if (isAudioEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = if (isAudioEnabled) "Mute" else "Unmute"
                )
            }
            
            // Camera toggle
            FloatingActionButton(
                onClick = onToggleVideo,
                containerColor = if (isVideoEnabled) {
                    MaterialTheme.colorScheme.surface
                } else {
                    Color.Red
                },
                contentColor = if (isVideoEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    Color.White
                }
            ) {
                Icon(
                    imageVector = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = if (isVideoEnabled) "Turn off camera" else "Turn on camera"
                )
            }
            
            // Camera switch
            FloatingActionButton(
                onClick = onSwitchCamera,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(
                    imageVector = Icons.Default.FlipCameraAndroid,
                    contentDescription = "Switch camera"
                )
            }
            
            // Leave call
            FloatingActionButton(
                onClick = onLeaveCall,
                containerColor = Color.Red,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Leave call"
                )
            }
        }
    }
}

@Composable
private fun NetworkIndicator(
    isConnected: Boolean,
    networkQuality: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
            contentDescription = "Network status",
            tint = if (isConnected) Color.Green else Color.Red,
            modifier = Modifier.size(16.dp)
        )
        
        Text(
            text = networkQuality,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
        )
    }
}

@Composable
private fun ReconnectingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Reconnecting...",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}