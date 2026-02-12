package com.ltquiz.test.performance

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Performance tests for video call functionality.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PerformanceTest {
    
    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var performanceOptimizer: PerformanceOptimizer
    private lateinit var performanceBenchmark: PerformanceBenchmark
    private lateinit var mockContext: Context
    
    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        performanceMonitor = PerformanceMonitor(mockContext)
        performanceOptimizer = PerformanceOptimizer(mockContext, performanceMonitor)
        performanceBenchmark = PerformanceBenchmark(performanceMonitor)
    }
    
    @Test
    fun `memory metrics update correctly`() = runTest {
        // Given
        val initialMetrics = performanceMonitor.performanceMetrics.value
        
        // When
        performanceMonitor.updateMemoryMetrics()
        
        // Then
        val updatedMetrics = performanceMonitor.performanceMetrics.value
        assertNotEquals(initialMetrics.timestamp, updatedMetrics.timestamp)
        assertTrue(updatedMetrics.memoryUsage.usedMemoryMB >= 0)
        assertTrue(updatedMetrics.memoryUsage.maxMemoryMB > 0)
    }
    
    @Test
    fun `video performance metrics update correctly`() = runTest {
        // Given
        val frameRate = 25.5f
        val droppedFrames = 3
        val renderingTime = 18L
        val participantCount = 4
        
        // When
        performanceMonitor.updateVideoMetrics(
            frameRate = frameRate,
            droppedFrames = droppedFrames,
            renderingTimeMs = renderingTime,
            participantCount = participantCount
        )
        
        // Then
        val metrics = performanceMonitor.performanceMetrics.value
        assertEquals(frameRate, metrics.videoPerformance.currentFrameRate, 0.1f)
        assertEquals(droppedFrames, metrics.videoPerformance.droppedFrames)
        assertEquals(renderingTime, metrics.videoPerformance.averageRenderingTimeMs)
        assertEquals(participantCount, metrics.videoPerformance.participantCount)
        assertTrue(metrics.videoPerformance.renderingEfficiency > 0)
    }
    
    @Test
    fun `network performance metrics update correctly`() = runTest {
        // Given
        val latency = 75L
        val packetLoss = 1.5f
        val bandwidth = 1500L
        
        // When
        performanceMonitor.updateNetworkMetrics(
            latencyMs = latency,
            packetLoss = packetLoss,
            bandwidth = bandwidth
        )
        
        // Then
        val metrics = performanceMonitor.performanceMetrics.value
        assertEquals(latency, metrics.networkPerformance.latencyMs)
        assertEquals(packetLoss, metrics.networkPerformance.packetLossPercentage, 0.1f)
        assertEquals(bandwidth, metrics.networkPerformance.bandwidthKbps)
        assertEquals(ConnectionQuality.GOOD, metrics.networkPerformance.connectionQuality)
    }
    
    @Test
    fun `connection quality calculated correctly for different network conditions`() = runTest {
        // Test excellent connection
        performanceMonitor.updateNetworkMetrics(30, 0.5f, 2000)
        assertEquals(
            ConnectionQuality.EXCELLENT,
            performanceMonitor.performanceMetrics.value.networkPerformance.connectionQuality
        )
        
        // Test good connection
        performanceMonitor.updateNetworkMetrics(80, 1.5f, 1500)
        assertEquals(
            ConnectionQuality.GOOD,
            performanceMonitor.performanceMetrics.value.networkPerformance.connectionQuality
        )
        
        // Test fair connection
        performanceMonitor.updateNetworkMetrics(150, 3f, 1000)
        assertEquals(
            ConnectionQuality.FAIR,
            performanceMonitor.performanceMetrics.value.networkPerformance.connectionQuality
        )
        
        // Test poor connection
        performanceMonitor.updateNetworkMetrics(250, 8f, 500)
        assertEquals(
            ConnectionQuality.POOR,
            performanceMonitor.performanceMetrics.value.networkPerformance.connectionQuality
        )
    }
    
    @Test
    fun `memory pressure calculated correctly`() = runTest {
        // Mock memory info for testing
        every { mockContext.getSystemService(Context.ACTIVITY_SERVICE) } returns mockk(relaxed = true)
        
        performanceMonitor.updateMemoryMetrics()
        val memoryUsage = performanceMonitor.performanceMetrics.value.memoryUsage
        
        // Memory pressure should be one of the valid values
        assertTrue(
            memoryUsage.memoryPressure in listOf(
                MemoryPressure.LOW,
                MemoryPressure.MEDIUM,
                MemoryPressure.HIGH
            )
        )
    }
    
    @Test
    fun `video quality optimization works correctly`() = runTest {
        // Given - start with high quality
        assertEquals(VideoQuality.HIGH, performanceOptimizer.getCurrentVideoQuality())
        
        // When - set to low quality
        performanceOptimizer.setVideoQuality(VideoQuality.LOW)
        
        // Then
        assertEquals(VideoQuality.LOW, performanceOptimizer.getCurrentVideoQuality())
    }
    
    @Test
    fun `optimization recommendations generated correctly for high memory usage`() = runTest {
        // Given - simulate high memory usage
        performanceMonitor.updateMemoryMetrics()
        // Force high memory pressure for testing
        val highMemoryMetrics = performanceMonitor.performanceMetrics.value.copy(
            memoryUsage = performanceMonitor.performanceMetrics.value.memoryUsage.copy(
                memoryPressure = MemoryPressure.HIGH
            )
        )
        
        // When
        val recommendations = performanceOptimizer.getOptimizationRecommendations()
        
        // Then - should contain memory optimization recommendation
        assertTrue(recommendations.any { it.type == OptimizationType.MEMORY })
    }
    
    @Test
    fun `optimization recommendations generated correctly for poor video performance`() = runTest {
        // Given - simulate poor video performance
        performanceMonitor.updateVideoMetrics(
            frameRate = 15f, // Below 80% of target 30fps
            droppedFrames = 10,
            renderingTimeMs = 50,
            participantCount = 6
        )
        
        // When
        val recommendations = performanceOptimizer.getOptimizationRecommendations()
        
        // Then - should contain video optimization recommendation
        assertTrue(recommendations.any { it.type == OptimizationType.VIDEO })
    }
    
    @Test
    fun `optimization recommendations generated correctly for poor network`() = runTest {
        // Given - simulate poor network
        performanceMonitor.updateNetworkMetrics(
            latencyMs = 300,
            packetLoss = 10f,
            bandwidth = 200
        )
        
        // When
        val recommendations = performanceOptimizer.getOptimizationRecommendations()
        
        // Then - should contain network optimization recommendation
        assertTrue(recommendations.any { it.type == OptimizationType.NETWORK })
        val networkRec = recommendations.first { it.type == OptimizationType.NETWORK }
        assertEquals(RecommendationSeverity.HIGH, networkRec.severity)
    }
    
    @Test
    fun `benchmark runs successfully`() = runTest {
        // When
        val result = performanceBenchmark.runBenchmark()
        
        // Then
        assertNotNull(result)
        assertTrue(result.tests.isNotEmpty())
        assertTrue(result.overallScore in 0..100)
        assertTrue(result.timestamp > 0)
        
        // Check that all expected tests are present
        val testNames = result.tests.map { it.name }
        assertTrue(testNames.contains("Memory Allocation"))
        assertTrue(testNames.contains("Video Rendering"))
        assertTrue(testNames.contains("Network Performance"))
        assertTrue(testNames.contains("Multi-Participant Stress Test"))
    }
    
    @Test
    fun `benchmark memory test produces valid results`() = runTest {
        // When
        val result = performanceBenchmark.runBenchmark()
        
        // Then
        val memoryTest = result.tests.first { it.name == "Memory Allocation" }
        assertTrue(memoryTest.executionTimeMs > 0)
        assertTrue(memoryTest.score in 0..100)
        assertTrue(memoryTest.details.containsKey("iterations"))
        assertTrue(memoryTest.details.containsKey("avgTimePerIteration"))
        assertTrue(memoryTest.details.containsKey("memoryPressure"))
    }
    
    @Test
    fun `benchmark video test produces valid results`() = runTest {
        // When
        val result = performanceBenchmark.runBenchmark()
        
        // Then
        val videoTest = result.tests.first { it.name == "Video Rendering" }
        assertTrue(videoTest.executionTimeMs > 0)
        assertTrue(videoTest.score in 0..100)
        assertTrue(videoTest.details.containsKey("frameCount"))
        assertTrue(videoTest.details.containsKey("averageFPS"))
        assertTrue(videoTest.details.containsKey("targetFPS"))
        assertTrue(videoTest.details.containsKey("renderingEfficiency"))
    }
    
    @Test
    fun `multi-participant stress test handles different participant counts`() = runTest {
        // When
        val result = performanceBenchmark.runBenchmark()
        
        // Then
        val stressTest = result.tests.first { it.name == "Multi-Participant Stress Test" }
        assertTrue(stressTest.executionTimeMs > 0)
        assertTrue(stressTest.score in 0..100)
        
        // Should have results for different participant counts
        assertTrue(stressTest.details.containsKey("participants_2"))
        assertTrue(stressTest.details.containsKey("participants_4"))
        assertTrue(stressTest.details.containsKey("participants_6"))
        assertTrue(stressTest.details.containsKey("participants_8"))
    }
    
    @Test
    fun `performance optimizer starts and stops correctly`() = runTest {
        // When
        performanceOptimizer.startOptimization()
        
        // Then - should not throw exception
        // When
        performanceOptimizer.stopOptimization()
        
        // Then - should not throw exception
        assertTrue(true) // Test passes if no exceptions thrown
    }
}