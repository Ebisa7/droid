package com.ltquiz.test.performance

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

/**
 * Performance benchmarking and stress testing for video calls.
 */
@Singleton
class PerformanceBenchmark @Inject constructor(
    private val performanceMonitor: PerformanceMonitor
) {
    
    /**
     * Runs a comprehensive performance benchmark.
     */
    suspend fun runBenchmark(): BenchmarkResult = withContext(Dispatchers.Default) {
        val results = mutableListOf<BenchmarkTest>()
        
        // Memory allocation benchmark
        results.add(benchmarkMemoryAllocation())
        
        // Video rendering benchmark
        results.add(benchmarkVideoRendering())
        
        // Network simulation benchmark
        results.add(benchmarkNetworkPerformance())
        
        // Multi-participant stress test
        results.add(benchmarkMultiParticipantCall())
        
        BenchmarkResult(
            tests = results,
            overallScore = calculateOverallScore(results),
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Benchmarks memory allocation and garbage collection performance.
     */
    private suspend fun benchmarkMemoryAllocation(): BenchmarkTest {
        val testName = "Memory Allocation"
        val iterations = 1000
        
        val executionTime = measureTimeMillis {
            repeat(iterations) {
                // Simulate memory allocation patterns similar to video call
                val largeArray = ByteArray(1024 * 1024) // 1MB allocation
                largeArray.fill(0)
                
                if (it % 100 == 0) {
                    System.gc() // Force GC periodically
                    delay(1) // Allow GC to run
                }
            }
        }
        
        performanceMonitor.updateMemoryMetrics()
        val memoryUsage = performanceMonitor.performanceMetrics.value.memoryUsage
        
        return BenchmarkTest(
            name = testName,
            executionTimeMs = executionTime,
            score = calculateMemoryScore(executionTime, memoryUsage),
            details = mapOf(
                "iterations" to iterations.toString(),
                "avgTimePerIteration" to "${executionTime / iterations}ms",
                "memoryPressure" to memoryUsage.memoryPressure.name,
                "usedMemoryMB" to memoryUsage.usedMemoryMB.toString()
            )
        )
    }
    
    /**
     * Benchmarks video rendering performance.
     */
    private suspend fun benchmarkVideoRendering(): BenchmarkTest {
        val testName = "Video Rendering"
        val frameCount = 300 // Simulate 10 seconds at 30fps
        
        val startTime = System.currentTimeMillis()
        val executionTime = measureTimeMillis {
            repeat(frameCount) { frame ->
                // Simulate video frame processing
                simulateFrameProcessing()
                
                // Update video metrics every 30 frames (1 second)
                if (frame % 30 == 0) {
                    val currentFps = 30f * (frame + 1) / ((System.currentTimeMillis() - startTime) / 1000f)
                    performanceMonitor.updateVideoMetrics(
                        frameRate = currentFps,
                        droppedFrames = 0,
                        renderingTimeMs = 16, // Target 16ms per frame for 60fps
                        participantCount = 4
                    )
                }
                
                delay(16) // Simulate 60fps rendering (16ms per frame)
            }
        }
        
        val avgFps = frameCount * 1000f / executionTime
        
        return BenchmarkTest(
            name = testName,
            executionTimeMs = executionTime,
            score = calculateVideoScore(avgFps),
            details = mapOf(
                "frameCount" to frameCount.toString(),
                "averageFPS" to String.format("%.2f", avgFps),
                "targetFPS" to "30",
                "renderingEfficiency" to "${(avgFps / 30f * 100).toInt()}%"
            )
        )
    }
    
    /**
     * Benchmarks network performance simulation.
     */
    private suspend fun benchmarkNetworkPerformance(): BenchmarkTest {
        val testName = "Network Performance"
        val packetCount = 100
        
        val executionTime = measureTimeMillis {
            repeat(packetCount) {
                // Simulate network packet processing
                val latency = simulateNetworkLatency()
                val packetLoss = simulatePacketLoss()
                
                performanceMonitor.updateNetworkMetrics(
                    latencyMs = latency,
                    packetLoss = packetLoss,
                    bandwidth = 1000 // 1Mbps
                )
                
                delay(10) // Simulate packet interval
            }
        }
        
        val networkPerf = performanceMonitor.performanceMetrics.value.networkPerformance
        
        return BenchmarkTest(
            name = testName,
            executionTimeMs = executionTime,
            score = calculateNetworkScore(networkPerf),
            details = mapOf(
                "packetCount" to packetCount.toString(),
                "averageLatency" to "${networkPerf.latencyMs}ms",
                "packetLoss" to "${networkPerf.packetLossPercentage}%",
                "connectionQuality" to networkPerf.connectionQuality.name
            )
        )
    }
    
    /**
     * Stress test for multi-participant calls.
     */
    private suspend fun benchmarkMultiParticipantCall(): BenchmarkTest {
        val testName = "Multi-Participant Stress Test"
        val participantCounts = listOf(2, 4, 6, 8)
        val testResults = mutableMapOf<Int, Long>()
        
        val totalExecutionTime = measureTimeMillis {
            for (participantCount in participantCounts) {
                val testTime = measureTimeMillis {
                    // Simulate multi-participant call load
                    repeat(participantCount) { participant ->
                        CoroutineScope(Dispatchers.Default).launch {
                            // Simulate peer connection for each participant
                            simulatePeerConnection(participant)
                        }
                    }
                    
                    // Run for 5 seconds
                    delay(5000)
                }
                
                testResults[participantCount] = testTime
                
                // Update metrics for this participant count
                performanceMonitor.updateVideoMetrics(
                    frameRate = 30f - (participantCount * 2), // Simulate degradation
                    droppedFrames = participantCount,
                    renderingTimeMs = (16 + (participantCount * 2)).toLong(),
                    participantCount = participantCount
                )
            }
        }
        
        return BenchmarkTest(
            name = testName,
            executionTimeMs = totalExecutionTime,
            score = calculateMultiParticipantScore(testResults),
            details = testResults.mapKeys { "participants_${it.key}" }
                .mapValues { "${it.value}ms" }
        )
    }
    
    /**
     * Simulates video frame processing workload.
     */
    private suspend fun simulateFrameProcessing() {
        // Simulate CPU-intensive video processing
        var sum = 0
        repeat(1000) {
            sum += it * it
        }
    }
    
    /**
     * Simulates network latency.
     */
    private fun simulateNetworkLatency(): Long {
        return (20..100).random().toLong()
    }
    
    /**
     * Simulates packet loss.
     */
    private fun simulatePacketLoss(): Float {
        return kotlin.random.Random.nextFloat() * 2f
    }
    
    /**
     * Simulates peer connection workload.
     */
    private suspend fun simulatePeerConnection(participantId: Int) {
        repeat(100) {
            // Simulate WebRTC processing
            delay(10)
        }
    }
    
    /**
     * Calculates memory benchmark score.
     */
    private fun calculateMemoryScore(executionTime: Long, memoryUsage: MemoryUsage): Int {
        val timeScore = maxOf(0, 100 - (executionTime / 100).toInt())
        val memoryScore = when (memoryUsage.memoryPressure) {
            MemoryPressure.LOW -> 100
            MemoryPressure.MEDIUM -> 70
            MemoryPressure.HIGH -> 30
        }
        return (timeScore + memoryScore) / 2
    }
    
    /**
     * Calculates video rendering score.
     */
    private fun calculateVideoScore(avgFps: Float): Int {
        return minOf(100, (avgFps / 30f * 100).toInt())
    }
    
    /**
     * Calculates network performance score.
     */
    private fun calculateNetworkScore(networkPerf: NetworkPerformance): Int {
        return when (networkPerf.connectionQuality) {
            ConnectionQuality.EXCELLENT -> 100
            ConnectionQuality.GOOD -> 80
            ConnectionQuality.FAIR -> 60
            ConnectionQuality.POOR -> 30
            ConnectionQuality.UNKNOWN -> 50
        }
    }
    
    /**
     * Calculates multi-participant stress test score.
     */
    private fun calculateMultiParticipantScore(results: Map<Int, Long>): Int {
        val avgTime = results.values.average()
        return maxOf(0, 100 - (avgTime / 100).toInt())
    }
    
    /**
     * Calculates overall benchmark score.
     */
    private fun calculateOverallScore(tests: List<BenchmarkTest>): Int {
        return tests.map { it.score }.average().toInt()
    }
}

/**
 * Benchmark test result.
 */
data class BenchmarkTest(
    val name: String,
    val executionTimeMs: Long,
    val score: Int, // 0-100
    val details: Map<String, String>
)

/**
 * Complete benchmark result.
 */
data class BenchmarkResult(
    val tests: List<BenchmarkTest>,
    val overallScore: Int,
    val timestamp: Long
)