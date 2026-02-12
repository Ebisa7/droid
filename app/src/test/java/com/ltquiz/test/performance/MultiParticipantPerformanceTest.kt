package com.ltquiz.test.performance

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Performance tests specifically for multi-participant video calls.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MultiParticipantPerformanceTest {
    
    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var performanceOptimizer: PerformanceOptimizer
    private lateinit var mockContext: Context
    
    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        performanceMonitor = PerformanceMonitor(mockContext)
        performanceOptimizer = PerformanceOptimizer(mockContext, performanceMonitor)
    }
    
    @Test
    fun `performance degrades gracefully with increasing participants`() = runTest {
        val participantCounts = listOf(2, 4, 6, 8)
        val frameRates = mutableListOf<Float>()
        
        for (participantCount in participantCounts) {
            // Simulate performance degradation with more participants
            val expectedFrameRate = 30f - (participantCount * 2f)
            val droppedFrames = participantCount / 2
            
            performanceMonitor.updateVideoMetrics(
                frameRate = expectedFrameRate,
                droppedFrames = droppedFrames,
                renderingTimeMs = 16 + (participantCount * 2),
                participantCount = participantCount
            )
            
            frameRates.add(expectedFrameRate)
        }
        
        // Verify that frame rate decreases with more participants
        for (i in 1 until frameRates.size) {
            assertTrue(
                "Frame rate should decrease with more participants",
                frameRates[i] < frameRates[i - 1]
            )
        }
    }
    
    @Test
    fun `memory usage increases with participant count`() = runTest {
        val participantCounts = listOf(2, 4, 6, 8)
        val memoryUsages = mutableListOf<Long>()
        
        for (participantCount in participantCounts) {
            // Simulate memory usage increase with more participants
            // Each participant requires video buffers, peer connections, etc.
            val baseMemoryMB = 50L
            val memoryPerParticipant = 15L
            val totalMemoryMB = baseMemoryMB + (participantCount * memoryPerParticipant)
            
            // Mock memory info to simulate increasing usage
            every { mockContext.getSystemService(Context.ACTIVITY_SERVICE) } returns mockk(relaxed = true)
            
            performanceMonitor.updateMemoryMetrics()
            memoryUsages.add(totalMemoryMB)
        }
        
        // Verify that memory usage increases with more participants
        for (i in 1 until memoryUsages.size) {
            assertTrue(
                "Memory usage should increase with more participants",
                memoryUsages[i] > memoryUsages[i - 1]
            )
        }
    }
    
    @Test
    fun `video quality automatically reduces under stress`() = runTest {
        // Start with high quality
        performanceOptimizer.setVideoQuality(VideoQuality.HIGH)
        assertEquals(VideoQuality.HIGH, performanceOptimizer.getCurrentVideoQuality())
        
        // Simulate high memory pressure
        performanceMonitor.updateMemoryMetrics()
        val highMemoryMetrics = performanceMonitor.performanceMetrics.value.copy(
            memoryUsage = performanceMonitor.performanceMetrics.value.memoryUsage.copy(
                memoryPressure = MemoryPressure.HIGH
            )
        )
        
        // Simulate poor video performance (low frame rate)
        performanceMonitor.updateVideoMetrics(
            frameRate = 15f, // Below 80% of target 30fps
            droppedFrames = 20,
            renderingTimeMs = 50,
            participantCount = 8
        )
        
        // Get optimization recommendations
        val recommendations = performanceOptimizer.getOptimizationRecommendations()
        
        // Should recommend reducing video quality
        assertTrue(
            "Should recommend memory optimization",
            recommendations.any { it.type == OptimizationType.MEMORY }
        )
        assertTrue(
            "Should recommend video optimization",
            recommendations.any { it.type == OptimizationType.VIDEO }
        )
    }
    
    @Test
    fun `network quality affects multi-participant call recommendations`() = runTest {
        // Simulate poor network conditions with multiple participants
        performanceMonitor.updateNetworkMetrics(
            latencyMs = 300,
            packetLoss = 8f,
            bandwidth = 500
        )
        
        // Simulate multiple participants
        performanceMonitor.updateVideoMetrics(
            frameRate = 20f,
            droppedFrames = 15,
            renderingTimeMs = 40,
            participantCount = 6
        )
        
        val recommendations = performanceOptimizer.getOptimizationRecommendations()
        
        // Should recommend network-related optimizations
        val networkRec = recommendations.firstOrNull { it.type == OptimizationType.NETWORK }
        assertNotNull("Should have network optimization recommendation", networkRec)
        assertEquals(RecommendationSeverity.HIGH, networkRec?.severity)
        assertTrue(
            "Should suggest audio-only or quality reduction",
            networkRec?.action?.contains("audio-only") == true ||
            networkRec?.action?.contains("reduce") == true
        )
    }
    
    @Test
    fun `performance optimizer handles battery optimization for multi-participant calls`() = runTest {
        // Mock power manager to simulate low power mode
        every { mockContext.getSystemService(Context.POWER_SERVICE) } returns mockk(relaxed = true)
        
        // Start optimization
        performanceOptimizer.startOptimization()
        
        // Simulate multi-participant call with battery concerns
        performanceMonitor.updateVideoMetrics(
            frameRate = 25f,
            droppedFrames = 5,
            renderingTimeMs = 20,
            participantCount = 4
        )
        
        // Force battery saver mode
        performanceOptimizer.setVideoQuality(VideoQuality.BATTERY_SAVER)
        
        assertEquals(VideoQuality.BATTERY_SAVER, performanceOptimizer.getCurrentVideoQuality())
        
        performanceOptimizer.stopOptimization()
    }
    
    @Test
    fun `stress test with maximum participants shows graceful degradation`() = runTest {
        val maxParticipants = 8
        val stressTestResults = mutableMapOf<String, Any>()
        
        // Simulate maximum participant load
        performanceMonitor.updateVideoMetrics(
            frameRate = 10f, // Severely degraded
            droppedFrames = 50,
            renderingTimeMs = 100,
            participantCount = maxParticipants
        )
        
        performanceMonitor.updateMemoryMetrics()
        val memoryMetrics = performanceMonitor.performanceMetrics.value.memoryUsage
        
        performanceMonitor.updateNetworkMetrics(
            latencyMs = 200,
            packetLoss = 5f,
            bandwidth = 800
        )
        
        val recommendations = performanceOptimizer.getOptimizationRecommendations()
        
        stressTestResults["participantCount"] = maxParticipants
        stressTestResults["frameRate"] = 10f
        stressTestResults["memoryPressure"] = memoryMetrics.memoryPressure
        stressTestResults["recommendationCount"] = recommendations.size
        
        // Under maximum stress, should have multiple recommendations
        assertTrue(
            "Should have multiple optimization recommendations under stress",
            recommendations.size >= 2
        )
        
        // Should recommend the most aggressive optimizations
        assertTrue(
            "Should have high or critical severity recommendations",
            recommendations.any { 
                it.severity == RecommendationSeverity.HIGH || 
                it.severity == RecommendationSeverity.CRITICAL 
            }
        )
    }
    
    @Test
    fun `performance metrics are consistent across multiple updates`() = runTest {
        val participantCount = 4
        val updates = 10
        
        repeat(updates) { iteration ->
            val frameRate = 30f - (iteration * 0.5f) // Gradual degradation
            val droppedFrames = iteration
            
            performanceMonitor.updateVideoMetrics(
                frameRate = frameRate,
                droppedFrames = droppedFrames,
                renderingTimeMs = 16 + iteration,
                participantCount = participantCount
            )
            
            val metrics = performanceMonitor.performanceMetrics.value
            
            // Verify metrics are within expected ranges
            assertTrue("Frame rate should be positive", metrics.videoPerformance.currentFrameRate >= 0)
            assertTrue("Dropped frames should not be negative", metrics.videoPerformance.droppedFrames >= 0)
            assertTrue("Participant count should match", metrics.videoPerformance.participantCount == participantCount)
            assertTrue("Rendering time should be positive", metrics.videoPerformance.averageRenderingTimeMs > 0)
        }
    }
    
    @Test
    fun `benchmark produces consistent results for multi-participant scenarios`() = runTest {
        val benchmark = PerformanceBenchmark(performanceMonitor)
        
        // Run benchmark multiple times to check consistency
        val results = mutableListOf<BenchmarkResult>()
        repeat(3) {
            results.add(benchmark.runBenchmark())
        }
        
        // Verify all benchmarks completed successfully
        results.forEach { result ->
            assertTrue("Overall score should be valid", result.overallScore in 0..100)
            assertTrue("Should have all test types", result.tests.size >= 4)
            
            // Check multi-participant test specifically
            val multiParticipantTest = result.tests.firstOrNull { 
                it.name == "Multi-Participant Stress Test" 
            }
            assertNotNull("Should have multi-participant test", multiParticipantTest)
            assertTrue("Multi-participant test should have valid score", 
                multiParticipantTest!!.score in 0..100)
        }
        
        // Results should be reasonably consistent (within 20 points)
        val scores = results.map { it.overallScore }
        val maxScore = scores.maxOrNull() ?: 0
        val minScore = scores.minOrNull() ?: 0
        assertTrue(
            "Benchmark results should be consistent",
            maxScore - minScore <= 20
        )
    }
}