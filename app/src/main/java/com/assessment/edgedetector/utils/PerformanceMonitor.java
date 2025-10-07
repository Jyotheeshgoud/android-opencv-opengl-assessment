package com.assessment.edgedetector.utils;

import android.util.Log;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance monitor for tracking processing pipeline efficiency
 */
public class PerformanceMonitor {
    private static final String TAG = "PerformanceMonitor";
    
    // Performance thresholds
    private static final double MIN_TARGET_FPS = 15.0;
    private static final double OPTIMAL_TARGET_FPS = 30.0;
    private static final double MAX_PROCESSING_TIME_MS = 50.0; // Max 50ms per frame
    
    // Atomic counters for thread safety
    private final AtomicLong totalFramesProcessed = new AtomicLong(0);
    private final AtomicLong totalProcessingTimeMs = new AtomicLong(0);
    private final AtomicLong frameDropCount = new AtomicLong(0);
    
    // Performance tracking
    private volatile long lastLogTime = System.currentTimeMillis();
    private volatile double currentFps = 0.0;
    private volatile boolean performanceWarningShown = false;
    
    // Optimization flags
    private volatile boolean adaptiveQualityEnabled = true;
    private volatile int currentQualityLevel = 3; // 1=low, 2=medium, 3=high, 4=ultra
    
    public static class ProcessingResult {
        public final boolean success;
        public final double processingTimeMs;
        public final int qualityLevel;
        public final String optimizationHint;
        
        public ProcessingResult(boolean success, double processingTimeMs, 
                              int qualityLevel, String optimizationHint) {
            this.success = success;
            this.processingTimeMs = processingTimeMs;
            this.qualityLevel = qualityLevel;
            this.optimizationHint = optimizationHint;
        }
    }
    
    /**
     * Record frame processing metrics
     */
    public ProcessingResult recordFrameProcessing(double processingTimeMs, boolean success) {
        totalFramesProcessed.incrementAndGet();
        
        if (success) {
            totalProcessingTimeMs.addAndGet((long) processingTimeMs);
        } else {
            frameDropCount.incrementAndGet();
        }
        
        // Check if we need to adjust quality for performance
        String optimizationHint = checkPerformanceAndOptimize(processingTimeMs);
        
        // Log performance periodically
        logPerformancePeriodically();
        
        return new ProcessingResult(success, processingTimeMs, currentQualityLevel, optimizationHint);
    }
    
    /**
     * Update current FPS for performance monitoring
     */
    public void updateFPS(double fps) {
        this.currentFps = fps;
        
        // Check if performance is below acceptable threshold
        if (fps < MIN_TARGET_FPS && !performanceWarningShown) {
            Log.w(TAG, "Performance warning: FPS below minimum threshold (" + fps + " < " + MIN_TARGET_FPS + ")");
            performanceWarningShown = true;
        } else if (fps >= MIN_TARGET_FPS && performanceWarningShown) {
            Log.i(TAG, "Performance recovered: FPS back to acceptable level (" + fps + ")");
            performanceWarningShown = false;
        }
    }
    
    /**
     * Check performance and suggest optimizations
     */
    private String checkPerformanceAndOptimize(double processingTimeMs) {
        String hint = null;
        
        if (!adaptiveQualityEnabled) {
            return "Adaptive quality disabled";
        }
        
        // If processing is too slow, reduce quality
        if (processingTimeMs > MAX_PROCESSING_TIME_MS && currentQualityLevel > 1) {
            currentQualityLevel--;
            hint = "Reduced quality to level " + currentQualityLevel + " for better performance";
            Log.i(TAG, hint);
            
        // If processing is fast and FPS is good, increase quality
        } else if (processingTimeMs < MAX_PROCESSING_TIME_MS * 0.5 && 
                   currentFps > OPTIMAL_TARGET_FPS && 
                   currentQualityLevel < 4) {
            currentQualityLevel++;
            hint = "Increased quality to level " + currentQualityLevel + " due to good performance";
            Log.i(TAG, hint);
        }
        
        return hint;
    }
    
    /**
     * Get current quality level for processing pipeline
     */
    public int getCurrentQualityLevel() {
        return currentQualityLevel;
    }
    
    /**
     * Set quality level manually (disables adaptive quality)
     */
    public void setQualityLevel(int level) {
        this.currentQualityLevel = Math.max(1, Math.min(4, level));
        this.adaptiveQualityEnabled = false;
        Log.i(TAG, "Quality manually set to level " + currentQualityLevel);
    }
    
