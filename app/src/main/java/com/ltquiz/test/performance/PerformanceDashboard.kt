package com.ltquiz.test.performance

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performance dashboard for monitoring and displaying performance metrics.
 */
@Singleton
class PerformanceDashboard @Inject constructor(
    private val performanceMonitor: PerformanceMonitor,
    private val performanceOptimizer: PerformanceOptimizer
) {
    
    /**
     * Gets combined performance data for dashboard display.
     */
    fun getPerformanceDashboardData(): Flow<PerformanceDashboardData> {
        return performanceMonitor.performanceMetrics.map { metrics ->
            PerformanceDashboardData(
                metrics = metrics,
                currentVideoQuality = performanceOptimizer.getCurrentVideoQuality(),
                recommendations = performanceOptimizer.getOptimizationRecommendations(),
                overallHealthScore = calculateOverallHealthScore(metrics)
            )
        }
    }
    
    /**
     * Calculates overall performance health score (0-100).
     */
    private fun calculateOverallHealthScore(metrics: PerformanceMetrics): Int {
        val memoryScore = when (metrics.memoryUsage.memoryPressure) {
            MemoryPressure.LOW -> 100
            MemoryPressure.MEDIUM -> 70
            MemoryPressure.HIGH -> 30
        }
        
        val videoScore = if (metrics.videoPerformance.currentFrameRate > 0) {
            (metrics.videoPerformance.renderingEfficiency).toInt()
        } else {
            100 // No video active
        }
        
        val networkScore = when (metrics.networkPerformance.connectionQuality) {
            ConnectionQuality.EXCELLENT -> 100
            ConnectionQuality.GOOD -> 80
            ConnectionQuality.FAIR -> 60
            ConnectionQuality.POOR -> 30
            ConnectionQuality.UNKNOWN -> 70
        }
        
        return (memoryScore + videoScore + networkScore) / 3
    }
    
    /**
     * Gets performance summary for quick overview.
     */
    fun getPerformanceSummary(): PerformanceSummary {
        val metrics = performanceMonitor.performanceMetrics.value
        val recommendations = performanceOptimizer.getOptimizationRecommendations()
        
        return PerformanceSummary(
            overallHealth = calculateOverallHealthScore(metrics),
            memoryStatus = getMemoryStatus(metrics.memoryUsage),
            videoStatus = getVideoStatus(metrics.videoPerformance),
            networkStatus = getNetworkStatus(metrics.networkPerformance),
            criticalIssues = recommendations.filter { 
                it.severity == RecommendationSeverity.HIGH || 
                it.severity == RecommendationSeverity.CRITICAL 
            }.size,
            activeOptimizations = getActiveOptimizations()
        )
    }
    
    private fun getMemoryStatus(memoryUsage: MemoryUsage): String {
        return when (memoryUsage.memoryPressure) {
            MemoryPressure.LOW -> "Good (${memoryUsage.usedMemoryMB}MB used)"
            MemoryPressure.MEDIUM -> "Moderate (${memoryUsage.usedMemoryMB}MB used)"
            MemoryPressure.HIGH -> "High (${memoryUsage.usedMemoryMB}MB used)"
        }
    }
    
    private fun getVideoStatus(videoPerformance: VideoPerformance): String {
        return if (videoPerformance.currentFrameRate > 0) {
            "Active (${String.format("%.1f", videoPerformance.currentFrameRate)}fps, ${videoPerformance.droppedFrames} dropped)"
        } else {
            "Inactive"
        }
    }
    
    private fun getNetworkStatus(networkPerformance: NetworkPerformance): String {
        return "${networkPerformance.connectionQuality.name.lowercase().replaceFirstChar { it.uppercase() }} " +
                "(${networkPerformance.latencyMs}ms latency)"
    }
    
    private fun getActiveOptimizations(): List<String> {
        val optimizations = mutableListOf<String>()
        
        when (performanceOptimizer.getCurrentVideoQuality()) {
            VideoQuality.HIGH -> optimizations.add("High Quality Video")
            VideoQuality.MEDIUM -> optimizations.add("Medium Quality Video")
            VideoQuality.LOW -> optimizations.add("Low Quality Video")
            VideoQuality.BATTERY_SAVER -> optimizations.add("Battery Saver Mode")
        }
        
        return optimizations
    }
}

/**
 * Complete performance dashboard data.
 */
data class PerformanceDashboardData(
    val metrics: PerformanceMetrics,
    val currentVideoQuality: VideoQuality,
    val recommendations: List<OptimizationRecommendation>,
    val overallHealthScore: Int
)

/**
 * Performance summary for quick overview.
 */
data class PerformanceSummary(
    val overallHealth: Int,
    val memoryStatus: String,
    val videoStatus: String,
    val networkStatus: String,
    val criticalIssues: Int,
    val activeOptimizations: List<String>
)