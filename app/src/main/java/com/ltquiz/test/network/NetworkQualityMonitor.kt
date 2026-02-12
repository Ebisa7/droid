package com.ltquiz.test.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.PeerConnection
import javax.inject.Inject
import javax.inject.Singleton

data class NetworkQuality(
    val level: NetworkLevel,
    val isConnected: Boolean,
    val connectionType: ConnectionType,
    val bandwidth: Long = 0L, // in kbps
    val latency: Long = 0L, // in ms
    val packetLoss: Float = 0f, // percentage
    val isLocalNetwork: Boolean = false
)

enum class NetworkLevel(val displayName: String, val color: androidx.compose.ui.graphics.Color) {
    EXCELLENT("Excellent", androidx.compose.ui.graphics.Color.Green),
    GOOD("Good", androidx.compose.ui.graphics.Color.Green),
    FAIR("Fair", androidx.compose.ui.graphics.Color(0xFFFF9800)), // Orange
    POOR("Poor", androidx.compose.ui.graphics.Color.Red),
    DISCONNECTED("Disconnected", androidx.compose.ui.graphics.Color.Gray)
}

enum class ConnectionType {
    WIFI, CELLULAR, ETHERNET, UNKNOWN
}

@Singleton
class NetworkQualityMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _networkQuality = MutableStateFlow(
        NetworkQuality(
            level = NetworkLevel.DISCONNECTED,
            isConnected = false,
            connectionType = ConnectionType.UNKNOWN
        )
    )
    val networkQuality: StateFlow<NetworkQuality> = _networkQuality.asStateFlow()
    
    private val _peerConnectionStats = MutableStateFlow<Map<String, PeerConnectionStats>>(emptyMap())
    val peerConnectionStats: StateFlow<Map<String, PeerConnectionStats>> = _peerConnectionStats.asStateFlow()
    
    private var isMonitoring = false
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateNetworkQuality()
        }
        
        override fun onLost(network: Network) {
            updateNetworkQuality()
        }
        
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            updateNetworkQuality()
        }
    }
    
    fun startMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        updateNetworkQuality()
    }
    
    fun stopMonitoring() {
        if (!isMonitoring) return
        
        isMonitoring = false
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
    
    private fun updateNetworkQuality() {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        
        val isConnected = networkCapabilities != null
        val connectionType = getConnectionType(networkCapabilities)
        val isLocalNetwork = isLocalNetworkConnection(networkCapabilities)
        
        val quality = if (isConnected) {
            calculateNetworkLevel(networkCapabilities, connectionType)
        } else {
            NetworkLevel.DISCONNECTED
        }
        
        _networkQuality.value = NetworkQuality(
            level = quality,
            isConnected = isConnected,
            connectionType = connectionType,
            isLocalNetwork = isLocalNetwork
        )
    }
    
    private fun getConnectionType(capabilities: NetworkCapabilities?): ConnectionType {
        return when {
            capabilities == null -> ConnectionType.UNKNOWN
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            else -> ConnectionType.UNKNOWN
        }
    }
    
    private fun isLocalNetworkConnection(capabilities: NetworkCapabilities?): Boolean {
        return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
               capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
    }
    
    private fun calculateNetworkLevel(capabilities: NetworkCapabilities?, connectionType: ConnectionType): NetworkLevel {
        if (capabilities == null) return NetworkLevel.DISCONNECTED
        
        // Base quality on connection type and capabilities
        return when (connectionType) {
            ConnectionType.WIFI -> {
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
                    NetworkLevel.EXCELLENT
                } else {
                    NetworkLevel.GOOD
                }
            }
            ConnectionType.ETHERNET -> NetworkLevel.EXCELLENT
            ConnectionType.CELLULAR -> {
                // Could be enhanced with signal strength if available
                NetworkLevel.FAIR
            }
            ConnectionType.UNKNOWN -> NetworkLevel.POOR
        }
    }
    
    fun updatePeerConnectionStats(participantId: String, stats: PeerConnectionStats) {
        val currentStats = _peerConnectionStats.value.toMutableMap()
        currentStats[participantId] = stats
        _peerConnectionStats.value = currentStats
        
        // Update overall network quality based on peer connection performance
        updateQualityBasedOnStats()
    }
    
    private fun updateQualityBasedOnStats() {
        val stats = _peerConnectionStats.value.values
        if (stats.isEmpty()) return
        
        val avgPacketLoss = stats.map { it.packetLoss }.average().toFloat()
        val avgLatency = stats.map { it.roundTripTime }.average().toLong()
        
        val currentQuality = _networkQuality.value
        val adjustedLevel = when {
            avgPacketLoss > 5.0f || avgLatency > 500 -> NetworkLevel.POOR
            avgPacketLoss > 2.0f || avgLatency > 200 -> NetworkLevel.FAIR
            avgPacketLoss > 1.0f || avgLatency > 100 -> NetworkLevel.GOOD
            else -> NetworkLevel.EXCELLENT
        }
        
        // Only downgrade quality, never upgrade based on stats alone
        if (adjustedLevel.ordinal > currentQuality.level.ordinal) {
            _networkQuality.value = currentQuality.copy(
                level = adjustedLevel,
                latency = avgLatency,
                packetLoss = avgPacketLoss
            )
        }
    }
    
    fun shouldReduceVideoQuality(): Boolean {
        val quality = _networkQuality.value
        return quality.level == NetworkLevel.POOR || quality.level == NetworkLevel.FAIR
    }
    
    fun getRecommendedVideoQuality(): VideoQualityLevel {
        return when (_networkQuality.value.level) {
            NetworkLevel.EXCELLENT, NetworkLevel.GOOD -> VideoQualityLevel.HD_720P
            NetworkLevel.FAIR -> VideoQualityLevel.SD_480P
            NetworkLevel.POOR -> VideoQualityLevel.SD_360P
            NetworkLevel.DISCONNECTED -> VideoQualityLevel.SD_360P
        }
    }
}

data class PeerConnectionStats(
    val participantId: String,
    val bytesReceived: Long,
    val bytesSent: Long,
    val packetsReceived: Long,
    val packetsSent: Long,
    val packetsLost: Long,
    val roundTripTime: Long, // in ms
    val jitter: Long, // in ms
    val timestamp: Long = System.currentTimeMillis()
) {
    val packetLoss: Float
        get() = if (packetsReceived > 0) {
            (packetsLost.toFloat() / packetsReceived.toFloat()) * 100f
        } else 0f
}

enum class VideoQualityLevel(
    val displayName: String,
    val width: Int,
    val height: Int,
    val fps: Int,
    val bitrate: Int // kbps
) {
    SD_360P("360p", 640, 360, 30, 500),
    SD_480P("480p", 854, 480, 30, 800),
    HD_720P("720p", 1280, 720, 30, 1200),
    HD_1080P("1080p", 1920, 1080, 30, 2000)
}