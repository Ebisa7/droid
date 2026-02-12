package com.ltquiz.test.di

import android.content.Context
import com.ltquiz.test.managers.MediaManager
import com.ltquiz.test.managers.MediaManagerImpl
import com.ltquiz.test.performance.PerformanceBenchmark
import com.ltquiz.test.performance.PerformanceMonitor
import com.ltquiz.test.performance.PerformanceOptimizer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for performance-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PerformanceModule {
    
    @Binds
    @Singleton
    abstract fun bindMediaManager(
        mediaManagerImpl: MediaManagerImpl
    ): MediaManager
    
    companion object {
        @Provides
        @Singleton
        fun providePerformanceMonitor(
            @ApplicationContext context: Context
        ): PerformanceMonitor {
            return PerformanceMonitor(context)
        }
        
        @Provides
        @Singleton
        fun providePerformanceOptimizer(
            @ApplicationContext context: Context,
            performanceMonitor: PerformanceMonitor
        ): PerformanceOptimizer {
            return PerformanceOptimizer(context, performanceMonitor)
        }
        
        @Provides
        @Singleton
        fun providePerformanceBenchmark(
            performanceMonitor: PerformanceMonitor
        ): PerformanceBenchmark {
            return PerformanceBenchmark(performanceMonitor)
        }
    }
}