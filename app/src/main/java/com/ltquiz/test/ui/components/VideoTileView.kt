package com.ltquiz.test.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ltquiz.test.models.Participant
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun VideoTileView(
    participant: Participant,
    videoTrack: VideoTrack? = null,
    isLocalVideo: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var surfaceViewRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
    ) {
        // Video surface or placeholder
        if (participant.isVideoEnabled && videoTrack != null) {
            AndroidView(
                factory = { context ->
                    SurfaceViewRenderer(context).apply {
                        init(null, null)
                        setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                        setMirror(isLocalVideo) // Mirror local video
                        surfaceViewRenderer = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { renderer ->
                    videoTrack.addSink(renderer)
                }
            )
        } else {
            // Placeholder when video is disabled
            VideoPlaceholder(
                participant = participant,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Participant info overlay
        ParticipantInfoOverlay(
            participant = participant,
            modifier = Modifier.align(Alignment.BottomStart)
        )
        
        // Video disabled indicator
        if (!participant.isVideoEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            ) {
                Text(
                    text = "Video Off",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
    
    // Cleanup when composable is disposed
    DisposableEffect(videoTrack) {
        onDispose {
            surfaceViewRenderer?.let { renderer ->
                videoTrack?.removeSink(renderer)
                renderer.release()
            }
        }
    }
}

@Composable
private fun VideoPlaceholder(
    participant: Participant,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Gray),
        contentAlignment = Alignment.Center
    ) {
        // Show participant initials
        Text(
            text = participant.displayName
                .split(" ")
                .take(2)
                .map { it.firstOrNull()?.uppercase() ?: "" }
                .joinToString(""),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ParticipantInfoOverlay(
    participant: Participant,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                Color.Black.copy(alpha = 0.6f),
                RoundedCornerShape(topEnd = 8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = participant.displayName,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            
            if (!participant.isAudioEnabled) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.MicOff,
                    contentDescription = "Muted",
                    tint = Color.Red,
                    modifier = Modifier.size(12.dp)
                )
            }
            
            if (participant.isHost) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Host",
                    tint = Color.Yellow,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * Calculates the optimal grid columns based on participant count
 */
fun calculateGridColumns(participantCount: Int): Int {
    return when (participantCount) {
        1 -> 1
        2 -> 1 // Stack vertically for 2 participants
        3, 4 -> 2
        5, 6 -> 2
        7, 8, 9 -> 3
        else -> 3 // Max 3 columns for larger groups
    }
}

/**
 * Calculates the aspect ratio for video tiles based on participant count
 */
fun calculateTileAspectRatio(participantCount: Int): Float {
    return when (participantCount) {
        1 -> 16f / 9f // Full screen
        2 -> 16f / 9f // Split screen
        else -> 4f / 3f // Grid layout
    }
}