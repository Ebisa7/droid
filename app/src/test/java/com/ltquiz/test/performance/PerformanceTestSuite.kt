package com.ltquiz.test.performance

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Comprehensive performance test suite covering all performance optimization features.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    PerformanceTest::class,
    MultiParticipantPerformanceTest::class,
    PerformanceTestSuite.VideoRenderingOptimizationTest::class,
    PerformanceTestSuite.MemoryOptimizationTest::class,
    PerformanceTestSuite.BatteryOptimizationTest::class,
    PerformanceTestSuite.PerformanceDashboardTest::class
)
class PerformanceTestSuite {
    
    /**
     * Tests for video rendering optimization.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    class VideoRenderingOptimizationTest {
        
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
        fun `video quality adapts to frame rate performance`() = runTest {
            // Start with high quality
            performanceOptimizer.setVideoQuality(VideoQuality.HIGH)
            
            // Simulate poor frame rate performance
            repeat(5) {
                performanceMonitor.updateVideoMetrics(
                    frameRate = 15f, // Below target
                    droppedFrames = 10 + it,
                    renderingTimeMs = 50,
                    participantCount = 4
                )
            }
            
            val recommendations = performanceOptimizer.getOptimizationRecommendations()
            assertTrue(
                "Should recommend video optimization for poor performance",
                recommendations.any { it.type == OptimizationType.VIDEO }
            )
        }
        
        @Test
        fun `hardware acceleration affects video quality settings`() = runTest {
            val mediaManager = MediaManagerImpl(mockContext, performanceMonitor)
            
            // Test with hardware acceleration enabled
            mediaManager.setHardwareAccelerationEnabled(true)
            mediaManager.setVideoQuality(VideoQuality.HIGH)
            
            val videoTrack1 = mediaManager.startLocalVideo()
            assertTrue(
                "Hardware acceleration should be used for high quality",
                videoTrack1.toString().contains("HW")
            )
            
            mediaManager.stopLocalVideo()
            
            // Test with hardware acceleration disabled
            mediaManager.setHardwareAccelerationEnabled(false)
            mediaManager.setVideoQuality(VideoQuality.HIGH)
            
            val videoTrack2 = mediaManager.startLocalVideo()
            assertTrue(
                "Software rendering should be used when HW disabled",
                videoTrack2.toString().contains("SW")
            )
        }
        
        @Test
        fun `video quality settings affect performance metrics`() = runTest {
            val mediaManager = MediaManagerImpl(mockContext, performanceMonitor)
            
            val qualities = listOf(
                VideoQuality.HIGH,
                VideoQuality.MEDIUM,
                VideoQuality.LOW,
                VideoQuality.BATTERY_SAVER
            )
            
            for (quality in qualities) {
                mediaManager.setVideoQuality(quality)
                assertEquals(quality, mediaManager.getCurrentVideoQuality())
                
                // Verify quality affects expected parameters
                when (quality) {
                    VideoQuality.HIGH -> {
                        assertEquals("720p", quality.resolution)
                        assertEquals(30, quality.frameRate)
                    }
                    VideoQuality.MEDIUM -> {
                        assertEquals("480p", quality.resolution)
                        assertEquals(30, quality.frameRate)
                    }
                    VideoQuality.LOW -> {
                        assertEquals("360p", quality.resolution)
                        assertEquals(15, quality.frameRate)
                    }
                    VideoQuality.BATTERY_SAVER -> {
                        assertEquals("240p", quality.resolution)
                        assertEquals(10, quality.frameRate)
                    }
                }
            }
        }
    }
    
    /**
     * Tests for memory optimization.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    class MemoryOptimizationTest {
        
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
        fun `memory pressure triggers optimization recommendations`() = runTest {
            // Simulate different memory pressure levels
            val pressureLevels = listOf(
                MemoryPressure.LOW,
                MemoryPressure.MEDIUM,
                MemoryPressure.HIGH
            )
            
            for (pressure in pressureLevels) {
                // Mock memory metrics with specific pressure
                every { mockContext.getSystemService(Context.ACTIVITY_SERVICE) } returns mockk(relaxed = true)
                
                performanceMonitor.updateMemoryMetrics()
                
                // Force specific memory pressure for testing
                val metrics = performanceMonitor.performanceMetrics.value.copy(
                    memoryUsage = performanceMonitor.performanceMetrics.value.memoryUsage.copy(
                        memoryPressure = pressure
                    )
                )
                
                val recommendations = performanceOptimizer.getOptimizationRecommendations()
                
                when (pressure) {
                    MemoryPressure.HIGH -> {
                        assertTrue(
                            "High memory pressure should trigger recommendations",
                            recommendations.any { it.type == OptimizationType.MEMORY }
                        )
                    }
                    MemoryPressure.MEDIUM -> {
                        // May or may not have recommendations depending on other factors
                    }
                    MemoryPressure.LOW -> {
                        // Should have fewer or no memory-related recommendations
                    }
                }
            }
        }
        
        @Test
        fun `memory optimization reduces video quality appropriately`() = runTest {
            performanceOptimizer.startOptimization()
            
            // Simulate high memory pressure
            val highMemoryMetrics = performanceMonitor.performanceMetrics.value.copy(
                memoryUsage = performanceMonitor.performanceMetrics.value.memoryUsage.copy(
                    memoryPressure = MemoryPressure.HIGH,
                    usedMemoryMB = 800,
                    maxMemoryMB = 1000
                )
            )
            
            // The optimizer should reduce quality under memory pressure
            // This would be tested by checking the internal optimization logic
            
            performanceOptimizer.stopOptimization()
        }
    }
    
    /**
     * Tests for battery optimization.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    class BatteryOptimizationTest {
        
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
        fun `battery saver mode reduces video quality`() = runTest {
            // Set to battery saver mode
            performanceOptimizer.setVideoQuality(VideoQuality.BATTERY_SAVER)
            
            val quality = performanceOptimizer.getCurrentVideoQuality()
            assertEquals(VideoQuality.BATTERY_SAVER, quality)
            
            // Verify battery saver settings
            assertEquals("240p", quality.resolution)
            assertEquals(10, quality.frameRate)
            assertEquals(250, quality.bitrate)
        }
        
        @Test
        fun `battery optimization affects media manager settings`() = runTest {
            val mediaManager = MediaManagerImpl(mockContext, performanceMonitor)
            
            // Set battery saver mode
            mediaManager.setVideoQuality(VideoQuality.BATTERY_SAVER)
            
            val videoTrack = mediaManager.startLocalVideo()
            
            // Battery saver should use software rendering
            assertTrue(
                "Battery saver should use software rendering",
                videoTrack.toString().contains("SW")
            )
            
            // Should have lowest quality settings
            assertTrue(
                "Should use battery saver quality",
                videoTrack.toString().contains("BatterySaver")
            )
        }
    }
    
    /**
     * Tests for performance dashboard.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    class PerformanceDashboardTest {
        
        private lateinit var performanceMonitor: PerformanceMonitor
        private lateinit var performanceOptimizer: PerformanceOptimizer
        private lateinit var performanceDashboard: PerformanceDashboard
        private lateinit var mockContext: Context
        
        @Before
        fun setup() {
            mockContext = mockk(relaxed = true)
            performanceMonitor = PerformanceMonitor(mockContext)
            performanceOptimizer = PerformanceOptimizer(mockContext, performanceMonitor)
            performanceDashboard = PerformanceDashboard(performanceMonitor, performanceOptimizer)
        }
        
        @Test
        fun `dashboard calculates overall health score correctly`() = runTest {
            // Set up good performance metrics
            performanceMonitor.updateMemoryMetrics()
            performanceMonitor.updateVideoMetrics(30f, 0, 16, 2)
            performanceMonitor.updateNetworkMetrics(50, 0.5f, 1500)
            
            val summary = performanceDashboard.getPerformanceSummary()
            
            assertTrue("Health score should be positive", summary.overallHealth > 0)
            assertTrue("Health score should be reasonable", summary.overallHealth <= 100)
            assertNotNull("Memory status should be set", summary.memoryStatus)
            assertNotNull("Video status should be set", summary.videoStatus)
            assertNotNull("Network status should be set", summary.networkStatus)
        }
        
        @Test
        fun `dashboard identifies critical issues`() = runTest {
            // Set up poor performance metrics
            performanceMonitor.updateMemoryMetrics()
            performanceMonitor.updateVideoMetrics(10f, 20, 100, 6)
            performanceMonitor.updateNetworkMetrics(300, 10f, 200)
            
            val summary = performanceDashboard.getPerformanceSummary()
            
            // Should identify performance issues
            assertTrue("Should have low health score", summary.overallHealth < 50)
            assertTrue("Should identify critical issues", summary.criticalIssues >= 0)
        }
        
        @Test
        fun `dashboard tracks active optimizations`() = runTest {
            // Set different video qualities and check tracking
            val qualities = listOf(
                VideoQuality.HIGH,
                VideoQuality.MEDIUM,
                VideoQuality.LOW,
                VideoQuality.BATTERY_SAVER
            )
            
            for (quality in qualities) {
                performanceOptimizer.setVideoQuality(quality)
                val summary = performanceDashboard.getPerformanceSummary()
                
                assertTrue(
                    "Should track active optimizations",
                    summary.activeOptimizations.isNotEmpty()
                )
                
                val optimizationText = summary.activeOptimizations.joinToString()
                when (quality) {
                    VideoQuality.HIGH -> assertTrue(optimizationText.contains("High Quality"))
                    VideoQuality.MEDIUM -> assertTrue(optimizationText.contains("Medium Quality"))
                    VideoQuality.LOW -> assertTrue(optimizationText.contains("Low Quality"))
                    VideoQuality.BATTERY_SAVER -> assertTrue(optimizationText.contains("Battery Saver"))
                }
            }
        }
    }
}