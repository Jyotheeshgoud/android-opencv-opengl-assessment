package com.assessment.edgedetector.utils;

import android.os.Handler;
import android.os.Looper;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * High-performance FPS counter with rolling average calculation
 */
public class FPSCounter {
    private static final String TAG = "FPSCounter";
    
    private static final int DEFAULT_SAMPLE_SIZE = 30;
    private static final int UPDATE_INTERVAL_MS = 100; // Update every 100ms
    
    private final Queue<Long> frameTimestamps;
    private final int maxSampleSize;
    private final Handler mainHandler;
    
    private double currentFps = 0.0;
    private long frameCount = 0;
    private long startTime;
    private FPSUpdateCallback callback;
    
    public interface FPSUpdateCallback {
        void onFPSUpdate(double fps, long frameCount, double avgProcessingTime);
    }
    
    public FPSCounter() {
        this(DEFAULT_SAMPLE_SIZE);
    }
    
    public FPSCounter(int sampleSize) {
        this.maxSampleSize = sampleSize;
        this.frameTimestamps = new ArrayDeque<>(maxSampleSize);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.startTime = System.currentTimeMillis();
    }
    
    /**
     * Record a new frame timestamp
     */
    public synchronized void recordFrame() {
        long currentTime = System.currentTimeMillis();
        frameCount++;
        
        // Add current timestamp
        frameTimestamps.offer(currentTime);
        
        // Remove old timestamps if we exceed max sample size
        if (frameTimestamps.size() > maxSampleSize) {
            frameTimestamps.poll();
        }
        
        // Calculate FPS if we have enough samples
        if (frameTimestamps.size() >= 2) {
            calculateFPS();
        }
    }
    
    /**
     * Record frame with processing time
     */
    public synchronized void recordFrame(double processingTimeMs) {
        recordFrame();
        // Processing time can be used for additional statistics if needed
    }
    
    /**
     * Calculate current FPS based on recent frames
     */
    private void calculateFPS() {
        if (frameTimestamps.size() < 2) {
            return;
        }
        
        Long[] timestamps = frameTimestamps.toArray(new Long[0]);
        long oldestTime = timestamps[0];
        long newestTime = timestamps[timestamps.length - 1];
        
        double timeDiffSeconds = (newestTime - oldestTime) / 1000.0;
        
        if (timeDiffSeconds > 0) {
            currentFps = (timestamps.length - 1) / timeDiffSeconds;
            
            // Notify callback on main thread
            if (callback != null) {
                mainHandler.post(() -> callback.onFPSUpdate(currentFps, frameCount, 0.0));
            }
        }
    }
    
    /**
     * Get current FPS
     */
    public synchronized double getCurrentFPS() {
        return currentFps;
    }
    
    /**
     * Get total frame count
     */
    public synchronized long getFrameCount() {
        return frameCount;
    }
    
    /**
     * Get average FPS since start
     */
    public synchronized double getAverageFPS() {
        long currentTime = System.currentTimeMillis();
        double totalTimeSeconds = (currentTime - startTime) / 1000.0;
        
        if (totalTimeSeconds > 0) {
            return frameCount / totalTimeSeconds;
        }
        return 0.0;
    }
    
    /**
     * Set callback for FPS updates
     */
    public void setCallback(FPSUpdateCallback callback) {
        this.callback = callback;
    }
    
    /**
     * Reset all counters
     */
    public synchronized void reset() {
        frameTimestamps.clear();
        frameCount = 0;
        currentFps = 0.0;
        startTime = System.currentTimeMillis();
    }
    
    /**
     * Get performance statistics
     */
    public synchronized PerformanceStats getStats() {
        return new PerformanceStats(
            currentFps,
            getAverageFPS(),
            frameCount,
            frameTimestamps.size(),
            System.currentTimeMillis() - startTime
        );
    }
    
    /**
     * Performance statistics data class
     */
    public static class PerformanceStats {
        public final double currentFps;
        public final double averageFps;
        public final long totalFrames;
        public final int sampleSize;
        public final long uptimeMs;
        
        public PerformanceStats(double currentFps, double averageFps, long totalFrames, 
                              int sampleSize, long uptimeMs) {
            this.currentFps = currentFps;
            this.averageFps = averageFps;
            this.totalFrames = totalFrames;
            this.sampleSize = sampleSize;
            this.uptimeMs = uptimeMs;
        }
        
        @Override
        public String toString() {
            return String.format("FPS: %.1f (avg: %.1f), Frames: %d, Uptime: %.1fs", 
                currentFps, averageFps, totalFrames, uptimeMs / 1000.0);
        }
    }
}