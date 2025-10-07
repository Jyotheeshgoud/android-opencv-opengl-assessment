#ifndef FRAME_PROCESSOR_H
#define FRAME_PROCESSOR_H

#include <opencv2/opencv.hpp>
#include <android/log.h>

class FrameProcessor {
public:
    FrameProcessor();
    ~FrameProcessor();
    
    // Initialize the processor with frame dimensions
    bool initialize(int width, int height);
    
    // Process frame with Canny edge detection
    bool processFrameCanny(const cv::Mat& input, cv::Mat& output, double threshold1 = 50.0, double threshold2 = 150.0);
    
    // Process frame with grayscale conversion
    bool processFrameGrayscale(const cv::Mat& input, cv::Mat& output);
    
    // Convert YUV420 to RGB (for camera frames)
    bool convertYUV420ToRGB(const uint8_t* yuvData, cv::Mat& rgbOutput, int width, int height);
    
    // Convert RGB to grayscale
    bool convertRGBToGray(const cv::Mat& input, cv::Mat& output);
    
    // Get processing statistics
    double getLastProcessingTime() const { return lastProcessingTime; }
    int getProcessedFrameCount() const { return processedFrameCount; }
    
private:
    int frameWidth;
    int frameHeight;
    bool initialized;
    
    // Processing statistics
    double lastProcessingTime;
    int processedFrameCount;
    
    // Working matrices to avoid repeated allocations
    cv::Mat workingMat1;
    cv::Mat workingMat2;
    cv::Mat grayMat;
    
    // Performance timing
    int64_t getTimeMs();
};

#endif // FRAME_PROCESSOR_H