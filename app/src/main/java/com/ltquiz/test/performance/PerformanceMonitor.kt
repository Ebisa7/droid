package com.ltquiz.test.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Process
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors application performance metrics including memory usage, CPU usage,
 * and video rendering performance.
 */
@Singleton
class PerformanceMonitor @Inject constructor(
    private val context: Context
) {
    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    /**
     * Updates memory usage metrics.
     */
    fun updateMemoryMetrics() {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        
        val nativeHeapSize = Debug.getNativeHeapSize()
        val nativeHeapAllocated = Debug.getNativeHeapAllocatedSize()
        
        val currentMetrics = _performanceMetrics.value
        _performanceMetrics.value = currentMetrics.copy(
            memoryUsage = MemoryUsage(
                usedMemoryMB = usedMemory / (1024 * 1024),
                maxMemoryMB = maxMemory / (1024 * 1024),
                availableMemoryMB = memoryInfo.availMem / (1024 * 1024),
                totalMemoryMB = memoryInfo.totalMem / (1024 * 1024),
                nativeHeapSizeMB = nativeHeapSize / (1024 * 1024),
                nativeHeapAllocatedMB = nativeHeapAllocated / (1024 * 1024),
                memoryPressure = when {
                    memoryInfo.lowMemory -> MemoryPressure.HIGH
                    (usedMemory.toFloat() / maxMemory) > 0.8f -> MemoryPressure.MEDIUM
                    else -> MemoryPressure.LOW
                }
            )
        )
    }
    
    /**
     * Updates video rendering performance metrics.
     */
    fun updateVideoMetrics(
        frameRate: Float,
        droppedFrames: Int,
        renderingTimeMs: Long,
        participantCount: Int
    ) {
        val currentMetrics = _performanceMetrics.value
        _performanceMetrics.value = currentMetrics.copy(
            videoPerformance = VideoPerformance(
                currentFrameRate = frameRate,
                targetFrameRate = 30f,
                droppedFrames = droppedFrames,
                averageRenderingTimeMs = renderingTimeMs,
                participantCount = participantCount,
                renderingEfficiency = if (frameRate > 0) (frameRate / 30f) * 100f else 0f
            )
        )
    }
    
    /**
     * Updates network performance metrics.
     */
    fun updateNetworkMetrics(
        latencyMs: Long,
        packetLoss: Float,
        bandwidth: Long
    ) {
        val currentMetrics = _performanceMetrics.value
        _performanceMetrics.value = currentMetrics.copy(
            networkPerformance = NetworkPerformance(
                latencyMs = latencyMs,
                packetLossPercentage = packetLoss,
                bandwidthKbps = bandwidth,
                connectionQuality = when {
                    latencyMs > 200 || packetLoss > 5f -> ConnectionQuality.POOR
                    latencyMs > 100 || packetLoss > 2f -> ConnectionQuality.FAIR
                    latencyMs > 50 || packetLoss > 1f -> ConnectionQuality.GOOD
                    else -> ConnectionQuality.EXCELLENT
                }
            )
        )
    }
    
    /**
     * Gets current CPU usage percentage.
     */
    fun getCpuUsage(): Float {
        return try {
            val pid = Process.myPid()
            val stat1 = readProcStat(pid)
            Thread.sleep(100) // Small delay for measurement
            val stat2 = readProcStat(pid)
            
            val totalTime1 = stat1.utime + stat1.stime
            val totalTime2 = stat2.utime + stat2.stime
            
            val timeDiff = totalTime2 - totalTime1
            val clockTicks = 100 // Typical value for Android
            
            (timeDiff * 100f) / clockTicks
        } catch (e: Exception) {
            0f
        }
    }
    
    private fun readProcStat(pid: Int): ProcStat {
        return try {
            val statFile = "/proc/$pid/stat"
            val content = java.io.File(statFile).readText()
            val parts = content.split(" ")
            
            ProcStat(
                utime = parts[13].toLong(),
                stime = parts[14].toLong()
            )
        } catch (e: Exception) {
            ProcStat(0, 0)
        }
    }
    
    private data class ProcStat(
        val utime: Long,
        val stime: Long
    )
}

/**
 * Complete performance metrics for the application.
 */
data class PerformanceMetrics(
    val memoryUsage: MemoryUsage = MemoryUsage(),
    val videoPerformance: VideoPerformance = VideoPerformance(),
    val networkPerformance: NetworkPerformance = NetworkPerformance(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Memory usage metrics.
 */
data class MemoryUsage(
    val usedMemoryMB: Long = 0,
    val maxMemoryMB: Long = 0,
    val availableMemoryMB: Long = 0,
    val totalMemoryMB: Long = 0,
    val nativeHeapSizeMB: Long = 0,
    val nativeHeapAllocatedMB: Long = 0,
    val memoryPressure: MemoryPressure = MemoryPressure.LOW
)

/**
 * Video rendering performance metrics.
 */
data class VideoPerformance(
    val currentFrameRate: Float = 0f,
    val targetFrameRate: Float = 30f,
    val droppedFrames: Int = 0,
    val averageRenderingTimeMs: Long = 0,
    val participantCount: Int = 0,
    val renderingEfficiency: Float = 0f
)

/**
 * Network performance metrics.
 */
data class NetworkPerformance(
    val latencyMs: Long = 0,
    val packetLossPercentage: Float = 0f,
    val bandwidthKbps: Long = 0,
    val connectionQuality: ConnectionQuality = ConnectionQuality.UNKNOWN
)

enum class MemoryPressure {
    LOW, MEDIUM, HIGH
}

enum class ConnectionQuality {
    EXCELLENT, GOOD, FAIR, POOR, UNKNOWN
}