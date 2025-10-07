#include <jni.h>
#include <string>
#include <android/bitmap.h>
#include <android/log.h>
#include "frame_processor.h"

#define LOG_TAG "EdgeDetectorJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global frame processor instance
static FrameProcessor* g_frameProcessor = nullptr;

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_assessment_edgedetector_NativeLib_stringFromJNI(JNIEnv *env, jobject thiz) {
    std::string hello = "Hello from C++ EdgeDetector";
    return env->NewStringUTF(hello.c_str());
}

JNIEXPORT jboolean JNICALL
Java_com_assessment_edgedetector_NativeLib_initializeProcessor(JNIEnv *env, jobject thiz,
                                                               jint width, jint height) {
    LOGI("Initializing frame processor for %dx%d", width, height);
    
    if (g_frameProcessor != nullptr) {
        delete g_frameProcessor;
    }
    
    g_frameProcessor = new FrameProcessor();
    
    if (g_frameProcessor->initialize(width, height)) {
        LOGI("Frame processor initialized successfully");
        return JNI_TRUE;
    } else {
        LOGE("Failed to initialize frame processor");
        delete g_frameProcessor;
        g_frameProcessor = nullptr;
        return JNI_FALSE;
    }
}

JNIEXPORT jbyteArray JNICALL
Java_com_assessment_edgedetector_NativeLib_processFrameCanny(JNIEnv *env, jobject thiz,
                                                             jbyteArray inputData, jint width, jint height) {
    if (g_frameProcessor == nullptr) {
        LOGE("Frame processor not initialized");
        return nullptr;
    }
    
    // Get input data
    jbyte* inputBytes = env->GetByteArrayElements(inputData, nullptr);
    if (inputBytes == nullptr) {
        LOGE("Failed to get input byte array");
        return nullptr;
    }
    
    jsize inputLength = env->GetArrayLength(inputData);
    
    try {
        // Create OpenCV Mat from input data (assuming RGB format)
        cv::Mat inputMat(height, width, CV_8UC3, inputBytes);
        cv::Mat outputMat;
        
        // Process with Canny edge detection
        if (g_frameProcessor->processFrameCanny(inputMat, outputMat)) {
            // Create result byte array
            jsize outputLength = outputMat.total() * outputMat.elemSize();
            jbyteArray result = env->NewByteArray(outputLength);
            
            if (result != nullptr) {
                env->SetByteArrayRegion(result, 0, outputLength, 
                                       reinterpret_cast<const jbyte*>(outputMat.data));
            }
            
            env->ReleaseByteArrayElements(inputData, inputBytes, JNI_ABORT);
            return result;
        } else {
            LOGE("Canny processing failed");
            env->ReleaseByteArrayElements(inputData, inputBytes, JNI_ABORT);
            return nullptr;
        }
        
    } catch (const std::exception& e) {
        LOGE("Exception in processFrameCanny: %s", e.what());
        env->ReleaseByteArrayElements(inputData, inputBytes, JNI_ABORT);
        return nullptr;
    }
}

JNIEXPORT jbyteArray JNICALL
Java_com_assessment_edgedetector_NativeLib_processFrameGrayscale(JNIEnv *env, jobject thiz,
                                                                 jbyteArray inputData, jint width, jint height) {
    if (g_frameProcessor == nullptr) {
        LOGE("Frame processor not initialized");
        return nullptr;
    }
    
    // Get input data
    jbyte* inputBytes = env->GetByteArrayElements(inputData, nullptr);
    if (inputBytes == nullptr) {
        LOGE("Failed to get input byte array");
        return nullptr;
    }
    
    try {
        // Create OpenCV Mat from input data
        cv::Mat inputMat(height, width, CV_8UC3, inputBytes);
        cv::Mat outputMat;
        
        // Process with grayscale conversion
        if (g_frameProcessor->processFrameGrayscale(inputMat, outputMat)) {
            // Create result byte array
            jsize outputLength = outputMat.total() * outputMat.elemSize();
            jbyteArray result = env->NewByteArray(outputLength);
            
            if (result != nullptr) {
                env->SetByteArrayRegion(result, 0, outputLength, 
                                       reinterpret_cast<const jbyte*>(outputMat.data));
            }
            
            env->ReleaseByteArrayElements(inputData, inputBytes, JNI_ABORT);
            return result;
        } else {
            LOGE("Grayscale processing failed");
            env->ReleaseByteArrayElements(inputData, inputBytes, JNI_ABORT);
            return nullptr;
        }
        
    } catch (const std::exception& e) {
        LOGE("Exception in processFrameGrayscale: %s", e.what());
        env->ReleaseByteArrayElements(inputData, inputBytes, JNI_ABORT);
        return nullptr;
    }
}

