# JNI/NDK Native Processing Module

This directory contains the C++ native code for real-time frame processing using OpenCV.

## Architecture

### Core Components

1. **FrameProcessor Class** - Handles OpenCV frame processing operations
2. **JNI Bridge** - Bridges Java and C++ code using JNI

### Features Implemented

- ✅ **Canny Edge Detection**: Real-time edge detection with configurable thresholds
- ✅ **Grayscale Conversion**: Efficient color space conversion
- ✅ **Performance Monitoring**: Processing time and frame count tracking

## Files Structure

- `opencv_processor.cpp` - Main OpenCV processing functions
- `jni_bridge.cpp` - JNI interface for Java ↔ C++ communication
- `frame_utils.h` - Frame manipulation utilities
- `CMakeLists.txt` - CMake build configuration

## Features

- Real-time Canny edge detection
- Grayscale conversion
- Frame buffer management
- Direct OpenGL texture updates

## Build Requirements

- OpenCV Android SDK 4.5+
- NDK 21.0+
- CMake 3.18+