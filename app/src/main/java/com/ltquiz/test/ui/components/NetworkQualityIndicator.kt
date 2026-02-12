package com.ltquiz.test.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.ltquiz.test.network.ConnectionType
import com.ltquiz.test.network.NetworkLevel
import com.ltquiz.test.network.NetworkQuality

@Composable
fun NetworkQualityIndicator(
    networkQuality: NetworkQuality,
    modifier: Modifier = Modifier,
    showDetails: Boolean = false
) {
    val animatedColor by animateColorAsState(
        targetValue = networkQuality.level.color,
        animationSpec = tween(300),
        label = "network_color"
    )
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Connection type icon
        Icon(
            imageVector = when (networkQuality.connectionType) {
                ConnectionType.WIFI -> Icons.Default.Wifi
                ConnectionType.CELLULAR -> Icons.Default.SignalCellularAlt
                ConnectionType.ETHERNET -> Icons.Default.Cable
                ConnectionType.UNKNOWN -> if (networkQuality.isConnected) Icons.Default.NetworkCheck else Icons.Default.SignalWifiOff
            },
            contentDescription = "Connection type",
            tint = animatedColor,
            modifier = Modifier.size(16.dp)
        )
        
        if (showDetails) {
            // Quality level text
            Text(
                text = networkQuality.level.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = animatedColor,
                fontWeight = FontWeight.Medium
            )
            
            // Local network badge
            if (networkQuality.isLocalNetwork) {
                Badge(
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = "LAN",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        } else {
            // Quality indicator dots
            QualityDots(
                level = networkQuality.level,
                color = animatedColor
            )
        }
    }
}

@Composable
private fun QualityDots(
    level: NetworkLevel,
    color: Color
) {
    val dotCount = when (level) {
        NetworkLevel.EXCELLENT -> 4
        NetworkLevel.GOOD -> 3
        NetworkLevel.FAIR -> 2
        NetworkLevel.POOR -> 1
        NetworkLevel.DISCONNECTED -> 0
    }
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < dotCount) color else color.copy(alpha = 0.3f)
                    )
            )
        }
    }
}

@Composable
fun NetworkQualityBanner(
    networkQuality: NetworkQuality,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (networkQuality.level == NetworkLevel.POOR || !networkQuality.isConnected) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (networkQuality.isConnected) "Poor Network Quality" else "No Connection",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    
                    Text(
                        text = if (networkQuality.isConnected) {
                            "Call quality may be affected. Consider switching to a better network."
                        } else {
                            "Please check your internet connection."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
fun DetailedNetworkInfo(
    networkQuality: NetworkQuality,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Network Information",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            NetworkInfoRow(
                label = "Quality",
                value = networkQuality.level.displayName,
                valueColor = networkQuality.level.color
            )
            
            NetworkInfoRow(
                label = "Connection",
                value = networkQuality.connectionType.name.lowercase().replaceFirstChar { it.uppercase() }
            )
            
            if (networkQuality.isLocalNetwork) {
                NetworkInfoRow(
                    label = "Network Type",
                    value = "Local Network",
                    valueColor = Color.Green
                )
            }
            
            if (networkQuality.latency > 0) {
                NetworkInfoRow(
                    label = "Latency",
                    value = "${networkQuality.latency}ms"
                )
            }
            
            if (networkQuality.packetLoss > 0) {
                NetworkInfoRow(
                    label = "Packet Loss",
                    value = "${String.format("%.1f", networkQuality.packetLoss)}%",
                    valueColor = if (networkQuality.packetLoss > 2.0f) Color.Red else Color(0xFFFF9800) // Orange color
                )
            }
        }
    }
}

@Composable
private fun NetworkInfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}