    /**
     * Enable/disable adaptive quality adjustment
     */
    public void setAdaptiveQualityEnabled(boolean enabled) {
        this.adaptiveQualityEnabled = enabled;
        Log.i(TAG, "Adaptive quality " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Get processing parameters for current quality level
     */
    public ProcessingParams getProcessingParams() {
        switch (currentQualityLevel) {
            case 1: // Low quality - fastest processing
                return new ProcessingParams(160, 120, 80.0, 200.0, false);
            case 2: // Medium quality
                return new ProcessingParams(320, 240, 60.0, 160.0, false);
            case 3: // High quality - default
                return new ProcessingParams(640, 480, 50.0, 150.0, true);
            case 4: // Ultra quality - best results
                return new ProcessingParams(1280, 720, 30.0, 100.0, true);
            default:
                return new ProcessingParams(640, 480, 50.0, 150.0, true);
        }
    }
    
    /**
     * Processing parameters for different quality levels
     */
    public static class ProcessingParams {
        public final int width;
        public final int height;
        public final double cannyThreshold1;
        public final double cannyThreshold2;
        public final boolean useGaussianBlur;
        
        public ProcessingParams(int width, int height, double cannyThreshold1, 
                              double cannyThreshold2, boolean useGaussianBlur) {
            this.width = width;
            this.height = height;
            this.cannyThreshold1 = cannyThreshold1;
            this.cannyThreshold2 = cannyThreshold2;
            this.useGaussianBlur = useGaussianBlur;
        }
        
        @Override
        public String toString() {
            return String.format("ProcessingParams{%dx%d, thresholds: %.1f/%.1f, blur: %s}", 
                width, height, cannyThreshold1, cannyThreshold2, useGaussianBlur);
        }
    }
    
    /**
     * Log performance statistics periodically
     */
    private void logPerformancePeriodically() {
        long currentTime = System.currentTimeMillis();
        
        // Log every 5 seconds
        if (currentTime - lastLogTime >= 5000) {
            logPerformanceStats();
            lastLogTime = currentTime;
        }
    }
    
    /**
     * Log current performance statistics
     */
    public void logPerformanceStats() {
        long totalFrames = totalFramesProcessed.get();
        long totalTime = totalProcessingTimeMs.get();
        long droppedFrames = frameDropCount.get();
        
        if (totalFrames > 0) {
            double avgProcessingTime = (double) totalTime / totalFrames;
            double dropRate = (double) droppedFrames / totalFrames * 100;
            
            Log.i(TAG, String.format(
                "Performance Stats - FPS: %.1f, Avg Processing: %.1fms, " +
                "Frames: %d, Dropped: %d (%.1f%%), Quality: %d",
                currentFps, avgProcessingTime, totalFrames, droppedFrames, dropRate, currentQualityLevel
            ));
        }
    }
    
    /**
     * Reset all performance counters
     */
    public void reset() {
        totalFramesProcessed.set(0);
        totalProcessingTimeMs.set(0);
        frameDropCount.set(0);
        currentFps = 0.0;
        performanceWarningShown = false;
        currentQualityLevel = 3; // Reset to high quality
        lastLogTime = System.currentTimeMillis();
        
        Log.i(TAG, "Performance monitor reset");
    }
    
    /**
     * Get comprehensive performance report
     */
    public String getPerformanceReport() {
        long totalFrames = totalFramesProcessed.get();
        long totalTime = totalProcessingTimeMs.get();
        long droppedFrames = frameDropCount.get();
        
        if (totalFrames == 0) {
            return "No performance data available";
        }
        
        double avgProcessingTime = (double) totalTime / totalFrames;
        double dropRate = (double) droppedFrames / totalFrames * 100;
        
        return String.format(
            "Performance Report:\n" +
            "- Current FPS: %.1f\n" +
            "- Average Processing Time: %.1fms\n" +
            "- Total Frames Processed: %d\n" +
            "- Frames Dropped: %d (%.1f%%)\n" +
            "- Current Quality Level: %d\n" +
            "- Adaptive Quality: %s\n" +
            "- Performance Status: %s",
            currentFps, avgProcessingTime, totalFrames, droppedFrames, dropRate,
            currentQualityLevel, adaptiveQualityEnabled ? "Enabled" : "Disabled",
            currentFps >= MIN_TARGET_FPS ? "Good" : "Below Target"
        );
    }
}