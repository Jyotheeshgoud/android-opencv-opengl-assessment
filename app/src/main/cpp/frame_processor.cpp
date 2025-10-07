#include "frame_processor.h"
#include <chrono>

#define LOG_TAG "FrameProcessor"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

FrameProcessor::FrameProcessor() 
    : frameWidth(0)
    , frameHeight(0)
    , initialized(false)
    , lastProcessingTime(0.0)
    , processedFrameCount(0) {
    LOGI("FrameProcessor created");
}

FrameProcessor::~FrameProcessor() {
    LOGI("FrameProcessor destroyed");
}

bool FrameProcessor::initialize(int width, int height) {
    if (width <= 0 || height <= 0) {
        LOGE("Invalid dimensions: %dx%d", width, height);
        return false;
    }
    
    frameWidth = width;
    frameHeight = height;
    
    // Pre-allocate working matrices to avoid runtime allocations
    try {
        workingMat1 = cv::Mat(height, width, CV_8UC3);
        workingMat2 = cv::Mat(height, width, CV_8UC1);
        grayMat = cv::Mat(height, width, CV_8UC1);
        initialized = true;
        
        LOGI("FrameProcessor initialized for %dx%d frames", width, height);
        return true;
    } catch (const cv::Exception& e) {
        LOGE("OpenCV initialization error: %s", e.what());
        return false;
    }
}

bool FrameProcessor::processFrameCanny(const cv::Mat& input, cv::Mat& output, double threshold1, double threshold2) {
    if (!initialized || input.empty()) {
        LOGE("Processor not initialized or empty input frame");
        return false;
    }
    
    int64_t startTime = getTimeMs();
    
    try {
        // Convert to grayscale if needed
        cv::Mat grayFrame;
        if (input.channels() == 3) {
            cv::cvtColor(input, grayFrame, cv::COLOR_RGB2GRAY);
        } else if (input.channels() == 4) {
            cv::cvtColor(input, grayFrame, cv::COLOR_RGBA2GRAY);
        } else {
            grayFrame = input;
        }
        
        // Apply Gaussian blur to reduce noise
        cv::GaussianBlur(grayFrame, workingMat2, cv::Size(5, 5), 1.4);
        
        // Apply Canny edge detection
        cv::Canny(workingMat2, output, threshold1, threshold2);
        
        // Update statistics
        processedFrameCount++;
        lastProcessingTime = getTimeMs() - startTime;
        
        return true;
        
    } catch (const cv::Exception& e) {
        LOGE("Canny processing error: %s", e.what());
        return false;
    }
}

bool FrameProcessor::processFrameGrayscale(const cv::Mat& input, cv::Mat& output) {
    if (!initialized || input.empty()) {
        LOGE("Processor not initialized or empty input frame");
        return false;
    }
    
    int64_t startTime = getTimeMs();
    
    try {
        if (input.channels() == 3) {
            cv::cvtColor(input, output, cv::COLOR_RGB2GRAY);
        } else if (input.channels() == 4) {
            cv::cvtColor(input, output, cv::COLOR_RGBA2GRAY);
        } else {
            input.copyTo(output);
        }
        
        // Update statistics
        processedFrameCount++;
        lastProcessingTime = getTimeMs() - startTime;
        
        return true;
        
    } catch (const cv::Exception& e) {
        LOGE("Grayscale processing error: %s", e.what());
        return false;
    }
}

bool FrameProcessor::convertYUV420ToRGB(const uint8_t* yuvData, cv::Mat& rgbOutput, int width, int height) {
    if (!yuvData) {
        LOGE("Invalid YUV data pointer");
        return false;
    }
    
    try {
        // Create Mat from YUV420 data
        cv::Mat yuvMat(height + height/2, width, CV_8UC1, (void*)yuvData);
        
        // Convert YUV420 to RGB
        cv::cvtColor(yuvMat, rgbOutput, cv::COLOR_YUV420p2RGB);
        
        return true;
        
    } catch (const cv::Exception& e) {
        LOGE("YUV to RGB conversion error: %s", e.what());
        return false;
    }
}

bool FrameProcessor::convertRGBToGray(const cv::Mat& input, cv::Mat& output) {
    if (input.empty()) {
        LOGE("Empty input for RGB to Gray conversion");
        return false;
    }
    
    try {
        cv::cvtColor(input, output, cv::COLOR_RGB2GRAY);
        return true;
    } catch (const cv::Exception& e) {
        LOGE("RGB to Gray conversion error: %s", e.what());
        return false;
    }
}

int64_t FrameProcessor::getTimeMs() {
    auto now = std::chrono::high_resolution_clock::now();
    auto duration = now.time_since_epoch();
    return std::chrono::duration_cast<std::chrono::milliseconds>(duration).count();
}