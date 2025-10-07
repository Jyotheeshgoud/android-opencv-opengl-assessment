package com.assessment.edgedetector;

import android.graphics.Bitmap;

/**
 * Native library wrapper for OpenCV processing
 */
public class NativeLib {

    // Load the native library
    static {
        System.loadLibrary("edgedetector");
    }

    /**
     * Test method to verify JNI connection
     */
    public native String stringFromJNI();

    /**
     * Initialize the native frame processor
     * @param width Frame width
     * @param height Frame height
     * @return true if initialization successful
     */
    public native boolean initializeProcessor(int width, int height);

    /**
     * Process frame data with Canny edge detection
     * @param inputData Input frame data (RGB format)
     * @param width Frame width
     * @param height Frame height
     * @return Processed frame data (grayscale)
     */
    public native byte[] processFrameCanny(byte[] inputData, int width, int height);

    /**
     * Process frame data with grayscale conversion
     * @param inputData Input frame data (RGB format)
     * @param width Frame width
     * @param height Frame height
     * @return Processed frame data (grayscale)
     */
    public native byte[] processFrameGrayscale(byte[] inputData, int width, int height);

    /**
     * Process bitmap with Canny edge detection (in-place processing)
     * @param inputBitmap Input bitmap (ARGB_8888)
     * @param outputBitmap Output bitmap (grayscale)
     */
    public native void processBitmapCanny(Bitmap inputBitmap, Bitmap outputBitmap);

    /**
     * Get the processing time of the last frame
     * @return Processing time in milliseconds
     */
    public native double getLastProcessingTime();

    /**
     * Get the total number of processed frames
     * @return Number of processed frames
     */
    public native int getProcessedFrameCount();

    /**
     * Cleanup native resources
     */
    public native void cleanup();
}