JNIEXPORT void JNICALL
Java_com_assessment_edgedetector_NativeLib_processBitmapCanny(JNIEnv *env, jobject thiz,
                                                              jobject inputBitmap, jobject outputBitmap) {
    if (g_frameProcessor == nullptr) {
        LOGE("Frame processor not initialized");
        return;
    }
    
    AndroidBitmapInfo inputInfo, outputInfo;
    void* inputPixels = nullptr;
    void* outputPixels = nullptr;
    
    try {
        // Get input bitmap info and pixels
        if (AndroidBitmap_getInfo(env, inputBitmap, &inputInfo) < 0) {
            LOGE("Failed to get input bitmap info");
            return;
        }
        
        if (AndroidBitmap_lockPixels(env, inputBitmap, &inputPixels) < 0) {
            LOGE("Failed to lock input bitmap pixels");
            return;
        }
        
        // Get output bitmap info and pixels
        if (AndroidBitmap_getInfo(env, outputBitmap, &outputInfo) < 0) {
            LOGE("Failed to get output bitmap info");
            AndroidBitmap_unlockPixels(env, inputBitmap);
            return;
        }
        
        if (AndroidBitmap_lockPixels(env, outputBitmap, &outputPixels) < 0) {
            LOGE("Failed to lock output bitmap pixels");
            AndroidBitmap_unlockPixels(env, inputBitmap);
            return;
        }
        
        // Create OpenCV Mats
        cv::Mat inputMat(inputInfo.height, inputInfo.width, CV_8UC4, inputPixels);
        cv::Mat outputMat(outputInfo.height, outputInfo.width, CV_8UC1, outputPixels);
        
        // Process with Canny edge detection
        g_frameProcessor->processFrameCanny(inputMat, outputMat);
        
        // Unlock bitmaps
        AndroidBitmap_unlockPixels(env, inputBitmap);
        AndroidBitmap_unlockPixels(env, outputBitmap);
        
    } catch (const std::exception& e) {
        LOGE("Exception in processBitmapCanny: %s", e.what());
        if (inputPixels) AndroidBitmap_unlockPixels(env, inputBitmap);
        if (outputPixels) AndroidBitmap_unlockPixels(env, outputBitmap);
    }
}

JNIEXPORT jdouble JNICALL
Java_com_assessment_edgedetector_NativeLib_getLastProcessingTime(JNIEnv *env, jobject thiz) {
    if (g_frameProcessor == nullptr) {
        return 0.0;
    }
    return g_frameProcessor->getLastProcessingTime();
}

JNIEXPORT jint JNICALL
Java_com_assessment_edgedetector_NativeLib_getProcessedFrameCount(JNIEnv *env, jobject thiz) {
    if (g_frameProcessor == nullptr) {
        return 0;
    }
    return g_frameProcessor->getProcessedFrameCount();
}

JNIEXPORT void JNICALL
Java_com_assessment_edgedetector_NativeLib_cleanup(JNIEnv *env, jobject thiz) {
    LOGI("Cleaning up frame processor");
    if (g_frameProcessor != nullptr) {
        delete g_frameProcessor;
        g_frameProcessor = nullptr;
    }
}

} // extern "C"